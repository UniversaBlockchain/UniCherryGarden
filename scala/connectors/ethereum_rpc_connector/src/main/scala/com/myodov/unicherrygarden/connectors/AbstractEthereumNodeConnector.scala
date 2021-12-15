package com.myodov.unicherrygarden.connectors

import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.ethereum.EthUtils

import scala.annotation.switch

/** Any implementation of Ethereum node connector, no matter of the underlying network mechanism. */
abstract class AbstractEthereumNodeConnector(protected[this] val nodeUrl: String)


/** What operations can be supported by some Ethereum node connector: read-only operations. */
trait Web3ReadOperations {
  /** Get the status of the syncing process for this Ethereum node (`eth.syncing`)
   * and the number of the last block synced by this Ethereum node (`eth.blockNumber`),
   * simultaneously in a single call.
   *
   * @return The option of the tuple with two elements:
   *         <ol>
   *         <li>the data about the syncing process (`eth.syncing`);</li>
   *         <li>the number of the last block synced by the node (`eth.blockNumber`).</li>
   *         </ol>
   *         The Option is empty if the data could not be received
   *         (probably due to some network error).
   */
  def ethSyncingBlockNumber: Option[(SyncingStatusResult, BigInt)]

  /** Read the block from Ethereum node (by the block number), returning all parseable data.
   *
   * @param blockNumber what block to read (by its number).
   */
  def readBlock(blockNumber: BigInt): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])]

  /** Read the block from Ethereum node (by the block number), filtering it for specific addresses.
   *
   * @param blockNumber         what block to read (by its number).
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   */
  def readBlock(blockNumber: BigInt,
                addressesOfInterest: Set[String]): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])] = {
    assert(addressesOfInterest.forall(EthUtils.Addresses.isValidLowercasedAddress), addressesOfInterest)
    // Convert the addresses (which should be used to filter) to their Uint256 representations
    val addressesOfInterestUint256: Set[String] = addressesOfInterest.map(EthUtils.Uint256Str.fromAddress)

    (readBlock(blockNumber): @switch) match {
      case None => None
      case Some((block, minedTransactionsUnfiltered)) => {
        val minedTransactionsFiltered =
          for (tr: dlt.EthereumMinedTransaction <- minedTransactionsUnfiltered
               // We take a transaction if it is sent from any address of interest...
               if addressesOfInterest.contains(tr.from) ||
                 // ... or sent to any address of interest...
                 (tr.to.nonEmpty && addressesOfInterest.contains(tr.to.get)) ||
                 // ... or any of addresses-of-interest matches any txlog topic.
                 addressesOfInterestUint256.exists(tr.anyTxLogContainsTopic)
               )
            yield tr

        Some((block, minedTransactionsFiltered))
      }
    }
  }
}

private[this] final case class SyncingStatusData(currentBlock: Int = 0, highestBlock: Int = 0) {
  assert(currentBlock >= 0)
  assert(highestBlock >= 0)
}

/** Result of querying `eth.syncing` query of the node, i.e. getting the overall status of the node sync. */
private[connectors] class SyncingStatusResult(data: Option[SyncingStatusData]) {
  val isStillSyncing: Boolean = data.nonEmpty
  lazy val currentBlock: Int = {
    if (isStillSyncing) data.get.currentBlock
    else throw new RuntimeException("currentBlock available only if `isStillSyncing`")
  }
  lazy val highestBlock: Int = {
    if (isStillSyncing) data.get.highestBlock
    else throw new RuntimeException("highestBlock available only if `isStillSyncing`")
  }

  override val toString: String = {
    if (isStillSyncing)
      s"SyncingStatusResult(currentBlock=$currentBlock, highestBlock=$highestBlock)"
    else
      s"SyncingStatusResult(not_syncing)"
  }
}

object SyncingStatusResult {
  @inline
  private[this] def apply(data: Option[SyncingStatusData]): SyncingStatusResult = new SyncingStatusResult(data)

  @inline
  private[connectors] def createSyncing(currentBlock: Int, highestBlock: Int): SyncingStatusResult =
    new SyncingStatusResult(Some(SyncingStatusData(currentBlock, highestBlock)))

  @inline
  private[connectors] def createNotSyncing(): SyncingStatusResult =
    new SyncingStatusResult(None)
}
