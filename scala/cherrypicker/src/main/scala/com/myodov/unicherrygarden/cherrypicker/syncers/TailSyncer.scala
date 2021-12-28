package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{IterateTailSyncer, TailSyncerMessage, TailSyncing}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import scalikejdbc.{DB, DBSession}

import scala.language.postfixOps

/** Performs the “Tail sync” – (re)syncing the older blocks, which have to be resynced
 * due to some currencies or tokens added.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 * @param headSyncer the actor of HeadSyncer, to which this TailSyncer will report about its syncing plans.
 */
private class TailSyncer(dbStorage: DBStorageAPI,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
                        (headSyncer: ActorRef[TailSyncing])
  extends AbstractSyncer[TailSyncerMessage, TailSyncer.State, IterateTailSyncer](
    dbStorage,
    ethereumConnector,
    state = TailSyncer.State()
  ) {

  import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages._

  final def launch(): Behavior[TailSyncerMessage] = {
    logger.debug(s"Launching syncer: ${this.getClass.getSimpleName}")

    // Then go to the mainLoop, with the initial state
    Behaviors.setup[TailSyncerMessage] { context =>
      // Start the iterations
      context.self ! makeIterateMessage

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

  @inline override final def makeIterateMessage(): IterateTailSyncer = IterateTailSyncer()

  @inline override final def pauseThenReiterateOnError(): Behavior[TailSyncerMessage] =
    pauseThenReiterate

  override final def iterate(): Behavior[TailSyncerMessage] = {
    logger.debug(s"FSM: iterate - running an iteration with $state")
    // Since this moment, we may want to use DB in a single atomic DB transaction;
    // even though this will involve querying the Ethereum node, maybe even multiple times.
    DB localTx { implicit session =>
      withValidatedProgressAndSyncingState[Behavior[TailSyncerMessage]](
        dbStorage.progress.getProgress,
        state.ethereumNodeStatus,
        onError = pauseThenReiterateOnError
      ) { (overallProgress, nodeSyncingStatus) =>
        // Sanity test passed, node is reachable. Only here we can proceed.
        logger.debug(s"Ethereum node is reachable: $overallProgress, $nodeSyncingStatus")
        tailSync(overallProgress, nodeSyncingStatus)
      }
    }
  }

  /** Do the actual tail sync syncing phase. */
  private[this] final def tailSync(
                                    progress: Progress.ProgressData,
                                    nodeSyncingStatus: EthereumNodeStatus
                                  )(implicit session: DBSession): Behavior[TailSyncerMessage] = {

    val syncStartBlock = 0 // TODO
    val syncEndBlock = Math.min(syncStartBlock + HeadSyncer.BATCH_SIZE, nodeSyncingStatus.currentBlock)

    if (syncEndBlock == nodeSyncingStatus.currentBlock) {
      // We actually reached HeadSync position.

      logger.debug("TailSyncer reached the HeadSyncer; let HeadSyncer go on (if it was on brake)")
      headSyncer ! TailSyncing(None)
      pauseThenReiterate()
    } else {
      Behaviors.empty // TODO!
    }
  }
}

/** TailSyncer companion object. */
object TailSyncer {

  val BATCH_SIZE = 100 // TODO: must be configured through application.conf

  protected final case class State()
    extends AbstractSyncer.SyncerState

  /** Main constructor.
   *
   * @param headSyncer the actor of HeadSyncer, to which this TailSyncer will report about its syncing plans.
   */
  @inline def apply(dbStorage: DBStorageAPI,
                    ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations,
                    headSyncer: ActorRef[TailSyncing]): Behavior[TailSyncerMessage] =
    new TailSyncer(dbStorage, ethereumConnector)(headSyncer).launch()
}
