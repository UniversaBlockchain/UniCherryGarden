package com.myodov.unicherrygarden.storages

import java.sql.SQLException

import com.myodov.unicherrygarden.Tools.seqIsIncrementing
import com.myodov.unicherrygarden.api.DBStorage.Currencies.DBCurrency
import com.myodov.unicherrygarden.api.types.MinedTransfer
import com.myodov.unicherrygarden.api.types.dlt.{Block, MinedTx}
import com.myodov.unicherrygarden.api.types.planted.transactions.SignedOutgoingTransfer
import com.myodov.unicherrygarden.api.{DBStorageAPI, dlt}
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.StartTrackingAddressMode
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact
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

    import com.myodov.unicherrygarden.api.DBStorage.Progress._

    override final def getProgress(implicit session: DBSession = ReadOnlyAutoSession): Option[ProgressData] = {
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
            minFrom = rs.intOpt("address_from_min"),
            maxFrom = rs.intOpt("address_from_max")
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
        .apply()
    }

    override final def getFirstBlockResolvingSomeNeverStartedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int] = {
      sql"""
      SELECT
          GREATEST(currency.sync_from_block_number, address.synced_from_block_number)
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
        .apply()
        .flatten // Option[Option[Int]] to Option[Int]
    }

    override final def getFirstBlockResolvingSomeNeverSyncedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int] = {
      sql"""
      SELECT
          GREATEST(currency.sync_from_block_number, address.synced_from_block_number)
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
        .apply()
        .flatten // Option[Option[Int]] to Option[Int]
    }
  }

  object state extends DBStorageAPI.State {
    def setRestartedAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET restarted_at=now()
      """.execute.apply()
    }

    override final def setLastHeartbeatAt(implicit session: DBSession = AutoSession) = {
      sql"""
      UPDATE ucg_state SET last_heartbeat_at=now()
      """.execute.apply()
    }

    override final def setSyncState(state: String)(implicit session: DBSession = AutoSession) = {
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

    /** Move the progress to the next block:
     *
     * @param syncedBlockNumber the number of the block that has just been synced/stored in the DB.
     * @param trackedAddresses  the sequence of tracked addresses for which the blockchain progress has been read,
     *                          parsed and stored.
     *                          We cannot read it from the DB right now and rely upon it,
     *                          because what if the tracked addresses have changed already since
     *                          the reading/parsing time?
     */
    override final def advanceProgress(
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
      //    (whichever is higher - so we can start at later block and sync less blocks overall).
      {
        sql"""
        WITH
            -- Function argument
            arg(block_number) AS (
                SELECT $syncedBlockNumber
            ),
            -- Function argument
            actually_tracked_address_arg(address) AS (
                SELECT unnest(ARRAY[$trackedAddresses]::TEXT[])
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
                    GREATEST(currency.sync_from_block_number,
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
                    -- to the desired cta.synced_from
                    -- (which is the larger of currency.from and address.from).
                    arg.block_number = GREATEST(currency.sync_from_block_number,
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
        """.execute.apply()
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
                SELECT unnest(ARRAY[$trackedAddresses]::TEXT[])
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
                      -- only if it the current block matches the currently recorded sync_from_block_number
                      (
                          synced_to_block_number IS NULL AND
                          arg.block_number = cta_progress.synced_from_block_number
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
        """.execute.apply()
      }
    }
  }

  object currencies extends DBStorageAPI.Currencies {

    import com.myodov.unicherrygarden.api.DBStorage.Currencies._

    override final def getCurrencies(
                                      currencyKeys: Option[Set[String]],
                                      getVerified: Boolean,
                                      getUnverified: Boolean
                                    )
                                    (
                                      implicit session: DBSession = ReadOnlyAutoSession
                                    ): List[DBCurrency] =
      if (getVerified || getUnverified) {
        // What values are allowed for `verified` field in a query we’ll run?
        val verifiedValues = (
          (if (getVerified) Seq(true) else Seq())
            ++
            (if (getUnverified) Seq(false) else Seq())
          )

        sql"""
        WITH
          _vars AS (
              SELECT
                  ARRAY [${currencyKeys.map(_.toSeq).orNull}]::TEXT[] AS filter_currency_keys
          ),
          vars AS (
              SELECT
                  _vars.*,
                  filter_currency_keys != ARRAY[NULL]::TEXT[] AS has_filter_currency_keys
              FROM _vars
          )
        SELECT *
        FROM
            vars,
            ucg_currency AS currency
        WHERE
            currency.verified IN ($verifiedValues) AND
            (
                NOT has_filter_currency_keys OR
                ucg_get_currency_code(currency.type, currency.dapp_address) = ANY (filter_currency_keys)
            );
        """.map(DBCurrency.fromUcgCurrency(_)).list.apply()
      } else {
        // Haven't asked for any currencies; so the result is definitely empty
        List.empty
      }
  }

  object trackedAddresses extends DBStorageAPI.TrackedAddresses {

    import com.myodov.unicherrygarden.api.DBStorage.TrackedAddresses._

    override final def getTrackedAddresses(
                                            filterAddresses: Option[Set[String]],
                                            includeComment: Boolean,
                                            includeSyncedFrom: Boolean
                                          )(implicit
                                            session: DBSession = ReadOnlyAutoSession
                                          ): List[TrackedAddress] = {
      sql"""
      WITH
          _vars AS (
              SELECT
                  ARRAY [${filterAddresses.map(_.toSeq).orNull}]::TEXT[] AS filter_addresses
          ),
          vars AS (
              SELECT
                  _vars.*,
                  filter_addresses != ARRAY[NULL]::TEXT[] AS has_filter_addresses
              FROM _vars
          )
      SELECT DISTINCT
          ucg_tracked_address.address,
          CASE WHEN $includeComment THEN ucg_tracked_address.ucg_comment ELSE NULL END AS ucg_comment,
          CASE WHEN $includeSyncedFrom THEN ucg_tracked_address.synced_from_block_number ELSE NULL END AS synced_from_block_number,
          1 + first_value(ucg_planted_transfer.nonce) OVER w AS next_planting_nonce
      FROM
          vars,
          ucg_tracked_address
          LEFT JOIN ucg_planted_transfer
              ON ucg_planted_transfer.sender = ucg_tracked_address.address
      WHERE
          CASE
              WHEN has_filter_addresses
                  THEN ucg_tracked_address.address = ANY (filter_addresses)
              ELSE TRUE
          END
          WINDOW w AS (PARTITION BY address ORDER BY nonce DESC);
      """.map(rs => TrackedAddress(
        rs.string("address"),
        rs.stringOpt("ucg_comment"),
        rs.intOpt("synced_from_block_number"),
        rs.intOpt("next_planting_nonce")
      )).list.apply()
    }

    override final def getJustAddresses(implicit session: DBSession = ReadOnlyAutoSession): Set[String] = {
      sql"""
      SELECT address FROM ucg_tracked_address;
      """.map(_.string("address")).list.apply().toSet
    }

    override final def getTrackedAddress(
                                          address: String
                                        )(implicit
                                          session: DBSession = ReadOnlyAutoSession
                                        ): Option[TrackedAddress] = {
      sql"""
      SELECT
          ucg_tracked_address.address,
          ucg_tracked_address.ucg_comment,
          ucg_tracked_address.synced_from_block_number,
          1 + first_value(ucg_planted_transfer.nonce) OVER w AS next_planting_nonce
      FROM
          ucg_tracked_address
          LEFT JOIN ucg_planted_transfer
                    ON ucg_planted_transfer.sender = ucg_tracked_address.address
      WHERE
          address = $address
          WINDOW w AS (PARTITION BY address ORDER BY nonce DESC);
      """.map(rs => TrackedAddress(
        rs.string("address"),
        rs.stringOpt("ucg_comment"),
        rs.intOpt("synced_from_block_number"),
        rs.intOpt("next_planting_nonce")
      )).single
        .apply()
    }

    override final def addTrackedAddress(
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
            WHEN 'LATEST_CHERRYGARDEN_SYNCED_BLOCK' THEN (SELECT currency_address_to_max FROM ucg_progress)
            ELSE NULL -- should fail
          END
        );
        """.execute.apply()
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

    override final def addBlock(block: dlt.EthereumBlock
                               )(implicit
                                 session: DBSession = AutoSession
                               ) = {
      sql"""
      INSERT INTO ucg_block(number, hash, parent_hash, timestamp)
      VALUES (${block.number}, ${block.hash}, ${block.parentHash}, ${block.timestamp})
      """.execute.apply()
    }

    override final def getBlockByNumber(
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
        .apply()
    }

    override final def getLatestHashes(
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
          .apply()
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

    override final def rewind(
                               startBlockNumber: Int
                             )(implicit session: DBSession = AutoSession): Boolean = {
      try {
        logger.debug(s"Performing rewind of blocks since $startBlockNumber")

        sql"""
        DELETE FROM ucg_tx_log
        WHERE block_number >= $startBlockNumber
        """.execute.apply()
        logger.debug(s"Rewound ucg_tx_log")

        sql"""
        DELETE FROM ucg_transaction
        WHERE block_number >= $startBlockNumber
        """.execute.apply()
        logger.debug(s"Rewound ucg_transaction")

        sql"""
        DELETE FROM ucg_block
        WHERE number >= $startBlockNumber
        """.execute.apply()
        logger.debug(s"Rewound ucg_block")

        sql"""
        UPDATE ucg_currency_tracked_address_progress
        SET synced_to_block_number = $startBlockNumber - 1
        WHERE synced_to_block_number >= $startBlockNumber
        """.execute.apply()
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

    override final def addTransaction(
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
        txhash, "from", "to",
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
        "from" = EXCLUDED."from",
        "to" = EXCLUDED."to",
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
      """.execute.apply()
    }
  }

  object txLogs extends DBStorageAPI.TxLogs {

    override final def addTxLogs(
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

  object balances extends DBStorageAPI.Balances {
    override final def getBalances(
                                    address: String,
                                    maxBlock: Int,
                                    currencyKeys: Option[Set[String]]
                                  )(implicit session: DBSession = ReadOnlyAutoSession): List[CurrencyBalanceFact] =
      sql"""
      WITH
          _vars AS (
              SELECT
                  $maxBlock AS end_block,
                  $address AS address,
                  ARRAY [${currencyKeys.map(_.toSeq).orNull}]::TEXT[] AS filter_currency_keys
          ),
          vars AS (
              SELECT
                  _vars.*,
                  filter_currency_keys != ARRAY[NULL]::TEXT[] AS has_filter_currency_keys
              FROM _vars
          ),
          currencies AS (
              SELECT currencies.*
              FROM
                  vars
                  CROSS JOIN ucg_get_currencies_for_keys_filter(has_filter_currency_keys, filter_currency_keys) AS currencies
          ),
          -- ETH transfers for the requested address,
          -- from the block calculated using `ucg_latest_block_with_eth_transfer_for_address`.
          -- All ETH transfers from this block are retrieved.
          latest_eth_transfers_ambig AS (
              SELECT
                  eth_transfer.*,
                  (last_value(transaction_index)
                   OVER (PARTITION BY currency_id, address
                       ORDER BY block_number, transaction_index
                       ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)) AS max_transaction_index_in_block
              FROM
                  vars
                  CROSS JOIN currencies
                  INNER JOIN ucg_eth_transfer_tr_addr_w_balance AS eth_transfer
                             USING (currency_id, address)
                  INNER JOIN ucg_latest_block_with_eth_transfer_for_address(
                          currencies.currency_id, vars.address, vars.end_block)
                             USING(block_number)
          ),
          -- Only the latest ETH transfer in the block.
          latest_eth_transfer AS (
              SELECT *
              FROM latest_eth_transfers_ambig
              WHERE transaction_index = max_transaction_index_in_block
          ),
          -- ERC20 transfers for the requested address and all the requested currencies,
          -- from the block calculated using `ucg_latest_block_with_verified_erc20_transfer_for_address`.
          -- All ERC20 transfers (for these currencies) from this block are retrieved (even multiple ones).
          latest_erc20_transfers_ambig AS (
              SELECT
                  erc20_transfer.*,
                  (last_value(log_index)
                   OVER (PARTITION BY currency_id, address
                       ORDER BY block_number, log_index
                       ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)) AS max_log_index_in_block
              FROM
                  vars
                  CROSS JOIN currencies
                  INNER JOIN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance AS erc20_transfer
                             USING (currency_id, address)
                  INNER JOIN ucg_latest_block_with_verified_erc20_transfer_for_address(
                          currencies.currency_id, vars.address, vars.end_block) USING (block_number)
          ),
          -- Only the latest ERC20 transfers in the block.
          latest_erc20_transfers AS (
              SELECT *
              FROM latest_erc20_transfers_ambig
              WHERE log_index = max_log_index_in_block
          ),
          latest_transfers AS (
              SELECT
                  currency_id,
                  block_number,
                  balance
              FROM latest_eth_transfer
              UNION
              SELECT
                  currency_id,
                  block_number,
                  balance
              FROM latest_erc20_transfers
          )
      SELECT
          latest_transfers.*,
          ucg_currency.type AS currency_type,
          ucg_currency.name AS currency_name,
          ucg_currency.symbol AS currency_symbol,
          ucg_currency.dapp_address AS currency_dapp_address,
          ucg_currency.ucg_comment AS currency_ucg_comment,
          ucg_currency.verified AS currency_verified,
          ucg_currency.decimals AS currency_decimals
      FROM
          latest_transfers
          INNER JOIN ucg_currency
                     ON ucg_currency.id = latest_transfers.currency_id;
          """.map(rs => new CurrencyBalanceFact(
        DBCurrency.fromUcgCurrency(rs, "currency_").asCurrency,
        rs.bigDecimal("balance"),
        rs.int("block_number")
      )).list.apply()
  }

  object transfers extends DBStorageAPI.Transfers {
    override final def getTransfers(
                                     sender: Option[String],
                                     receiver: Option[String],
                                     optStartBlock: Option[Int],
                                     endBlock: Int,
                                     currencyKeys: Option[Set[String]]
                                   )(implicit session: DBSession = ReadOnlyAutoSession): List[MinedTransfer] =
      sql"""
      WITH
          _vars AS (
              SELECT
                  $sender::TEXT AS "from",            -- NULL, but not together with receiver
                  $receiver::TEXT AS "to",            -- NULL, but not together with sender
                  $optStartBlock::INT AS start_block, -- NULL
                  $endBlock AS end_block,             -- NON NULL
                  ARRAY [${currencyKeys.map(_.toSeq).orNull}] AS filter_currency_keys
          ),
          vars AS (
              SELECT
                  _vars.*,
                  filter_currency_keys != ARRAY [NULL]::TEXT[] AS has_filter_currency_keys
              FROM _vars
          ),
          currencies AS (
              SELECT currencies.*
              FROM
                  vars
                  CROSS JOIN ucg_get_currencies_for_keys_filter(has_filter_currency_keys, filter_currency_keys) AS currencies
          ),
          eth_transfers AS (
              SELECT eth_transfer.*
              FROM
                  vars
                  CROSS JOIN currencies
                  INNER JOIN ucg_eth_transfer AS eth_transfer
                             USING (currency_id)
              WHERE
                  -- Extra condition for "from"
                  CASE vars."from" IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE eth_transfer."from" = vars."from" -- "from" condition
                  END AND
                  -- Extra condition for "to"
                  CASE vars."to" IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE eth_transfer."to" = vars."to" -- "to" condition
                  END AND
                  -- Extra condition for start block
                  CASE start_block IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE eth_transfer.block_number >= start_block -- start_block condition
                  END AND
                  -- Extra condition for end block
                  eth_transfer.block_number <= end_block
          ),
          erc20_transfers AS (
              SELECT erc20_transfer.*
              FROM
                  vars
                  CROSS JOIN currencies
                  INNER JOIN ucg_erc20_transfer_for_verified_currency AS erc20_transfer
                             USING (currency_id)
              WHERE
                  -- Extra condition for "from"
                  CASE vars."from" IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE erc20_transfer."from" = vars."from" -- "from" condition
                  END AND
                  -- Extra condition for "to"
                  CASE vars."to" IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE erc20_transfer."to" = vars."to" -- "to" condition
                  END AND
                  -- Extra condition for start block
                  CASE start_block IS NULL
                      WHEN TRUE THEN TRUE -- no condition
                      ELSE erc20_transfer.block_number >= start_block -- start_block condition
                  END AND
                  -- Extra condition for end block
                  erc20_transfer.block_number <= end_block
          )
      SELECT
          transfers.*,
          COALESCE(ucg_currency.dapp_address, '') AS currency_key,
          ucg_block.hash AS block_hash,
          ucg_block.timestamp AS block_timestamp,
          ucg_transaction."from" AS tx_from,
          ucg_transaction."to" AS tx_to,
          ucg_transaction.transaction_index
      FROM
          (
              SELECT
                  transaction_id,
                  txhash,
                  block_number,
                  currency_id,
                  transaction_index AS second_order_key_in_block,
                  NULL AS log_index,
                  "from",
                  "to",
                  value_human,
                  fees_total_human
              FROM eth_transfers
              UNION
              SELECT
                  transaction_id,
                  transaction_hash AS txhash,
                  block_number,
                  currency_id,
                  log_index AS second_order_key_in_block,
                  log_index,
                  "from",
                  "to",
                  value_human,
                  0 AS fees_total_human
              FROM erc20_transfers
          ) AS transfers
          INNER JOIN ucg_currency
              ON ucg_currency.id = transfers.currency_id
          INNER JOIN ucg_block
              ON ucg_block.number = transfers.block_number
          INNER JOIN ucg_transaction
              ON ucg_transaction.id = transfers.transaction_id
      ORDER BY
          block_number,
          currency_id,
          second_order_key_in_block;
      """.map(rs => new MinedTransfer(
        rs.string("from"),
        rs.string("to"),
        rs.string("currency_key"),
        rs.bigDecimal("value_human"),
        new MinedTx(
          rs.string("txhash"),
          rs.string("tx_from"),
          rs.stringOpt("tx_to").orNull,
          new Block(
            rs.int("block_number"),
            rs.string("block_hash"),
            rs.timestamp("block_timestamp").toInstant
          ),
          rs.int("transaction_index"),
          rs.bigDecimal("fees_total_human")
        ),
        rs.intOpt("log_index").map(Integer.valueOf).orNull
      )).list.apply()
  }

  object plants extends DBStorageAPI.Plants {
    override final def addTransferToPlant(
                                           transfer: SignedOutgoingTransfer,
                                           comment: Option[String]
                                         )(
                                           implicit session: DBSession
                                         ): (Boolean, Long) = {
      (false, 5)
    }
  }

}

object PostgreSQLStorage {
  @inline final def apply(jdbcUrl: String,
                          dbUser: String,
                          dbPassword: String,
                          wipeOnStart: Boolean,
                          migrationPaths: List[String]
                         ): PostgreSQLStorage = {
    // Initial size is 7:
    // 2 for TailSyncer,
    // 2 for HeadSyncer,
    // 1 for CherryGardener,
    // 1 for CherryPlanter,
    // 1 reserved for any incoming connection
    val poolSettings = ConnectionPoolSettings(initialSize = 7, maxSize = 16)
    ConnectionPool.singleton(jdbcUrl, dbUser, dbPassword, poolSettings)
    new PostgreSQLStorage(jdbcUrl, dbUser, dbPassword, wipeOnStart, migrationPaths)
  }
}
