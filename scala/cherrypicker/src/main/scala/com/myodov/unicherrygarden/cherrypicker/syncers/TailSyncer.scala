package com.myodov.unicherrygarden.cherrypicker.syncers

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.dlt.EthereumBlock
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{EthereumNodeStatus, IterateTailSyncer, TailSyncerMessage, TailSyncing}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import scalikejdbc.{DB, DBSession}

import scala.concurrent.duration.Duration
import scala.language.postfixOps

/** Performs the “Tail sync” – (re)syncing the older blocks, which have to be resynced
 * due to some currencies or tokens added.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 * @param maxReorg   maximum length of reorganization in Ethereum blockchain that we support and allow.
 * @param headSyncer the actor of HeadSyncer, to which this TailSyncer will report about its syncing plans.
 */
private class TailSyncer(dbStorage: DBStorageAPI,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                         maxReorg: Int)
                        (batchSize: Int,
                         headSyncer: ActorRef[TailSyncing])
  extends AbstractSyncer[TailSyncerMessage, TailSyncer.State, IterateTailSyncer](
    dbStorage,
    ethereumConnector,
    state = TailSyncer.State()
  ) {

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  final def launch(): Behavior[TailSyncerMessage] = {
    logger.debug(s"FSM: launch - ${this.getClass.getSimpleName}")

    // Then go to the mainLoop, with the initial state
    Behaviors.setup[TailSyncerMessage] { context =>
      // Start the iterations
      context.self ! iterateMessage

      Behaviors.receiveMessage[TailSyncerMessage] {
        case IterateTailSyncer() =>
          logger.debug(s"Iteration in TailSyncer $state")
          iterate()
        case message@EthereumNodeStatus(current, highest) =>
          logger.debug(s"TailSyncer received Ethereum node syncing status: $message")
          state.ethereumNodeStatus = Some(message)
          Behaviors.same
      }
    }
  }

  @inline override val iterateMessage: IterateTailSyncer = IterateTailSyncer()

  @inline override final def pauseThenReiterateOnError(): Behavior[TailSyncerMessage] =
    pauseThenReiterate()

  override final def iterate(): Behavior[TailSyncerMessage] = {
    logger.debug(s"FSM: iterate - running an iteration with $state")
    // Since this moment, we may want to use DB in a single atomic DB transaction;
    // even though this will involve querying the Ethereum node, maybe even multiple times.
    DB localTx { implicit session =>
      val iterationStartTime = System.nanoTime

      withValidatedProgressAndSyncingState[Behavior[TailSyncerMessage]](
        dbStorage.progress.getProgress,
        state.ethereumNodeStatus,
        onError = pauseThenReiterateOnError
      ) { (overallProgress, nodeSyncingStatus) =>
        // Sanity test passed, node is reachable. Only here we can proceed.
        logger.debug(s"Ethereum node is reachable: $overallProgress, $nodeSyncingStatus")
        tailSync(overallProgress, nodeSyncingStatus, iterationStartTime)
      }
    }
  }

  /** Do the actual  tail sync syncing phase.
   *
   * Due to being called from `withValidatedProgressAndSyncingState`,
   * `progress.overall.from` may be safely assumed non-None.
   *
   * @param iterationStartNanotime : the time of iteration start (to estimate the performance);
   *                               result of calling `System.nanoTime` at the beginning.
   */
  private[this] final def tailSync(
                                    progress: Progress.ProgressData,
                                    nodeSyncingStatus: EthereumNodeStatus,
                                    iterationStartNanotime: Long
                                  )(implicit session: DBSession): Behavior[TailSyncerMessage] = {
    val overallFrom = progress.overall.from.get // `progress.overall.from` safely assumed non-None

    // There may be multiple options of blocks to choose:
    // 1. Just the next block to read.
    val firstUnreadBlock: Option[Int] = progress.blocks.to.map(_ + 1)
    // 2. We never started some (currency, tracked address) pair?
    // Use the smallest from_block (from either currency or tracked address).
    val firstNeverCTAStartedBlock: Option[Int] = dbStorage.progress.getFirstBlockResolvingSomeNeverStartedCTAddress
    // 3. We never completed some (currency, tracked address) pair?
    // Use the least from_block (from either currency or tracked address).
    val firstNeverCTASyncedBlock: Option[Int] = dbStorage.progress.getFirstBlockResolvingSomeNeverSyncedCTAddress
    // 4. Some of CTA to-blocks is smaller than others?
    // Use it.
    val firstMismatchingCTAToBlock: Option[Int] = (progress.perCurrencyTrackedAddresses.minTo, progress.perCurrencyTrackedAddresses.maxTo) match {
      case (Some(minTo), Some(maxTo)) if minTo < maxTo =>
        Some(minTo + 1)
      case _ =>
        None
    }

    val blocksToCompare = List(
      firstUnreadBlock,
      firstNeverCTAStartedBlock,
      firstNeverCTASyncedBlock,
      firstMismatchingCTAToBlock,
    )

    logger.debug(s"Progress is $progress: choosing between $blocksToCompare; headsyncer will start from ${progress.headSyncerStartBlock}")

    val syncStartBlock = blocksToCompare.flatten.minOption.getOrElse(overallFrom)
    val syncEndBlock = Math.min(syncStartBlock + batchSize - 1, nodeSyncingStatus.currentBlock)

    (syncStartBlock, syncEndBlock) match {
      case (start, endSmallerThanStart) if endSmallerThanStart < start =>
        logger.error(s"When choosing between blocks $blocksToCompare to tailsync, found $syncStartBlock/$syncEndBlock: " +
          "end block is earlier than start!")
        pauseThenReiterateOnError()
      case (startReachedHeadSync, end) if progress.headSyncerStartBlock == Some(startReachedHeadSync) =>
        // We actually reached HeadSync position
        logger.debug(s"When choosing between blocks $blocksToCompare to tailsync, found $syncStartBlock/$syncEndBlock: " +
          s"deciding to tailsync since $startReachedHeadSync which reached HeadSyncer ($progress); " +
          "let HeadSyncer go on (if it was on brake)")
        headSyncer ! TailSyncing(None)
        pauseThenReiterate()
      case (validStart, validEnd) =>
        val tailSyncingRange: EthereumBlock.BlockNumberRange = validStart to validEnd
        logger.debug(s"Ready to TailSync $tailSyncingRange")
        // Inform HeadSyncer early, so maybe it will brake early
        headSyncer ! TailSyncing(Some(tailSyncingRange))

        // Do the actual syncing

        if (syncBlocks(tailSyncingRange)) {
          // TailSync completed successfully. Should we pause, or instantly go to the next round?
          dbStorage.state.setLastHeartbeatAt

          val iterationDuration = Duration(System.nanoTime - iterationStartNanotime, TimeUnit.NANOSECONDS)
          val durationStr = s"${iterationDuration.toMillis} ms"

          if (validEnd == nodeSyncingStatus.currentBlock) {
            logger.info(s"TailSyncer reached eth.syncing.currentBlock! Let HeadSyncer go on.")
            headSyncer ! TailSyncing(None)
            pauseThenReiterate()
          } else {
            logger.debug(s"TailSyncer just synced $tailSyncingRange (${tailSyncingRange.size} blocks) in $durationStr; " +
              "let's immediately proceed")
            reiterate() // go to the next round instantly
          }
        } else {
          logger.error(s"TailSyncing failure for $tailSyncingRange")
          pauseThenReiterateOnError
        }
    }
  }
}

/** TailSyncer companion object. */
object TailSyncer {

  protected final case class State(@volatile override var ethereumNodeStatus: Option[EthereumNodeStatus] = None)
    extends AbstractSyncer.SyncerState

  /** Main constructor.
   *
   * @param headSyncer the actor of HeadSyncer, to which this TailSyncer will report about its syncing plans.
   */
  @inline def apply(dbStorage: DBStorageAPI,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                    maxReorg: Int)
                   (batchSize: Int,
                    headSyncer: ActorRef[TailSyncing]): Behavior[TailSyncerMessage] =
    new TailSyncer(dbStorage, ethereumConnector, maxReorg)(batchSize, headSyncer).launch()
}
