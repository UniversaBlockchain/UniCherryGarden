package com.myodov.unicherrygarden

import java.time
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.types.{BlockchainSyncStatus, MinedTransfer}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.CherryPickerRequest
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.Response
import com.myodov.unicherrygarden.messages.cherrypicker.{AddTrackedAddresses, GetBalances, GetTrackedAddresses, GetTransfers}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.{DB, DBSession}

import scala.annotation.switch
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.control.NonFatal


/** The main actor “cherry-picking” the data from the Ethereum blockchain into the DB.
 *
 * It has two sub-systems/sub-behaviors:  and [[CherryPicker#SlowSyncer]],
 * which both work independently but assume the other counterpart does its job too.
 *
 * [[CherryPicker#FastSyncer]]:
 *
 * [[CherryPicker#SlowSyncer]]:
 * <ul>
 * <li>Once per each (about) 10–15 seconds, it reads the `eth.syncing`
 * and `eth.blockNumber` from Ethereum node (geth).</li>
 * <li>If the new syncing state moved exactly by 1 block from the last block fully-synced by CherryPicker, it:
 * <ol>
 * <li>Syncs just the new block;</li>
 * <li>... and only after this, it stores the `eth.syncing`/`eth.blockNumber` in the DB
 * (so that FastSync running concurrently doesn’t clash with this block and doesn’t try to sync it).</li>
 * <li></li>
 * </ol>
 * </li>
 * <ul>
 *
 * If the new syncing state moved further than by 1 block, it relies on fast-sync to catch up on its next iteration.
 *
 * TODO...
 *
 * <ul>
 * <li>First goes `launch`.</li>
 * <li>On each `ReadEthNodeSyncState`, it goes to `rereadEthSyncingBlockNumber()`;
 * which rereads the `eth.syncing` and `eth.blockNumber` from Ethereum node (geth).</li>
 * <li>On each `DoSyncIteration`, it goes to `nextIteration()`;
 * which does the iteration, and, on its end, goes to either `reiterateImmediately()` or `reiterateAfterDelay()`.</li>
 * <li>`reiterateImmediately()` issues `DoSyncIteration` message immediately.</li>
 * <li>`reiterateAfterDelay()` schedules a `DoSyncIteration` message after a delay.</li>
 * </ul>
 * */
