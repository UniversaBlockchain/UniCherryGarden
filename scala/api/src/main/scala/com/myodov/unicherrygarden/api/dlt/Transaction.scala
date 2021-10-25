package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single transaction in blockchain, containing one or more transfers. */
trait Transaction {
  /** Transaction hash. */
  val hash: String
  require(hash != null && EthUtils.Hashes.isValidTransactionHash(hash), hash)

  /** Sender of the transaction (address). */
  val from: String
  require(from != null && EthUtils.Addresses.isValidLowercasedAddress(from), from)

  /** Receiver of the transaction (address). May be absent (if this is a transaction creating the smart contract). */
  val to: Option[String]
  require(to.isEmpty || EthUtils.Addresses.isValidLowercasedAddress(to.get), to)

  //  val transactionFee: BigDecimal
  //  require(transactionFee != null && transactionFee >= 0)

  val gasPrice: BigDecimal
  require(gasPrice != null && gasPrice >= 0, gasPrice)

  val gasLimit: BigInt
  require(gasLimit != null && gasLimit >= 0, gasLimit)

  val gasUsed: BigInt
  require(gasUsed != null && gasUsed >= 0, gasUsed)
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
                               to: Option[String],
                               gasPrice: BigDecimal,
                               gasLimit: BigInt,
                               gasUsed: BigInt,
                              ) extends Transaction
