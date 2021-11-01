package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single transaction in blockchain, containing one or more transfers.
 *
 * It may or may not have been mined; so it typically contains the data available
 * from `eth.getTransaction()` call.
 */
trait Transaction {
  /** Transaction hash. */
  val txhash: String
  require(txhash != null && EthUtils.Hashes.isValidTransactionHash(txhash), txhash)

  /** Sender of the transaction (address). */
  val from: String
  require(from != null && EthUtils.Addresses.isValidLowercasedAddress(from), from)

  /** Receiver of the transaction (address). May be absent (if this is a transaction creating the smart contract). */
  val to: Option[String]
  require(to.isEmpty || EthUtils.Addresses.isValidLowercasedAddress(to.get), to)

  /** Gas (in gas units).
   *
   * From `eth.getTransaction()`.
   */
  val gas: BigInt
  require(gas != null && gas >= 0, gas)

  /** Gas price (in wei).
   *
   * From `eth.getTransaction()`.
   */
  val gasPrice: BigInt
  require(gasPrice != null && gasPrice >= 0, gasPrice)

  /** Gas price (in ETH). */
  def gasPriceInEth: BigDecimal = EthUtils.Wei.valueFromWeis(gasPrice.bigInteger)

  /** Nonce value.
   *
   * From `eth.getTransaction()`.
   */
  val nonce: Int
  require(nonce >= 0, nonce)

  /** Transaction value (in wei). */
  val value: BigInt
  require(value != null && value >= 0, value)

  /** Transaction value (in ETH). */
  def valueInEth: BigDecimal = EthUtils.Wei.valueFromWeis(value.bigInteger)


  override def toString = s"Transaction(txhash=$txhash, from=$from, to=$to, nonce=$nonce, value=$value)"
}

/** This is a transaction in blockchain, which has been mined and present in some block.
 *
 * Comparing to [[Transaction]] (and extending it), it contains the extra data available
 * from `eth.getTransactionReceipt()` call.
 */
trait MinedTransaction extends Transaction {
  /** Status of the transaction.
   *
   * From `eth.getTransactionReceipt()`. EIP 658, available since Byzantium fork, since block 4,370,000.
   */
  val status: Option[Int]

  /** Whether the status of transaction means it is valid. */
  def isStatusOk: Boolean = status.getOrElse(1) == 1

  /** The block in which the transaction is mined.
   *
   * From `eth.getTransaction()`, though will be non-null only if the transaction is mined already.
   */
  val blockNumber: BigInt
  require(blockNumber != null && blockNumber >= 0)

  /** The index of transaction in the block.
   *
   * From `eth.getTransaction()`, though will be non-null only if the transaction is mined already.
   */
  val transactionIndex: Int
  require(transactionIndex >= 0, transactionIndex)

  /** Gas used (in gas units).
   *
   * From `eth.getTransactionReceipt()`.
   */
  val gasUsed: BigInt
  require(gasUsed != null && gasUsed >= 0, gasUsed)

  /** Effective gas price (in wei).
   *
   * From `eth.getTransactionReceipt()`.
   */
  val effectiveGasPrice: BigInt
  require(effectiveGasPrice >= 0, effectiveGasPrice)

  /** Effective gas price (in ETH). */
  def effectiveGasPriceInEth: BigDecimal = EthUtils.Wei.valueFromWeis(effectiveGasPrice.bigInteger)

  /** Cumulative gas used (in gas units).
   *
   * From `eth.getTransactionReceipt()`.
   */
  val cumulativeGasUsed: BigInt
  require(cumulativeGasUsed >= 0, cumulativeGasUsed)


  override def toString = s"MinedTransaction(" +
    s"txhash=$txhash, from=$from, to=$to, nonce=$nonce, value=$value; " +
    s"status=$status, blockNumber=$blockNumber)"
}

/** Standard implementation of [[Transaction]] trait. */
class EthereumTransaction(
                           val txhash: String,
                           val from: String,
                           val to: Option[String],
                           val gas: BigInt,
                           val gasPrice: BigInt,
                           val nonce: Int,
                           val value: BigInt
                         ) extends Transaction

/** Standard implementation of [[MinedTransaction]] trait. */
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
                                val txLogs: Seq[TxLog]
                              ) extends MinedTransaction()

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
                     txLogs: Seq[TxLog] = Seq()
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
