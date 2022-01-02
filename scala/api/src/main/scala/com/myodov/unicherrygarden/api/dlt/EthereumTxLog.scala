package com.myodov.unicherrygarden.api.dlt

import java.util.Objects

import com.myodov.unicherrygarden.api.dlt.events.Erc20TransferEvent
import com.myodov.unicherrygarden.ethereum.{EthUtils, Ethereum}
import com.typesafe.scalalogging.LazyLogging
import org.web3j.utils.Numeric.{hexStringToByteArray, toHexString}

/** Information for any transaction log (in a transaction) in Ethereum blockchain.
 * In the DB, stored in `ucg_tx_log` table.
 *
 * Each single log contains an information about a single event emitted.
 */
class EthereumTxLog(val logIndex: Int,
                    val address: String,
                    val topics: Seq[Seq[Byte]],
                    val data: Seq[Byte]
                   ) extends LazyLogging {
  require(logIndex >= 0, logIndex)
  //  require(topics != null && topics.forall(EthUtils.isValidHexString(_, 66)), topics)
  require(address != null && EthUtils.Addresses.isValidLowercasedAddress(address), address)
  require(topics != null && topics.forall(_.size == 32), (topics, topics.map(_.size)))
  //  require(data != null && (data.equals("") || data.equals("0x") || EthUtils.isValidHexString(data)), data)
  require(data != null, topics)

  override def toString = s"EthereumTxLog(address=$address, logIndex=$logIndex, topics=$topics, data=$data)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[EthereumTxLog]

  override def equals(other: Any): Boolean = other match {
    case that: EthereumTxLog =>
      (that canEqual this) &&
        logIndex == that.logIndex &&
        topics == that.topics &&
        data == that.data
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(logIndex, topics, data)
    Objects.hash(state: _*)
  }

  /** Whether any of the topics is equal to the `needle` (as in, “needle in haystack”), some data to be searched.
   *
   * @param needle some topic data to find.
   */
  @inline final def topicsContain(needle: Seq[Byte]): Boolean = {
    require(needle.size == 32, needle)
    topics.contains(needle)
  }

  /** Whether any of the topics is equal to the `needle` (as in, “needle in haystack”), some data to be searched.
   *
   * @param needle some topic data to find.
   */
  @inline final def topicsContain(needle: String): Boolean = {
    require(EthUtils.isValidHexString(needle, 66), needle)
    topicsContain(hexStringToByteArray(needle).toSeq)
  }

  /** Checks if the log is for ERC20 Transfer event; returns the parsed details if yes. */
  lazy val isErc20Transfer: Option[Erc20TransferEvent] = {
    val erc20TransferSig: Seq[Byte] = hexStringToByteArray(Ethereum.ERC20.TRANSFER_EVENT_SIGNATURE).toSeq

    topics match {
      case Seq(`erc20TransferSig`, fromBytes, toBytes) =>
        // This is real ERC20 Transfer signature
        require(fromBytes != null, fromBytes)
        require(toBytes != null, toBytes)

        Some(Erc20TransferEvent(
          EthUtils.Uint256Str.toAddress(toHexString(fromBytes.toArray[Byte])),
          EthUtils.Uint256Str.toAddress(toHexString(toBytes.toArray[Byte])),
          EthUtils.Uint256Str.toBigInteger(toHexString(data.toArray[Byte]))
        ))
      case _ => None
    }
  }
}

object EthereumTxLog {
  @inline def apply(logIndex: Int,
                    address: String,
                    topics: Seq[Seq[Byte]],
                    data: Seq[Byte]): EthereumTxLog =
    new EthereumTxLog(
      logIndex,
      address,
      topics,
      data)

  @inline def apply(logIndex: Int,
                    address: String,
                    topics: Seq[String],
                    data: String): EthereumTxLog =
    new EthereumTxLog(
      logIndex,
      address,
      topics.map(hexStringToByteArray(_).toSeq),
      hexStringToByteArray(data).toSeq)
}
