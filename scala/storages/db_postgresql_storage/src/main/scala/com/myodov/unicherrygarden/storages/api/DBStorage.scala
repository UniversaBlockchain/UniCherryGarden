package com.myodov.unicherrygarden.storages.api

import java.math.BigInteger

import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.types.MinedTransfer
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.StartTrackingAddressMode
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact
import com.myodov.unicherrygarden.storages.api.DBStorage.Currencies.DBCurrency
import com.myodov.unicherrygarden.storages.api.DBStorage.Progress.ProgressData
import com.myodov.unicherrygarden.storages.api.DBStorage.TrackedAddresses.TrackedAddress
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, WrappedResultSet}

import scala.collection.immutable.SortedMap

/** Any storage/database access connector. */
trait DBStorageAPI {
  /** All functions with the overall UniCherrypicker syncing progress. */
  val progress: DBStorageAPI.Progress

  /** Access `ucg_state` table. */
  val state: DBStorageAPI.State

  /** Access `ucg_currency` table. */
  val currencies: DBStorageAPI.Currencies

  /** Access `ucg_tracked_address` table. */
  val trackedAddresses: DBStorageAPI.TrackedAddresses

  /** Access `ucg_block` table. */
  val blocks: DBStorageAPI.Blocks

  /** Access `ucg_transaction` table. */
  val transactions: DBStorageAPI.Transactions

  /** Access `ucg_tx_log` table. */
  val txLogs: DBStorageAPI.TxLogs

  /** Access overall information about balances. */
  val balances: DBStorageAPI.Balances

  /** Access overall information about transfers. */
  val transfers: DBStorageAPI.Transfers
}

object DBStorageAPI {

  trait Progress {
    /** Get the overall syncing progress. */
    def getProgress(implicit session: DBSession = ReadOnlyAutoSession): Option[ProgressData]

    /** If we have any Currency Tracked Addresses (CT Addresses) which have never been started to sync,
     * find a (earliest possible) block to sync any of them.
     *
     * @return: [[Option]] with the first never-yet-started CT address;
     *          Option is empty if there is no such address found.
     */
    def getFirstBlockResolvingSomeNeverStartedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int]

    /** If we have any Currency Tracked Addresses (CT Addresses) which have never been synced till the end,
     * find a (earliest possible) block to sync any of them.
     *
     * @return: [[Option]] with the first unsynced PCT address;
     *          Option is empty if there is no such address found.
     */
    def getFirstBlockResolvingSomeNeverSyncedCTAddress(implicit session: DBSession = ReadOnlyAutoSession): Option[Int]
  }

  trait State {
    def setLastHeartbeatAt(implicit session: DBSession = AutoSession): Unit

    def setSyncState(state: String)(implicit session: DBSession = AutoSession): Unit

    def advanceProgress(
                         syncedBlockNumber: Long,
                         trackedAddresses: Set[String]
                       )(implicit session: DBSession): Unit
  }

  trait Currencies {
    /** Get all the currencies (filtered for verified/unverified) in the system. */
    def getCurrencies(
                       currencyKeys: Option[Set[String]],
                       getVerified: Boolean,
                       getUnverified: Boolean
                     )
                     (
                       implicit session: DBSession = ReadOnlyAutoSession
                     ): List[DBCurrency]
  }

  trait TrackedAddresses {

    /** Get the list of all tracked addresses;
     * optionally containing (or not containing) various extra information about each address.
     */
    def getTrackedAddresses(
                             includeComment: Boolean,
                             includeSyncedFrom: Boolean
                           )(implicit
                             session: DBSession = ReadOnlyAutoSession
                           ): List[TrackedAddress]

    /** Get the set of all tracked addresses (and just of the address strings, nothing else). */
    def getJustAddresses(implicit session: DBSession = ReadOnlyAutoSession): Set[String]

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
                         ): Boolean
  }

  trait Blocks {

    /** Add a new block record to the DB. */
    def addBlock(block: dlt.EthereumBlock
                )(implicit
                  session: DBSession = AutoSession
                ): Unit

    def getBlockByNumber(blockNumber: Int
                        )(implicit session: DBSession = ReadOnlyAutoSession): Option[dlt.EthereumBlock]

    /** Get a mapping from block number to block hash, for (up to, inclusive) `howMany` latest blocks. */
    def getLatestHashes(
                         howMany: Int
                       )(implicit session: DBSession = ReadOnlyAutoSession): SortedMap[Int, String]

    /** Perform a “rewind” of all blocks, starting from `startBlockNumber`. */
    def rewind(
                startBlockNumber: Int
              )(implicit session: DBSession = ReadOnlyAutoSession): Boolean
  }

  trait Transactions {

    /** Store the details about the transaction, optionally overwriting the existing if it is present already. */
    def addTransaction(
                        tx: dlt.EthereumMinedTransaction,
                        // The transaction already has the block number, but, passing the block hash,
                        // we ensure that the block hasn’t been reorganized
                        blockHash: String,
                      )(implicit
                        session: DBSession = AutoSession
                      ): Unit
  }

  trait TxLogs {
    /** Add a pack of Ethereum TX logs, all at once (atomically); overwrites the existing if present already. */
    def addTxLogs(
                   blockNumber: Int,
                   transactionHash: String,
                   txLogs: Seq[dlt.EthereumTxLog]
                 )(implicit
                   session: DBSession = AutoSession
                 ): Unit
  }

  trait Balances {
    /** Get information about ETH/ERC20 balances at some address `address` and no newer than at some block `maxBlock`,
     * optionally filtered by currency keys `currencyKeys`.
     */
    def getBalances(
                     address: String,
                     maxBlock: Int,
                     currencyKeys: Option[Set[String]]
                   )(implicit session: DBSession = ReadOnlyAutoSession): List[CurrencyBalanceFact]
  }

  trait Transfers {
    /** Get information about ETH/ERC20 balances at some address `address` and no newer than at some block `maxBlock`,
     * optionally filtered by currency keys `currencyKeys`.
     */
    def getTransfers(
                      sender: Option[String],
                      receiver: Option[String],
                      optStartBlock: Option[Int],
                      endBlock: Int,
                      currencyKeys: Option[Set[String]]
                    )(implicit session: DBSession = ReadOnlyAutoSession): List[MinedTransfer]
  }

}


