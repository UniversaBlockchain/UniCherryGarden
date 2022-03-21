package com.myodov.unicherrygarden

import java.time.Instant

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.DBStorage.Progress
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.api.{DBStorage, DBStorageAPI}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal

/**
 * Any actor/component of the cluster, such as: CherryGardener, CherryPicker, CherryPlanter.
 */
abstract class CherryGardenComponent(
                                      protected[this] val realm: String,
                                      protected[this] val dbStorage: DBStorageAPI
                                    ) {
}


object CherryGardenComponent extends LazyLogging {
  val BLOCK_ITERATION_PERIOD = 10 seconds // Each block is generated about once per 13 seconds, let’s be safe

  /**
   * Execute some action `action` and return its result (as a part of handling the message `messageName`),
   * but only if the Ethereum node/blockchain status and the CherryGarden sync status are sane meaning that
   * the system is fully up and ready.
   */
  @inline
  final def whenStateAndProgressAllow[T](ethereumStatusOpt: Option[SystemStatus.Blockchain],
                                         syncProgressOpt: Option[DBStorage.Progress.ProgressData],
                                         messageName: String,
                                         resultOnError: T
                                        )(
                                          action: (SystemStatus.Blockchain, DBStorage.Progress.ProgressData) => T,
                                        ): T =
    (ethereumStatusOpt, syncProgressOpt) match {
      case (None, _) | (_, None) =>
        logger.warn(s"Received $messageName request while not ready; respond with error: $resultOnError")
        resultOnError
      case (Some(ethereumNodeStatus), Some(progress)) if progress.blocks.to.isEmpty =>
        logger.warn(s"Received $messageName request but blocks are not ready; respond with error: $resultOnError")
        resultOnError
      case (Some(ethereumNodeStatus), Some(progress)) =>
        // Real use-case handling
        action(ethereumNodeStatus, progress)
    }

  /**
   * Make a [[SystemStatus]] structure from both the node/blockchain status data (received from the blockchain)
   * and the sync data (received from the DB).
   */
  @inline
  final def buildSystemSyncStatus(ethereumNodeStatus: SystemStatus.Blockchain,
                                  progress: Progress.ProgressData
                                 ): SystemStatus = {
    val cherryPickerStatusOpt: Option[SystemStatus.CherryPicker] = (progress.blocks.to, progress.perCurrencyTrackedAddresses.maxTo, progress.perCurrencyTrackedAddresses.minTo) match {
      case (Some(blocksTo), Some(partiallySynced), Some(fullySynced)) =>
        Some(SystemStatus.CherryPicker.create(blocksTo, partiallySynced, fullySynced))
      case other =>
        None
    }

    new SystemStatus(
      Instant.now,
      ethereumNodeStatus,
      cherryPickerStatusOpt.orNull
    )
  }

  /** For a message incoming to some CherryGardenComponent, launch a new handler as a child Actor.
   *
   * @param messageName the name of the message (to use in logs and as the name for the Actor).
   * @param replyTo     the actor that will receive a reply (typed `RESP`) to to the request.
   * @param onError     a function generating a proper “on error” response message to reply, in case of any exception.
   * @param handler     the code that will generate the proper reply (typed `RESP`) to the requestor
   *                    (likely located at `replyTo`).
   */
  @inline
  final def requestHandlerActor[RESP](messageName: String,
                                      replyTo: ActorRef[RESP],
                                      onError: String => RESP)
                                     (handler: () => RESP): Behavior[Any] = Behaviors.setup { context =>
    logger.debug(s"Handling $messageName message in child actor")

    val response: RESP = try {
      handler()
    } catch {
      case NonFatal(e) =>
        val msg = s"Unexpected error in handling $messageName"
        logger.error(msg, e)
        onError(s"{msg}: $e")
    }

    logger.debug(s"Replying to $messageName") // do not log $response here, it may be very large
    replyTo ! response
    Behaviors.stopped
  }

}
