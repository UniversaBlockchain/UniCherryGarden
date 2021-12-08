package com.myodov.unicherrygarden.api.dlt

import java.util.Objects

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single transaction in blockchain, containing one or more transfers.
 *
 * It may or may not have been mined; so it typically contains the data available
 * from `eth.getTransaction()` call.
 *
 * @param txhash   Transaction hash.
 * @param from     Sender of the transaction (address).
 * @param to       Receiver of the transaction (address).
 *                 May be absent (if this is a transaction creating the smart contract).
 * @param gas      Gas (in gas units).
 *                 From `eth.getTransaction()`.
 * @param gasPrice Gas price (in wei).
 *                 From `eth.getTransaction()`.
 * @param nonce    Nonce value.
 *                 From `eth.getTransaction()`.
 * @param value    Transaction value (in wei).
 */
class EthereumTransaction(
                           val txhash: String,
                           val from: String,
                           val to: Option[String],
                           val gas: BigInt,
                           val gasPrice: BigInt,
                           val nonce: Int,
                           val value: BigInt
                         ) {
  require(txhash != null && EthUtils.Hashes.isValidTransactionHash(txhash), txhash)
  require(from != null && EthUtils.Addresses.isValidLowercasedAddress(from), from)
  require(to.isEmpty || EthUtils.Addresses.isValidLowercasedAddress(to.get), to)
  require(gas != null && gas >= 0, gas)
  require(gasPrice != null && gasPrice >= 0, gasPrice)
  require(nonce >= 0, nonce)
  require(value != null && value >= 0, value)

  override def toString = s"EthereumTransaction(txhash=$txhash, from=$from, to=$to, nonce=$nonce, " +
    s"value=$value)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[EthereumTransaction]

  override def equals(other: Any): Boolean = other match {
    case that: EthereumTransaction =>
      (that canEqual this) &&
        txhash == that.txhash &&
        from == that.from &&
        to == that.to &&
        gas == that.gas &&
        gasPrice == that.gasPrice &&
        nonce == that.nonce &&
        value == that.value
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(txhash, from, to, gas, gasPrice, nonce, value)
    Objects.hash(state: _*)
  }


  /** Gas price (in ETH). */
  lazy val gasPriceInEth: BigDecimal = EthUtils.Wei.valueFromWeis(gasPrice.bigInteger)

  /** Transaction value (in ETH). */
  lazy val valueInEth: BigDecimal = EthUtils.Wei.valueFromWeis(value.bigInteger)
}

/** This is a transaction in blockchain, which has been mined and present in some block.
 *
 * Comparing to [[EthereumTransaction]] (and extending it), it contains the extra data available
 * from `eth.getTransactionReceipt()` call.
 *
 * @param status            Status of the transaction.
 *                          From `eth.getTransactionReceipt()`.
 *                          EIP 658, available since Byzantium fork, since block 4,370,000.
 * @param blockNumber       The block in which the transaction is mined.
 *                          From `eth.getTransaction()`, though will be non-null only if the transaction
 *                          is mined already.
 * @param transactionIndex  The index of transaction in the block.
 *                          From `eth.getTransaction()`, though will be non-null only if the transaction
 *                          is mined already.
 * @param gasUsed           Gas used (in gas units).
 *                          From `eth.getTransactionReceipt()`.
 * @param effectiveGasPrice Effective gas price (in wei).
 *                          From `eth.getTransactionReceipt()`.
 * @param cumulativeGasUsed Cumulative gas used (in gas units).
 *                          From `eth.getTransactionReceipt()`.
 * @param txLogs            The transaction logs.
 *                          From `eth.getTransactionReceipt()`.
 */
