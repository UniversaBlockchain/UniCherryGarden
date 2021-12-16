package com.myodov.unicherrygarden.connectors

import java.time.Instant
import java.util.concurrent.TimeUnit

import caliban.client.CalibanClientError
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.connectors.graphql._
import com.typesafe.scalalogging.LazyLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject
import org.web3j.protocol.core.methods.response._
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric.decodeQuantity
import sttp.client3._
import sttp.client3.akkahttp.AkkaHttpBackend

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._
import scala.language.postfixOps
import scala.util.control.NonFatal

/** Connector that communicates with a single Ethereum node using JSON-RPC (via Web3J library). */
class EthereumSingleNodeJsonRpcConnector(nodeUrl: String)
  extends AbstractEthereumNodeConnector(nodeUrl)
    with Web3ReadOperations
    with LazyLogging {

  override def toString: String = s"EthereumWeb3jSingleNodeConnector($nodeUrl)"

  private[this] var web3j: Web3j = rebuildWeb3j()

  private[this] def reconnect(): Unit = {
    web3j.shutdown()
    web3j = rebuildWeb3j()
  }

  private[this] def rebuildWeb3j(): Web3j = {
    Web3j.build(new HttpService(nodeUrl))
  }

  private[this] def ethSyncing: Option[SyncingStatusResult] = {
    try {
      val result: EthSyncing.Result = web3j.ethSyncing.send.getResult
      if (!result.isSyncing) {
        logger.debug("eth.syncing = not syncing")
        Some(SyncingStatusResult.createNotSyncing())
      } else {
        val syncingResult: EthSyncing.Syncing = result.asInstanceOf[EthSyncing.Syncing]

        val currentBlockStr = syncingResult.getCurrentBlock
        val highestBlockStr = syncingResult.getHighestBlock
        logger.debug(s"eth.syncing = current $currentBlockStr, highest $highestBlockStr")

        Some(SyncingStatusResult.createSyncing(
          decodeQuantity(currentBlockStr).intValueExact(),
          decodeQuantity(highestBlockStr).intValueExact()))
      }
    } catch {
      case NonFatal(e) => {
        logger.error("Cannot call eth.syncing!", e)
        None
      }
    }
  }

  private[this] def ethBlockNumber: Option[BigInt] = {
    try {
      Some(web3j.ethBlockNumber.send.getBlockNumber)
    } catch {
      case NonFatal(e) => {
        logger.error("Cannot call eth.blockNumber!", e)
        None
      }
    }
  }

  override def ethSyncingBlockNumber: Option[(SyncingStatusResult, BigInt)] =
    ethSyncing zip ethBlockNumber

  /** Get the Ethereum data for a block, by its number; the data is returned in Web3j-style classes.
   *
   * @return an [[Option]] (empty if reading the block failed somehow), containing a tuple of:
   *         1. [[EthBlock.Block]] (with full transaction objects),
   *         2. [[Seq]] of [[Transaction]] in the order as they go in the block (actually it is just the preprocessed
   *         list directly from the [[EthBlock.Block]]).
   *         3. Mapping from the transaction hash to [[TransactionReceipt]].
   */
  protected[this] def readBlockWeb3j(blockNumber: BigInt): Option[(EthBlock.Block, Seq[Transaction], Map[String, TransactionReceipt])] = {
    require(blockNumber >= 0, blockNumber)

    try {
      val startTime = System.nanoTime

      val block: EthBlock.Block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber.bigInteger), true).send.getBlock

      // Very basic validation
      if (block.getNumber != blockNumber.bigInteger) {
        // Very basic validation failed
        logger.error(s"We've requested block $blockNumber but received block ${block.getNumber} (${block.getHash})")
        None
      } else {
        // Very basic validation succeeded!

        // Get the web3j-style transactions
        val transactions: Seq[Transaction] = block.getTransactions.asScala.to(List).map(_.asInstanceOf[TransactionObject].get)

        // Get all the receipts, as fast as we can, in a parallel way
        val validReceipts: List[TransactionReceipt] = {
          // There is a complex asynchronous launch, do it inside
          val receiptFutures: LazyList[Future[EthGetTransactionReceipt]] =
            transactions.to(LazyList).map(
              tr => web3j.ethGetTransactionReceipt(tr.getHash).sendAsync.asScala
            )

          val batchSize = 64
          val receiptFuturesBatched: LazyList[LazyList[Future[EthGetTransactionReceipt]]] = receiptFutures.sliding(batchSize, batchSize).to(LazyList)

          val totalReceipts: Seq[Option[TransactionReceipt]] = receiptFuturesBatched.map(lazyBatchOfFutures => {
            val batchOfFutures: Seq[Future[EthGetTransactionReceipt]] = lazyBatchOfFutures.toList
            val futureOfBatch: Future[Seq[EthGetTransactionReceipt]] = Future.sequence(batchOfFutures)
            val batchResult: Seq[EthGetTransactionReceipt] = Await.result(futureOfBatch, Duration.Inf)

            batchResult.map(trr => trr.getTransactionReceipt.toScala)
          }).flatten.toList

          logger.debug(s"Total receipts: ${totalReceipts.length}")
          totalReceipts.flatten.to(List)
        }
        logger.debug(s"Valid receipts: ${validReceipts.length}")

        // Map from transaction hash to transaction receipt
        val receiptsByTrHash: Map[String, TransactionReceipt] =
          validReceipts.iterator.map(v => v.getTransactionHash -> v).toMap
        require(receiptsByTrHash.size == validReceipts.size, (receiptsByTrHash.size, validReceipts.size))

        //        logger.debug("Transactions:")
        //        for (tr <- transactions) {
        //          val value = EthUtils.Wei.valueFromWeis(tr.getValue)
        //          logger.debug(s"Transaction: Hash ${tr.getHash}: value $value, gas ${tr.getGas}, gas price ${tr.getGasPrice}")
        //        }

        //        logger.debug(s"Transaction receipts (very detailed): $receiptsByTrHash")
        //        for ((key, trRec) <- receiptsByTrHash) {
        //          logger.debug(s"TR receipt ($key): $trRec")
        //        }

        val duration = Duration(System.nanoTime - startTime, TimeUnit.NANOSECONDS)
        logger.debug(s"Duration ${duration.toMillis} ms")

        Some((block, transactions, receiptsByTrHash))
      }
    } catch {
      case NonFatal(e) => {
        logger.error("On iteration, got a error", e)
        None
      }
    }
  }

  override def readBlock(blockNumber: BigInt): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])] = {
    require(blockNumber >= 0, blockNumber)

    readBlockWeb3j(blockNumber) match {
      case None => None
      case Some((w3jBlock, w3jTransactions, w3jReceiptsByTrHash)) => {
        assert(blockNumber.bigInteger == w3jBlock.getNumber,
          (blockNumber, w3jBlock.getNumber))
        assert(w3jBlock.getTransactions.size == w3jTransactions.size,
          (blockNumber, w3jBlock.getTransactions.size, w3jTransactions.size))
        assert(w3jTransactions.size == w3jReceiptsByTrHash.size,
          (blockNumber, w3jTransactions.size, w3jReceiptsByTrHash.size))

        val blockHash = w3jBlock.getHash

        val w3jTransactionsByHash = w3jTransactions.map(tr => tr.getHash -> tr).toMap

        // Now let’s validate all of the transaction receipts.
        // Let’s find any one bad transaction receipt, that may be:
        // 1. a receipt where the referred block doesn’t match the block we’ve requested, or:
        // 2. a receipt where the referred transaction doesn’t match
        //    the transaction from this block;
        val badReceipt: Option[TransactionReceipt] = w3jReceiptsByTrHash.values.find(trRcpt =>
          // The transaction receipt refers to a different block hash
          (trRcpt.getBlockHash != blockHash) ||
            // The transaction receipt refers to a different block number
            (trRcpt.getBlockNumber != blockNumber.bigInteger) ||
            // The transaction receipt hash for some reason don’t present in the original list of transactions
            !w3jTransactionsByHash.contains(trRcpt.getTransactionHash) ||
            // Original transaction in a block has a different transaction index
            (w3jTransactionsByHash(trRcpt.getTransactionHash).getTransactionIndex != trRcpt.getTransactionIndex)
        )

        if (badReceipt.nonEmpty) {
          // We have some bad receipt; treat it as error
          logger.error(s"Receipt ${badReceipt.get} is invalid! " +
            s"Whole block $blockNumber is considered invalid, need to reread")
          None
        } else {
          // We don’t have bad receipts, so it’s actually a good set of data
          val blockTime = Instant.ofEpochSecond(w3jBlock.getTimestamp.longValueExact)

          val block = dlt.EthereumBlock(
            blockNumber.bigInteger.intValueExact,
            blockHash,
            Some(w3jBlock.getParentHash),
            blockTime
          )
          logger.debug(s"Read block $block")

          val minedTransactions = for (w3jTr <- w3jTransactions) yield {
            val trHash = w3jTr.getHash
            val w3jTrReceipt = w3jReceiptsByTrHash(trHash) // it must exist
            assert(trHash == w3jTrReceipt.getTransactionHash, (trHash, w3jTrReceipt.getTransactionHash))

            dlt.EthereumMinedTransaction(
              // *** Before-mined transaction ***
              txhash = w3jTr.getHash,
              from = w3jTr.getFrom,
              to = Option(w3jTr.getTo), // Option(nullable)
              gas = w3jTr.getGas,
              gasPrice = w3jTr.getGasPrice,
              nonce = w3jTr.getNonce.intValueExact,
              value = w3jTr.getValue,
              // *** Mined transaction ***
              // "status" – EIP 658, since Byzantium fork
              // using Option(nullable)
              status = Option(w3jTrReceipt.getStatus).map(decodeQuantity(_).intValueExact),
              blockNumber = w3jTr.getBlockNumber,
              transactionIndex = w3jTrReceipt.getTransactionIndex.intValueExact,
              gasUsed = w3jTrReceipt.getGasUsed,
              effectiveGasPrice = decodeQuantity(w3jTrReceipt.getEffectiveGasPrice),
              cumulativeGasUsed = w3jTrReceipt.getCumulativeGasUsed,
              txLogs = EthereumSingleNodeJsonRpcConnector.getLogsFromTransactionReceipt(w3jTrReceipt)
            )
          }
          Some((block, minedTransactions))
        }
      }
    }
  }

  //    try {
  //      val block: EthBlock.Block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber.bigInteger), true).send.getBlock
  //      require(blockNumber.bigInteger == block.getNumber, (blockNumber, block.getNumber))
  //
  //      val blockTime = Instant.ofEpochSecond(block.getTimestamp.longValueExact)
  //

  //      val args = util.Arrays.asList(new TypeReference[Uint256]() {}, new TypeReference[Uint256]() {})
  //      val transferEvent: Event = new Event("Notify", [new TypeReference[Uint256]() {}
  //      , new TypeReference[Uint256]() {}
  //      ] );
  //      println(s"Event transferEventSignature is ${Erc20TransferEvent.signature}")
  //      println(s"  Transfer event Arguments ${Erc20TransferEvent.eventIndexedParameters}")
  //      val transferIndexedParameters = Erc20TransferEvent.eventIndexedParameters
  //      val arg0 = transferIndexedParameters(0)
  //      val arg1 = transferIndexedParameters(1)
  //      println(s"    Transfer event indexed argument0 ${arg0.getType}")
  //      println(s"    Transfer event indexed argument1 ${arg1.getType}")
  //
  //      val transferNonIndexedParameters  = Erc20TransferEvent.eventNonIndexedParameters
  //      println(s"  Transfer event nonindexed ${transferNonIndexedParameters}")
  //      val niarg0 = Erc20TransferEvent.eventNonIndexedParameters(0)
  //      println(s"    Transfer event indexed argument1 ${niarg0.getType}")
  //

  //    getBlock data:
  //      {
  //        difficulty: 5152735828159683,
  //        extraData: "0x73706964657231320655f59e",
  //        gasLimit: 12481669,
  //        gasUsed: 12471673,
  //        hash: "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
  //        logsBloom: "0x326660486c2388350981cb848b0510a3280095103ad86270c9818b86b0e8058e630d1615c4301a94474f9d02406965b98f66c09049030105879993a0423095185ba920c467f214482884630e150170ad1814ca0c824b85400862ceb8ab049097d651889cc66ce524760d1210e250ee9edc88a943a60c85bc5500401356114c22980229169144b2804040301ca126659100a144814708221a509b10ce02f74684a371a3c50664625caa5fa693201095706b1014619c428416516a47a95088a504cd1020824000c629380200704ad301dc35941e44b6e0e030017018a2f0c0a8088216660c0902258294a14087c481047e2548a18b885cb350e4d20a3c84020022",
  //        miner: "0x04668ec2f57cc15c381b461b9fedab5d451c8f7f",
  //        mixHash: "0xbc77c8cd4d9517133b468ef44b927dc24234882c85df5ebc5155438854be7807",
  //        nonce: "0x47663545aedeb1fa",
  //        number: 11906373,
  //        parentHash: "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
  //        receiptsRoot: "0x9448d5d80ea1a1750658886b62482269efaba017ede53b4dd80bac49c875ce77",
  //        sha3Uncles: "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
  //        size: 53661,
  //        stateRoot: "0x2179780ff1557cbe04505bc91eff6fa7fa7c9020a4756ebd9e82dd13b3dee939",
  //        timestamp: 1613991022,
  //        totalDifficulty: 2.1333813410871069485224e+22,
  //        transactions: [{...}],
  //        transactionsRoot: "0x92fc93f2d0b108e22af828695cc4b1322a151157ea72be5a9db763b8a25405b2",
  //        uncles: []
  //      }
  //
  //    getBlock transaction data:
  //      {
  //        blockHash: "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
  //        blockNumber: 11906373,
  //        from: "0x00bdb5699745f5b860228c8f939abf1b9ae374ed",
  //        gas: 500000,
  //        gasPrice: 302030587421,
  //        hash: "0x636675b92b823b4317a5391cc2897386592cd017363cd51b2c66ec5c7e8152d5",
  //        input: "0x2da03409000000000000000000000000a08240ffeb57ea1cddf9b02a8ba835c8690f08d6000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
  //        nonce: 987791,
  //        r: "0x773d1cd9e16fc6c1d1f969356a08f6bb110ea1c1963921d9750c5bd650d1d82e",
  //        s: "0x2b9009aa8b411f75e4ef1637ef4279d8f8d52ae0114bd1a92c555cd3662fc06c",
  //        to: "0x1522900b6dafac587d499a862861c0869be6e428",
  //        transactionIndex: 1,
  //        v: "0x1c",
  //        value: 0
  //      }

  //      eth.getTransactionReceipt():
  //        {
  //          blockHash: "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
  //          blockNumber: 11906373,
  //          contractAddress: null,
  //          cumulativeGasUsed: 121128,
  //          from: "0x00bdb5699745f5b860228c8f939abf1b9ae374ed",
  //          gasUsed: 50082,
  //          logs: [
  //            {
  //              address: "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48",
  //              blockHash: "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
  //              blockNumber: 11906373,
  //              data: "0x00000000000000000000000000000000000000000000000000000001781af580", // amount
  //              logIndex: 1,
  //              removed: false,
  //              topics: [
  //                  "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",  // transfer event
  //                  "0x000000000000000000000000a08240ffeb57ea1cddf9b02a8ba835c8690f08d6",  // transferring from
  //                  "0x0000000000000000000000001522900b6dafac587d499a862861c0869be6e428"   // transferring to
  //                ],
  //              transactionHash: "0x636675b92b823b4317a5391cc2897386592cd017363cd51b2c66ec5c7e8152d5",
  //              transactionIndex: 1
  //            },
  //            {
  //              address: "0xa08240ffeb57ea1cddf9b02a8ba835c8690f08d6",
  //              blockHash: "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
  //              blockNumber: 11906373,
  //              data: "0x000000000000000000000000a0b86991c6218b36c1d19d4a2e9eb0ce3606eb4800000000000000000000000000000000000000000000000000000001781af580",
  //              logIndex: 2,
  //              removed: false,
  //              topics: ["0x9401e4e79c19cbe2bd774cb70a94ba660e6718be1bac1298ab3b07f454a60821"],
  //              transactionHash: "0x636675b92b823b4317a5391cc2897386592cd017363cd51b2c66ec5c7e8152d5",
  //              transactionIndex: 1
  //            }
  //          ],
  //          logsBloom: "0x00000000000000000000000000000000000004002010000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000800000000008000008000000000000000800000000000002000000000000000000000000000000001000000000000000000000000000000010000000000000000000000000000000000000000000000000010000000000000000000000000000000000200020000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000400000001000000000000000000000000000800000000000000000000000000000000000000000000000",
  //          status: "0x1",
  //          to: "0x1522900b6dafac587d499a862861c0869be6e428", // contract (Bitstamp)
  //          transactionHash: "0x636675b92b823b4317a5391cc2897386592cd017363cd51b2c66ec5c7e8152d5",
  //          transactionIndex: 1
  //        }

  //        for (l: Log <- logs) {
  //          val maybeTransferEvent: Option[Erc20TransferEvent] = Erc20TransferEvent.getEventFromEthereumLog(l)
  //
  //          // Only if found something
  //          maybeTransferEvent.map( t => {
  //            println(s"    FOUND TRANSFER $maybeTransferEvent")
  //            println(s"      topics: ${l.getTopics}")
  //          })
  //
  ////          val topics: List[String] = l.getTopics.asScala.toList
  ////          if (topics(0) == Erc20TransferEvent.signature) {
  ////            println(s"    - Spotted transfer event! ${topics}")
  ////            val argFrom: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(topics(1), transferIndexedParameters(0)).asInstanceOf[Type[Address]]
  ////            val argTo: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(topics(2), transferIndexedParameters(1)).asInstanceOf[Type[Address]]
  ////
  ////            val resultList = FunctionReturnDecoder.decode(l.getData, Erc20TransferEvent.eventNonIndexedParametersJava).asScala.toList
  ////            val transferAmountType = resultList(0)
  ////            val transferAmount = transferAmountType.getValue

  /** Using GraphQL, read the block from Ethereum node (by the block number), filtered for specific addresses.
   *
   * @param blockNumber         what block to read (by its number).
   * @param addressesOfInterest list of address hashes (all lowercased); only these addresses are returned.
   */
  def readBlockGraphQL(blockNumber: BigInt,
                       addressesOfInterest: Set[String]): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])] = {
    import caliban.Geth._

    val query = Query.block(number = Some(blockNumber.longValue)) {
      BlockBasic.view
    }

    val rq = query.toRequest(uri"$nodeUrl/graphql")

    //    val backend = AkkaHttpBackend.usingActorSystem()
    val backend = AkkaHttpBackend()

    try {
      val value: Response[Either[CalibanClientError, Option[BlockBasicView]]] =
        Await.result(rq.send(backend), EthereumSingleNodeJsonRpcConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          logger.error(s"Error for GraphQL querying block $blockNumber", err)
          None
        case Right(optBlockBasic) =>
          // This is a legit response; but it may have no contents.
          // For None, return None; for Some return a result,... hey it’s a flatMap!
          optBlockBasic.flatMap { blockBasic =>
            // Validate block
            {
              // Different validations depending on whether parent is Some(block) or None:
              // “parent is absent” may happen only on the block 0;
              // “parent is not absent” implies the parent block has number lower by one.
              require(blockBasic.parent match {
                case None => blockBasic.number == 0
                case Some(parentBlock) => parentBlock.number == blockBasic.number - 1
              },
                blockBasic)
              require(
                blockBasic.transactions match {
                  // If the transactions are not available at all – that’s legit
                  case None => true
                  // If the transactions are available - all of them must refer to the same block
                  case Some(trs) => trs.forall { tr =>
                    // Inner block must refer to the outer block
                    (tr.block match {
                      case Some(innerBlock) => innerBlock == blockBasic.asMinimalBlock
                      case None => false
                    }) &&
                      // All inner logs must refer to the outer transaction
                      (tr.logs match {
                        // If there are no logs at all, that’s okay
                        case None => true
                        // But if there are some logs, all of them must refer to the same transaction
                        case Some(logs) => logs.forall(_.transaction == tr.asMinimalTransaction)
                      })
                  }
                },
                blockBasic
              )
            }

            System.err.println(s"Received block $blockBasic")

            val block = dlt.EthereumBlock(
              number = blockBasic.number.toInt,
              hash = blockBasic.hash,
              parentHash = Some(blockBasic.parent.get.hash),
              timestamp = Instant.ofEpochSecond(blockBasic.timestamp)
            )
            val transactions = Seq.empty[dlt.EthereumMinedTransaction]
            Some((block, transactions))
          }
          None
        case other =>
          logger.error(s"Unhandled GraphQL response for block $blockNumber: $other")
          None
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Some nonfatar error happened during GraphQL query for $blockNumber", e)
        None
    }
  }
}

/** Connector that handles a connection to single Ethereum node via RPC, and communicates with it. */
object EthereumSingleNodeJsonRpcConnector {
  val NETWORK_TIMEOUT: FiniteDuration = 10 seconds

  @inline def apply(nodeUrl: String): EthereumSingleNodeJsonRpcConnector = new EthereumSingleNodeJsonRpcConnector(nodeUrl)

  /** Convert the web3j-provided [[TransactionReceipt]] to the [[Seq]] of [[dlt.EthereumTxLog]]. */
  def getLogsFromTransactionReceipt(trReceipt: TransactionReceipt): Seq[dlt.EthereumTxLog] = trReceipt
    .getLogs
    .asScala
    .map((l: Log) => dlt.EthereumTxLog(
      l.getLogIndex.intValueExact,
      l.getAddress,
      l.getTopics.asScala.toList,
      l.getData
    )).toList
}
