package com.myodov.unicherrygarden.storages

import com.myodov.unicherrygarden.api.types.dlt
import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}
import scalikejdbc._

/** Stores the blockchain information in PostgreSQL database. */
class PostgreSQLStorage(jdbcUrl: String,
                        dbUser: String,
                        dbPassword: String,
                        wipeOnStart: Boolean,
                        migrationPaths: List[String]
                       ) extends LazyLogging {
  //  private[this] implicit val session: AutoSession.type = AutoSession

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
     **/
    case class OverallSyncStatus(from: Option[Int], to: Option[Int])

    /** Sync status of `ucg_currency` table.
     *
     * @param minSyncFrom : minimum `sync_from_block_number` value among all supported currencies (may be missing).
     * @param maxSyncFrom : maximum `sync_from_block_number` value among all supported currencies (may be missing).
     **/
    case class CurrenciesSyncStatus(minSyncFrom: Option[Int], maxSyncFrom: Option[Int])

    /** Sync status of `ucg_block` table
     *
     * @param from : minimum block number in `ucg_block` table (may be missing).
     * @param to   : maximum block number in `ucg_block` tble (may be missing).
     **/
    case class BlocksSyncStatus(from: Option[Int], to: Option[Int])

    /** Sync status of `ucg_tracked_address` table.
     *
     * @param minFrom    : minimum `synced_from_block_number` value among all tracked addresses.
     * @param maxFrom    : maximum `synced_from_block_number` value among all tracked addresses.
     * @param minTo      : minimum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param maxTo      : maximum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param toHasNulls : whether `synced_to_block_number` has nulls;
     *                   i.e., for some tracked addresses, the synced_to value is missing.
     **/
    case class TrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int,
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
     **/
    case class PerCurrencyTrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int,
                                                     minTo: Option[Int], maxTo: Option[Int],
                                                     toHasNulls: Boolean)

    /** Whole-system syncing progress.
     *
     * @param overall                     : overall progress.
     * @param currencies                  : progress of syncing as per `ucg_currency` tble.
     * @param blocks                      : progress of syncing as per `ucg_block` table.
     * @param trackedAddresses            : progress of syncing as per `ucg_tracked_address` table.
     * @param perCurrencyTrackedAddresses : progress of syncing as per `ucg_currency_tracked_address_progress` tble.
     * */
    case class Progress(overall: OverallSyncStatus,
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
     * */
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
      sql"""
      UPDATE ucg_state
      SET sync_state = $state
      WHERE sync_state != $state
      """.execute.apply()
      logger.debug(s"Setting sync state: “${state}”")
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
      type CurrencyType = Value
      val Eth, Erc20 = Value

      def fromString(str: String): Value = {
        str match {
          case "ETH" => CurrencyTypes.Eth
          case "ERC20" => CurrencyTypes.Erc20
          case other => throw new RuntimeException(s"Unsupported currency type $other")
        }
      }

      def toInteropType(v: Value): dlt.Currency.CurrencyType = {
        v match {
          case CurrencyTypes.Eth => dlt.Currency.CurrencyType.ETH
          case CurrencyTypes.Erc20 => dlt.Currency.CurrencyType.ERC20
          case other => throw new RuntimeException(s"Unsupported currency type $other")
        }
      }
    }

    case class Currency(currencyType: CurrencyTypes.CurrencyType,
                        dAppAddress: Option[String],
                        name: Option[String],
                        symbol: Option[String],
                        ucgComment: Option[String]
                       )

    def getCurrencies(implicit session: DBSession = ReadOnlyAutoSession): List[Currency] = {
      sql"""
      SELECT * FROM ucg_currency;
      """.map(rs => Currency(
        CurrencyTypes.fromString(rs.string("type")),
        rs.stringOpt("dapp_address"),
        rs.stringOpt("name"),
        rs.stringOpt("symbol"),
        rs.stringOpt("ucg_comment"),
      )).list.apply()
    }
  }

  lazy val currencies: Currencies = new Currencies
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
