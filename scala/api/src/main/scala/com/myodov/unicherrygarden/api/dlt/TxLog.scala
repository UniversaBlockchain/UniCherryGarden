package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.ethereum.{EthUtils, Ethereum}

case class ERC20TransferEvent(from: String, to: String, value: BigInt)

/** Information for any transaction log (in a transaction) in Ethereum blockchain;
 * stored in `ucg_tx_log` table.
 *
 * Each single log contains an information about a single event emitted.
 */
trait TxLog {
  val logIndex: Int
  require(logIndex >= 0, logIndex)

  val topics: Seq[String]
  require(topics != null && topics.forall(EthUtils.isValidHexString(_, 66)), topics)

  val data: String
  require(data != null && (data.equals("") || EthUtils.isValidHexString(data)), data)

  /** Whether any of the topics is the `needle` (as in, “needle in haystack”), some data to be searched.
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

  override def toString = s"TxLog(logIndex=$logIndex, topics=$topics, data=$data)"
}

/** Standard implementation of [[TxLog]] trait. */
class EthereumTxLog(val logIndex: Int,
                    val topics: Seq[String],
                    val data: String
                   ) extends TxLog

object EthereumTxLog {
  @inline def apply(logIndex: Int,
                    topics: Seq[String],
                    data: String): EthereumTxLog = new EthereumTxLog(logIndex, topics, data)
}
