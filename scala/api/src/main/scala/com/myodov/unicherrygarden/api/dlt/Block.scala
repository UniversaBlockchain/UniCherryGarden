package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

/** This is a single block in blockchain, containing one or more transactions. */
trait Block {
  /** Return the block number, typically sequentially increasing.
   *
   * @note `Integer` type instead of BigInt` saves space but must be fixed in 1657 years.
   * */
  val number: Integer
  require(number != null && number >= 0)

  /** Block hash. */
  val hash: String
  require(hash != null && EthUtils.Hashes.isValidBlockHash(hash))

  /** Parent block hash. Note: we may don't know or don't have it. */
  val parentHash: String
  require(parentHash != null && EthUtils.Hashes.isValidBlockHash(parentHash))

  /** Timestamp of the block. */
  val timestamp: Instant
  require(timestamp != null)

  //  /** Return the (transfer or creation) operations contained in this block. */
  //  def ops: Seq[Operation]
}

/** Standard implementation of [[Block]] trait. */
case class EthereumBlock(number: Integer,
                         hash: String,
                         parentHash: String,
                         timestamp: Instant) extends Block
