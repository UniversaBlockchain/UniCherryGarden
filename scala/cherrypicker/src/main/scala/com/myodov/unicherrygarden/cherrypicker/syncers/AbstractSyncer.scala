package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector.SingleBlockData
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
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
 * <li></li>
 * </ul>
 */
abstract private class AbstractSyncer[
  M <: SyncerMessages.Message,
  IS <: SyncerMessages.IterateSyncer[M] with M
]
(protected[this] val dbStorage: DBStorageAPI,
 protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends LazyLogging {

  /** Most important method doing some next iteration of a syncer. */
  def iterate(): Behavior[M]

  /** Construct the specific implementation of `S` generic instance. */
  def makeIterateMessage(): IS

  /** Go to the next iteration ([[iterate]] state), but after a pause. */
  def pauseThenIterate(): Behavior[M] =
    Behaviors.withTimers[M] { timers =>
      timers.startSingleTimer(
        makeIterateMessage(),
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }

  /** Perform the regular iteration for a specific block number:
   * read the block from the Ethereum connector, store it into the DB.
   *
   * @return whether syncing of the blocks succeeded.
   */
  protected[this] def syncBlocks(
                                  blocksToSync: dlt.EthereumBlock.BlockNumberRange
                                )(implicit session: DBSession): Boolean = {
    val trackedAddresses: Set[String] = dbStorage.trackedAddresses.getJustAddresses

    logger.debug(s"Processing blocks $blocksToSync with tracked addresses $trackedAddresses")

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

  trait SyncerState

}
