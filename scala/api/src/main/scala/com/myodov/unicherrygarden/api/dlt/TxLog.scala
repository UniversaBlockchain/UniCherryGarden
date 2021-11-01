package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.ethereum.{EthUtils, Ethereum}

case class ERC20TransferEvent(from: String, to: String, value: BigInt)

/** Information for any transaction log (in a transaction) in Ethereum blockchain;
 * stored in `ucg_tx_log` table.
 */
trait TxLog {
  val logIndex: Int
  require(logIndex >= 0, logIndex)

  val topics: Seq[String]
  require(topics != null && topics.forall(topic => EthUtils.isValidHexString(topic, 66)), topics)

  val data: String
  require(data != null && (data.equals("") || EthUtils.isValidHexString(data)), data)

  /** Checks if the logs are for ERC20 Transfer event; returns the parsed details if yes. */
  lazy val isErc20Transfer: Option[ERC20TransferEvent] = {
    topics match {
      case Seq(Ethereum.ERC20.TRANSFER_EVENT_SIGNATURE, fromStr: String, toStr: String) =>
        Option(ERC20TransferEvent(
          EthUtils.Uint256Str.toAddress(fromStr),
          EthUtils.Uint256Str.toAddress(toStr),
          EthUtils.Uint256Str.toBigInteger(data)
        ))
      case _ => Option.empty
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
