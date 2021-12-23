package com.myodov.unicherrygarden.cherrypicker.syncers

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{EthereumNodeStatus, GoingToTailSync, HeadSyncerMessage, IterateHeadSyncer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import scalikejdbc.{DB, DBSession}

import scala.annotation.switch
import scala.collection.immutable.SortedMap
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
   * with the previous state will be posted by the timer. So alas, `state` has to be variable.
   */
  private[this] val state: HeadSyncer.State = HeadSyncer.State() // initialized with default state

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  def launch(): Behavior[HeadSyncerMessage] = {
    Behaviors.setup { context =>
      logger.debug(s"Syncer mainloop: ${this.getClass.getSimpleName}")

      // the initial value is anyway to “must check reorg”, but let’s just be explicit
      //      iterateMustCheckReorg()
      // Start the iterations
      context.self ! makeIterateMessage

      Behaviors.receiveMessage[HeadSyncerMessage] {
        case IterateHeadSyncer() =>
          logger.debug(s"Iteration in HeadSyncer $state")
          iterate()
        case message@EthereumNodeStatus(current, highest) =>
          logger.debug(s"HeadSyncer received Ethereum node syncing status: $message")
          state.synchronized {
            state.ethereumNodeStatus = Some(message)
          }
          Behaviors.same
        case message@GoingToTailSync(range) =>
          logger.debug(s"TailSyncer notified us it is going to sync $range.")
          state.synchronized {
            state.tailSyncStatus = Some(message);
          }
          Behaviors.same
      }
    }
  }

  override def makeIterateMessage(): IterateHeadSyncer = IterateHeadSyncer()

  def iterateMustCheckReorg(): Behavior[HeadSyncerMessage] = {
    logger.debug("iterateMustCheckReorg")
    state.synchronized {
      state.nextIterationMustCheckReorg.set(true)
    }
    iterateMayCheckReorg()
  }

  def iterateMayCheckReorg(): Behavior[HeadSyncerMessage] = {
    logger.debug("iterateMayCheckReorg")
    Behaviors.setup { context =>
      logger.debug("Sending iterate message to self")
      context.self ! makeIterateMessage
      Behaviors.same
    }
  }

  def pauseThenMustCheckReorg(): Behavior[HeadSyncerMessage] = {
    state.synchronized {
      state.nextIterationMustCheckReorg.set(true)
    }
    Behaviors.withTimers[HeadSyncerMessage] { timers =>
      timers.startSingleTimer(
        makeIterateMessage,
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }
  }

  def pauseThenMayCheckReorg(): Behavior[HeadSyncerMessage] =
    Behaviors.withTimers[HeadSyncerMessage] { timers =>
      timers.startSingleTimer(
        makeIterateMessage,
        CherryPicker.BLOCK_ITERATION_PERIOD)
      Behaviors.same
    }

  override def iterate(): Behavior[HeadSyncerMessage] = {
    logger.debug(s"Iterating with $state")
    // Atomically get the value and unset it
    val mustCheckReorg = state.nextIterationMustCheckReorg.getAndSet(false)
    logger.debug(s"Do we need to check for reorg? $mustCheckReorg")
    if (mustCheckReorg) {
      // Since this moment, we may want to use DB in a single atomic DB transaction;
      // even though this will involve querying the Ethereum node, maybe even multiple times.
      DB localTx { implicit session =>
        val checkReorgResult = checkReorg
        (checkReorgResult: @switch) match {
          case Left(None) =>
            logger.debug("No reorg needed")
            Behaviors.unhandled // TODO
          case Left(Some(invalidRange)) =>
            logger.debug(s"Need reorg for $invalidRange")
            Behaviors.unhandled // TODO
          case Right(error) =>
            logger.error(s"During checkReorg, there was a problem: $error")
            pauseThenMustCheckReorg()
        }
      }
    }
    Behaviors.unhandled // TODO
  }

  /** Check if the blockchain reorganization happened.
   *
   * @return [[Either]] the data of reorganization (left); or the error message if any error occured during the check.
   *         The data of reorganization (Either.left) contains the following:
   *         <ul>
   *         <li>`None` if no reorganization occured.</li>
   *         <li>`Some(BlockRange)` containing the affected block range (which must be completely wiped)
   *         if the reorg happened, and this BlockRange was affected.</li>
   *         </ul>
   */
  private[this] def checkReorg()(implicit session: DBSession): Either[Option[dlt.EthereumBlock.BlockNumberRange], String] = {
    val maxReorg = CherryPicker.MAX_REORG
    val blockHashesInDb: SortedMap[Int, String] = pgStorage.blocks.getLatestHashes(maxReorg)
    logger.debug(s"We have ${blockHashesInDb.size} blocks stored in DB, checking for reorg sized $maxReorg")
    if (blockHashesInDb.isEmpty) {
      // We don’t have any blocks stored yet, so no reorg check needed; but this is valid
      Left(None)
    } else {
      val firstInDb = blockHashesInDb.keys.head
      val lastInDb = blockHashesInDb.keys.last
      ethereumConnector.readBlockHashes(firstInDb to lastInDb) match {
        case None =>
          logger.error(s"Asked to read the hashes $firstInDb to $lastInDb but failed")
          Right(s"Failure during reading blocks $firstInDb to $lastInDb from Ethereum node")
        case Some(blockHashesInBlockchain) =>
          logger.debug(s"Checking DB hashes $blockHashesInDb against blockchain hashes $blockHashesInBlockchain")
          // Loop over the block numbers in `blockHashesInDb` (i.e. block numbers actually stored in DB);
          // Find first block which isn’t either even stored in Ethereum; or stored but the hash mismatches.
          val firstInvalidBlockOpt = blockHashesInDb.keys.find { blNum =>
            blockHashesInBlockchain.get(blNum) match {
              case None =>
                true // We found a traitor! This block from DB doesn’t even exist in blockchain
              case Some(blockHashInBlockchain) =>
                // Block is bad – i.e. the predicate is true – if the hash in blockchain mismatches the hash in DB
                blockHashInBlockchain != blockHashesInDb(blNum)
            }
          }
          logger.debug(s"Block hash mismatch lookup result: $firstInvalidBlockOpt")
          // Now return the result
          firstInvalidBlockOpt match {
            case None =>
              // checkReorg successful but no invalid blocks
              Left(None)
            case Some(rewindStartBlock) =>
              // checkReorg successful but found some blocks to fix
              Left(Some(rewindStartBlock to lastInDb))
          }
      }
    }
  }
}

object HeadSyncer {

  val BATCH_SIZE = 100 // TODO: must be configured through application.conf
  val CATCH_UP_BRAKE_MAX_LEAD = 100 // TODO: must be configured through application.conf

  private final case class State(var ethereumNodeStatus: Option[EthereumNodeStatus] = None,
                                 nextIterationMustCheckReorg: AtomicBoolean = new AtomicBoolean(true),
                                 var tailSyncStatus: Option[GoingToTailSync] = None)
    extends AbstractSyncer.SyncerState

  /** Main constructor. */
  @inline def apply(pgStorage: PostgreSQLStorage,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations): Behavior[SyncerMessages.HeadSyncerMessage] =
    new HeadSyncer(pgStorage, ethereumConnector).launch()
}
