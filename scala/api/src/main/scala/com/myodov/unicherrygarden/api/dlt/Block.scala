package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single block in blockchain, containing one or more transactions. */
trait Block {
  /** Return the block number, typically sequentially increasing.
   *
   * @note [[Integer]] type instead of [[BigInt]] saves space but must be fixed in 1657 years.
   */
  val number: Int
  require(number >= 0, number)

  /** Block hash. */
  val hash: String
  require(hash != null && EthUtils.Hashes.isValidBlockHash(hash), hash)

  /** Parent block hash. Note: we may don't know or don't have it. */
  val parentHash: Option[String]
  require(parentHash.isEmpty || EthUtils.Hashes.isValidBlockHash(parentHash.get), parentHash)

  /** Timestamp of the block. */
  val timestamp: Instant
  require(timestamp != null, timestamp)


  override def toString = s"Block($number, $hash, $parentHash, $timestamp)"
}

/** Standard implementation of [[Block]] trait. */
class EthereumBlock(val number: Int,
                    val hash: String,
                    val parentHash: Option[String],
                    val timestamp: Instant
                   ) extends Block {
  require(hash != null && EthUtils.Hashes.isValidBlockHash(hash), hash)
  require(parentHash.isEmpty || (parentHash.get != null && EthUtils.Hashes.isValidBlockHash(parentHash.get)), parentHash)

  /** Return a copy of this Ethereum block, but having [[EthereumBlock#parentHash]] disabled
   * (e.g. to store it as a first block in the DB).
   */
  def withoutParentHash = new EthereumBlock(number, hash, Option.empty, timestamp)
}

object EthereumBlock {
  @inline def apply(number: Int,
                    hash: String,
                    parentHash: String,
                    timestamp: Instant
                   ): EthereumBlock = {
    require(hash != null, hash)
    require(parentHash != null, parentHash)
    new EthereumBlock(number, hash, Option(parentHash), timestamp)
  }
}