private class CherryPicker(protected[this] val pgStorage: PostgreSQLStorage,
                           protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations)
  extends LazyLogging {

  import CherryPicker._

  /** First state: when just launching the CherryPicker after shutdown/restart. */
  private def launch(): Behavior[CherryPickerRequest] = {
    Behaviors.setup { context =>
      logger.info(s"Launching CherryPicker: v. $propVersionStr, built at $propBuildTimestampStr")

      // Register all service keys
      List(
        GetTrackedAddresses.SERVICE_KEY,
        AddTrackedAddresses.SERVICE_KEY,
        GetBalances.SERVICE_KEY,
        GetTransfers.SERVICE_KEY
      ).foreach(context.system.receptionist ! Receptionist.Register(_, context.self))

      // On startup, schedule a single iteration;
      // It will re-schedule itself when/if needed.
      // We don’t setup a “regular” timer because sometimes a new iteration should happen without a pause;
      // and sometimes a new iteration should happen not 5 (or so) seconds after the previous iteration *started*,
      // but 5 seconds after a previous iteration *completed*.
      // The latter though can be resolved by scheduleWithFixedDelay.
      logger.error("Running first iteration of CherryPicker...")
      Behaviors.withTimers[CherryPickerRequest] { timers: TimerScheduler[CherryPickerRequest] =>
        timers.startTimerWithFixedDelay(
          ReadEthNodeSyncState(),
          0 seconds,
          ITERATION_PERIOD
        ) // due to initialDelay = 0 seconds, it also sends this message, instantly, too.
        // ..   And even before the next line.
        context.self ! DoSyncIteration(CherryPickerState()) // TODO: enable to start iterations

        Behaviors.receiveMessage {
          case DoSyncIteration(state: CherryPickerState) => {
            nextIteration(state)
          }
          case ReadEthNodeSyncState() => {
            rereadEthSyncingBlockNumber()
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
            Behaviors.unhandled
          }
        }
      }
    }
  }

  private case class FastSyncer() {

  }

  /** Performs the “Slow sync” – after the fast-synced block reached
   * the last-known-to-Ethereum-node synced block, it just starts slowly read the next block,
   * once about 10–15 seconds.
   */
  private case class SlowSyncer() {

  }

  /** FSM state: “Reread `eth.syncing` and `eth.blockNumber`”. */
  private[this] def rereadEthSyncingBlockNumber(): Behavior[CherryPickerRequest] = {
    logger.debug("Rereading eth.syncing/eth.blockNumber")

    (ethereumConnector.ethSyncingBlockNumber: @switch) match {
      case None =>
        logger.error("Cannot get eth.syncing / eth.blockNumber !")
        pgStorage.state.setSyncState("Cannot connect to Ethereum node!")
      case Some((ethSyncing, ethBlockNumberBI)) =>
        val ethBlockNumber: Int = ethBlockNumberBI.bigInteger.intValueExact
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
    }

    Behaviors.same
  }

  /** FSM state: “Run the next iteration; and reschedule it”. */
  private def nextIteration(state: CherryPickerState): Behavior[CherryPickerRequest] = {
    try {
      // It is `synchronized`, so that if some iteration takes too long, the next iteration won’t intervene
      // and work on the partially-updated state.
      this.synchronized {
        logger.debug("Iteration...")
        val startTime = System.nanoTime
        // First, let's ask the Ethereum node what's the status of the syncing process

        val behavior: Behavior[CherryPickerRequest] = {
          // Since this moment, we may want to use DB in a single atomic DB transaction;
          // even though this will involve querying the Ethereum node, maybe even multiple times.
          DB localTx { implicit session =>
            val optProgress = pgStorage.progress.getProgress
            lazy val progress = optProgress.get
            lazy val overallFrom = progress.overall.from.get // only if overall.from is not Empty

            if (optProgress.isEmpty) {
              logger.error("Cannot get the progress, something failed!")
              pgStorage.state.setSyncState("Cannot get the progress state!")
              reiterateAfterDelay(state)

            } else if (progress.overall.from.isEmpty) {
              logger.warn("CherryPicker is not configured: missing `ucg_state.synced_from_block_number`!");
              reiterateAfterDelay(state)

            } else if (progress.currencies.minSyncFrom.exists(_ < overallFrom)) {
              logger.error("The minimum `ucg_currency.sync_from_block_number` value " +
                s"is ${progress.currencies.minSyncFrom.get}; " +
                s"it should not be lower than $overallFrom!")
              reiterateAfterDelay(state)

            } else if (progress.trackedAddresses.minFrom < overallFrom) {
              logger.error("The minimum `ucg_tracked_address.synced_from_block_number` value " +
                s"is ${progress.trackedAddresses.minFrom}; " +
                s"it should not be lower than $overallFrom!")
              reiterateAfterDelay(state)

              // ------------------------------------------------------------------------------
              // Since this point go all the options where the iteration should actually happen
              // ------------------------------------------------------------------------------
            } else if (progress.blocks.from.isEmpty) {
              // progress.overall.from is not Empty
              val blockToSync = progress.overall.from.get
              logger.info(s"Starting from the very first block ($blockToSync)...")
              handleSyncedBlock(blockToSync)
              reiterateImmediately(state)

            } else { // Fast sync
              // progress.overall.from is not Empty

              // There may be multiple options of blocks to choose:
              // 1. Just the next block to read.
              val firstUnreadBlock: Option[Int] = progress.blocks.to.map(_ + 1)
              // 2. We never started some (currency, tracked address) pair?
              // Use the least from_block (from either currency or tracked address).
              val firstNeverCTAStartedBlock: Option[Int] =
              pgStorage.progress.getFirstBlockResolvingSomeNeverStartedCTAddress
              // 3. We never completed some (currency, tracked address) pair?
              // Use the least from_block (from either currency or tracked address).
              val firstNeverCTASyncedBlock: Option[Int] =
              pgStorage.progress.getFirstBlockResolvingSomeNeverSyncedCTAddress
              // 4. Some of CTA to-blocks is smaller than others?
              // Use it.
              val firstMismatchingCTAToBlock: Option[Int] =
              (progress.perCurrencyTrackedAddresses.minTo, progress.perCurrencyTrackedAddresses.maxTo) match {
                case (Some(minTo), Some(maxTo)) if minTo < maxTo =>
                  Some(minTo + 1)
                case _ =>
                  None
              }

              val blocksToCompare = List(
                firstUnreadBlock,
                firstNeverCTAStartedBlock,
                firstNeverCTASyncedBlock,
                firstMismatchingCTAToBlock
              )

              logger.debug(s"Progress is $progress: " +
                s"choosing between $blocksToCompare")

              val blockToSync = blocksToCompare.flatten.min

              logger.info(s"Fast-syncing $blockToSync block")
              handleSyncedBlock(blockToSync)
              reiterateImmediately(state)
            }
          } // DB localTx
        }

        pgStorage.state.setLastHeartbeatAt
        val duration = Duration(System.nanoTime - startTime, TimeUnit.NANOSECONDS)
        logger.debug(s"Iteration completed in ${duration.toMillis} ms.")
        behavior
      }
    } catch {
      case NonFatal(e) =>
        logger.error("On iteration, got a error", e)
        reiterateAfterDelay(state)
    }
  }

  /** FSM state: “and now let’s immediately run next syncing iterate”. */
  private def reiterateImmediately(state: CherryPickerState): Behavior[CherryPickerRequest] = {
    logger.debug("Next iteration will happen immediately: fast sync!")
    val newState = state.advanceFastSync()

    Behaviors.setup[CherryPickerRequest] { context =>
      context.self ! DoSyncIteration(newState)
      Behaviors.same
    }
  }

  /** FSM state: “and now let’s run next syncing iterate after a short pause”. */
  private def reiterateAfterDelay(state: CherryPickerState): Behavior[CherryPickerRequest] = {
    logger.debug("Next iteration will happen after timer")
    val newState = state.stopFastSync() // this is not a fast sync anymore

    Behaviors.withTimers[CherryPickerRequest] { timers =>
      timers.startSingleTimer(DoSyncIteration(newState), ITERATION_PERIOD)
      Behaviors.same
    }
  }

  /** Perform the regular iteration for a specific block number. */
  private[this] def handleSyncedBlock(blockToSync: Int)(implicit session: DBSession): Unit = {
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
  final case class DoSyncIteration(state: CherryPickerState) extends CherryPickerRequest

  /** A message informing CherryPicker it needs to recheck the Ethereum node sync state.
   * (i.e. to read `eth.blockNumber`).
   */
  final case class ReadEthNodeSyncState() extends CherryPickerRequest

  private val ITERATION_PERIOD = 10 seconds // Each block is generated about once per 13 seconds
  //  private val ITERATION_PERIOD = 1 second

  /** Object constructor. */
  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations): Behavior[CherryPickerRequest] =
    new CherryPicker(pgStorage, ethereumConnector).launch()
}


