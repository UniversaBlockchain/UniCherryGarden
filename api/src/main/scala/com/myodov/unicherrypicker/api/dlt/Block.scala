package com.myodov.unicherrypicker.api.dlt

import java.time.Instant

/** This is a single block in blockchain, containing one or more transfers. */
trait Block {
  /** Return the block number, typically sequentially increasing. */
  val number: BigInt

  /** Block hash. */
  val hash: String

  /** Parent block hash. Note: we may don't know or don't have it. */
  val parentHash: Option[String]

  /** Timestamp of the block. */
  val timestamp: Instant

  //  /** Return the (transfer or creation) operations contained in this block. */
  //  def ops: Seq[Operation]
}

/** Standard implementation of [[Block]] trait. */
case class EthereumBlock(number: BigInt,
                         hash: String,
                         parentHash: Option[String],
                         timestamp: Instant) extends Block
