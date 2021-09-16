package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.language.postfixOps

/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB. */
class CherryPicker(protected[this] val pgStorage: PostgreSQLStorage,
                   protected[this] val ethereumConnector: EthereumRpcSingleConnector) extends LazyLogging {

  /** What should happen within each iteration. */
  private[this] def insideEachIteration(): Unit = {
    // It is `synchronized`, so that if some iteration takes too long, the next iteration won’t intervene
    // and work on the partially-updated state.
    this.synchronized {
      logger.debug("Iteration...")
      val optProgress = pgStorage.progress.getProgress
      lazy val progress = optProgress.get
      lazy val overallFrom = progress.overall.from.get // only if overall.from is not Empty

      if (optProgress.isEmpty) {
        logger.error("Cannot get the progress, something failed!")
        pgStorage.state.setSyncState("Cannot get the progress state!")
      } else if (progress.overall.from.isEmpty) {
        logger.warn("CherryPicker is not configured: missing `ucp_state.synced_from_block_number`!");
      } else if (progress.currencies.minSyncFrom.exists(_ < overallFrom)) {
        logger.error("The minimum `ucp_currency.sync_from_block_number` value " +
          s"is ${progress.currencies.minSyncFrom.get}; " +
          s"it should not be lower than $overallFrom!")
      } else if (progress.trackedAddresses.minFrom < overallFrom) {
        logger.error("The minimum `ucp_tracked_address.synced_from_block_number` value " +
          s"is ${progress.trackedAddresses.minFrom}; " +
          s"it should not be lower than $overallFrom!")
      } else {
        // optProgress is not Empty
        // progress.overall.from is not Empty

        if (progress.trackedAddresses.toHasNulls || progress.perCurrencyTrackedAddresses.toHasNulls) {
          // Some of tracked_addresses (or currency/tracked address M2Ms) have never been synced.
          // Find the earliest of them and sync.
          logger.debug(s"Progress is: $progress")

          pgStorage.progress.getFirstBlockResolvingSomeUnsyncedPCTAddress match {
            case None => logger.error(s"Progress is $progress and some tracked addresses are untouched, but could not find them")
            case Some(blockToSync) => {
              pgStorage.state.setSyncState(s"Resyncing untouched addresses: block $blockToSync")
              logger.debug(s"processing block $blockToSync")

              ethereumConnector.readBlock(blockToSync, filterAddresses = Set.empty, filterCurrencies = Set.empty) match {
                case None => logger.error(s"Cannot read block $blockToSync")
                case Some(readBlock) => {
                  logger.debug(s"Reading block $readBlock")
                }
              }
            }
          }
        }
      }
    }
  }

  /** Run next iteration; and reschedule it. */
  def nextIteration(): Unit = {
    try {
      insideEachIteration()
    } catch {
      case e: Throwable => logger.error("On iteration, got a error", e)
    }

    pgStorage.state.setLastHeartbeatAt
  }
}


/** Akka actor to run CherryPicker operations. */
object CherryPicker extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrypicker.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  trait CherryPickerMessage

  /** A message informing CherryPicker it needs to run a next iteration. */
  final case class Iterate() extends CherryPickerMessage

  //  private val ITERATION_PERIOD = 15 seconds
  protected val ITERATION_PERIOD = 1 second

  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector): Behavior[CherryPickerMessage] = {

    val picker = new CherryPicker(pgStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryPicker: v. $propVersionStr, built at $propBuildTimestampStr")

      logger.debug("Setting up CherryPicker actor with timers")
      Behaviors.withTimers(timers => {
        def scheduleNextIteration() = timers.startSingleTimer(Iterate(), ITERATION_PERIOD)

        def handleIteration(): Behaviors.Receive[CherryPickerMessage] = Behaviors.receiveMessage {
          (message: CherryPickerMessage) => {
            logger.debug("Receiving CherryPicker message")
            message match {
              case Iterate() => {
                picker.nextIteration()
              }
              case unknownMessage => {
                logger.error(s"Unexpected message $unknownMessage")
              }
            }
            scheduleNextIteration
            Behaviors.same
          }
        }

        //      scheduleNextIteration
        handleIteration()
      })
    }
  }
}
