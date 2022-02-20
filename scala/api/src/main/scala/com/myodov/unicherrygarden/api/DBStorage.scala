package com.myodov.unicherrygarden.api

import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.messages.cherrypicker.GetTrackedAddresses.TrackedAddressesRequestResultPayload
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.WrappedResultSet

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
                                    ) {
      lazy val toTrackedAddressInformation: TrackedAddressesRequestResultPayload.TrackedAddressInformation =
        new TrackedAddressesRequestResultPayload.TrackedAddressInformation(
          address,
          // The subsequent items may be Java-nullable
          comment.orNull,
          // Converting the Option[Int] to nullable Java Integers needs some cunning processing,
          // to avoid Null getting converted to 0
          syncedFrom.map(Integer.valueOf).orNull
        )
    }

  }

  object Blocks {

  }

  object Transactions {

  }

  object TxLogs {

  }

}