object DBStorage {

  object Progress {

    /** Sync configuration of whole UniCherrypicker.
     *
     * @param from : UniCherrypicker in general should been synced from this block (may be missing).
     *             No blocks earlier than this block will exist.
     */
    sealed case class OverallSyncConfiguration(from: Option[Int])

    /** Whole-system syncing progress.
     *
     * @param overall                     : overall progress.
     * @param currencies                  : progress of syncing as per `ucg_currency` table.
     * @param blocks                      : progress of syncing as per `ucg_block` table.
     * @param trackedAddresses            : progress of syncing as per `ucg_tracked_address` table.
     * @param perCurrencyTrackedAddresses : progress of syncing as per `ucg_currency_tracked_address_progress` table.
     */
    sealed case class ProgressData(overall: OverallSyncConfiguration,
                                   currencies: CurrenciesSyncStatus,
                                   blocks: BlocksSyncStatus,
                                   trackedAddresses: TrackedAddressesSyncStatus,
                                   perCurrencyTrackedAddresses: PerCurrencyTrackedAddressesSyncStatus) extends LazyLogging {

      /** The block from which the HeadSyncer iteration should start.
       *
       * `None` if cannot be calculated.
       */
      lazy val headSyncerStartBlock: Option[Int] = (overall.from, blocks.to) match {
        case (None, _) =>
          // Cannot proceed
          logger.error(s"`ucg_state.synced_from_block_number` is not configured!")
          None
        case (Some(overallFrom), None) =>
          logger.debug(s"We have no blocks stored; starting from the very first block ($overallFrom)...")
          Some(overallFrom)
        case (Some(_), Some(maxStoredBlock)) =>
          logger.info(s"We have some blocks stored ($blocks); syncing from ${maxStoredBlock + 1}")
          Some(maxStoredBlock + 1)
      }

      /** Check if configuration is valid; log errors/warnings in any problematic case. */
      lazy val isConfigurationValid: Boolean = {
        lazy val overallFrom = overall.from.get // only if overall.from is not Empty

        if (overall.from.isEmpty) {
          logger.warn("CherryPicker is not configured: missing `ucg_state.synced_from_block_number`!")
          false
          // Since this point we can use overallFrom
        } else if (currencies.minSyncFrom.exists(_ < overallFrom)) {
          logger.error("The minimum `ucg_currency.sync_from_block_number` value " +
            s"is ${currencies.minSyncFrom.get}; " +
            s"it should not be lower than $overallFrom!")
          false
        } else if (trackedAddresses.minFrom < overallFrom) {
          logger.error("The minimum `ucg_tracked_address.synced_from_block_number` value " +
            s"is ${trackedAddresses.minFrom}; " +
            s"it should not be lower than $overallFrom!")
          false
        } else {
          true
        }
      }
    }

