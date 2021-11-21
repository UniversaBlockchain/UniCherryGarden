package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

import com.myodov.unicherrygarden.ethereum.EthUtils

/** This is a single block in blockchain, containing one or more transactions.
 * In the DB, stored in `ucg_block` table.
 *
 * @param number     the block number, typically sequentially increasing;
 *                   [[Integer]] type instead of [[BigInt]] saves space but must be fixed in 1657 years.
 * @param hash       block hash.
 * @param parentHash parent block hash. Note: we may don't know or don't have it.
 * @param timestamp  timestamp of the block.
 */
class EthereumBlock(val number: Int,
                    val hash: String,
                    val parentHash: Option[String],
                    val timestamp: Instant
                   ) {
  require(number >= 0, number)
  require(hash != null && EthUtils.Hashes.isValidBlockHash(hash), hash)
  require(parentHash.isEmpty || (parentHash.get != null && EthUtils.Hashes.isValidBlockHash(parentHash.get)), parentHash)
  require(timestamp != null, timestamp)

  /** Return a copy of this Ethereum block, but having [[EthereumBlock#parentHash]] disabled
   * (e.g. to store it as a first block in the DB).
   */
  def withoutParentHash = EthereumBlock(number, hash, None, timestamp)

  def canEqual(a: Any) = a.isInstanceOf[EthereumBlock]

  override def toString = s"EthereumBlock(number=$number, hash=$hash, parentHash=$parentHash, timestamp=$timestamp)"

  override def equals(that: Any): Boolean =
    that match {
      case that: EthereumBlock => {
        that.canEqual(this) &&
          this.number == that.number &&
          this.hash == that.hash &&
          this.parentHash == that.parentHash &&
          this.timestamp == that.timestamp
      }
      case _ => false
    }
}

object EthereumBlock {
  @inline def apply(number: Int,
                    hash: String,
                    parentHash: Option[String],
                    timestamp: Instant
                   ): EthereumBlock = {
    require(hash != null, hash)
    require(parentHash != null, parentHash)
    new EthereumBlock(number, hash, parentHash, timestamp)
  }
}
