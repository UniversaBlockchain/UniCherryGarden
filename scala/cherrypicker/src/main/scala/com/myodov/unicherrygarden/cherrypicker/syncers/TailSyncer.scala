package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.cherrypicker.syncers.AbstractSyncer.SyncerState
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{IterateTailSyncer, TailSyncerMessage}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage

import scala.language.postfixOps

/** Performs the “Tail sync” – (re)syncing the older blocks, which have to be resynced
 * due to some currencies or tokens added.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 */
private class TailSyncer(pgStorage: PostgreSQLStorage,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends AbstractSyncer[
    TailSyncerMessage,
//    TailSyncerState,
    IterateTailSyncer
  ](pgStorage, ethereumConnector) {

  /** The overall state of the syncer.
   *
   * Unfortunately, we cannot go fully Akka-way having the system state passed through the methods, FSM states
   * and behaviors. If we receive some state-changing message from outside (e.g. the latest state of Ethereum node
   * syncing process; or, for HeadSyncer, the message from TailSyncer), we need to alter the state immediately.
   * But the FSM may be in a 10-second delay after the latest block being processed, and after it a message
   * with the previous state will be posted by the timer. So alas, `state` has to be a var.
   */
  protected[this] var state: TailSyncerState = TailSyncerState() // initialized with default state

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  def launch(headSyncer: ActorRef[SyncerMessages.HeadSyncerMessage]): Behavior[TailSyncerMessage] = {
    logger.debug(s"Launching syncer: ${this.getClass.getSimpleName}")
    // Then go to the mainLoop, with the initial state
    Behaviors.setup[TailSyncerMessage] { context =>
      Behaviors.receiveMessage[TailSyncerMessage] {
        case IterateTailSyncer() =>
          //          nextIteration()
          logger.debug(s"Iteration in TailSyncer $state")
          Behaviors.same
        case EthereumNodeStatus(current, highest) =>
          logger.debug(s"TailSyncer received $current, $highest")
          Behaviors.same
      }
    }
  }

  //  override def mainLoop(state: TailSyncerState): Behavior[TailSyncerMessage] =
  //    Behaviors.receiveMessage[TailSyncerMessage] {
  //      case IterateTailSyncer(state) =>
  //        //          nextIteration()
  //        logger.debug(s"Iteration in TailSyncer $state")
  //        Behaviors.same
  //      case EthereumNodeStatus(current, highest) =>
  //        logger.debug(s"TailSyncer received $current, $highest")
  //        Behaviors.same
  //    }

  override def makeIterateMessage(): IterateTailSyncer =
    IterateTailSyncer()

  override def iterate(): Behavior[TailSyncerMessage] = {
    logger.debug(s"Iterating with $state")
    Behaviors.same
  }

  //  def nextIteration(): Unit = {
  //    logger.debug("FFF  Trying Fast-sync...")
  //    Behaviors.same
  //  }
}

object TailSyncer {
  /** Main constructor. */
  @inline def apply(pgStorage: PostgreSQLStorage,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                    headSyncer: ActorRef[SyncerMessages.HeadSyncerMessage]): Behavior[SyncerMessages.TailSyncerMessage] =
    new TailSyncer(pgStorage, ethereumConnector).launch(headSyncer)
}

private final case class TailSyncerState()
  extends SyncerState
