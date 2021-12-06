package com.myodov.unicherrygarden

import java.util.concurrent.TimeUnit

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.ReiterateDelays.ReiterateDelay
import com.myodov.unicherrygarden.api.types.{BlockchainSyncStatus, MinedTransfer}
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.Response
import com.myodov.unicherrygarden.messages.cherrypicker.{AddTrackedAddresses, GetBalances, GetTrackedAddresses, GetTransfers}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.{DB, DBSession}

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.control.NonFatal


/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB. */
class CherryPicker(protected[this] val pgStorage: PostgreSQLStorage,
                   protected[this] val ethereumConnector: EthereumRpcSingleConnector) extends LazyLogging {

  /** Run next iteration; and reschedule it. */
  def nextIteration(): ReiterateDelay = {
    val result =
      try {
        insideEachIteration()
      } catch {
        case NonFatal(e) => logger.error("On iteration, got a error", e)
          ReiterateDelays.ReiterateAfterTimer
      }

    pgStorage.state.setLastHeartbeatAt
    result
  }

  /** What should happen within each iteration. */
  private[this] def insideEachIteration(): ReiterateDelay = {
    // It is `synchronized`, so that if some iteration takes too long, the next iteration won’t intervene
    // and work on the partially-updated state.
    this.synchronized {
      logger.debug("Iteration...")
      val startTime = System.nanoTime
      // First, let's ask the Ethereum node what's the status of the syncing process

      val syncingStatusOpt = ethereumConnector.syncingStatus
      val blockNumberOpt = ethereumConnector.latestSyncedBlockNumber

      val reiterateAfter: ReiterateDelay =
        if (syncingStatusOpt.isEmpty || blockNumberOpt.isEmpty) {
          logger.error(s"Cannot get eth.syncing ($syncingStatusOpt) / eth.blockNumber ($blockNumberOpt)!")
          pgStorage.state.setSyncState("Cannot connect to Ethereum node!")
          ReiterateDelays.ReiterateAfterTimer
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

          // Since this moment, we may want to use DB in a single atomic DB transaction;
          // even though this will involve querying the Ethereum node, maybe even multiple times.
          DB localTx { implicit session =>
            val optProgress = pgStorage.progress.getProgress
            lazy val progress = optProgress.get
            lazy val overallFrom = progress.overall.from.get // only if overall.from is not Empty

            if (optProgress.isEmpty) {
              logger.error("Cannot get the progress, something failed!")
              pgStorage.state.setSyncState("Cannot get the progress state!")
              ReiterateDelays.ReiterateAfterTimer
            } else if (progress.overall.from.isEmpty) {
              logger.warn("CherryPicker is not configured: missing `ucg_state.synced_from_block_number`!");
              ReiterateDelays.ReiterateAfterTimer
            } else if (progress.currencies.minSyncFrom.exists(_ < overallFrom)) {
              logger.error("The minimum `ucg_currency.sync_from_block_number` value " +
                s"is ${progress.currencies.minSyncFrom.get}; " +
                s"it should not be lower than $overallFrom!")
              ReiterateDelays.ReiterateAfterTimer
            } else if (progress.trackedAddresses.minFrom < overallFrom) {
              logger.error("The minimum `ucg_tracked_address.synced_from_block_number` value " +
                s"is ${progress.trackedAddresses.minFrom}; " +
                s"it should not be lower than $overallFrom!")
              ReiterateDelays.ReiterateAfterTimer
              //
              // Since this point start all the options where the iteration should actually happen
              //
            } else if (progress.trackedAddresses.toHasNulls || progress.perCurrencyTrackedAddresses.toHasNulls) {
              // optProgress is not Empty
              // progress.overall.from is not Empty

              // Some of tracked_addresses (or currency/tracked address M2Ms) have never been synced.
              // Find the earliest of them and sync.
              logger.debug(s"Progress is: $progress")

              pgStorage.progress.getFirstBlockResolvingSomeUnsyncedCTAddress match {
                case None =>
                  logger.error(s"Progress is $progress and some tracked addresses are untouched, " +
                    "but could not find them")
                  ReiterateDelays.ReiterateAfterTimer
                case Some(blockToSync) => {
                  pgStorage.state.setSyncState(s"Resyncing untouched addresses: block $blockToSync")

                  iterateBlock(blockToSync)
                  ReiterateDelays.ReiterateImmediately
                }
              }
            } else {
              logger.warn(s"Some other progress case: $progress")
              ReiterateDelays.ReiterateAfterTimer
            }
          } // DB localTx
        }

      val duration = Duration(System.nanoTime - startTime, TimeUnit.NANOSECONDS)
      logger.debug(s"Iteration completed in ${duration.toMillis} ms.")
      reiterateAfter
    }
  }

  /** Perform the regular iteration for a specific block number. */
  private[this] def iterateBlock(blockToSync: Int)(implicit session: DBSession): Unit = {
    val trackedAddresses: Set[String] = pgStorage.trackedAddresses.getJustAddresses

    logger.debug(s"processing block $blockToSync " +
      s"with tracked addresses $trackedAddresses")

    ethereumConnector.readBlock(blockToSync, trackedAddresses) match {
      case None => logger.error(s"Cannot read block $blockToSync")
      case Some((block, transactions)) => {
        logger.debug(s"Reading block $block:" +
          s"txes $transactions")

        val thisBlockInDbOpt = pgStorage.blocks.getBlockByNumber(block.number)
        val prevBlockInDbOpt = pgStorage.blocks.getBlockByNumber(block.number - 1)

        logger.debug(s"Storing block: $block; " +
          s"block may be present as $thisBlockInDbOpt, " +
          s"parent may be present as $prevBlockInDbOpt")

        (thisBlockInDbOpt, prevBlockInDbOpt) match {
          case (None, None) => {
            // This is the simplest case: this is probably the very first block in the DB
            logger.debug(s"Adding first block ${
              block.number
            }: " +
              s"neither it nor previous block exist in the DB")
            pgStorage.blocks.addBlock(block.withoutParentHash)
          }
          case (None, Some(prevBlockInDb)) if prevBlockInDb.hash == block.parentHash.get => {
            // Another simplest case: second and further blocks in the DB.
            // Very new block, and its parent matches the existing one
            logger.debug(s"Adding new block ${
              block.number
            }; parent block ${
              block.number - 1
            } " +
              s"exists already with proper hash")
            pgStorage.blocks.addBlock(block)
          }
          case (Some(thisBlockInDb), _) if thisBlockInDb.hash == block.hash => {
            logger.debug(s"Block ${
              block.number
            } exists already in the DB with the same hash ${
              block.hash
            }; " +
              s"no need to readd the block itself")
          }
          case (Some(thisBlockInDb), _) if thisBlockInDb.hash != block.hash => {
            logger.debug(s"Block ${
              block.number
            } exists already in the DB but with ${
              thisBlockInDb.hash
            } " +
              s"rather than ${
                block.hash
              }; need to wipe some blocks!")
            throw new RuntimeException("TODO")
          }
          case (None, Some(prevBlockInDb)) if prevBlockInDb.hash != block.parentHash.get => {
            logger.debug(s"Adding new block ${
              block.number
            }: " +
              s"expecting parent block to be ${
                prevBlockInDb.hash
              } but it is ${
                block.parentHash.get
              }; " +
              s"need to wipe some blocks!")
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

      // Register all service keys
      List(
        GetTrackedAddresses.SERVICE_KEY,
        AddTrackedAddresses.SERVICE_KEY,
        GetBalances.SERVICE_KEY,
        GetTransfers.SERVICE_KEY
      ).foreach(context.system.receptionist ! Receptionist.Register(_, context.self))

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
            picker.nextIteration() match {
              case ReiterateDelays.ReiterateImmediately =>
                logger.debug("After iteration, next iteration will happen immediately")
                context.self ! Iterate()
              case ReiterateDelays.ReiterateAfterTimer =>
                logger.debug("After iteration, next iteration will happen after timer")
                timers.startSingleTimer(Iterate(), ITERATION_PERIOD)
            }
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
            // (as Options; with `None` instead of any address that failed to add)
            val addressesMaybeAdded: List[Option[String]] = (
              for (addr: AddTrackedAddresses.AddressDataToTrack <- payload.addressesToTrack.asScala.toList)
                yield {
                  if (pgStorage.trackedAddresses.addTrackedAddress(
                    addr.address,
                    Option(addr.comment), // nullable
                    payload.trackingMode,
                    // The next line needs cunning processing of java.lang.Integer,
                    // as otherwise Option(null:Integer): Option[Int]
                    // will be evaluated as Some(0)
                    Option(payload.fromBlock).map(_.toInt) // nullable
                  )) {
                    Some(addr.address)
                  } else {
                    None
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
              new GetBalances.BalanceRequestResult(
                false,
                0,
                //                List[CurrencyBalanceFact](
                //                  new CurrencyBalanceFact(
                //                    Currency.newEthCurrency(),
                //                    BigDecimal(123.45).underlying(),
                //                    BalanceRequestResult.CurrencyBalanceFact.BalanceSyncState.SYNCED_TO_LATEST_UNICHERRYGARDEN_TOKEN_STATE,
                //                    15
                //                  )
                //                ).asJava,
                List.empty[GetBalances.BalanceRequestResult.CurrencyBalanceFact].asJava,
                new BlockchainSyncStatus(0, 0, 0))
            )
            Behaviors.same
          }
          case message: GetTransfers.Request => {
            logger.debug(s"Receiving GetTransfers message: $message")
            message.replyTo ! new GetTransfers.Response(
              new GetTransfers.TransfersRequestResult(
                true,
                0,
                // List(
                //   new MinedTransfer(
                //     "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                //     "0xedcc6f8f20962e6747369a71a5b89256289da87f",
                //     "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                //     BigDecimal("10045.6909000003").underlying,
                //     new MinedTx(
                //       "0x9cb54df2444658891df0c8165fecaecb4a2f1197ebe7b175dda1130b91ea4c9f",
                //       "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                //       "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                //       new Block(
                //         13628884,
                //         "0xbaafd3ce570a2ebc9cf87ebc40680ceb1ff8c0f158e4d03fe617d8d5e67fd4e5",
                //         Instant.ofEpochSecond(1637096843)),
                //       111
                //     ),
                //     144),
                //   // UTNP out #6
                //   new MinedTransfer(
                //     "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                //     "0x74644fd700c11dcc262eed1c59715ee874f65251",
                //     "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                //     BigDecimal("30000").underlying,
                //     new MinedTx(
                //       "0x3f0c1e4f1e903381c1e8ad2ad909482db20a747e212fbc32a4c626cad6bb14ab",
                //       "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
                //       "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                //       new Block(
                //         13631007,
                //         "0x57e6c79ffcbcc1d77d9d9debb1f7bbe1042e685e0d2f5bb7e7bf37df0494e096",
                //         Instant.ofEpochSecond(1637125704)),
                //       133
                //     ),
                //     173)
                // ).asJava,
                List.empty[MinedTransfer].asJava,
                new BlockchainSyncStatus(0, 0, 0)
              )
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

/** How long to delay between reiterations.
 *
 * Use `Immediate` to reiterate immediately.
 * Use `Pause` to reiterate after a regular pause.
 */
object ReiterateDelays extends Enumeration {
  type ReiterateDelay = Value

  val ReiterateImmediately, ReiterateAfterTimer = Value
}
