package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.api.dlt.Asset
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResult
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResult.CurrencyBalanceFact
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.Response
import com.myodov.unicherrygarden.messages.cherrypicker.{AddTrackedAddresses, GetBalances, GetTrackedAddresses}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.control.NonFatal


/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB. */
class CherryPicker(protected[this] val pgStorage: PostgreSQLStorage,
                   protected[this] val ethereumConnector: EthereumRpcSingleConnector) extends LazyLogging {

  /** What should happen within each iteration. */
  private[this] def insideEachIteration(): Unit = {
    // It is `synchronized`, so that if some iteration takes too long, the next iteration won’t intervene
    // and work on the partially-updated state.
    this.synchronized {
      logger.debug("Iteration...")

      // First, let's ask the Ethereum node what's the status of the syncing process

      val syncingStatusOpt = ethereumConnector.syncingStatus
      val blockNumberOpt = ethereumConnector.latestSyncedBlockNumber

      if (syncingStatusOpt.isEmpty || blockNumberOpt.isEmpty) {
        logger.error(s"Cannot get eth.syncing ($syncingStatusOpt) / eth.blockNumber ($blockNumberOpt)!")
        pgStorage.state.setSyncState("Cannot connect to Ethereum node!")
      } else {
        val ethSyncing = syncingStatusOpt.get
        val ethBlockNumber: Int = blockNumberOpt.get.bigInteger.intValueExact

        logger.debug(s"Found eth.syncing ($ethSyncing) / eth.blockNumber ($ethBlockNumber)!")

        // If eth.syncing returned False, i.e. we are not syncing – we should assume currentBlock and highestBlock
        // are equal to eth.blockNumber
        val (currentBlock, highestBlock) = {
          if (ethSyncing.isStillSyncing)
            (ethSyncing.currentBlock, ethSyncing.highestBlock)
          else
            (ethBlockNumber, ethBlockNumber)
        }

        pgStorage.state.setEthNodeData(ethBlockNumber, currentBlock, highestBlock)

        // Since this moment, we may want to use DB in a single atomic DB transaction
        DB localTx { implicit session =>
          val optProgress = pgStorage.progress.getProgress
          lazy val progress = optProgress.get
          lazy val overallFrom = progress.overall.from.get // only if overall.from is not Empty

          if (optProgress.isEmpty) {
            logger.error("Cannot get the progress, something failed!")
            pgStorage.state.setSyncState("Cannot get the progress state!")
          } else if (progress.overall.from.isEmpty) {
            logger.warn("CherryPicker is not configured: missing `ucg_state.synced_from_block_number`!");
          } else if (progress.currencies.minSyncFrom.exists(_ < overallFrom)) {
            logger.error("The minimum `ucg_currency.sync_from_block_number` value " +
              s"is ${progress.currencies.minSyncFrom.get}; " +
              s"it should not be lower than $overallFrom!")
          } else if (progress.trackedAddresses.minFrom < overallFrom) {
            logger.error("The minimum `ucg_tracked_address.synced_from_block_number` value " +
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

                  val trackedAddresses: Set[String] = pgStorage.trackedAddresses.getJustAddresses
                  val currencies: Set[Asset] = pgStorage.currencies.getCurrencies.map(_.asAsset).toSet

                  logger.debug(s"processing block $blockToSync " +
                    s"with tracked addresses $trackedAddresses, " +
                    s"currencies $currencies")

                  ethereumConnector.readBlock(blockToSync, trackedAddresses, currencies) match {
                    case None => logger.error(s"Cannot read block $blockToSync")
                    case Some((block, transactions, transfers)) => {
                      logger.debug(s"Reading block $block: txes $transactions, transfers $transfers")

                      logger.debug(s"Storing block: $block")
                      pgStorage.blocks.addBlock(block.withoutParentHash)
                    }
                  }
                }
              }
            }
          }
        } // DB localTx
      }
    }
  }

  /** Run next iteration; and reschedule it. */
  def nextIteration(): Unit = {
    try {
      insideEachIteration()
    } catch {
      case NonFatal(e) => logger.error("On iteration, got a error", e)
    }

    pgStorage.state.setLastHeartbeatAt
  }
}


/** Akka actor to run CherryPicker operations. */
object CherryPicker extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrypicker.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  /** A message informing CherryPicker it needs to run a next iteration. */
  final case class Iterate() extends CherryPickerRequest

    private val ITERATION_PERIOD = 15 seconds
