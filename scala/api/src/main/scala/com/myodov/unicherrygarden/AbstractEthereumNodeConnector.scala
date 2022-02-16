package com.myodov.unicherrygarden

import com.myodov.unicherrygarden.AbstractEthereumNodeConnector.SingleBlockData
import com.myodov.unicherrygarden.Tools.{reduceOptionSeq, seqIsIncrementing}
import com.myodov.unicherrygarden.Web3ReadOperations.validateBlockHashes
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.dlt.EthereumBlock
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.switch
import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.language.postfixOps

/** Any implementation of Ethereum node connector, no matter of the underlying network mechanism. */
abstract class AbstractEthereumNodeConnector(protected[this] val nodeUrl: String)


/** What operations can be supported by some Ethereum node connector: read-only operations. */
trait Web3ReadOperations extends LazyLogging {

  import Web3ReadOperations.filterSingleBlock

  /** Get the status of the Ethereum node and the blockchain:
   * the object that contains the syncing progress (`eth.syncing`, and the number of the last block synced
   * by this Ethereum node: `eth.blockNumber`),
   * as well as the useful information about the most recent/latest block
   * (such as the information useful for gas limit calculation).
   *
   * @note using Int for block number should be fine up to 2B blocks;
   *       it must be fixed in about 1657 years.
   * @return The information about the blockchain syncing/most recent status.
   *         When the data about syncing is not available from `eth.syncing`,
   *         it is derived from `eth.blockNumber` (if possible).
   *         The Option is empty if the data could not be received
   *         (probably due to some network error, or the node still being synced).
   */
  def ethBlockchainStatus: Option[SystemStatus.Blockchain]

  /** Read the block from Ethereum node (by the block number), returning all parseable data.
   *
   * @param blockNumber what block to read (by its number). Must be 0 or higher!
   */
  def readBlock(blockNumber: BigInt): Option[SingleBlockData]

  /** Read the block from Ethereum node (by the block number), filtering it for specific addresses.
   *
   * @param blockNumber         what block to read (by its number).
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   */
  def readBlock(blockNumber: BigInt,
                addressesOfInterest: Set[String]): Option[SingleBlockData] = {
    require(blockNumber >= 0)

    // None to None, Some[seq] to Some[seq] - this is map!
    readBlock(blockNumber)
      .map(filterSingleBlock(_, addressesOfInterest))
  }

  /** Read the blocks from Ethereum node (in a range defined by the start and end block numbers).
   *
   * @param range the range of block numbers to read.
   * @return an [[Option]] of with the sequence of pairs defining a block and its transactions;
   *         the Option is `None` if any network problem occurred during returning this sequence;
   *         otherwise it contains the sequence with exactly the requested blocks (and the transactions in these blocks),
   *         in strictly increasing order.
   */
  def readBlocks(range: EthereumBlock.BlockNumberRange): Option[Seq[SingleBlockData]] = {
    val seqOfOptions: Seq[Option[SingleBlockData]] =
      range.map(readBlock(_))

    val optionOfSeqs: Option[Seq[SingleBlockData]] =
      reduceOptionSeq(seqOfOptions)

    // Let’s do the extra validation of this result; and everything is fine, return it.
    // None to None, Some[seq] to Some[seq] - this is map!
    optionOfSeqs.map { seq =>
      require(
        seqIsIncrementing(seq.map { case (block, txes) => block.number }),
        s"readBlocks($range) didn't return strictly incrementing blocks: $seq"
      )
      require(
        seq.length == range.length,
        (range, seq.length, seq)
      )

      // Otherwise return the validated input without any changes.
      seq
    }
  }

