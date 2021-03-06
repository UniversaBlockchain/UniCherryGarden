package com.myodov.unicherrygarden

import java.util.UUID

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.DBStorage.TrackedAddresses
import com.myodov.unicherrygarden.api.GardenMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload
import com.myodov.unicherrygarden.api.{DBStorageAPI, GardenMessages}
import com.myodov.unicherrygarden.cherrypicker.syncers.{HeadSyncer, TailSyncer}
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.AddTrackedAddressesRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.TrackedAddressesRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrypicker.GetTransfers.TransfersRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrypicker._
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB.
 *
 * It has two primary sub-systems/sub-behaviors: [[HeadSyncer]] and [[TailSyncer]],
 * which both work independently but assume the other counterpart does its job too.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 * @param maxReorg maximum lenmaxReorggth of reorganization in Ethereum blockchain that we support and allow.
 */
private class CherryPicker(
                            // CherryGardenComponent-specific
                            realm: String,
                            dbStorage: DBStorageAPI,
                            // CherryPicker-specific
                            protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                            protected[this] val state: CherryPicker.State = CherryPicker.State(),
                            maxReorg: Int,
                            headSyncerBatchSize: Int,
                            tailSyncerBatchSize: Int,
                            catchUpBrakeMaxLeadSetting: Int
                          ) extends CherryGardenComponent(realm, dbStorage) with LazyLogging {
  assert(maxReorg >= 1, maxReorg)
  assert(headSyncerBatchSize >= 1, headSyncerBatchSize)
  assert(tailSyncerBatchSize >= 1, tailSyncerBatchSize)
  assert(catchUpBrakeMaxLeadSetting >= 1 && catchUpBrakeMaxLeadSetting >= Math.max(headSyncerBatchSize, tailSyncerBatchSize),
    (catchUpBrakeMaxLeadSetting, headSyncerBatchSize, tailSyncerBatchSize))

  import CherryPicker._

  /** First state: when just launching the CherryPicker after shutdown/restart. */
  private def launch(): Behavior[CherryPickerRequest] = {
    Behaviors.setup { context =>
      logger.info(s"Launching CherryPicker in realm \"$realm\": v. $propVersionStr, built at $propBuildTimestampStr")

      logger.debug("CherryPicker: Launching HeadSyncer...")
      val headSyncer: ActorRef[GardenMessages.HeadSyncerMessage] = context.spawn(
        HeadSyncer(dbStorage, ethereumConnector, maxReorg)(headSyncerBatchSize, catchUpBrakeMaxLeadSetting),
        "HeadSyncer")
      logger.debug("CherryPicker: Launching TailSyncer...")
      val tailSyncer: ActorRef[GardenMessages.TailSyncerMessage] = context.spawn(
        TailSyncer(dbStorage, ethereumConnector, maxReorg)(tailSyncerBatchSize, headSyncer),
        "TailSyncer")
      logger.debug("CherryPicker: launched sub-syncers!")

      // Register all service keys
      List(
        GetTrackedAddresses.makeServiceKey(realm),
        AddTrackedAddresses.makeServiceKey(realm),
        GetAddressDetails.makeServiceKey(realm),
        GetBalances.makeServiceKey(realm),
        GetTransfers.makeServiceKey(realm),
      ).foreach(context.system.receptionist ! Receptionist.Register(_, context.self))

      // On an `EthereumNodeStatus`, we just write its data into the state;
      // On any incoming Request, we spawn a new child actor which will handle it (for better concurrency).
      Behaviors.receiveMessage {
        case message@EthereumNodeStatus(status) =>
          logger.debug(s"CherryPicker received Ethereum node syncing status: $message")
          // 1. Store the status message for our own usage,..
          state.ethereumStatus = Some(status)
          // 2. ... and forward it to sub-actors (syncers)
          for (listener <- List(headSyncer, tailSyncer)) {
            listener ! message
          }
          Behaviors.same
        case message: GetTrackedAddresses.Request => {
          val msgName = "GetTrackedAddresses"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[GetTrackedAddresses.Response](
              msgName,
              message.replyTo,
              msg => GetTrackedAddresses.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handleGetTrackedAddresses(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: AddTrackedAddresses.Request => {
          val msgName = "AddTrackedAddresses"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[AddTrackedAddresses.Response](
              msgName,
              message.replyTo,
              msg => AddTrackedAddresses.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handleAddTrackedAddresses(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetAddressDetails.Request => {
          val msgName = "GetAddressDetails"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[GetAddressDetails.Response](
              msgName,
              message.replyTo,
              msg => GetAddressDetails.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handleGetAddressDetails(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetBalances.Request => {
          val msgName = "GetBalances"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[GetBalances.Response](
              msgName,
              message.replyTo,
              msg => GetBalances.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handleGetBalances(message.payload) },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetTransfers.Request => {
          val msgName = "GetTransfers"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[GetTransfers.Response](
              msgName,
              message.replyTo,
              msg => GetTransfers.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handleGetTransfers(message.payload) },
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
      val results: List[TrackedAddressesRequestResultPayload.TrackedAddressInformation] = dbStorage
        .trackedAddresses
        .getTrackedAddresses(
          Option(payload.filterAddresses).map(_.asScala.toSet),
          payload.includeComment,
          payload.includeSyncedFrom)
        .map(_.toTrackedAddressInformation)

      new GetTrackedAddresses.Response(
        new TrackedAddressesRequestResultPayload(
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
      val toAdd = payload.addressesToTrack.asScala.toList

      val addressesMaybeAdded: List[Option[String]] = (
        for (addr: AddTrackedAddresses.AddressDataToTrack <- toAdd)
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

      val addressesToAdd = toAdd.map(_.address).toSet
      val addressesAlreadyPresent = addressesToAdd diff addressesActuallyAdded

      logger.debug(s"Actually added the following addresses to watch: $addressesActuallyAdded; " +
        s"present already were $addressesAlreadyPresent")

      new AddTrackedAddresses.Response(
        new AddTrackedAddressesRequestResultPayload(
          addressesActuallyAdded.asJava,
          addressesAlreadyPresent.asJava
        )
      )
    }

  private[this] def handleGetAddressDetails(payload: GetAddressDetails.GADRequestPayload): GetAddressDetails.Response =
  // Construct all the response DB in a single atomic read-write DB transaction.
    DB readOnly { implicit session =>
      val trackedAddr: Option[TrackedAddresses.TrackedAddress] = dbStorage
        .trackedAddresses
        .getTrackedAddress(payload.address)

      ethereumConnector.getAddressNonces(payload.address) match {
        case None =>
          logger.error(s"Could not get getAddressNonces(${payload.address}) hence failing")
          GetAddressDetails.Response.fromCommonFailure(FailurePayload.NODE_REQUEST_FAILURE);
        case Some((nonceLatest, optNoncePending)) =>
          new GetAddressDetails.Response(
            new GetAddressDetails.AddressDetailsRequestResultPayload(
              new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails(
                payload.address,
                trackedAddr.map(_.toTrackedAddressInformation).orNull,
                new GetAddressDetails.AddressDetailsRequestResultPayload.AddressDetails.Nonces(
                  nonceLatest,
                  optNoncePending.map(Integer.valueOf).orNull,
                  trackedAddr.flatMap(_.nextPlantingNonce).map(Integer.valueOf).orNull
                )
              )
            )
          )
      }
    }

  private[this] def handleGetBalances(payload: GetBalances.GBRequestPayload): GetBalances.Response =
  // Construct all the response DB in a single atomic readonly DB transaction.
    DB readOnly { implicit session =>
      CherryGardenComponent.whenStateAndProgressAllow[GetBalances.Response](
        state.ethereumStatus,
        dbStorage.progress.getProgress,
        "GetBalances",
        GetBalances.Response.fromCommonFailure(FailurePayload.CHERRY_GARDEN_NOT_READY)
      ) { (ethereumNodeStatus, progress) =>
        // Real use-case handling
        val blocksTo = progress.blocks.to.get // non-empty, due to previous `case` check
        val maxBlock = blocksTo - payload.confirmations
        logger.debug(s"Get balances for $payload at $maxBlock")

        val results = dbStorage.balances.getBalances(
          payload.address,
          maxBlock,
          Option(payload.filterCurrencyKeys).map(_.asScala.toSet)
        )
        new GetBalances.Response(
          new BalanceRequestResultPayload(
            CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
            results.asJava
          )
        )
      }
    }

  private[this] def handleGetTransfers(payload: GetTransfers.GTRequestPayload): GetTransfers.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      CherryGardenComponent.whenStateAndProgressAllow[GetTransfers.Response](
        state.ethereumStatus,
        dbStorage.progress.getProgress,
        "GetTransfers",
        GetTransfers.Response.fromCommonFailure(FailurePayload.CHERRY_GARDEN_NOT_READY)
      ) { (ethereumNodeStatus, progress) =>
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
        val balances: Map[String, List[BalanceRequestResultPayload.CurrencyBalanceFact]] =
          if (payload.includeBalances)
            balanceKeys.map(addr => addr -> dbStorage.balances.getBalances(addr, endBlock, optCurrencyKeys)).toMap
          else
            Map.empty

        new GetTransfers.Response(
          new TransfersRequestResultPayload(
            CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
            transfers.asJava,
            balances.map { case (k, v) => k -> v.asJava }.asJava
          )
        )
      }
    }
}


/** Akka actor to run CherryPicker operations. */
object CherryPicker extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrypicker.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  protected final case class State(@volatile var ethereumStatus: Option[SystemStatus.Blockchain] = None)

  /** Main constructor. */
  @inline final def apply(realm: String,
                          dbStorage: DBStorageAPI,
                          ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                          maxReorg: Int,
                          headSyncerBatchSize: Int,
                          tailSyncerBatchSize: Int,
                          catchUpBrakeMaxLeadSetting: Int
                         ): Behavior[CherryPickerRequest] =
    new CherryPicker(
      realm,
      dbStorage,
      ethereumConnector,
      state = CherryPicker.State(),
      maxReorg,
      headSyncerBatchSize,
      tailSyncerBatchSize,
      catchUpBrakeMaxLeadSetting
    ).launch()
}
