package com.myodov.unicherrygarden

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.EthereumStatePoller.LogActorSystemStatus
import com.myodov.unicherrygarden.api.GardenMessages
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
  import GardenMessages.EthereumNodeStatus

  private def launch(listeners: Seq[ActorRef[GardenMessages.EthereumNodeStatus]]): Behavior[Message] =
    Behaviors.setup { context =>
      logger.debug(s"Launching Ethereum state poller for $listeners")

      Behaviors.withTimers[Message] { timers: TimerScheduler[Message] =>
        timers.startTimerWithFixedDelay(
          Poll(),
          0 seconds,
          CherryGardenComponent.BLOCK_ITERATION_PERIOD,
        )
        timers.startTimerWithFixedDelay(
          LogActorSystemStatus(),
          0 seconds,
          1 minute,
        )
        // due to initialDelay = 0 seconds, it also sends this message, instantly, too.
        Behaviors.receiveMessage[Message] {
          case Poll() =>
            logger.debug("Polling Ethereum node for syncing status...")
            // If polling successful (and only then), resend the syncing status to listeners
            ethereumConnector.ethBlockchainStatus.foreach { syncingStatus =>
              val msg = EthereumNodeStatus(syncingStatus)
              for (listener <- listeners) {
                listener ! msg
              }
            }
            Behaviors.same
          case LogActorSystemStatus() =>
            logger.info(s"ActorSystem status: ${context.system}\n${context.system.printTree}")
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

  /** The message to log the actor system status. */
  private final case class LogActorSystemStatus() extends Message

  /** Main constructor. */
  @inline def apply(ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                    listeners: Seq[ActorRef[GardenMessages.EthereumNodeStatus]]): Behavior[Message] =
    new EthereumStatePoller(ethereumConnector).launch(listeners)
}
