package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.api.dlt.events.ERC20TransferEvent
import com.myodov.unicherrygarden.ethereum.{EthUtils, Ethereum}

/** Information for any transaction log (in a transaction) in Ethereum blockchain.
 * In the DB, stored in `ucg_tx_log` table.
 *
 * Each single log contains an information about a single event emitted.
 */
class EthereumTxLog(val logIndex: Int,
                    val topics: Seq[String],
                    val data: String
                   ) {
  require(logIndex >= 0, logIndex)
  require(topics != null && topics.forall(EthUtils.isValidHexString(_, 66)), topics)
  require(data != null && (data.equals("") || EthUtils.isValidHexString(data)), data)

  override def toString = s"EthereumTxLog(logIndex=$logIndex, topics=$topics, data=$data)"

  /** Whether any of the topics is equal to the `needle` (as in, “needle in haystack”), some data to be searched.
   *
   * @param needle some topic data to find.
   */
  def topicsContain(needle: String): Boolean = {
    require(EthUtils.isValidHexString(needle, 66), needle)
    topics.contains(needle)
  }

  /** Checks if the log is for ERC20 Transfer event; returns the parsed details if yes. */
  lazy val isErc20Transfer: Option[ERC20TransferEvent] = {
    topics match {
      case Seq(Ethereum.ERC20.TRANSFER_EVENT_SIGNATURE, fromStr: String, toStr: String) =>
        Some(ERC20TransferEvent(
          EthUtils.Uint256Str.toAddress(fromStr),
          EthUtils.Uint256Str.toAddress(toStr),
          EthUtils.Uint256Str.toBigInteger(data)
        ))
      case _ => None
    }
  }
}

object EthereumTxLog {
  @inline def apply(logIndex: Int,
                    topics: Seq[String],
                    data: String): EthereumTxLog = new EthereumTxLog(logIndex, topics, data)
}
