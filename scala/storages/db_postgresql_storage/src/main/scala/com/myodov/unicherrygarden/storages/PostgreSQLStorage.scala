package com.myodov.unicherrygarden.storages

import java.sql.SQLException

import com.myodov.unicherrygarden.Tools.seqIsIncrementing
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.StartTrackingAddressMode
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}
import scalikejdbc._

import scala.collection.compat.Factory
import scala.collection.immutable.SortedMap
import scala.util.control.NonFatal

/** Stores the blockchain information in PostgreSQL database. */
class PostgreSQLStorage(jdbcUrl: String,
                        dbUser: String,
                        dbPassword: String,
                        wipeOnStart: Boolean,
                        migrationPaths: List[String]
                       ) extends DBStorageAPI with LazyLogging {
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


  object progress extends DBStorageAPI.Progress {

    import com.myodov.unicherrygarden.storages.api.DBStorage.Progress._

    override def getProgress(implicit session: DBSession = ReadOnlyAutoSession): Option[ProgressData] = {
      sql"""
      SELECT * FROM ucg_progress;
      """.map(rs => {
        ProgressData(
          OverallSyncConfiguration(
            from = rs.intOpt("overall_from")
          ),
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
            maxFrom = rs.int("address_from_max")
          ),
          PerCurrencyTrackedAddressesSyncStatus(
            minFrom = rs.intOpt("currency_address_from_min"),
            maxFrom = rs.intOpt("currency_address_from_max"),
            minTo = rs.intOpt("currency_address_to_min"),
            maxTo = rs.intOpt("currency_address_to_max"),
            toHasNulls = rs.boolean("currency_address_to_has_nulls")
          )
        )
      }).single
        .apply
    }

    override def getFirstBlockResolvingSomeNeverStartedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int] = {
      sql"""
      SELECT
          LEAST(currency.sync_from_block_number, address.synced_from_block_number)
            AS least_sync_from_block_number
      FROM
          ucg_currency currency
          CROSS JOIN ucg_tracked_address address
          LEFT JOIN ucg_currency_tracked_address_progress cta_progress
                    ON currency.id = cta_progress.currency_id AND
                       address.id = cta_progress.tracked_address_id
      WHERE
          -- If no cta_progress record at all
          cta_progress.id IS NULL
      ORDER BY least_sync_from_block_number
      LIMIT 1;
      """.map(_.intOpt("least_sync_from_block_number")) // may be null if no such records
        .single
        .apply
        .flatten // Option[Option[Int]] to Option[Int]
    }

    override def getFirstBlockResolvingSomeNeverSyncedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int] = {
      sql"""
      SELECT
          LEAST(currency.sync_from_block_number, address.synced_from_block_number)
            AS least_sync_from_block_number
      FROM
          ucg_currency currency
          CROSS JOIN ucg_tracked_address address
          LEFT JOIN ucg_currency_tracked_address_progress cta_progress
                    ON currency.id = cta_progress.currency_id AND
                       address.id = cta_progress.tracked_address_id
      WHERE
          -- If no cta_progress record at all; or maybe even `synced_to_block_number` IS NULL
          cta_progress.synced_to_block_number IS NULL
      ORDER BY least_sync_from_block_number
      LIMIT 1;
      """.map(_.intOpt("least_sync_from_block_number")) // may be null
        .single
        .apply
        .flatten // Option[Option[Int]] to Option[Int]
    }
  }

  object state extends DBStorageAPI.State {
    def setRestartedAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET restarted_at=now()
      """.execute.apply
    }

    override def setLastHeartbeatAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET last_heartbeat_at=now()
      """.execute.apply
    }

    override def setSyncState(state: String)(implicit session: DBSession = AutoSession) = {
      logger.debug(s"Setting sync state: “${state}”")
      sql"""
      UPDATE ucg_state
      SET sync_state = $state
      WHERE sync_state != $state
      """.execute.apply
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
      """.execute.apply
    }

    def setSyncedFromBlockNumber(blockNumber: Long)(implicit session: DBSession) = {
      sql"""
      UPDATE ucg_state
      SET synced_from_block_number = $blockNumber
      WHERE synced_from_block_number != $blockNumber
      """.execute.apply
    }

    /** Move the progress to the next block:
     *
     * @param syncedBlockNumber the number of the block that has just been synced/stored in the DB.
     * @param trackedAddresses  the sequence of tracked addresses for which the blockchain progress has been read,
     *                          parsed and stored.
     *                          We cannot read it from the DB right now and rely upon it,
     *                          because what if the tracked addresses have changed already since
     *                          the reading/parsing time?
     */
    override def advanceProgress(
                                  syncedBlockNumber: Long,
                                  trackedAddresses: Set[String]
                                )(implicit session: DBSession) = {
      assert(
        trackedAddresses.forall(EthUtils.Addresses.isValidLowercasedAddress),
        trackedAddresses)

      logger.debug(s"DB advanceProgress: $syncedBlockNumber, $trackedAddresses")

      // 1. Insert ucg_currency_tracked_address_progress records
      //    where they didn’t exist before;
      //    and where the syncedBlockNumber matches the start block number of either a currency or a tracked address
      //    (whichever is lower).
      {
        sql"""
        WITH
            -- Function argument
            arg(block_number) AS (
                SELECT $syncedBlockNumber
            ),
            -- Function argument
            actually_tracked_address_arg(address) AS (
                SELECT unnest(ARRAY[$trackedAddresses])
            ),
            -- Preprocess the function argument and find the IDs of tracked addresses
            actually_tracked_address(id, address, synced_from_block_number) AS (
                SELECT
                    ucg_tracked_address.id,
                    ucg_tracked_address.address,
                    ucg_tracked_address.synced_from_block_number
                FROM
                    actually_tracked_address_arg
                    INNER JOIN ucg_tracked_address
                               USING (address)
            ),
            data_to_insert(currency_id, tracked_address_id, synced_from_block_number, synced_to_block_number) AS (
                SELECT
                    currency.id AS currency_id,
                    address.id AS tracked_address_id,
                    LEAST(currency.sync_from_block_number,
                          address.synced_from_block_number) AS synced_from_block_number,
                    arg.block_number AS synced_to_block_number
                FROM
                    arg,
                    -- Take all currencies...
                    ucg_currency currency
                        -- ... and all tracked addresses...
                    CROSS JOIN actually_tracked_address address
                        -- cta_progress.id will be NULL if such pair of (currency_id, tracked_address_id)
                        -- is not added yet.
                    LEFT JOIN ucg_currency_tracked_address_progress AS cta_progress
                              ON currency.id = cta_progress.currency_id AND
                                 address.id = cta_progress.tracked_address_id
                WHERE
                    -- This pair is not added yet
                    cta_progress.id IS NULL AND
                    -- We add a new record ONLY if the just-synced block number is equal
                    -- to the cta.synced_from (which is the smaller of currency.from and address.from).
                    arg.block_number = LEAST(currency.sync_from_block_number,
                                             address.synced_from_block_number)
            )
        INSERT INTO ucg_currency_tracked_address_progress(
          currency_id,
          tracked_address_id,
          synced_from_block_number,
          synced_to_block_number
        )
        SELECT *
        FROM data_to_insert
        """.execute.apply
      }

      // 2. Update ucg_currency_tracked_address_progress records
      //    where they existed before;
      //    and where the syncedBlockNumber has advanced by one.
      {
        sql"""
        WITH
            arg(block_number) AS (
                SELECT $syncedBlockNumber
            ),
            actually_tracked_address_arg(address) AS (
                SELECT unnest(ARRAY[$trackedAddresses])
            ),
            actually_tracked_address(id, address, synced_from_block_number) AS (
                SELECT
                    ucg_tracked_address.id,
                    ucg_tracked_address.address,
                    ucg_tracked_address.synced_from_block_number
                FROM
                    actually_tracked_address_arg
                    INNER JOIN ucg_tracked_address
                               USING (address)
            ),
            data_to_update (currency_id, tracked_address_id, synced_to_block_number) AS (
                SELECT
                    currency.id AS currency_id,
                    address.id AS tracked_address_id,
                    arg.block_number AS synced_to_block_number
                FROM
                    arg,
                    -- Take all currencies...
                    ucg_currency currency
                        -- ... and all tracked addresses...
                    CROSS JOIN actually_tracked_address address
                        -- cta_progress.id will be NOT NULL if such pair of (currency_id, tracked_address_id)
                        -- already exists
                    LEFT JOIN ucg_currency_tracked_address_progress AS cta_progress
                              ON currency.id = cta_progress.currency_id AND
                                 address.id = cta_progress.tracked_address_id
                WHERE
                    -- This pair exists already
                    cta_progress.id IS NOT NULL AND
                    -- We update the record differently depending on
                    -- whether synced_to_block_number IS NULL or not.
                    (
                      -- We change the NULL to_block_number to non-NULL
                      -- only if it the current block matches the smallest
                      -- of (sync-currency-from-block) and (sync-address-first-block)
                      (
                        synced_to_block_number IS NULL AND
                        arg.block_number = LEAST(currency.sync_from_block_number,
                                                 address.synced_from_block_number)
                      ) OR
                      -- When synced_to_block_number IS NOT NULL:
                      -- Non-NULL to_block_number can only be incremented.
                      (arg.block_number = synced_to_block_number + 1)
                    )
            )
        UPDATE ucg_currency_tracked_address_progress AS upd_cta_progress
        SET
            synced_to_block_number = data_to_update.synced_to_block_number
        FROM data_to_update
        WHERE
            upd_cta_progress.currency_id = data_to_update.currency_id AND
            upd_cta_progress.tracked_address_id = data_to_update.tracked_address_id;
        """.execute.apply
      }
    }
  }

  object currencies extends DBStorageAPI.Currencies {

    import com.myodov.unicherrygarden.storages.api.DBStorage.Currencies._

    override def getCurrencies(
                                getVerified: Boolean,
                                getUnverified: Boolean
                              )
                              (
                                implicit session: DBSession = ReadOnlyAutoSession
                              ): List[DBCurrency] = {
      // What values are allowed for `verified` field in a query we’ll run?
      val verifiedValues = (
        (if (getVerified) Seq(true) else Seq())
          ++
          (if (getUnverified) Seq(false) else Seq())
        )

      sql"""
      SELECT *
      FROM ucg_currency
      WHERE verified IN ($verifiedValues);
      """.map(rs => DBCurrency(
        CurrencyTypes.fromString(rs.string("type")),
        rs.stringOpt("dapp_address"),
        rs.stringOpt("name"),
        rs.stringOpt("symbol"),
        rs.stringOpt("ucg_comment"),
        rs.boolean("verified"),
        rs.intOpt("decimals")
      )).list.apply
    }
  }

  object trackedAddresses extends DBStorageAPI.TrackedAddresses {

    import com.myodov.unicherrygarden.storages.api.DBStorage.TrackedAddresses._

    override def getTrackedAddresses(
                                      includeComment: Boolean,
                                      includeSyncedFrom: Boolean
                                    )(implicit
                                      session: DBSession = ReadOnlyAutoSession
                                    ): List[TrackedAddress] = {
      sql"""
      SELECT
       address,
       CASE WHEN $includeComment THEN ucg_comment ELSE NULL END AS ucg_comment,
       CASE WHEN $includeSyncedFrom THEN synced_from_block_number ELSE NULL END AS synced_from_block_number
      FROM ucg_tracked_address;
      """.map(rs => TrackedAddress(
        rs.string("address"),
        rs.stringOpt("ucg_comment"),
        rs.intOpt("synced_from_block_number")
      )).list.apply
    }

    override def getJustAddresses(implicit session: DBSession = ReadOnlyAutoSession): Set[String] = {
      sql"""
      SELECT address FROM ucg_tracked_address;
      """.map(_.string("address")).list.apply.toSet
    }

    override def addTrackedAddress(
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
            WHEN 'LATEST_CHERRYGARDEN_SYNCED_BLOCK' THEN (SELECT block_to FROM ucg_progress)
            ELSE NULL -- should fail
          END
        );
        """.execute.apply
        true
      } catch {
        case ex: SQLException =>
          logger.warn(s"Reinserting prevented: $address, $comment, $mode")
          false
        case NonFatal(e) =>
          logger.error(s"Unexpected error", e)
          false
      }
    }
  }

  object blocks extends DBStorageAPI.Blocks {

    override def addBlock(block: dlt.EthereumBlock
                         )(implicit
                           session: DBSession = AutoSession
                         ) = {
      sql"""
      INSERT INTO ucg_block(number, hash, parent_hash, timestamp)
      VALUES (${block.number}, ${block.hash}, ${block.parentHash}, ${block.timestamp})
      """.execute.apply
    }

    override def getBlockByNumber(
                                   blockNumber: Int
                                 )(implicit session: DBSession = ReadOnlyAutoSession): Option[dlt.EthereumBlock] = {
      sql"""
      SELECT *
      FROM ucg_block
      WHERE number = $blockNumber
      """
        .map(
          rs => dlt.EthereumBlock(
            number = rs.int("number"),
            hash = rs.string("hash"),
            parentHash = rs.stringOpt("parent_hash"),
            timestamp = rs.timestamp("timestamp").toInstant
          ))
        .single
        .apply
    }

    override def getLatestHashes(
                                  howMany: Int
                                )(implicit session: DBSession = ReadOnlyAutoSession): SortedMap[Int, String] = {
      assert(howMany >= 1, howMany)
      val result =
        sql"""
      WITH
          last_known_block(number) AS (
              SELECT max(number)
              FROM ucg_block
          )
      SELECT
          ucg_block.number,
          ucg_block.hash
      FROM ucg_block, last_known_block
      WHERE ucg_block.number > last_known_block.number - 10
      ORDER BY ucg_block.number
      """
          .map(rs => (rs.int("number") -> rs.string("hash")))
          .list
          .apply
          .to(SortedMap)

      // Validate output before returning

      // The size must be <= of requested
      assert(result.size <= howMany, result)
      // Make sure all the keys are steadily increasing
      assert(
        seqIsIncrementing(result.keys),
        result)
      // All the hashes must be valid block hashes
      assert(
        result.values.forall(EthUtils.Hashes.isValidBlockHash),
        result
      )

      result
    }

    override def rewind(
                         startBlockNumber: Int
                       )(implicit session: DBSession = AutoSession): Boolean = {
      try {
        logger.debug(s"Performing rewind of blocks since $startBlockNumber")

        sql"""
        DELETE FROM ucg_tx_log
        WHERE block_number >= $startBlockNumber
        """.execute.apply
        logger.debug(s"Rewound ucg_tx_log")

        sql"""
        DELETE FROM ucg_transaction
        WHERE block_number >= $startBlockNumber
        """.execute.apply
        logger.debug(s"Rewound ucg_transaction")

        sql"""
        DELETE FROM ucg_block
        WHERE number >= $startBlockNumber
        """.execute.apply
        logger.debug(s"Rewound ucg_block")

        sql"""
        UPDATE ucg_currency_tracked_address_progress
        SET synced_to_block_number = $startBlockNumber - 1
        WHERE synced_to_block_number >= $startBlockNumber
        """.execute.apply
        logger.debug(s"Rewound ucg_currency_tracked_address_progress") // ucg_tracked_address.synced_to_block_number is not seriously used

        true
      } catch {
        case ex: SQLException =>
          logger.warn(s"Rewind failed for blocks since $startBlockNumber")
          false
        case NonFatal(e) =>
          logger.error(s"Unexpected error", e)
          false
      }
    }
  }

  object transactions extends DBStorageAPI.Transactions {

    override def addTransaction(
                                 tx: dlt.EthereumMinedTransaction,
                                 // The transaction already has the block number, but, passing the block hash,
                                 // we ensure that the block hasn’t been reorganized
                                 blockHash: String,
                               )(implicit
                                 session: DBSession = AutoSession
                               ): Unit = {
      require(EthUtils.Hashes.isValidBlockHash(blockHash), blockHash)

      sql"""
      INSERT INTO ucg_transaction(
        block_number,
        txhash, from_hash, to_hash,
        status, is_status_ok, ucg_comment, gas_price,
        gas_used, nonce, transaction_index, gas,
        value, effective_gas_price, cumulative_gas_used
      )
      VALUES (
        (SELECT number FROM ucg_block WHERE hash = $blockHash),
        ${tx.txhash}, ${tx.from}, ${tx.to},
        ${tx.status}, ${tx.isStatusOk}, NULL, ${tx.gasPrice},
        ${tx.gasUsed}, ${tx.nonce}, ${tx.transactionIndex}, ${tx.gas},
        ${tx.value}, ${tx.effectiveGasPrice}, ${tx.cumulativeGasUsed}
      )
      ON CONFLICT (txhash) DO UPDATE SET
        block_number = EXCLUDED.block_number,
        from_hash = EXCLUDED.from_hash,
        to_hash = EXCLUDED.to_hash,
        status = EXCLUDED.status,
        is_status_ok = EXCLUDED.is_status_ok,
        ucg_comment = EXCLUDED.ucg_comment,
        gas_price = EXCLUDED.gas_price,
        gas_used = EXCLUDED.gas_used,
        nonce = EXCLUDED.nonce,
        transaction_index = EXCLUDED.transaction_index,
        gas = EXCLUDED.gas,
        value = EXCLUDED.value,
        effective_gas_price = EXCLUDED.effective_gas_price,
        cumulative_gas_used = EXCLUDED.cumulative_gas_used
      """.execute.apply
    }
  }

  object txLogs extends DBStorageAPI.TxLogs {

    override def addTxLogs(
                            blockNumber: Int,
                            transactionHash: String,
                            txLogs: Seq[dlt.EthereumTxLog]
                          )(implicit
                            session: DBSession = AutoSession
                          ): Unit = {
      val batchParams: Seq[Seq[Any]] = txLogs.map(t => Seq(
        transactionHash,
        blockNumber,
        t.logIndex,
        t.address,
        t.topics.map(_.toArray).toArray,
        t.data.toArray
      ))
      sql"""
      INSERT INTO ucg_tx_log(
        transaction_id,
        block_number,
        log_index,
        address,
        topics,
        data
      )
      VALUES(
        (SELECT id FROM ucg_transaction WHERE txhash = ?),
        ?,
        ?,
        ?,
        ?,
        ?
      )
      ON CONFLICT (block_number, log_index) DO UPDATE SET
        transaction_id = EXCLUDED.transaction_id,
        address = EXCLUDED.address,
        topics = EXCLUDED.topics,
        data = EXCLUDED.data
      """
        .batch(batchParams: _*)
        .apply()(session, implicitly[Factory[Int, Seq[Int]]])
    }
  }

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