class EthereumMinedTransaction( // Transaction-specific
                                override val txhash: String,
                                override val from: String,
                                override val to: Option[String],
                                override val gas: BigInt,
                                override val gasPrice: BigInt,
                                override val nonce: Int,
                                override val value: BigInt,
                                // MinedTransaction-specific
                                val status: Option[Int],
                                val blockNumber: BigInt,
                                val transactionIndex: Int,
                                val gasUsed: BigInt,
                                val effectiveGasPrice: BigInt,
                                val cumulativeGasUsed: BigInt,
                                val txLogs: Seq[EthereumTxLog]
                              ) extends EthereumTransaction(txhash, from, to, gas, gasPrice, nonce, value) {
  require(blockNumber != null && blockNumber >= 0)
  require(transactionIndex >= 0, transactionIndex)
  require(gasUsed != null && gasUsed >= 0, gasUsed)
  require(effectiveGasPrice >= 0, effectiveGasPrice)
  require(cumulativeGasUsed >= 0, cumulativeGasUsed)

  private final val _DEBUG_DETAILED_TOSTRING = false

  override def toString = if (_DEBUG_DETAILED_TOSTRING)
    s"EthereumMinedTransaction(" +
      s"txhash=$txhash, from=$from, to=$to, gas=$gas, gasPrice=$gasPrice, nonce=$nonce, value=$value; " +
      s"status=$status, blockNumber=$blockNumber, transactionIndex=$transactionIndex, " +
      s"gasUsed=$gasUsed, effectiveGasPrice=$effectiveGasPrice, cumulativeGasUsed=$cumulativeGasUsed, " +
      s"txLogs=$txLogs"
  else
    s"EthereumMinedTransaction(" +
      s"txhash=$txhash, from=$from, to=$to, nonce=$nonce, value=$value; " +
      s"status=$status, blockNumber=$blockNumber; txLogs=List(${txLogs.size} items))"

  override def canEqual(other: Any): Boolean = other.isInstanceOf[EthereumMinedTransaction]

  override def equals(other: Any): Boolean = other match {
    case that: EthereumMinedTransaction =>
      super.equals(that) &&
        (that canEqual this) &&
        status == that.status &&
        blockNumber == that.blockNumber &&
        transactionIndex == that.transactionIndex &&
        gasUsed == that.gasUsed &&
        effectiveGasPrice == that.effectiveGasPrice &&
        cumulativeGasUsed == that.cumulativeGasUsed &&
        txLogs == that.txLogs
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(super.hashCode(), status, blockNumber, transactionIndex, gasUsed, effectiveGasPrice, cumulativeGasUsed, txLogs)
    Objects.hash(state: _*)
  }


  /** Whether the status of transaction means it is valid. */
  lazy val isStatusOk: Boolean = status.getOrElse(1) == 1

  /** Effective gas price (in ETH). */
  lazy val effectiveGasPriceInEth: BigDecimal = EthUtils.Wei.valueFromWeis(effectiveGasPrice.bigInteger)

  /** Whether any of the logs contains any of the topics matching the `needle` (as in, “needle in haystack”),
   * some data to be searched.
   */
  def anyTxLogContainsTopic(needle: String): Boolean = {
    require(EthUtils.isValidHexString(needle, 66), needle)
    txLogs.exists(_.topicsContain(needle))
  }
}

object EthereumMinedTransaction {
  @inline def apply( // Transaction-specific
                     txhash: String,
                     from: String,
                     to: Option[String],
                     gas: BigInt,
                     gasPrice: BigInt,
                     nonce: Int,
                     value: BigInt,
                     // MinedTransaction-specific
                     status: Option[Int],
                     blockNumber: BigInt,
                     transactionIndex: Int,
                     gasUsed: BigInt = 0,
                     effectiveGasPrice: BigInt = 0,
                     cumulativeGasUsed: BigInt = 0,
                     txLogs: Seq[EthereumTxLog] = Seq()
                   ): EthereumMinedTransaction = {
    require(txhash != null, txhash)
    require(from != null, from)
    require(to.getOrElse("") != null, to)

    new EthereumMinedTransaction(
      txhash,
      from,
      to,
      gas,
      gasPrice,
      nonce,
      value,
      status,
      blockNumber,
      transactionIndex,
      gasUsed,
      effectiveGasPrice,
      cumulativeGasUsed,
      txLogs
    )
  }
}