    /** Sync status of `ucg_currency` table.
     *
     * @param minSyncFrom : minimum `sync_from_block_number` value among all supported currencies (may be missing).
     * @param maxSyncFrom : maximum `sync_from_block_number` value among all supported currencies (may be missing).
     */
    sealed case class CurrenciesSyncStatus(minSyncFrom: Option[Int], maxSyncFrom: Option[Int])

    /** Sync status of `ucg_block` table
     *
     * @param from : minimum block number in `ucg_block` table (may be missing).
     * @param to   : maximum block number in `ucg_block` table (may be missing).
     */
    sealed case class BlocksSyncStatus(from: Option[Int], to: Option[Int])

    /** Sync status of `ucg_tracked_address` table.
     *
     * @param minFrom : minimum `synced_from_block_number` value among all tracked addresses.
     * @param maxFrom : maximum `synced_from_block_number` value among all tracked addresses.
     */
    sealed case class TrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int)

    /** Sync status of `ucg_currency_tracked_address_progress` table.
     *
     * @param minFrom    : minimum `synced_from_block_number` value among all currency/tracked-address pairs
     *                   (may be missing on very beginning of the sync).
     * @param maxFrom    : maximum `synced_from_block_number` value among all currency/tracked-address pairs
     *                   (may be missing on very beginning of the sync).
     * @param minTo      : minimum `synced_to_block_number` value among all currency/tracked-address pairs
     *                   (may be missing).
     * @param maxTo      : maximum `synced_to_block_number` value among all currency/tracked-address pairs
     *                   (may be missing).
     * @param toHasNulls : whether `synced_to_block_number` has nulls;
     *                   i.e., for some currency/tracked-address pairs, the synced_to value is missing.
     */
    sealed case class PerCurrencyTrackedAddressesSyncStatus(minFrom: Option[Int], maxFrom: Option[Int],
                                                            minTo: Option[Int], maxTo: Option[Int],
                                                            toHasNulls: Boolean)

  }

  object State {

  }

  object Currencies {

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


    /** Single instance of currency/token/asset, with all the data stored. */
    sealed case class DBCurrency(currencyType: CurrencyTypes.DBCurrencyType,
                                 dAppAddress: Option[String],
                                 name: Option[String],
                                 symbol: Option[String],
                                 ucgComment: Option[String],
                                 verified: Boolean,
                                 decimals: Option[Int],
                                 transferGasLimit: Option[BigInt]
                                ) {
      require((currencyType == CurrencyTypes.Eth) == dAppAddress.isEmpty, (currencyType, dAppAddress))
      require(dAppAddress.isEmpty || EthUtils.Addresses.isValidLowercasedAddress(dAppAddress.get), dAppAddress)

      lazy val asAsset: dlt.Asset = {
        currencyType match {
          case CurrencyTypes.Eth => dlt.Ether
          case CurrencyTypes.Erc20 => dlt.ERC20Token(dAppAddress.get)
          case _ => {
            throw new RuntimeException(s"Unsupported currencyType $currencyType")
          }
        }
      }

      lazy val asCurrency: Currency = new Currency(
        DBStorage.Currencies.CurrencyTypes.toInteropType(currencyType),
        dAppAddress.orNull,
        name.orNull,
        symbol.orNull,
        ucgComment.orNull,
        verified,
        decimals.map(Integer.valueOf).orNull,
        transferGasLimit.map(_.bigInteger).orNull
      )
    }

    object DBCurrency {
      def fromUcgCurrency(rs: WrappedResultSet, prefix: String = ""): DBCurrency =
        DBCurrency(
          CurrencyTypes.fromString(rs.string(s"${prefix}type")),
          rs.stringOpt(s"${prefix}dapp_address"),
          rs.stringOpt(s"${prefix}name"),
          rs.stringOpt(s"${prefix}symbol"),
          rs.stringOpt(s"${prefix}ucg_comment"),
          rs.boolean(s"${prefix}verified"),
          rs.intOpt(s"${prefix}decimals"),
          rs.bigIntOpt(s"${prefix}transfer_gas_limit").map(BigInt(_)),
        )
    }
  }

  object TrackedAddresses {

    /** Single instance of tracked address.
     * Note that the [[Option]] arguments being [[None]] don’t necessary mean the data really has NULL here:
     * they may be empty if the result has been requested without this piece of data.
     */
    sealed case class TrackedAddress(address: String,
                                     comment: Option[String],
                                     syncedFrom: Option[Int]
                                    )

  }

  object Blocks {

  }

  object Transactions {

  }

  object TxLogs {

  }

}
