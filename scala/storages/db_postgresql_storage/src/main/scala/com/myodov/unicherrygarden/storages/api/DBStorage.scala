package com.myodov.unicherrygarden.storages.api

trait DBStorage {
}

object DBStorage {

  object Progress {

    /** Sync status of whole UniCherrypicker.
     *
     * @param from : UniCherrypicker in general has been synced from this block (may be missing).
     * @param to   : UniCherrypicker in general has been synced to this block (may be missing).
     */
    sealed case class OverallSyncStatus(from: Option[Int], to: Option[Int])

    /** Whole-system syncing progress.
     *
     * @param overall                     : overall progress.
     * @param currencies                  : progress of syncing as per `ucg_currency` table.
     * @param blocks                      : progress of syncing as per `ucg_block` table.
     * @param trackedAddresses            : progress of syncing as per `ucg_tracked_address` table.
     * @param perCurrencyTrackedAddresses : progress of syncing as per `ucg_currency_tracked_address_progress` table.
     */
    sealed case class ProgressData(overall: OverallSyncStatus,
                                   currencies: CurrenciesSyncStatus,
                                   blocks: BlocksSyncStatus,
                                   trackedAddresses: TrackedAddressesSyncStatus,
                                   perCurrencyTrackedAddresses: PerCurrencyTrackedAddressesSyncStatus)

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
     * @param minFrom    : minimum `synced_from_block_number` value among all tracked addresses.
     * @param maxFrom    : maximum `synced_from_block_number` value among all tracked addresses.
     * @param minTo      : minimum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param maxTo      : maximum `synced_to_block_number` value among all tracked addresses (may be missing).
     * @param toHasNulls : whether `synced_to_block_number` has nulls;
     *                   i.e., for some tracked addresses, the synced_to value is missing.
     */
    sealed case class TrackedAddressesSyncStatus(minFrom: Int, maxFrom: Int,
                                                 minTo: Option[Int], maxTo: Option[Int],
                                                 toHasNulls: Boolean)

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

}
