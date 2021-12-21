package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.cherrypicker.syncers.AbstractSyncer.SyncerState
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{HeadSyncerMessage, IterateHeadSyncer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage

import scala.language.postfixOps

/** Performs the “Head sync” – syncing the newest blocks, which haven’t been synced yet.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 */
private class HeadSyncer(pgStorage: PostgreSQLStorage,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends AbstractSyncer[
    HeadSyncerMessage,
    //    HeadSyncerState,
    IterateHeadSyncer
  ](pgStorage, ethereumConnector) {

  /** The overall state of the syncer.
   *
   * Unfortunately, we cannot go fully Akka-way having the system state passed through the methods, FSM states
   * and behaviors. If we receive some state-changing message from outside (e.g. the latest state of Ethereum node
   * syncing process; or, for HeadSyncer, the message from TailSyncer), we need to alter the state immediately.
   * But the FSM may be in a 10-second delay after the latest block being processed, and after it a message
   * with the previous state will be posted by the timer. So alas, `state` has to be a var.
   */
  protected[this] var state: HeadSyncerState = HeadSyncerState() // initialized with default state

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  def launch(): Behavior[HeadSyncerMessage] = {

    Behaviors.setup { context =>
      logger.debug(s"Syncer mainloop: ${this.getClass.getSimpleName}")

      // the initial value is anyway to “must check reorg”, but let’s just be explicit
      iterateMustCheckReorg()

      Behaviors.receiveMessage[HeadSyncerMessage] {
        case IterateHeadSyncer() =>
          //          nextIteration()
          logger.debug(s"Iteration in HeadSyncer $state")
          Behaviors.same
        case EthereumNodeStatus(current, highest) =>
          logger.debug(s"HeadSyncer received $current, $highest")
          Behaviors.same
        case GoingToTailSync(start, end) =>
          logger.debug(s"TailSyncer notified us it is going to sync $start..$end.")
          Behaviors.same
      }
    }
  }

  //  override def mainLoop(state: HeadSyncerState): Behavior[HeadSyncerMessage] = {
  //    Behaviors.same
  //  }


  override def makeIterateMessage(): IterateHeadSyncer =
    IterateHeadSyncer()

  override def iterate(): Behavior[HeadSyncerMessage] = {
    logger.debug(s"Iterating with $state")
    Behaviors.same
  }

  def iterateMayCheckReorg(): Behavior[HeadSyncerMessage] = {
    Behaviors.setup { context =>
      context.self ! IterateHeadSyncer()
      Behaviors.same
    }
  }

  def iterateMustCheckReorg(): Behavior[HeadSyncerMessage] = {
    state.synchronized {
      state.nextIterationMustCheckReorg = true
    }
    iterateMayCheckReorg()
  }

  def pauseThenMustCheckReorg(state: HeadSyncerState): Behavior[HeadSyncerMessage] =
    Behaviors.withTimers[HeadSyncerMessage] { timers =>
      timers.startSingleTimer(
        makeIterateMessage,
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }

  def pauseThenMayCheckReorg(state: HeadSyncerState): Behavior[HeadSyncerMessage] =
    Behaviors.withTimers[HeadSyncerMessage] { timers =>
      timers.startSingleTimer(
        makeIterateMessage,
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }

  //  def nextIteration(): Unit = {
  //    logger.debug("Trying next Slow-syncer iteration...")
  //    rereadEthSyncingBlockNumber()
  //
  //    logger.debug(s"Current node state: $lastKnownNodeSyncState")
  //    Behaviors.same
  //  }

  case class NodeSyncState(currentBlock: Int, highestBlock: Int)

  private var lastNodeSyncState: Option[NodeSyncState] = None

  /** FSM state: “Reread `eth.syncing` and `eth.blockNumber`”. */
  //  private[this] def rereadEthSyncingBlockNumber(): Unit = {
  //    logger.debug("Rereading eth.syncing/eth.blockNumber")
  //
  //    (ethereumConnector.ethSyncingBlockNumber: @switch) match {
  //      case None =>
  //        logger.error("Cannot get eth.syncing / eth.blockNumber !")
  //        pgStorage.state.setSyncState("Cannot connect to Ethereum node!")
  //      case Some(ethereumConnector.syn ethereumConnector.SyncingStatus(ethSyncing, ethBlockNumber)) =>
  //        logger.debug(s"Found eth.syncing ($ethSyncing) / eth.blockNumber ($ethBlockNumber)!")
  //        // If eth.syncing returned False, i.e. we are not syncing – we should assume currentBlock and highestBlock
  //        // are equal to eth.blockNumber
  //        val syncState: NodeSyncState = {
  //          if (ethSyncing.isStillSyncing)
  //            NodeSyncState(ethSyncing.currentBlock, ethSyncing.highestBlock)
  //          else
  //            NodeSyncState(ethBlockNumber, ethBlockNumber)
  //        }
  //        pgStorage.state.setEthNodeData(ethBlockNumber, syncState.currentBlock, syncState.highestBlock)
  //        lastNodeSyncState = Some(syncState)
  //    }
  //  }

  /** Get the last node sync state, returned from the Ethereum node. */
  def lastKnownNodeSyncState: Option[NodeSyncState] = lastNodeSyncState
}

object HeadSyncer {
  /** Main constructor. */
  @inline def apply(pgStorage: PostgreSQLStorage,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations): Behavior[SyncerMessages.HeadSyncerMessage] =
    new HeadSyncer(pgStorage, ethereumConnector).launch()
}

private final case class HeadSyncerState(var nextIterationMustCheckReorg: Boolean = true)
  extends SyncerState
