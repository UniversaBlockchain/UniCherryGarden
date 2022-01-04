package com.myodov.unicherrygarden

import java.util.Collections

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.types.{MinedTransfer, SystemSyncStatus}
import com.myodov.unicherrygarden.cherrypicker.EthereumStatePoller
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.cherrypicker.syncers.{HeadSyncer, SyncerMessages, TailSyncer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.AddTrackedAddressesRequestResult
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

      Behaviors.receiveMessage {
        case message@EthereumNodeStatus(status) =>
          logger.debug(s"CherryPicker received Ethereum node syncing status: $message")
          state.ethereumNodeStatus = Some(status)
          Behaviors.same
        case message: GetTrackedAddresses.Request => {
          logger.debug(s"Receiving GetTrackedAddresses message: $message")

          val response = try {
            handleGetTrackedAddresses(message.payload)
          } catch {
            case NonFatal(e) =>
              logger.error(s"Unexpected error in handleGetTrackedAddresses(${message.payload})", e)
              new GetTrackedAddresses.Response(null)
          }

          logger.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        }
        case message: AddTrackedAddresses.Request => {
          logger.debug(s"Receiving AddTrackedAddresses message: $message")

          val response = try {
            handleAddTrackedAddresses(message.payload)
          } catch {
            case NonFatal(e) =>
              logger.error(s"Unexpected error in handleAddTrackedAddresses(${message.payload})", e)
              new AddTrackedAddresses.Response(null)
          }

          logger.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        }
        case message: GetBalances.Request => {
          logger.debug(s"Receiving GetBalances message: $message")

          val response = try {
            handleGetBalances(message.payload)
          } catch {
            case NonFatal(e) =>
              logger.error(s"Unexpected error in handleGetBalances(${message.payload})", e)
              new GetBalances.Response(null)
          }

          logger.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        }
        case message: GetTransfers.Request => {
          logger.debug(s"Receiving GetTransfers message: $message")

          val response = try {
            handleGetTransfers(message.payload)
          } catch {
            case NonFatal(e) =>
              logger.error(s"Unexpected error in handleGetTransfers(${message.payload})", e)
              new GetTransfers.Response(null)
          }

          logger.debug(s"Replying with $response")
          message.replyTo ! response
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
            logger.warn(s"Received getBalances request while not ready; respond with error")
            null
          case (Some(ethereumNodeStatus), Some(progress)) if progress.blocks.to.isEmpty =>
            logger.warn(s"Received getBalances request but blocks are not ready; respond with error too")
            null
          case (ethereumNodeStatusOpt@Some(ethereumNodeStatus), progressOpt@Some(progress)) =>
            val blocksTo = progress.blocks.to.get // non-empty, due to previous `case` check
            val maxBlock = blocksTo - payload.confirmations
            logger.debug(s"Get balances for $payload at $maxBlock")

            val results = dbStorage.balances.getBalances(
              maxBlock,
              payload.address,
              Option(payload.filterCurrencyKeys).map(_.asScala.toSet)
            )
            logger.error(s"Results are: $results")
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
      // Read and remember the sync progress for the whole operation
      val ethereumNodeStatusOpt = state.ethereumNodeStatus
      val progressOpt = dbStorage.progress.getProgress

      new GetTransfers.Response(
        new GetTransfers.TransfersRequestResult(
          // List(
          //   new MinedTransfer(
          //     "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          //     "0xedcc6f8f20962e6747369a71a5b89256289da87f",
          //     "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
          //     BigDecimal("10045.6909000003").underlying,
          //     new MinedTx(
          //       "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
          //       "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          //       "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
          //       new Block(
          //         13628884,
          //         "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
          //         Instant.ofEpochSecond(1637096843)),
          //       111
          //     ),
          //     144),
          //   // UTNP out #6
          //   new MinedTransfer(
          //     "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          //     "0x74644fd700c11dcc262eed1c59715ee874f65251",
          //     "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
          //     BigDecimal("30000").underlying,
          //     new MinedTx(
          //       "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
          //       "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          //       "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
          //       new Block(
          //         13631007,
          //         "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
          //         Instant.ofEpochSecond(1637125704)),
          //       133
          //     ),
          //     173)
          // ).asJava,
          buildSystemSyncStatus(ethereumNodeStatusOpt, progressOpt),
          List.empty[MinedTransfer].asJava,
          Collections.emptyMap()
        )
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
  @inline def apply(dbStorage: DBStorageAPI,
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

  private[CherryPicker] def buildSystemSyncStatus(ethereumNodeStatusOpt: Option[SystemSyncStatus.Blockchain],
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
}
