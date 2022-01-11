package com.myodov.unicherrygarden

import java.util.UUID

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.types.SystemSyncStatus
import com.myodov.unicherrygarden.cherrypicker.EthereumStatePoller
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.cherrypicker.syncers.{HeadSyncer, SyncerMessages, TailSyncer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.AddTrackedAddressesRequestResult
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResult
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.TrackedAddressesRequestResult
import com.myodov.unicherrygarden.messages.cherrypicker.{AddTrackedAddresses, GetBalances, GetTrackedAddresses, GetTransfers}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.control.NonFatal

/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB.
 *
 * It has two primary sub-systems/sub-behaviors: [[HeadSyncer]] and [[TailSyncer]],
 * which both work independently but assume the other counterpart does its job too.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 * @param maxReorg maximum lenmaxReorggth of reorganization in Ethereum blockchain that we support and allow.
 */
private class CherryPicker(protected[this] val dbStorage: DBStorageAPI,
                           protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                           protected[this] val state: CherryPicker.State = CherryPicker.State(),
                           maxReorg: Int,
                           headSyncerBatchSize: Int,
                           tailSyncerBatchSize: Int,
                           catchUpBrakeMaxLeadSetting: Int
                          )
  extends LazyLogging {
  assert(maxReorg >= 1, maxReorg)
  assert(headSyncerBatchSize >= 1, headSyncerBatchSize)
  assert(tailSyncerBatchSize >= 1, tailSyncerBatchSize)
  assert(catchUpBrakeMaxLeadSetting >= 1 && catchUpBrakeMaxLeadSetting >= Math.max(headSyncerBatchSize, tailSyncerBatchSize),
    (catchUpBrakeMaxLeadSetting, headSyncerBatchSize, tailSyncerBatchSize))

  import CherryPicker._

  /** First state: when just launching the CherryPicker after shutdown/restart. */
  private def launch(): Behavior[CherryPickerRequest] = {
    Behaviors.setup { context =>
      logger.info(s"Launching CherryPicker: v. $propVersionStr, built at $propBuildTimestampStr")

      logger.debug("CherryPicker: Launching HeadSyncer...")
      val headSyncer: ActorRef[SyncerMessages.HeadSyncerMessage] = context.spawn(
        HeadSyncer(dbStorage, ethereumConnector, maxReorg)(headSyncerBatchSize, catchUpBrakeMaxLeadSetting),
        "HeadSyncer")
      logger.debug("CherryPicker: Launching TailSyncer...")
      val tailSyncer: ActorRef[SyncerMessages.TailSyncerMessage] = context.spawn(
        TailSyncer(dbStorage, ethereumConnector, maxReorg)(tailSyncerBatchSize, headSyncer),
        "TailSyncer")
      val ethereumStatePoller = context.spawn(
        EthereumStatePoller(ethereumConnector, Seq(headSyncer, tailSyncer, context.self)),
        "EthereumStatePoller")
      logger.debug("CherryPicker: launched sub-syncers!")

      // Register all service keys
      List(
        GetTrackedAddresses.SERVICE_KEY,
        AddTrackedAddresses.SERVICE_KEY,
        GetBalances.SERVICE_KEY,
        GetTransfers.SERVICE_KEY
      ).foreach(context.system.receptionist ! Receptionist.Register(_, context.self))

      // On an `EthereumNodeStatus`, we just write its data into the state;
      // On any incoming Request, we spawn a new child actor which will handle it (for better concurrency).
      Behaviors.receiveMessage {
        case message@EthereumNodeStatus(status) =>
          logger.debug(s"CherryPicker received Ethereum node syncing status: $message")
          state.ethereumNodeStatus = Some(status)
          Behaviors.same
        case message: GetTrackedAddresses.Request => {
          val msgName = "GetTrackedAddresses"
          context.spawn(
            requestHandlerActor[GetTrackedAddresses.Response](
              msgName,
              message.replyTo,
              GetTrackedAddresses.Response.failed) { () => handleGetTrackedAddresses(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: AddTrackedAddresses.Request => {
          val msgName = "AddTrackedAddresses"
          context.spawn(
            requestHandlerActor[AddTrackedAddresses.Response](
              msgName,
              message.replyTo,
              AddTrackedAddresses.Response.failed) { () => handleAddTrackedAddresses(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetBalances.Request => {
          val msgName = "GetBalances"
          context.spawn(
            requestHandlerActor[GetBalances.Response](
              msgName,
              message.replyTo,
              GetBalances.Response.failed) { () => handleGetBalances(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetTransfers.Request => {
          val msgName = "GetTransfers"
          context.spawn(
            requestHandlerActor[GetTransfers.Response](
              msgName,
              message.replyTo,
              GetTransfers.Response.failed) { () => handleGetTransfers(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case unknownMessage => {
          logger.error(s"Unexpected message $unknownMessage")
          Behaviors.unhandled
        }
      }
    }
  }

  private[this] def handleGetTrackedAddresses(payload: GetTrackedAddresses.GTARequestPayload): GetTrackedAddresses.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      val results: List[TrackedAddressesRequestResult.TrackedAddressInformation] = dbStorage
        .trackedAddresses
        .getTrackedAddresses(
          payload.includeComment,
          payload.includeSyncedFrom)
        .map { item =>
          new TrackedAddressesRequestResult.TrackedAddressInformation(
            item.address,
            // The subsequent items may be Java-nullable
            item.comment.orNull,
            // Converting the Option[Int] to nullable Java Integers needs some cunning processing,
            // to avoid Null getting converted to 0
            item.syncedFrom.map(Integer.valueOf).orNull
          )
        }

      new GetTrackedAddresses.Response(
        new GetTrackedAddresses.TrackedAddressesRequestResult(
          results.asJava,
          payload.includeComment,
          payload.includeSyncedFrom
        )
      )
    }

  private[this] def handleAddTrackedAddresses(payload: AddTrackedAddresses.ATARequestPayload): AddTrackedAddresses.Response =
  // Construct all the response DB in a single atomic read-write DB transaction.
    DB localTx { implicit session =>
      // Here goes the list of all added addresses
      // (as Options; with `None` instead of any address that failed to add)
      val addressesMaybeAdded: List[Option[String]] = (
        for (addr: AddTrackedAddresses.AddressDataToTrack <- payload.addressesToTrack.asScala.toList)
          yield {
            if (dbStorage.trackedAddresses.addTrackedAddress(
              addr.address,
              Option(addr.comment), // nullable
              payload.trackingMode,
              // The next line needs cunning processing of java.lang.Integer using .map(_.toInt),
              // as otherwise Option(null:Integer): Option[Int]
              // will be evaluated as Some(0)
              Option(payload.fromBlock).map(_.toInt) // nullable
            )) {
              Some(addr.address)
            } else {
              None
            }
          }
        )

      // Flatten it to just the added elements
      val addressesActuallyAdded: Set[String] = addressesMaybeAdded.flatten.toSet
      logger.debug(s"Actually added the following addresses to watch: $addressesActuallyAdded")

      new AddTrackedAddresses.Response(
        new AddTrackedAddressesRequestResult(addressesActuallyAdded.asJava)
      )
    }

  private[this] def handleGetBalances(payload: GetBalances.GBRequestPayload): GetBalances.Response =
  // Construct all the response DB in a single atomic readonly DB transaction.
    DB readOnly { implicit session =>
      new GetBalances.Response(
        // Read and remember the sync progress for the whole operation
        (state.ethereumNodeStatus, dbStorage.progress.getProgress) match {
          case (None, _) | (_, None) =>
            logger.warn(s"Received GetBalances request while not ready; respond with error")
            null
          case (Some(ethereumNodeStatus), Some(progress)) if progress.blocks.to.isEmpty =>
            logger.warn(s"Received GetBalances request but blocks are not ready; respond with error")
            null
          case (ethereumNodeStatusOpt@Some(ethereumNodeStatus), progressOpt@Some(progress)) =>
            // Real use-case handling
            val blocksTo = progress.blocks.to.get // non-empty, due to previous `case` check
            val maxBlock = blocksTo - payload.confirmations
            logger.debug(s"Get balances for $payload at $maxBlock")

            val results = dbStorage.balances.getBalances(
              payload.address,
              maxBlock,
              Option(payload.filterCurrencyKeys).map(_.asScala.toSet)
            )
            new GetBalances.BalanceRequestResult(
              buildSystemSyncStatus(ethereumNodeStatusOpt, progressOpt),
              results.asJava
            )
        }
      )
    }

  private[this] def handleGetTransfers(payload: GetTransfers.GTRequestPayload): GetTransfers.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      new GetTransfers.Response(
        // Read and remember the sync progress for the whole operation
        (state.ethereumNodeStatus, dbStorage.progress.getProgress) match {
          case (None, _) | (_, None) =>
            logger.warn(s"Received GetTransfers request while not ready; respond with error")
            null
          case (Some(ethereumNodeStatus), Some(progress)) if progress.blocks.to.isEmpty =>
            logger.warn(s"Received GetTransfers request but blocks are not ready; respond with error")
            null
          case (ethereumNodeStatusOpt@Some(ethereumNodeStatus), progressOpt@Some(progress)) =>
            // Real use-case handling
            val blocksTo = progress.blocks.to.get // non-empty, due to previous `case` check
            val maxBlock = blocksTo - payload.confirmations

            val optEndBlock = Option(payload.endBlock).map(_.toInt) // of nullable; safe conversion to Option[Int]

            val endBlock = optEndBlock match {
              case None => maxBlock
              case Some(endBlockCandidate) =>
                Math.min(endBlockCandidate, maxBlock)
            }

            logger.debug(s"Get transfers for $payload at $maxBlock ($endBlock)")

            val optSender = Option(payload.sender) // of nullable
            val optReceiver = Option(payload.receiver) // of nullable
            val optStartBlock = Option(payload.startBlock).map(_.toInt) // of nullable; safe conversion to Option[Int]
            val optCurrencyKeys = Option(payload.filterCurrencyKeys).map(_.asScala.toSet)

            val transfers = dbStorage.transfers.getTransfers(
              optSender,
              optReceiver,
              optStartBlock,
              endBlock,
              optCurrencyKeys
            )
            // We already have the transfers. But the query payload contained optional filters for sender and receiver;
            // so let's try to get balances for both.

            // Either sender; or receiver; or both; – can be optional. Make a sequence of those who are non-None.
            val balanceKeys: Seq[String] = Seq(Option(payload.sender), Option(payload.receiver)).flatten
            val balances: Map[String, List[BalanceRequestResult.CurrencyBalanceFact]] =
              if (payload.includeBalances)
                balanceKeys.map(addr => addr -> dbStorage.balances.getBalances(addr, endBlock, optCurrencyKeys)).toMap
              else
                Map.empty

            new GetTransfers.TransfersRequestResult(
              buildSystemSyncStatus(ethereumNodeStatusOpt, progressOpt),
              transfers.asJava,
              balances.map { case (k, v) => k -> v.asJava }.asJava
            )
        }
      )
    }
}


/** Akka actor to run CherryPicker operations. */
object CherryPicker extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrypicker.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  val BLOCK_ITERATION_PERIOD = 10 seconds // Each block is generated about once per 13 seconds, let’s be safe

  protected final case class State(@volatile var ethereumNodeStatus: Option[SystemSyncStatus.Blockchain] = None)


  /** Main constructor. */
  @inline final def apply(dbStorage: DBStorageAPI,
                          ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                          maxReorg: Int,
                          headSyncerBatchSize: Int,
                          tailSyncerBatchSize: Int,
                          catchUpBrakeMaxLeadSetting: Int): Behavior[CherryPickerRequest] =
    new CherryPicker(
      dbStorage,
      ethereumConnector,
      state = CherryPicker.State(),
      maxReorg,
      headSyncerBatchSize,
      tailSyncerBatchSize,
      catchUpBrakeMaxLeadSetting
    ).launch()

  @inline
  private[CherryPicker] final def buildSystemSyncStatus(ethereumNodeStatusOpt: Option[SystemSyncStatus.Blockchain],
                                                        progressOpt: Option[Progress.ProgressData]): SystemSyncStatus =
    new SystemSyncStatus(
      ethereumNodeStatusOpt.orNull,
      progressOpt
        .flatMap(pr => (pr.blocks.to, pr.perCurrencyTrackedAddresses.maxTo, pr.perCurrencyTrackedAddresses.minTo) match {
          case (Some(blocksTo), Some(partiallySynced), Some(fullySynced)) =>
            Some(SystemSyncStatus.CherryPicker.create(blocksTo, partiallySynced, fullySynced))
          case other =>
            None
        })
        .orNull
    )

  /** For a message incoming to CherryPicker, launch a new handler as a child Actor.
   *
   * @param messageName the name of the message (to use in logs and as the name for the Actor).
   * @param replyTo     the actor that will receive a reply (typed `RESP`) to to the request.
   * @param onError     a function generating a proper “on error” response message to reply, in case of any exception.
   * @param handler     the code that will generate the proper reply (typed `RESP`) to the requestor
   *                    (likely located at `replyTo`).
   */
  @inline
  private[CherryPicker] final def requestHandlerActor[RESP](messageName: String,
                                                            replyTo: ActorRef[RESP],
                                                            onError: () => RESP)
                                                           (handler: () => RESP): Behavior[Any] = Behaviors.setup { context =>
    logger.debug(s"Handling $messageName message in child actor")

    val response: RESP = try {
      handler()
    } catch {
      case NonFatal(e) =>
        logger.error(s"Unexpected error in handling $messageName", e)
        onError()
    }

    logger.debug(s"Replying to $messageName with $response")
    replyTo ! response
    Behaviors.stopped
  }
}
