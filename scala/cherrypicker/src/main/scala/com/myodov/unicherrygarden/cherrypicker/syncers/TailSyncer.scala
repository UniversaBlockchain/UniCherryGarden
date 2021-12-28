package com.myodov.unicherrygarden.cherrypicker.syncers

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.cherrypicker.syncers.SyncerMessages.{IterateTailSyncer, TailSyncerMessage}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import scalikejdbc.DB

import scala.language.postfixOps

/** Performs the “Tail sync” – (re)syncing the older blocks, which have to be resynced
 * due to some currencies or tokens added.
 *
 * @note For more details please read [[/docs/unicherrypicker-synchronization.md]] document.
 * @param headSyncer the actor of HeadSyncer, to which this TailSyncer will report about its syncing plans.
 */
private class TailSyncer(dbStorage: DBStorageAPI,
                         ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
                        (headSyncer: ActorRef[SyncerMessages.HeadSyncerMessage])
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

  override final def iterate(): Behavior[TailSyncerMessage] = {
    logger.debug(s"FSM: iterate - running an iteration with $state")
    // Since this moment, we may want to use DB in a single atomic DB transaction;
    // even though this will involve querying the Ethereum node, maybe even multiple times.
    DB localTx { implicit session =>
      (dbStorage.progress.getProgress, state.ethereumNodeStatus) match {
        case (None, _) =>
          // we could not even get the DB progress – go to the next round
          logger.error("Some unexpected error when reading the overall progress from the DB")
          pauseThenReiterate
        case (_, None) =>
          // we haven’t received the syncing state from the node
          logger.error("Could not read the syncing status from Ethereum node")
          pauseThenReiterate
        case (Some(overallProgress: Progress.ProgressData), Some(nodeSyncingStatus: EthereumNodeStatus))
          if !isNodeReachable(overallProgress, nodeSyncingStatus) =>
          // Does the overall data sanity allows us to proceed?
          // No, sanity test failed
          logger.error(s"Ethereum node is probably unavailable: $overallProgress, $nodeSyncingStatus")
          pauseThenReiterate
        case (Some(overallProgress: Progress.ProgressData), Some(nodeSyncingStatus: EthereumNodeStatus)) =>
          // Sanity test passed, node is reachable. Only here we can proceed.
          logger.debug(s"Ethereum node is reachable: $overallProgress, $nodeSyncingStatus")

          Behaviors.empty // TODO!
      }
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
                    headSyncer: ActorRef[SyncerMessages.HeadSyncerMessage]): Behavior[SyncerMessages.TailSyncerMessage] =
    new TailSyncer(dbStorage, ethereumConnector)(headSyncer).launch()
}
