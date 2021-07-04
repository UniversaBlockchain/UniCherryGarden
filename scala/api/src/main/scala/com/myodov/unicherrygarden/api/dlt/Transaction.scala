package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single transaction in blockchain, containing one or more transfers. */
trait Transaction {
  /** Transaction hash. */
  val hash: String
  require(hash != null && EthUtils.Hashes.isValidTransactionHash(hash))

  /** Sender of the transaction (address). */
  val from: String
  require(from != null && EthUtils.Hashes.isValidAddressHash(from))

  /** Receiver of the transaction (address). */
  val to: String
  require(to != null && EthUtils.Hashes.isValidAddressHash(to))

  //  val transactionFee: BigDecimal
  //  require(transactionFee != null && transactionFee >= 0)

  val gasPrice: BigDecimal
  require(gasPrice != null && gasPrice >= 0)

  val gasLimit: BigInt
  require(gasLimit != null && gasLimit >= 0)

  val gasUsed: BigInt
  require(gasUsed != null && gasUsed >= 0)
}

/** This is a transaction in blockchain, which has been mined and present in some block. */
//trait MinedTransaction extends Transaction {
//  val blockNumber: BigInt
//  require(blockNumber != null && blockNumber >= 0)
//}


///** This is a single block in blockchain, containing one or more transfers. */
//trait Block {
//  /** Return the block number, typically sequentially increasing. */
//  val number: BigInt
//  require(number != null)
//
//  /** Block hash. */
//  val hash: String
//  require(hash != null && EthUtils.isValidBlockHash(hash))
//
//  /** Parent block hash. Note: we may don't know or don't have it. */
//  val parentHash: Option[String]
//  require(parentHash != null && (parentHash.isEmpty || EthUtils.isValidBlockHash(parentHash.get)))
//
//  /** Timestamp of the block. */
//  val timestamp: Instant
//  require(timestamp != null)
//
//  //  /** Return the (transfer or creation) operations contained in this block. */
//  //  def ops: Seq[Operation]
//}
//
/** Standard implementation of [[Transaction]] trait. */
//case class EthereumTransaction() extends Transaction

/** Standard implementation of [[Transaction]] trait. */
case class EthereumTransaction(hash: String,
                               blockNumber: BigInt,
                               timestamp: Instant,
                               from: String,
                               to: String,
                               gasPrice: BigDecimal,
                               gasLimit: BigInt,
                               gasUsed: BigInt,
                              ) extends Transaction
