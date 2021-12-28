package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{EthereumNodeStatus, HeadSyncerMessage, TailSyncerMessage}
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector.SingleBlockData
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.{DBStorage, DBStorageAPI}
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DBSession

import scala.language.postfixOps
import scala.util.control.NonFatal

/** Superclass for [[HeadSyncer]] and [[TailSyncer]], containing the common logic.
 *
 * Generic class arguments:
 * <ul>
 * <li>`M` – one of [[HeadSyncerMessage]], [[TailSyncerMessage]].</li>
 * <li>`S` – one of [[HeadSyncerState]], [[TailSyncerState]].</li>
 * <li>`IS` – one of [[IterateHeadSyncer]], [[IterateTailSyncer]].</li>
 * </ul>
 *
 * `state` contains the overall state of the syncer.
 * Unfortunately, we cannot go fully Akka-way having the system state passed through the methods, FSM states
 * and behaviors. If we receive some state-changing message from outside (e.g. the latest state of Ethereum node
 * syncing process; or, for HeadSyncer, the message from TailSyncer), we need to alter the state immediately.
 * But the FSM may be in a 10-second delay after the latest block being processed, and after it a message
 * with the previous state will be posted by the timer. So alas, `state` has to be variable.
 */
abstract private class AbstractSyncer[
  M <: SyncerMessages.Message,
  S <: AbstractSyncer.SyncerState,
  IS <: SyncerMessages.IterateSyncer[M] with M
]
(protected[this] val dbStorage: DBStorageAPI,
 protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
 protected[this] val state: S)
  extends LazyLogging {

  /** Most important method doing some next iteration of a syncer. */
  def iterate(): Behavior[M]

  /** Construct the specific implementation of `S` generic instance. */
  def makeIterateMessage(): IS

  /** Most basic sanity test for the DB data;
   * fails if we cannot even go further and must wait for the node to continue syncing.
   */
  protected[this] def isNodeReachable(dbProgressData: DBStorage.Progress.ProgressData,
                                      nodeSyncingStatus: EthereumNodeStatus): Boolean =
    dbProgressData.blocks.to match {
      case None =>
        // All data is available; but there is no blocks in the DB. This actually is fully okay
        true
      case Some(maxBlocksNum) =>
        // Everything is fine if our latest stored block is not newer than the latest block available
        // to Ethereum node;
        // false/bad otherwise
        maxBlocksNum <= nodeSyncingStatus.currentBlock
    }

  /** Validate the syncing progress data/Ethereum node syncing state;
   * and execute the `code` if they are valid, assuming it (maybe) returns some `Behavior`.
   *
   * `RES` is the expected return type from the function; may be `Option[Behavior]`, `[Behavior]` or something similar.
   */
  protected[this] def withValidatedProgressAndSyncingState[RES](
                                                                 optProgress: Option[Progress.ProgressData],
                                                                 optNodeSyncingStatus: Option[EthereumNodeStatus],
                                                                 onError: RES
                                                               )
                                                               (
                                                                 code: (Progress.ProgressData, EthereumNodeStatus) => RES
                                                               )
                                                               (implicit session: DBSession): RES = {
    (optProgress, optNodeSyncingStatus) match {
      case (None, _) =>
        // we could not even get the DB progress – go to the next round
        logger.error("Some unexpected error when reading the overall progress from the DB")
        onError
      case (_, None) =>
        // we haven’t received the syncing state from the node
        logger.error("Could not read the syncing status from Ethereum node")
        onError
      case (Some(overallProgress: Progress.ProgressData), Some(nodeSyncingStatus: EthereumNodeStatus))
        if !isNodeReachable(overallProgress, nodeSyncingStatus) =>
        // Sanity test. Does the overall data sanity allows us to proceed?
        // In HeadSyncer, this is Reorg/rewind, phase 1/4: “is node reachable”.
        // In TailSyncer, this is just a sanity test.
        logger.error(s"Ethereum node is probably unavailable: $overallProgress, $nodeSyncingStatus")
        onError
      case (Some(overallProgress: Progress.ProgressData), Some(nodeSyncingStatus: EthereumNodeStatus)) =>
        code(overallProgress, nodeSyncingStatus)
    }
  }

  def reiterate(): Behavior[M] = {
    logger.debug("FSM: reiterate")
    Behaviors.setup { context =>
      logger.debug("Sending iterate message to self")
      context.self ! makeIterateMessage
      Behaviors.same
    }
  }

  def pauseThenReiterate(): Behavior[M] = {
    logger.debug("FSM: pauseThenReiterate")
    Behaviors.withTimers[M] { timers =>
      timers.startSingleTimer(
        makeIterateMessage,
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }
  }

  /** The pause-then-reiterate method that must be implemented in each syncer specifically. */
  def pauseThenReiterateOnError(): Behavior[M]

  /** Perform the regular iteration for a specific block number:
   * read the block from the Ethereum connector, store it into the DB.
   *
   * @return whether syncing of the blocks succeeded.
   */
  protected[this] def syncBlocks(
                                  blocksToSync: dlt.EthereumBlock.BlockNumberRange
                                )(implicit session: DBSession): Boolean = {
    val trackedAddresses: Set[String] = dbStorage.trackedAddresses.getJustAddresses

    logger.debug(s"FSM: syncBlocks - blocks $blocksToSync with tracked addresses $trackedAddresses")

    // Were all of the blocks read and stored well?
    val successes: Seq[Boolean] = ethereumConnector.readBlocks(blocksToSync, trackedAddresses) match {
      case None =>
        logger.error(s"Cannot read blocks $blocksToSync")
        Seq(false)
      case Some(blocks: Seq[SingleBlockData]) => blocks.map { case (block, transactions) =>
        try {
          logger.debug(s"Reading block $block: txes $transactions")

          val thisBlockInDbOpt = dbStorage.blocks.getBlockByNumber(block.number)
          val prevBlockInDbOpt = dbStorage.blocks.getBlockByNumber(block.number - 1)

          logger.debug(s"Storing block: $block; " +
            s"block may be present as $thisBlockInDbOpt, " +
            s"parent may be present as $prevBlockInDbOpt")

          val addingBlockSuccess: Boolean = (thisBlockInDbOpt, prevBlockInDbOpt) match {
            case (None, None) =>
              // This is the simplest case: this is probably the very first block in the DB
              logger.debug(s"Adding first block ${block.number}: " +
                s"neither it nor previous block exist in the DB")
              dbStorage.blocks.addBlock(block.withoutParentHash)
              true
            case (None, Some(prevBlockInDb)) if prevBlockInDb.hash == block.parentHash.get =>
              // Another simplest case: second and further blocks in the DB.
              // Very new block, and its parent matches the existing one
              logger.debug(s"Adding new block ${block.number}; parent block ${block.number - 1} " +
                s"exists already with proper hash")
              dbStorage.blocks.addBlock(block)
              true
            case (Some(thisBlockInDb), _) if thisBlockInDb.hash == block.hash =>
              logger.debug(s"Block ${block.number} exists already in the DB " +
                s"with the same hash ${block.hash}; " +
                "no need to readd the block itself")
              true
            case (Some(thisBlockInDb), _) if thisBlockInDb.hash != block.hash =>
              logger.debug(s"Block ${block.number} exists already in the DB " +
                s"but with ${thisBlockInDb.hash} rather than ${block.hash}; " +
                "need to wipe some blocks maybe!")
              false
            case (None, Some(prevBlockInDb)) if prevBlockInDb.hash != block.parentHash.get =>
              logger.debug(s"Adding new block ${block.number}: " +
                s"expecting parent block to be ${prevBlockInDb.hash} but it is ${block.parentHash.get}; " +
                "need to wipe some blocks maybe!")
              false
            case other =>
              logger.debug(s"No idea what's up with $thisBlockInDbOpt and $prevBlockInDbOpt")
              false
          } // addingBlockSuccess

          if (!addingBlockSuccess) {
            // Bail out early
            false
          } else {
            logger.debug(s"Now trying to store the transactions: $transactions")
            for (tx <- transactions) {
              dbStorage.transactions.addTransaction(tx, block.hash)
              dbStorage.txLogs.addTxLogs(block.number, tx.txhash, tx.txLogs)
            }
            dbStorage.state.advanceProgress(block.number, trackedAddresses)
            true
          }
        } catch {
          case NonFatal(e) =>
            logger.error(s"Unexpected error", e)
            false
        }
      }
    } // successes

    successes.forall(identity)
  }
}

object AbstractSyncer {

  trait SyncerState {
    @volatile var ethereumNodeStatus: Option[EthereumNodeStatus] = None
  }

}
