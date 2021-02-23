package com.myodov.unicherrypicker.api.dlt

import java.time.Instant

import com.myodov.unicherrypicker.eth.EthUtils

/** This is a single block in blockchain, containing one or more transfers. */
trait Block {
  /** Return the block number, typically sequentially increasing. */
  val number: BigInt
  require(number != null)

  /** Block hash. */
  val hash: String
  require(hash != null && EthUtils.isValidBlockHash(hash))

  /** Parent block hash. Note: we may don't know or don't have it. */
  val parentHash: Option[String]
  require(parentHash != null && (parentHash.isEmpty || EthUtils.isValidBlockHash(parentHash.get)))

  /** Timestamp of the block. */
  val timestamp: Instant
  require(timestamp != null)

  //  /** Return the (transfer or creation) operations contained in this block. */
  //  def ops: Seq[Operation]
}

/** Standard implementation of [[Block]] trait. */
case class EthereumBlock(number: BigInt,
                         hash: String,
                         parentHash: Option[String],
                         timestamp: Instant) extends Block
