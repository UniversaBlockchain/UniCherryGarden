package com.myodov.unicherrygarden.api

import com.myodov.unicherrygarden.api.DBStorage.Currencies.DBCurrency
import com.myodov.unicherrygarden.api.DBStorage.Progress.ProgressData
import com.myodov.unicherrygarden.api.DBStorage.TrackedAddresses.TrackedAddress
import com.myodov.unicherrygarden.api.types.MinedTransfer
import com.myodov.unicherrygarden.messages.cherrypicker.AddTrackedAddresses.StartTrackingAddressMode
import com.myodov.unicherrygarden.messages.cherrypicker.GetBalances.BalanceRequestResultPayload.CurrencyBalanceFact
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession}

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

    /** Get the information about a single tracked address (if present).
     */
    def getTrackedAddress(
                           address: String
                         )(implicit
                           session: DBSession = ReadOnlyAutoSession
                         ): Option[TrackedAddress]

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