/** Performance information about the ongoing fast sync. */
case class FastSyncStats(startedAt: Instant = Instant.now,
                         syncedBlocks: Int = 0) {

  override def toString: String =
    s"FastSyncBenchmark(started: $startedAt, synced: $syncedBlocks, elapsed; $elapsed: " +
      s"$blocksPerSecond bps, $secondsPerBlock s. per block)"

  /** Return a new benchmark value/state, with the number of blocks increased by one. */
  def incrementBlocks(): FastSyncStats =
    FastSyncStats(startedAt, syncedBlocks + 1)

  private[this] lazy val elapsed: time.Duration = time.Duration.between(startedAt, Instant.now)
  lazy val blocksPerSecond = (syncedBlocks: Double) / elapsed.getSeconds
  lazy val secondsPerBlock = 1 / blocksPerSecond
}

case class CherryPickerState(fastSyncStats: Option[FastSyncStats] = None) extends LazyLogging {
  /** Return new state, assuming the fast sync advanced by one block. */
  def advanceFastSync(): CherryPickerState = {
    val newFastSyncStats = fastSyncStats match {
      case None =>
        // Starting fast sync
        logger.debug("Starting fast sync anew")
        FastSyncStats()
      case Some(state) =>
        val newStats = state.incrementBlocks
        // Continuing fast sync
        logger.debug(s"Fast sync performance: $newStats")
        newStats
    }
    CherryPickerState(Some(newFastSyncStats))
  }

  /** Return new state, assuming the fast sync has just been stopped. */
  def stopFastSync(): CherryPickerState = {
    logger.debug("Stopping fast sync")
    CherryPickerState(fastSyncStats = None)
  }
}