  /** Read the blocks from Ethereum node (in a range defined by the start and end block numbers),
   * filtering it for specific addresses.
   *
   * @note the default implementation is suboptimal and based on [[Web3ReadOperations.readBlock]].
   *       Engine/connector-specific optimizations may be needed.
   * @param range               the range of block numbers to read.
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   * @return an [[Option]] containing the sequence of pairs defining a block and its transactions;
   *         the Option is `None` if any network problem occured during returning this sequence;
   *         otherwise it contains the sequence with exactly the requested blocks (and the transactions in these blocks),
   *         in strictly increasing order.
   */
  def readBlocks(range: EthereumBlock.BlockNumberRange,
                 addressesOfInterest: Set[String]): Option[Seq[SingleBlockData]] = {
    val optionOfSecs: Option[Seq[SingleBlockData]] = readBlocks(range)

    // None to None, Some to Some - this is map!
    optionOfSecs map { seq =>
      // If this option isn’t empty and contains a seq,
      // let’s map `filterSingleBlock` to each element
      seq.map(filterSingleBlock(_, addressesOfInterest))
    }
  }

  /** Read just the block hashes from Ethereum node (in a range defined by the start and end block numbers).
   *
   * @note the default implementation is suboptimal and based on [[Web3ReadOperations.readBlock]].
   *       Engine/connector-specific optimizations may be needed.
   * @param range the range of block numbers to read.
   * @return an [[Option]] SortedMap[Int, String];
   *         the Option is `None` if any network problem occurred during returning this sequence;
   *         otherwise it contains the map with steadily increasing blocks in the requested range.
   *         Note: if requesting a range like `5 to 10`, it may return the blocks in `5 to 7` range
   *         if only 5 to 7 is available, and this will be a valid non-`None` response; so always check
   *         for the real size of the response!
   */
  def readBlockHashes(range: EthereumBlock.BlockNumberRange): Option[SortedMap[Int, String]] = {
    val optionOfSecs: Option[Seq[SingleBlockData]] =
      readBlocks(range, Set.empty)

    // None to None, Some to Some - this is map!
    val result = optionOfSecs map { seq =>
      // If this option isn’t empty and contains a seq,
      // let’s map `filterSingleBlock` to each element
      seq
        .map { case (bl, tr) => bl.number -> bl.hash }
        .to(SortedMap)
    }
    validateBlockHashes(range, result)
  }
}

object AbstractEthereumNodeConnector extends LazyLogging {
  /** The blockchain details from a single block. */
  type SingleBlockData = (dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])

  val NETWORK_TIMEOUT: FiniteDuration = 30 seconds
}

private object Web3ReadOperations extends LazyLogging {
  /** The default implementation of filtering uses manual filtering of the input data. */
  def filterSingleBlock(blockData: SingleBlockData,
                        addressesOfInterest: Set[String]): SingleBlockData = {
    assert(addressesOfInterest.forall(EthUtils.Addresses.isValidLowercasedAddress), addressesOfInterest)

    val (block, transactionsUnfiltered) = blockData
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

  /** Default validator for the [[readBlockHashes]] result. */
  def validateBlockHashes(range: EthereumBlock.BlockNumberRange,
                          resultToValidate: Option[SortedMap[Int, String]]): Option[SortedMap[Int, String]] = {
    // Validate output before returning; treat any inconsistencies as network errors
    (resultToValidate: @switch) match {
      case None => None
      case Some(map) =>
        map match {
          case emptyMap if emptyMap.isEmpty =>
            // Well okay, we’ve requested a block range which is empty
            resultToValidate // original result, unmodified
          // The further cases are non-empty! we can rely upon it
          case tooBigMap if tooBigMap.size > range.length =>
            logger.error(s"Requested readBlockHashes($range) with size ${range.length} but received ${map.size} results")
            None
          case mapStartsFromWrongBlock if mapStartsFromWrongBlock.keys.head != range.start =>
            logger.error(s"Requested readBlockHashes($range) starting from ${range.start} " +
              s"but result starts from ${mapStartsFromWrongBlock.keys.head}")
            None
          case badlyOrderedMap if !seqIsIncrementing(badlyOrderedMap.keys) =>
            logger.error(s"Requested readBlockHashes($range) starting from ${range.start} " +
              s"but the result isn't strictly incrementing: ${badlyOrderedMap.keys}")
            None
          case _ =>
            resultToValidate // Otherwise it is good, return the original result unmodified
        }
    }
  }
}
