package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DBSession

import scala.language.postfixOps

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
//  S <: SyncerState[M],
  IS <: SyncerMessages.IterateSyncer[M] with M
]
(protected[this] val pgStorage: PostgreSQLStorage,
 protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends LazyLogging {

//  /** The overall state of the syncer.
//   *
//   * Unfortunately, we cannot go fully Akka-way having the system state passed through the methods, FSM states
//   * and behaviors. If we receive some state-changing message from outside (e.g. the latest state of Ethereum node
//   * syncing process; or, for HeadSyncer, the message from TailSyncer), we need to alter the state immediately.
//   * But the FSM may be in a 10-second delay after the latest block being processed, and after it a message
//   * with the previous state will be posted by the timer. So alas, `state` has to be a var.
//   */
//  protected[this] var state: S

//  /** Main message handler. */
//  def mainLoop(state: S): Behavior[M]

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


  //  def launch(): Behavior[SyncerMessages.Message] = {
  //
  //    logger.debug(s"Launching ${this.getClass.getName}")
  //    Behaviors.withTimers[Message] { timers: TimerScheduler[Message] =>
  //      timers.startTimerWithFixedDelay(
  //        Iterate(),
  //        startDelay,
  //        iterationDelay,
  //      )
  //      // due to initialDelay = 0 seconds, it also sends this message, instantly, too.
  //      Behaviors.receiveMessage[Message] {
  //        case Iterate() =>
  //          nextIteration()
  //          Behaviors.same
  //      }
  //    }
  //  }

  //  protected def nextIteration(): Unit

  /** Perform the regular iteration for a specific block number:
   * read the block from the Ethereum connector, store it into the DB.
   */
  private[this] def syncBlock(blockToSync: Int)(implicit session: DBSession): Unit = {
    val trackedAddresses: Set[String] = pgStorage.trackedAddresses.getJustAddresses

    logger.debug(s"processing block $blockToSync with tracked addresses $trackedAddresses")

    ethereumConnector.readBlock(blockToSync, trackedAddresses) match {
      case None => logger.error(s"Cannot read block $blockToSync")
      case Some((block, transactions)) => {
        logger.debug(s"Reading block $block: txes $transactions")

        val thisBlockInDbOpt = pgStorage.blocks.getBlockByNumber(block.number)
        val prevBlockInDbOpt = pgStorage.blocks.getBlockByNumber(block.number - 1)

        logger.debug(s"Storing block: $block; " +
          s"block may be present as $thisBlockInDbOpt, " +
          s"parent may be present as $prevBlockInDbOpt")

        (thisBlockInDbOpt, prevBlockInDbOpt) match {
          case (None, None) => {
            // This is the simplest case: this is probably the very first block in the DB
            logger.debug(s"Adding first block ${block.number}: " +
              s"neither it nor previous block exist in the DB")
            pgStorage.blocks.addBlock(block.withoutParentHash)
          }
          case (None, Some(prevBlockInDb)) if prevBlockInDb.hash == block.parentHash.get => {
            // Another simplest case: second and further blocks in the DB.
            // Very new block, and its parent matches the existing one
            logger.debug(s"Adding new block ${block.number}; parent block ${block.number - 1} " +
              s"exists already with proper hash")
            pgStorage.blocks.addBlock(block)
          }
          case (Some(thisBlockInDb), _) if thisBlockInDb.hash == block.hash => {
            logger.debug(s"Block ${block.number} exists already in the DB " +
              s"with the same hash ${block.hash}; " +
              "no need to readd the block itself")
          }
          case (Some(thisBlockInDb), _) if thisBlockInDb.hash != block.hash => {
            logger.debug(s"Block ${block.number} exists already in the DB " +
              s"but with ${thisBlockInDb.hash} rather than ${block.hash}; " +
              "need to wipe some blocks!")
            throw new RuntimeException("TODO")
          }
          case (None, Some(prevBlockInDb)) if prevBlockInDb.hash != block.parentHash.get => {
            logger.debug(s"Adding new block ${block.number}: " +
              s"expecting parent block to be ${prevBlockInDb.hash} but it is ${block.parentHash.get}; " +
              "need to wipe some blocks!")
            throw new RuntimeException("TODO")
          }
        }
        logger.debug(s"Now trying to store the transactions: $transactions")
        for (tx <- transactions) {
          pgStorage.transactions.addTransaction(tx, block.hash)
          pgStorage.txlogs.addTxLogs(block.number, tx.txhash, tx.txLogs)
        }
        pgStorage.state.advanceProgress(block.number, trackedAddresses)
      }
    }
  }
}

object AbstractSyncer {
  trait SyncerState
}
