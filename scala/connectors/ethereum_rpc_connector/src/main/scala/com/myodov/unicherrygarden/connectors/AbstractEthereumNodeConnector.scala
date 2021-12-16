package com.myodov.unicherrygarden.connectors

import com.myodov.unicherrygarden.Tools.reduceOptionSeq
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.dlt.{EthereumBlock, EthereumMinedTransaction}
import com.myodov.unicherrygarden.ethereum.EthUtils

/** Any implementation of Ethereum node connector, no matter of the underlying network mechanism. */
abstract class AbstractEthereumNodeConnector(protected[this] val nodeUrl: String)


/** What operations can be supported by some Ethereum node connector: read-only operations. */
trait Web3ReadOperations {

  import Web3ReadOperations.filterSingleBlock

  /** Get the status of the synccreateSyncinging process for this Ethereum node (`eth.syncing`)
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
   * @param blockNumber what block to read (by its number). Must be 0 or higher!
   */
  def readBlock(blockNumber: BigInt): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])]

  /** Read the block from Ethereum node (by the block number), filtering it for specific addresses.
   *
   * @param blockNumber         what block to read (by its number).
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   */
  def readBlock(blockNumber: BigInt,
                addressesOfInterest: Set[String]): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])] = {
    require(blockNumber >= 0)

    // None to None, Some[seq] to Some[seq] - this is map!
    readBlock(blockNumber)
      .map(filterSingleBlock(_, addressesOfInterest))
  }

  /** Read the blocks from Ethereum node (in a range defined by the start and end block numbers).
   *
   * @param startBlockNumber the number of the first block to read (inclusive).
   * @param endBlockNumber   the number of the last block to read (inclusive, should be ≥ `startBlockNumber`).
   * @return an [[Option]] of with the sequence of pairs defining a block and its transactions;
   *         the Option is `None` if any network problem occurred during returning this sequence;
   *         otherwise it contains the sequence with exactly the requested blocks (and the transactions in these blocks),
   *         in strictly increasing order.
   */
  def readBlocks(startBlockNumber: BigInt,
                 endBlockNumber: BigInt): Option[Seq[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])]] = {
    require(
      startBlockNumber >= 0 && endBlockNumber >= startBlockNumber,
      (startBlockNumber, endBlockNumber))

    val seqOfOptions: Seq[Option[(EthereumBlock, Seq[EthereumMinedTransaction])]] =
      (startBlockNumber to endBlockNumber).map(readBlock)

    val optionOfSeqs: Option[Seq[(EthereumBlock, Seq[EthereumMinedTransaction])]] = reduceOptionSeq(seqOfOptions)

    // Let’s do the extra validation of this result; and everything is fine, return it.
    // None to None, Some[seq] to Some[seq] - this is map!
    optionOfSeqs.map { seq =>
      require(
        seq
          .sliding(2)
          .forall { p =>
            p match {
              case Seq((lBlock, lTxes), (rBlock, rTxes)) => lBlock.number + 1 == rBlock.number
            }
          },
        s"readBlocks($startBlockNumber, $endBlockNumber) didn't return increasing blocks: $seq"
      )
      require(
        seq.length == (endBlockNumber - startBlockNumber + 1),
        (startBlockNumber, endBlockNumber, seq.length, seq)
      )

      // Return the validated input without any changes.
      seq
    }
  }

  /** Read the blocks from Ethereum node (in a range defined by the start and end block numbers),
   * filtering it for specific addresses.
   *
   * @param startBlockNumber    the number of the first block to read (inclusive).
   * @param endBlockNumber      the number of the last block to read (inclusive, should be ≥ `startBlockNumber`).
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   * @return an [[Option]] of with the sequence of pairs defining a block and its transactions;
   *         the Option is `None` if any network problem occured during returning this sequence;
   *         otherwise it contains the sequence with exactly the requested blocks (and the transactions in these blocks),
   *         in strictly increasing order.
   */
  def readBlocks(startBlockNumber: BigInt,
                 endBlockNumber: BigInt,
                 addressesOfInterest: Set[String]): Option[Seq[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])]] = {
    val optionOfSecs: Option[Seq[(EthereumBlock, Seq[EthereumMinedTransaction])]] =
      readBlocks(startBlockNumber, endBlockNumber)

    // None to None, Some to Some - this is map!
    optionOfSecs map { seq =>
      // If this option isn’t empty and contains a seq,
      // let’s map `filterSingleBlock` to each element
      seq.map(filterSingleBlock(_, addressesOfInterest))
    }
  }

  private[connectors] sealed case class SyncingStatusData(currentBlock: Int = 0, highestBlock: Int = 0) {
    assert(currentBlock >= 0)
    assert(highestBlock >= 0)
  }

  /** Result of querying `eth.syncing` query of the node, i.e. getting the overall status of the node sync. */
  private[connectors] sealed class SyncingStatusResult(data: Option[SyncingStatusData]) {
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

}

private object Web3ReadOperations {
  /** The default implementation of filtering uses manual filtering of the input data. */
  def filterSingleBlock(blockPair: (dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction]),
                        addressesOfInterest: Set[String]): (dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction]) = {
    assert(addressesOfInterest.forall(EthUtils.Addresses.isValidLowercasedAddress), addressesOfInterest)

    val (block, transactionsUnfiltered) = blockPair
    // Convert the addresses (which should be used to filter) to their Uint256 representations
    val addressesOfInterestUint256: Set[String] = addressesOfInterest.map(EthUtils.Uint256Str.fromAddress)

    val transactionsFiltered =
      for (tr: dlt.EthereumMinedTransaction <- transactionsUnfiltered
           // We take a transaction if it is sent from any address of interest...
           if addressesOfInterest.contains(tr.from) ||
             // ... or sent to any address of interest...
             (tr.to.nonEmpty && addressesOfInterest.contains(tr.to.get)) ||
             // ... or any of addresses-of-interest matches any txlog topic.
             addressesOfInterestUint256.exists(tr.anyTxLogContainsTopic)
           )
        yield tr

    (block, transactionsFiltered)
  }
}
