package com.myodov.unicherrygarden.cherrypicker.syncers

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.CherryPicker
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.dlt.EthereumBlock
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{HeadSyncerMessage, IterateHeadSyncer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.{DBStorage, DBStorageAPI}
import scalikejdbc.{DB, DBSession}

import scala.collection.immutable.SortedMap
import scala.language.postfixOps

/** Performs the “Head sync” – syncing the newest blocks, which haven’t been synced yet.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 */
private class HeadSyncer(dbStorage: DBStorageAPI,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends AbstractSyncer[HeadSyncerMessage, HeadSyncer.State, IterateHeadSyncer](
    dbStorage,
    ethereumConnector,
    state = HeadSyncer.State()
  ) {

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  final def launch(): Behavior[HeadSyncerMessage] = {
    Behaviors.setup { context =>
      logger.debug(s"Launching syncer: ${this.getClass.getSimpleName}")

      // Start the iterations
      context.self ! makeIterateMessage

      Behaviors.receiveMessage[HeadSyncerMessage] {
        case IterateHeadSyncer() =>
          logger.debug(s"Iteration in HeadSyncer $state")
          iterate()
        case message@EthereumNodeStatus(current, highest) =>
          logger.debug(s"HeadSyncer received Ethereum node syncing status: $message")
          state.ethereumNodeStatus = Some(message)
          Behaviors.same
        case TailSyncing(optRange) =>
          logger.debug(s"TailSyncer notified us it is going to sync $optRange")
          state.tailSyncStatus = optRange
          Behaviors.same
      }
    }
  }

  @inline override final def makeIterateMessage(): IterateHeadSyncer = IterateHeadSyncer()

  @inline final def reiterateMustCheckReorg(): Behavior[HeadSyncerMessage] = {
    logger.debug("FSM: reiterateMustCheckReorg")
    state.nextIterationMustCheckReorg.set(true)
    reiterateMayCheckReorg
  }

  @inline final def reiterateMayCheckReorg(): Behavior[HeadSyncerMessage] = {
    logger.debug("FSM: reiterateMayCheckReorg")
    reiterate
  }

  @inline final def pauseThenMustCheckReorg(): Behavior[HeadSyncerMessage] = {
    logger.debug("FSM: pauseThenMustCheckReorg")
    state.nextIterationMustCheckReorg.set(true)
    pauseThenMayCheckReorg
  }

  @inline final def pauseThenMayCheckReorg(): Behavior[HeadSyncerMessage] =
    pauseThenReiterate

  @inline override final def pauseThenReiterateOnError(): Behavior[HeadSyncerMessage] =
    pauseThenMustCheckReorg

  override final def iterate(): Behavior[HeadSyncerMessage] = {
    logger.debug(s"FSM: iterate - running an iteration with $state")
    // Since this moment, we may want to use DB in a single atomic DB transaction;
    // even though this will involve querying the Ethereum node, maybe even multiple times.
    DB localTx { implicit session =>
      // For more details on reorg handling phases, read the [[/docs/unicherrypicker-synchronization.md]] document.

      // Do the reorg-rewind phase; see if it already finalizes the behavior to return
      val reorgRewindProvidedBehaviorOpt = withValidatedProgressAndSyncingState[Option[Behavior[HeadSyncerMessage]]](
        dbStorage.progress.getProgress,
        state.ethereumNodeStatus,
        onError = Some(pauseThenReiterateOnError)
      ) { (overallProgress, nodeSyncingStatus) =>
        // Sanity test passed in withValidatedProgressAndSyncingState, node is reachable.
        // Only now we can proceed.
        logger.debug(s"Ethereum node is reachable: $overallProgress, $nodeSyncingStatus")

        // At this stage we either do or do not do the reorg check/rewind operations.
        // Any of the internal reorg/rewind operations may already decide to provide a Behavior
        // (i.e. maybe go to the next iteration).
        // If they provide some final behavior, let’s use it; otherwise, let’s move on to the regular sync
        // Reorg/rewind, phase 2/4: “reorg check check” – should we bother checking for reorg?
        val isReorgCheckNeeded = reorgCheckCheck(overallProgress, nodeSyncingStatus)
        logger.debug(s"Do we need to check for reorg? $isReorgCheckNeeded")

        if (!isReorgCheckNeeded) {
          // No suggested behavior to return; let’s go proceed with the headSync
          None
        } else {
          // Reorg/rewind, phase 3/4: “reorg check” – did reorg happened?
          reorgCheck match {
            case Left(None) =>
              logger.debug("No reorg needed")
              // outer Option is None, meaning we can proceed with syncing
              // (we don’t have a finished Behavior)
              None
            case Left(Some(invalidRange)) =>
              logger.debug(s"Need reorg rewind for $invalidRange")
              // Reorg/rewind, phase 4/4: “rewind”
              if (reorgRewind(invalidRange)) {
                logger.debug(s"We've successfully rewound $invalidRange; let’s go sync")
                // We’ve just completed rewind of some blocks, maybe like 100.
                // It seems obvious to go to `pauseThenReiterateOnError()` phase,..
                // ... but if something broke completely, it may cause rewinding 100 more blocks,
                // and 100 more, and so on. We end up wiping all the database – and that’s definitely
                // not what we want.
                // So right after the rewind, we try to do actual `headSync`;
                // and for this, we return None (as “no Behavior suggested”) here.
                None
              } else {
                logger.error(s"Some error occured in rewinding $invalidRange; pause and retry")
                Some(pauseThenReiterateOnError)
              }
            case Right(error) =>
              logger.error(s"During reorgCheck, there was a problem: $error")
              Some(pauseThenReiterateOnError)
          }
        }
      } // reorgRewindProvidedBehaviorOpt

      // Inside `reorgRewindProvidedBehaviorOpt`, we already have a suggested behavior to return... maybe.
      // If we don’t have it, we just go on with the regular `headSync`.
      // Note that at this point `dbStorage.progress.getProgress` can be inactual (if rewind happened),
      // we cannot trust it and may need to re-read it.
      reorgRewindProvidedBehaviorOpt match {
        case Some(rrwBehavior) =>
          rrwBehavior
        case None =>
          // Reorg/rewind thinks it is okay for us to move on with actual head-syncing;
          // so let’s head-sync what we can.
          // But we should do it after rereading `dbStorage.progress.getProgress`
          // (and `state.ethereumNodeStatus`, just to be sure)!
          withValidatedProgressAndSyncingState[Behavior[HeadSyncerMessage]](
            dbStorage.progress.getProgress,
            state.ethereumNodeStatus,
            onError = pauseThenReiterateOnError
          ) { (overallProgress, nodeSyncingStatus) =>
            headSync(overallProgress, nodeSyncingStatus)
          }
      }
    }
  }

  /** Check if we even need to check the blockchain for reorganization. */
  private[this] final def reorgCheckCheck(
                                           dbProgressData: DBStorage.Progress.ProgressData,
                                           nodeSyncingStatus: EthereumNodeStatus
                                         )(implicit session: DBSession): Boolean = {
    // Atomically get the value and unset it
    if (state.nextIterationMustCheckReorg.getAndSet(false)) {
      logger.debug("On previous iteration, the checkReorg was forced, so we must check for reorg")
      true
    } else {
      // What is the maximum block number in ucg_block? Is it available (are there any blocks)?
      dbProgressData.blocks.to match {
        case None =>
          // There are no blocks stored in the DB, so any reorg couldn’t have affected anything:
          // no check needed
          false
        case Some(maxBlocksNum) =>
          // Reorg check is needed (i.e. “return true”) if the maximum block number stored in the DB
          // is in “danger zone” (closer than `syncers.max_reorg`) from `eth.syncing.currentBlock`.
          maxBlocksNum >= nodeSyncingStatus.currentBlock - CherryPicker.MAX_REORG
      }
    }
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
  private[this] final def reorgCheck()(implicit session: DBSession): Either[Option[dlt.EthereumBlock.BlockNumberRange], String] = {
    val maxReorg = CherryPicker.MAX_REORG
    val blockHashesInDb: SortedMap[Int, String] = dbStorage.blocks.getLatestHashes(maxReorg)
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

  /** Perform the “rewind” for the range of blocks by numbers in `badBlockRange`.
   *
   * @return Whether the rewind executed successfully. `false` means some error occured.
   */
  private[this] final def reorgRewind(
                                       badBlockRange: dlt.EthereumBlock.BlockNumberRange
                                     )(implicit session: DBSession): Boolean = {
    logger.debug(s"Rewinding the blocks $badBlockRange")
    require(
      (badBlockRange.size <= CherryPicker.MAX_REORG) && (badBlockRange.head <= badBlockRange.end),
      (badBlockRange, CherryPicker.MAX_REORG))
    dbStorage.blocks.rewind(badBlockRange.head)
  }

  /** Do the actual head sync syncing phase. */
  private[this] final def headSync(
                                    progress: Progress.ProgressData,
                                    nodeSyncingStatus: EthereumNodeStatus
                                  )(implicit session: DBSession): Behavior[HeadSyncerMessage] = {
    logger.debug(s"Now we are ready to do headSync for progress $progress, node $nodeSyncingStatus")
    // headSync is called from within `withValidatedProgressAndSyncingState`, so we can rely upon
    // overall.from being non-empty
    val overallFrom = progress.overall.from.get

    val syncStartBlock = progress.blocks.to match { // do we have blocks stored? what is maximum stored one?
      case None =>
        logger.info(s"We have no blocks stored; starting from the very first block ($overallFrom)...")
        overallFrom
      case Some(maxStoredBlock) =>
        logger.info(s"We have some blocks stored (${progress.blocks}); syncing from ${maxStoredBlock + 1}")
        maxStoredBlock + 1
    }
    val syncEndBlock = Math.min(syncStartBlock + HeadSyncer.BATCH_SIZE, nodeSyncingStatus.currentBlock)

    val goingToHeadSync: EthereumBlock.BlockNumberRange = syncStartBlock to syncEndBlock

    val tailSyncStatus: Option[dlt.EthereumBlock.BlockNumberRange] = state.tailSyncStatus

    // We are ready to headsync; but maybe we want to brake, to let the tail syncer to catch up
    val shouldCatchUpBrake: Boolean = tailSyncStatus match {
      case None =>
        // Haven’t received anything from TailSyncer, so don’t brake for it
        false
      case Some(tailSyncing) =>
        // We brake if the end of tail syncing operation is close to begin of head sync
        tailSyncing.end >= goingToHeadSync.head - HeadSyncer.CATCH_UP_BRAKE_MAX_LEAD
    }

    if (shouldCatchUpBrake) {
      logger.info(s"Was going to HeadSync $goingToHeadSync; but TailSyncer is close ($tailSyncStatus), so braking")
      pauseThenMayCheckReorg
    } else {
      logger.debug(s"Ready to HeadSync $goingToHeadSync")
      if (syncBlocks(goingToHeadSync)) {
        // HeadSync completed successfully. Should we pause, or instantly go to the next round?

        dbStorage.state.setLastHeartbeatAt
        if (goingToHeadSync.end >= nodeSyncingStatus.currentBlock) { // should be “==” rather than “>=”, but just to be safe
          logger.debug(s"HeadSyncing success $goingToHeadSync, reached end")
          pauseThenMayCheckReorg
        } else {
          logger.debug(s"HeadSyncing success $goingToHeadSync, not reached end")
          reiterateMayCheckReorg // go to the next round instantly
        }
      } else {
        logger.error(s"HeadSyncing failure for $goingToHeadSync")
        pauseThenReiterateOnError
      }
    }
  }
}

/** HeadSyncer companion object. */
object HeadSyncer {

  val BATCH_SIZE = 100 // TODO: must be configured through application.conf
  val CATCH_UP_BRAKE_MAX_LEAD = 10000 // TODO: must be configured through application.conf

  protected final case class State(nextIterationMustCheckReorg: AtomicBoolean = new AtomicBoolean(true),
                                   @volatile var tailSyncStatus: Option[dlt.EthereumBlock.BlockNumberRange] = None)
    extends AbstractSyncer.SyncerState

  /** Main constructor. */
  @inline def apply(dbStorage: DBStorageAPI,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations): Behavior[SyncerMessages.HeadSyncerMessage] =
    new HeadSyncer(dbStorage, ethereumConnector).launch()
}
