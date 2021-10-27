package com.myodov.unicherrygarden.storages

import java.sql.SQLException

import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.StartTrackingAddressMode
import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}
import scalikejdbc._

import scala.util.control.NonFatal

/** Stores the blockchain information in PostgreSQL database. */
class PostgreSQLStorage(jdbcUrl: String,
                        dbUser: String,
                        dbPassword: String,
                        wipeOnStart: Boolean,
                        migrationPaths: List[String]
                       ) extends LazyLogging {
  private[this] lazy val flw: Flyway = {
    val stockMigrations = List("classpath:com/myodov/unicherrygarden/db/migrations")
    val totalMigrations = stockMigrations ::: migrationPaths
    logger.debug(s"Flyway migration locations: ${totalMigrations}")

    val flw = Flyway.configure.dataSource(jdbcUrl, dbUser, dbPassword)
      .table("flyway_schema_history") // Default in modern Flyway
      .baselineOnMigrate(true)
      .locations(totalMigrations: _*)

    flw.load
  }

  private[this] def clean: CleanResult = flw.clean

  private[this] def migrate: MigrateResult = flw.migrate

  if (wipeOnStart) {
    val cleanResult = clean // clean it once
    logger.debug(s"Cleaned Flyway: cleaned ${cleanResult.schemasCleaned}, dropped ${cleanResult.schemasDropped}");
  }

  flw.migrate()

  lazy val makeSession: AutoSession.type = AutoSession


  /** All functions with the overall UniCherrypicker syncing progress. */
  class Progress {

    /** Sync status of whole UniCherrypicker.
     *
     * @param from : UniCherrypicker in general has been synced from this block (may be missing).
     * @param to   : UniCherrypicker in general has been synced to this block (may be missing).
     */
    final case class OverallSyncStatus(from: Option[Int], to: Option[Int])

    /** Sync status of `ucg_currency` table.
     *
     * @param minSyncFrom : minimum `sync_from_block_number` value among all supported currencies (may be missing).
     * @param maxSyncFrom : maximum `sync_from_block_number` value among all supported currencies (may be missing).
     */
    final case class CurrenciesSyncStatus(minSyncFrom: Option[Int], maxSyncFrom: Option[Int])

    /** Sync status of `ucg_block` table
     *
     * @param from : minimum block number in `ucg_block` table (may be missing).
     * @param to   : maximum block number in `ucg_block` tble (may be missing).
     */
    final case class BlocksSyncStatus(from: Option[Int], to: Option[Int])

    /** Sync status of `ucg_tracked_address` table.
     *
     * @param minFrom    : minimum `synced_from_block_number` value among all tracked addresses.
     * @param maxFrom    : maximum `synced_from_block_number` value among all tracked addresses.
     * @param minTo      : minimum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param maxTo      : maximum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param toHasNulls : whether `synced_to_block_number` has nulls;
     *                   i.e., for some tracked addresses, the synced_to value is missing.
     */
    final case class TrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int,
                                                minTo: Option[Int], maxTo: Option[Int],
                                                toHasNulls: Boolean)

    /** Sync status of `ucg_currency_tracked_address_progress` table.
     *
     * @param minFrom    : minimum `synced_from_block_number` value among all currency/tracked-address pairs.
     * @param maxFrom    : maximum `synced_from_block_number` value among all currency/tracked-address pairs.
     * @param minTo      : minimum `synced_to_block_number` value among all currency/tracked-address pairs (may be missing).
     * @param maxTo      : maximum `synced_to_block_number` value among all currency/tracked-address pairs (may be missing).
     * @param toHasNulls : whether `synced_to_block_number` has nulls;
     *                   i.e., for some currency/tracked-address pairs, the synced_to value is missing.
     */
    final case class PerCurrencyTrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int,
                                                           minTo: Option[Int], maxTo: Option[Int],
                                                           toHasNulls: Boolean)

    /** Whole-system syncing progress.
     *
     * @param overall                     : overall progress.
     * @param currencies                  : progress of syncing as per `ucg_currency` tble.
     * @param blocks                      : progress of syncing as per `ucg_block` table.
     * @param trackedAddresses            : progress of syncing as per `ucg_tracked_address` table.
     * @param perCurrencyTrackedAddresses : progress of syncing as per `ucg_currency_tracked_address_progress` tble.
     */
    final case class Progress(overall: OverallSyncStatus,
                              currencies: CurrenciesSyncStatus,
                              blocks: BlocksSyncStatus,
                              trackedAddresses: TrackedAddressesSyncStatus,
                              perCurrencyTrackedAddresses: PerCurrencyTrackedAddressesSyncStatus)

    /** Get the overall syncing progress. */
    def getProgress(implicit session: DBSession = ReadOnlyAutoSession): Option[Progress] = {
      sql"""
      SELECT * FROM ucg_progress;
      """.map(rs => {
        Progress(
          OverallSyncStatus(
            from = rs.intOpt("overall_from"),
            to = rs.intOpt("overall_to")),
          CurrenciesSyncStatus(
            minSyncFrom = rs.intOpt("currency_sync_from_min"),
            maxSyncFrom = rs.intOpt("currency_sync_from_max"),
          ),
          BlocksSyncStatus(
            from = rs.intOpt("block_from"),
            to = rs.intOpt("block_to"),
          ),
          TrackedAddressesSyncStatus(
            minFrom = rs.int("address_from_min"),
            maxFrom = rs.int("address_from_max"),
            minTo = rs.intOpt("address_to_min"),
            maxTo = rs.intOpt("address_to_max"),
            toHasNulls = rs.boolean("address_to_has_nulls")
          ),
          PerCurrencyTrackedAddressesSyncStatus(
            minFrom = rs.int("currency_address_from_min"),
            maxFrom = rs.int("currency_address_from_max"),
            minTo = rs.intOpt("currency_address_to_min"),
            maxTo = rs.intOpt("currency_address_to_max"),
            toHasNulls = rs.boolean("currency_address_to_has_nulls")
          )
        )
      }).single()
        .apply()
    }

    /** If we have any Per Currency Tracked Addresses (PCT Addresses) which have never been synced,
     * find a (earliest possible) block to sync any of them.
     *
     * @return: [[Option]] with the first unsynced PCT address;
     *          Option is empty if there is no such address found.
     */
    def getFirstBlockResolvingSomeUnsyncedPCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int] = {
      sql"""
      SELECT MIN(ucg_currency_tracked_address_progress.synced_from_block_number) AS min_synced_from_block_number
      FROM ucg_currency_tracked_address_progress
      WHERE ucg_currency_tracked_address_progress.synced_to_block_number IS NULL;
      """.map(rs => rs.int("min_synced_from_block_number"))
        .single()
        .apply()
    }
  }

  lazy val progress: Progress = new Progress

  /** Access `ucg_state` table. */
  class State {
    def setRestartedAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET restarted_at=now()
      """.execute.apply()
    }

    def setLastHeartbeatAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET last_heartbeat_at=now()
      """.execute.apply()
    }

    def setSyncState(state: String)(implicit session: DBSession = AutoSession) = {
      logger.debug(s"Setting sync state: “${state}”")
      sql"""
      UPDATE ucg_state
      SET sync_state = $state
      WHERE sync_state != $state
      """.execute.apply()
    }

    def setEthNodeData(blockNumber: Int, currentBlock: Int, highestBlock: Int)(implicit session: DBSession = AutoSession) = {
      require(blockNumber >= 0, blockNumber)
      require(currentBlock >= 0, currentBlock)
      require(highestBlock >= 0, highestBlock)

      logger.debug(s"Setting eth_node data: $blockNumber, $currentBlock, $highestBlock")
      sql"""
      UPDATE ucg_state
      SET
        eth_node_blocknumber = $blockNumber,
        eth_node_current_block = $currentBlock,
        eth_node_highest_block = $highestBlock
      WHERE
        eth_node_blocknumber != $blockNumber OR
        eth_node_current_block != $currentBlock OR
        eth_node_highest_block != $highestBlock
      """.execute.apply()
    }

    def setSyncedFromBlockNumber(blockNumber: Long)(implicit session: DBSession) = {
      sql"""
      UPDATE ucg_state
      SET synced_from_block_number = $blockNumber
      WHERE synced_from_block_number != $blockNumber
      """.execute.apply()
    }

    def setSyncedToBlockNumber(blockNumber: Long)(implicit session: DBSession) = {
      sql"""
      UPDATE ucg_state
      SET synced_to_block_number = $blockNumber
      WHERE synced_to_block_number != $blockNumber
      """.execute.apply()
    }
  }

  lazy val state: State = new State

  /** Access `ucg_currency` table. */
  class Currencies {

    object CurrencyTypes extends Enumeration {
      type DBCurrencyType = Value
      val Eth, Erc20 = Value

      def fromString(str: String): Value = {
        str match {
          case "ETH" => CurrencyTypes.Eth
          case "ERC20" => CurrencyTypes.Erc20
          case other => throw new RuntimeException(s"Unsupported currency type $other")
        }
      }

      def toInteropType(v: Value): Currency.CurrencyType = {
        v match {
          case CurrencyTypes.Eth => Currency.CurrencyType.ETH
          case CurrencyTypes.Erc20 => Currency.CurrencyType.ERC20
          case other => throw new RuntimeException(s"Unsupported currency type $other")
        }
      }
    }


    /** Single instance of currency/token/asset. */
    final case class DBCurrency(currencyType: CurrencyTypes.DBCurrencyType,
                                dAppAddress: Option[String],
                                name: Option[String],
                                symbol: Option[String],
                                ucgComment: Option[String]
                               ) {
      require((currencyType == CurrencyTypes.Eth) == dAppAddress.isEmpty, (currencyType, dAppAddress))
      require(dAppAddress.isEmpty || EthUtils.Addresses.isValidLowercasedAddress(dAppAddress.get), dAppAddress)

      def asAsset: dlt.Asset = {
        currencyType match {
          case CurrencyTypes.Eth => dlt.Ether
          case CurrencyTypes.Erc20 => dlt.ERC20Token(dAppAddress.get)
          case _ => {
            throw new RuntimeException(s"Unsupported currencyType $currencyType")
          }
        }
      }
    }

    def getCurrencies(implicit session: DBSession = ReadOnlyAutoSession): List[DBCurrency] = {
      sql"""
      SELECT * FROM ucg_currency;
      """.map(rs => DBCurrency(
        CurrencyTypes.fromString(rs.string("type")),
        rs.stringOpt("dapp_address"),
        rs.stringOpt("name"),
        rs.stringOpt("symbol"),
        rs.stringOpt("ucg_comment"),
      )).list.apply()
    }
  }

  lazy val currencies: Currencies = new Currencies


  class TrackedAddresses {

    /** Single instance of tracked address.
     * Note that the [[Option]] arguments being [[Option.empty]] don’t necessary mean the data really has NULL here:
     * they may be empty if the result has been requested without this piece of data.
     */
    final case class TrackedAddress(address: String,
                                    comment: Option[String],
                                    syncedFrom: Option[Int],
                                    syncedTo: Option[Int]
                                   )

    /** Get the list of all tracked addresses;
     * optionally containing (or not containing) various extra information about each address.
     */
    def getTrackedAddresses(
                             includeComment: Boolean,
                             includeSyncedFrom: Boolean,
                             includeSyncedTo: Boolean
                           )(implicit
                             session: DBSession = ReadOnlyAutoSession
                           ): List[TrackedAddress] = {
      sql"""
      SELECT
       address,
       CASE WHEN $includeComment THEN ucg_comment ELSE NULL END AS ucg_comment,
       CASE WHEN $includeSyncedFrom THEN synced_from_block_number ELSE NULL END AS synced_from_block_number,
       CASE WHEN $includeSyncedTo THEN synced_to_block_number ELSE NULL END AS synced_to_block_number
      FROM ucg_tracked_address;
      """.map(rs => TrackedAddress(
        rs.string("address"),
        rs.stringOpt("ucg_comment"),
        rs.intOpt("synced_from_block_number"),
        rs.intOpt("synced_to_block_number")
      )).list.apply()
    }

    /** Get the set of all tracked addresses (and just of the address strings, nothing else). */
    def getJustAddresses(implicit session: DBSession = ReadOnlyAutoSession): Set[String] = {
      sql"""
      SELECT address FROM ucg_tracked_address;
      """.map(_.string("address")).list.apply().toSet
    }

    /** Add a new address to be tracked.
     *
     * @return whether the adding happened successfully.
     */
    def addTrackedAddress(
                           address: String,
                           comment: Option[String],
                           mode: StartTrackingAddressMode,
                           fromBlock: Option[Int]
                         )(implicit
                           session: DBSession = AutoSession
                         ): Boolean = {
      logger.debug(s"Tracking address $address: $comment, $mode, $fromBlock")
      require(EthUtils.Addresses.isValidLowercasedAddress(address), address)
      require((mode == StartTrackingAddressMode.FROM_BLOCK) == fromBlock.nonEmpty, (mode, fromBlock))

      try {
        sql"""
      INSERT INTO ucg_tracked_address(
        address,
        ucg_comment,
        synced_from_block_number)
      VALUES (
        $address,
        $comment,
        CASE ${mode.toString}
          WHEN 'FROM_BLOCK' THEN $fromBlock
          WHEN 'LATEST_KNOWN_BLOCK' THEN (SELECT eth_node_highest_block FROM ucg_state)
          WHEN 'LATEST_NODE_SYNCED_BLOCK' THEN (SELECT eth_node_current_block FROM ucg_state)
          WHEN 'LATEST_CHERRYGARDEN_SYNCED_BLOCK' THEN (SELECT synced_to_block_number FROM ucg_state)
          ELSE NULL -- should fail
        END
      );
      """.execute.apply()
        true
      } catch {
        case ex: SQLException => {
          logger.warn(s"Reinserting prevented: $address, $comment, $mode")
          false
        }
        case NonFatal(e) => {
          logger.error(s"Unexpected error", e)
          false
        }
      }
    }
  }

  lazy val trackedAddresses: TrackedAddresses = new TrackedAddresses


  /** Access `ucg_block` table. */
  class Blocks {

    def addBlock(block: dlt.Block
                )(implicit
                  session: DBSession = AutoSession
                ) = {
      sql"""
      INSERT INTO ucg_block(number, hash, parent_hash, timestamp)
      VALUES (${block.number}, ${block.hash}, ${block.parentHash}, ${block.timestamp})
      """.execute.apply()
    }
  }

  lazy val blocks: Blocks = new Blocks


  /** Access `ucg_transaction` table. */
  class Transactions {

    def addTransaction(transaction: dlt.Transaction,
                       // The transaction already has the block number, but, passing the block hash,
                       // we ensure that the block hasn’t been reorganized
                       blockHash: String,
                      )(implicit
                        session: DBSession = AutoSession
                      ): Boolean = {
      require(EthUtils.Hashes.isValidBlockHash(blockHash), blockHash)
      false
    }
  }

  lazy val transactions: Transactions = new Transactions

  /** Access `ucg_tx_log` table. */
  class TxLogs {

    /** Information for any transaction log (in a transaction) in Ethereum blockchain;
     * stored in `ucg_tx_log` table.
     */
    final case class TxLog(logIndex: Int,
                           topics: Array[String],
                           data: String
                          ) {
      require(logIndex >= 0, logIndex)
      require(topics.forall(topic => EthUtils.isValidHash(topic, 66)), topics)
      require(EthUtils.isValidHexString(data), data)
    }

    def addTxLogs(
                   blockNumber: Int,
                   transactionHash: String,
                   txLogs: Seq[TxLog]
                 )(implicit
                   session: DBSession = AutoSession
                 ): Boolean = {
      false
    }
  }

  lazy val txlogs: TxLogs = new TxLogs
}

object PostgreSQLStorage {
  @inline def apply(jdbcUrl: String,
                    dbUser: String,
                    dbPassword: String,
                    wipeOnStart: Boolean,
                    migrationPaths: List[String]
                   ): PostgreSQLStorage = {
    ConnectionPool.singleton(jdbcUrl, dbUser, dbPassword)
    new PostgreSQLStorage(jdbcUrl, dbUser, dbPassword, wipeOnStart, migrationPaths)
  }
}
