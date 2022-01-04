package com.myodov.unicherrygarden.cherrypicker

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.api.types.SystemSyncStatus
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.language.postfixOps

/** Very simple actor to read the Ethereum node syncing status (like `eth.syncing`/`eth.blockNumber` API commands,
 * and getting the “current”/“highest” known Ethereum block).
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 */
private class EthereumStatePoller(ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends LazyLogging {

  import EthereumStatePoller.{Message, Poll}
  import SyncerMessages.EthereumNodeStatus

  private def launch(listeners: Seq[ActorRef[SyncerMessages.EthereumNodeStatus]]): Behavior[Message] = {
    logger.debug(s"Launching Ethereum state poller for $listeners")

    Behaviors.withTimers[Message] { timers: TimerScheduler[Message] =>
      timers.startTimerWithFixedDelay(
        Poll(),
        0 seconds,
        CherryPicker.BLOCK_ITERATION_PERIOD,
      )
      // due to initialDelay = 0 seconds, it also sends this message, instantly, too.
      Behaviors.receiveMessage[Message] {
        case Poll() =>
          logger.debug("Polling Ethereum node for syncing status...")
          // If polling successful (and only then), resend the syncing status to listeners
          ethereumConnector.ethSyncingBlockNumber.map { syncingStatus =>
            val msg = EthereumNodeStatus(SystemSyncStatus.Blockchain.create(
              syncingStatus.currentBlock, syncingStatus.highestBlock))
            for (listener <- listeners) {
              listener ! msg
            }
          }
          Behaviors.same
      }
    }
  }
}

/** A simple helper to poll the Ethereum */
object EthereumStatePoller {

  // Sealed to make message matches exhaustive
  sealed trait Message

  /** The message to run the next polling attempt. */
  private final case class Poll() extends Message

  /** Main constructor. */
  @inline def apply(ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                    listeners: Seq[ActorRef[SyncerMessages.EthereumNodeStatus]]): Behavior[Message] =
    new EthereumStatePoller(ethereumConnector).launch(listeners)
}