//  private val ITERATION_PERIOD = 1 second

  /** Object constructor. */
  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector): Behavior[CherryPickerRequest] = {

    val picker = new CherryPicker(pgStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryPicker: v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(GetTrackedAddresses.SERVICE_KEY, context.self)
      context.system.receptionist ! Receptionist.Register(AddTrackedAddresses.SERVICE_KEY, context.self)
      context.system.receptionist ! Receptionist.Register(GetBalances.SERVICE_KEY, context.self)

      logger.debug("Setting up CherryPicker actor with timers")
      Behaviors.withTimers(timers => {
        // On startup, schedule a single iteration;
        // It will re-schedule itself when/if needed.
        // We don’t setup a “regular” timer because sometimes a new iteration should happen without a pause;
        // and sometimes a new iteration should happen not 5 (or so) seconds after the previous iteration *started*,
        // but 5 seconds after a previous iteration *completed*.
        // The latter though can be resolved by scheduleWithFixedDelay.
        logger.error("Running first iteration of CherryPicker...")
        context.self ! Iterate() // TODO: enable to start iterations

        Behaviors.receiveMessage {
          case Iterate() => {
            picker.nextIteration()
            timers.startSingleTimer(Iterate(), ITERATION_PERIOD)
            Behaviors.same
          }
          case message: GetTrackedAddresses.Request => {
            logger.debug(s"Receiving GetTrackedAddresses message: $message")
            val payload: GetTrackedAddresses.GTARequestPayload = message.payload

            val results: List[Response.TrackedAddressInformation] = pgStorage
              .trackedAddresses
              .getTrackedAddresses(
                payload.includeComment,
                payload.includeSyncedFrom,
                payload.includeSyncedTo)
              .map(item => new Response.TrackedAddressInformation(
                item.address,
                // The subsequent items may be Java-nullable
                item.comment.orNull,
                // Converting the Option[Int] to nullable Java Integers needs some cunning
                item.syncedFrom.map(Integer.valueOf).orNull,
                item.syncedTo.map(Integer.valueOf).orNull)
              )

            val response = new GetTrackedAddresses.Response(
              results.asJava,
              payload.includeComment,
              payload.includeSyncedFrom,
              payload.includeSyncedTo
            )
            logger.debug(s"Replying with $response")
            message.replyTo ! response
            Behaviors.same
          }
          case message: AddTrackedAddresses.Request => {
            logger.debug(s"Receiving AddTrackedAddresses message: $message")
            val payload: AddTrackedAddresses.ATARequestPayload = message.payload

            // Here goes the list of all added addresses
            // (as Options; with Option.empty instead of any address that failed to add)
            val addressesMaybeAdded: List[Option[String]] = (
              for (addr: AddTrackedAddresses.AddressDataToTrack <- payload.addressesToTrack.asScala.toList)
                yield {
                  if (pgStorage.trackedAddresses.addTrackedAddress(
                    addr.address,
                    Option(addr.comment),
                    payload.trackingMode,
                    Option(payload.fromBlock)
                  )) {
                    Option(addr.address)
                  } else {
                    Option.empty[String]
                  }
                }
              )

            // Flatten it to just the added elements
            val addressesActuallyAdded: Set[String] = addressesMaybeAdded.flatten.toSet
            logger.debug(s"Actually added the following addresses to watch: $addressesActuallyAdded")

            val response = new AddTrackedAddresses.Response(
              addressesActuallyAdded.asJava
            )
            logger.debug(s"Replying with $response")
            message.replyTo ! response
            Behaviors.same
          }
          case message: GetBalances.Request => {
            logger.debug(s"Receiving GetBalances message: $message")
            message.replyTo ! new GetBalances.Response(
              new BalanceRequestResult(
                false,
                //                List[CurrencyBalanceFact](
                //                  new CurrencyBalanceFact(
                //                    Currency.newEthCurrency(),
                //                    BigDecimal(123.45).underlying(),
                //                    BalanceRequestResult.CurrencyBalanceFact.BalanceSyncState.SYNCED_TO_LATEST_UNICHERRYGARDEN_TOKEN_STATE,
                //                    15
                //                  )
                //                ).asJava,
                List.empty[CurrencyBalanceFact].asJava,
                0,
                0,
                0)
            )
            Behaviors.same
          }
          case unknownMessage => {
            logger.error(s"Unexpected message $unknownMessage")
            Behaviors.same
          }
        }
      })
    }
  }
}
