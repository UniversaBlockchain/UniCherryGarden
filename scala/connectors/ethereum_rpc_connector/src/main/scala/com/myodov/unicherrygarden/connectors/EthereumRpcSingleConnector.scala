package com.myodov.unicherrygarden.connectors

import java.time.Instant
import java.util.concurrent.TimeUnit

import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.typesafe.scalalogging.LazyLogging
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject
import org.web3j.protocol.core.methods.response._
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Numeric.decodeQuantity

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._
import scala.util.control.NonFatal

/** Connector that handles a connection to single Ethereum node via RPC, and communicates with it.
 */
class EthereumRpcSingleConnector(private[this] val nodeUrl: String) extends LazyLogging {
  override def toString: String = s"EthereumRpcSingleConnector($nodeUrl)"

  private[this] var web3j: Web3j = rebuildWeb3j()

  private[this] def reconnect(): Unit = {
    web3j.shutdown()
    web3j = rebuildWeb3j()
  }

  private[this] def rebuildWeb3j(): Web3j = {
    Web3j.build(new HttpService(nodeUrl))
  }


  private[this] final case class SyncingStatusData(currentBlock: Int = 0, highestBlock: Int = 0) {
    assert(currentBlock >= 0)
    assert(highestBlock >= 0)
  }

  private[EthereumRpcSingleConnector] class SyncingStatusResult(private val data: Option[SyncingStatusData]) {
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
    private[EthereumRpcSingleConnector] def createSyncing(currentBlock: Int, highestBlock: Int): SyncingStatusResult =
      new SyncingStatusResult(Option(SyncingStatusData(currentBlock, highestBlock)))

    @inline
    private[EthereumRpcSingleConnector] def createNotSyncing(): SyncingStatusResult =
      new SyncingStatusResult(Option.empty)
  }


  /** Get the status of the syncing process for this Ethereum node (`eth.syncing`).
   *
   * @return [[Option]] with the data about the syncing process; the Option is empty if the data could not be received
   *         (probably due to some network error).
   */
  def syncingStatus: Option[SyncingStatusResult] = {
    try {
      val result: EthSyncing.Result = web3j.ethSyncing.send.getResult
      if (!result.isSyncing) {
        logger.debug("eth.syncing = not syncing")
        Option(SyncingStatusResult.createNotSyncing())
      } else {
        val syncingResult: EthSyncing.Syncing = result.asInstanceOf[EthSyncing.Syncing]

        val currentBlockStr = syncingResult.getCurrentBlock
        val highestBlockStr = syncingResult.getHighestBlock
        logger.debug(s"eth.syncing = current $currentBlockStr, highest $highestBlockStr")

        Option(SyncingStatusResult.createSyncing(
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


  /** Get the number of the last block synced by this Ethereum node (`eth.blockNumber`).
   *
   * @return [[Option]] with the data about the syncing process; the Option is empty if the data could not be received
   *         (probably due to some network error).
   */
  def latestSyncedBlockNumber: Option[BigInt] = {
    try {
      Some(web3j.ethBlockNumber.send.getBlockNumber)
    } catch {
      case NonFatal(e) => {
        logger.error("Cannot call eth.blockNumber!", e)
        None
      }
    }
  }

  /** Read the block from Ethereum node (by the block number).
   *
   * @param blockNumber      what block to read (by its number).
   * @param filterAddresses  list of address hashes (all lowercased); only these addresses are returned.
   * @param filterCurrencies list of currencies; only these currencies are returned.
   */
  def readBlock(blockNumber: BigInt,
                filterAddresses: Set[String] = Set.empty,
                filterCurrencies: Set[dlt.Asset] = Set.empty
               ): Option[(dlt.EthereumBlock, List[dlt.Transaction], List[dlt.Transfer])] = {
    val startTime = System.nanoTime

    try {
      val block: EthBlock.Block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber.bigInteger), true).send.getBlock
      require(blockNumber.bigInteger == block.getNumber, (blockNumber, block.getNumber))

      val blockTime = Instant.ofEpochSecond(block.getTimestamp.longValueExact)


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

      // Get the transactions
      //      val transactions: LazyList[EthBlock.TransactionResult[_]] = block.getTransactions.asScala.to(LazyList)
      val transactions: List[Transaction] = block.getTransactions.asScala.to(List).map(tr => tr.asInstanceOf[TransactionObject].get)

      // Get all the receipts, as fast as we can
      val validReceipts: List[TransactionReceipt] = {
        // There is a complex asynchronous launch, do it inside
        val receiptFutures: LazyList[Future[EthGetTransactionReceipt]] = transactions.to(LazyList).map((tr: Transaction) => {
          //        println(s"TR: Hash ${tr.getHash}: gas ${tr.getGas}, gas price ${tr.getGasPrice}")
          web3j.ethGetTransactionReceipt(tr.getHash).sendAsync.asScala
        })

        val batchSize = 64
        val receiptFuturesBatched: LazyList[LazyList[Future[EthGetTransactionReceipt]]] = receiptFutures.sliding(batchSize, batchSize).to(LazyList)
        //      println(s"Futures batched: $receiptFuturesBatched, ${receiptFuturesBatched.length}")

        val totalReceipts: Seq[Option[TransactionReceipt]] = receiptFuturesBatched.map(lazyBatchOfFutures => {
          //        println(s"Batch! $lazyBatchOfFutures")
          val batchOfFutures: Seq[Future[EthGetTransactionReceipt]] = lazyBatchOfFutures.toList
          val futureOfBatch: Future[Seq[EthGetTransactionReceipt]] = Future.sequence(batchOfFutures)
          val batchResult: Seq[EthGetTransactionReceipt] = Await.result(futureOfBatch, Duration.Inf)
          //        println(s"Batch result: $batchResult")
          //        println("----------\n")

          batchResult.map(trr => trr.getTransactionReceipt.toScala)
        }).flatten.toList

        logger.debug(s"Total receipts: ${totalReceipts.length}")
        totalReceipts.flatten.to(List)
      }
      logger.debug(s"Valid receipts: ${validReceipts.length}")

      val receiptsByTrHash: Map[String, TransactionReceipt] = validReceipts.iterator.map(v => v.getTransactionHash -> v).toMap
      require(receiptsByTrHash.size == validReceipts.size, (receiptsByTrHash.size, validReceipts.size))

      println("\nTransactions:")
      for (tr <- transactions) {
        val value = EthUtils.Wei.valueFromWeis(tr.getValue)
        println(s"TR: Hash ${tr.getHash}: value $value, gas ${tr.getGas}, gas price ${tr.getGasPrice}")
      }

      println("\nTransaction receipts:")
      for (tr <- transactions) {
        val value = EthUtils.Wei.valueFromWeis(tr.getValue)
        println(s"TRR: Hash ${tr.getHash}: value $value")
      }

      //
      // First, analyze the transactions for ETH
      //

      // These are the transactions in web3j format
      val addressFromToFilteredW3jEthTransactions =
        for (tr <- transactions if filterAddresses.contains(tr.getFrom) || filterAddresses.contains(tr.getTo))
          yield tr
      val addressFromFilteredW3jEthTransactions =
        for (tr <- transactions if filterAddresses.contains(tr.getFrom))
          yield tr

      val addressFilteredEthTransactions: List[dlt.Transaction] = addressFromToFilteredW3jEthTransactions.map(trw3j => {
        val trReceipt: TransactionReceipt = receiptsByTrHash(trw3j.getHash)
        val logs = EthereumRpcSingleConnector.getLogsFromTransactionReceipt(trReceipt)
        logger.debug(s"Found logs for ETH transaction: $logs")

        dlt.EthereumMinedTransaction(
          // Before-mined transaction
          txhash = trw3j.getHash,
          from = trw3j.getFrom,
          to = Option(trw3j.getTo),
          gas = trw3j.getGas,
          gasPrice = trw3j.getGasPrice,
          nonce = trw3j.getNonce.intValueExact,
          value = trw3j.getValue,
          // Mined transaction
          status = decodeQuantity(trReceipt.getStatus).intValueExact,
          blockNumber = trw3j.getBlockNumber,
          transactionIndex = trReceipt.getTransactionIndex.intValueExact,
          gasUsed = trReceipt.getGasUsed,
          effectiveGasPrice = decodeQuantity(trReceipt.getEffectiveGasPrice),
          cumulativeGasUsed = trReceipt.getCumulativeGasUsed,
          txLogs = logs
        )
      })
      println(s"Filtered ETH! $addressFilteredEthTransactions")

      val addressFilteredEthTransactionsByHash: Map[String, dlt.Transaction] =
        addressFilteredEthTransactions.iterator.map(tr => tr.txhash -> tr).toMap

      val addressFilteredEthTransfers = {
        val addressFilteredEthTransfersDirect = addressFromToFilteredW3jEthTransactions.map(trw3j =>
          dlt.Transfer(
            from = Option(trw3j.getFrom),
            to = Option(trw3j.getTo),
            currency = dlt.Ether,
            amount = EthUtils.Wei.valueFromWeis(trw3j.getValue),
            tr = addressFilteredEthTransactionsByHash(trw3j.getHash)
          )
        )

        println(s"Transfers ETH direct: $addressFilteredEthTransfersDirect")

        val addressFilteredEthTransfersFeePayments = addressFromFilteredW3jEthTransactions.map(trw3j =>
          dlt.Transfer(
            from = Option(trw3j.getFrom),
            to = None,
            currency = dlt.Ether,
            amount = BigDecimal(EthUtils.Wei.valueFromWeis(trw3j.getGasPrice)) * BigDecimal(receiptsByTrHash(trw3j.getHash).getGasUsed),
            tr = addressFilteredEthTransactionsByHash(trw3j.getHash)
          )
        )
        println(s"Transfers ETH for fees: $addressFilteredEthTransfersFeePayments")

        addressFilteredEthTransfersDirect ::: addressFilteredEthTransfersFeePayments
      }

      println(s"Total ETH transfers: $addressFilteredEthTransfers")

      //        val value = EthUtils.Wei.valueFromWeis(tr.getValue)
      //        println(s"TR: Hash ${tr.getHash}: value $value; from ${tr.getFrom} to ${tr.getTo}")
      //        println(s"VS: $filterAddresses")


      //      for (trr <- validReceipts) {
      //        println(s"TR: Hash ${trr.getTransactionHash}: gas ${trr.getV}")
      //      }

      //      for (trr <- transactions) {
      ////        println(s"TR: Hash ${tr.getHash}: gas ${tr.getGas}, gas price ${tr.getGasPrice}")
      //
      //        println(s"  > Receipt ${receipt.getTransactionHash}")
      //
      //        val logs: List[Log] = receipt.getLogs.asScala.toList
      //
      //
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
      //        }

      //        val transaction = EthereumTransaction(
      //          tr.getHash,
      //          tr.getBlockNumber,
      ////          Instant.ofEpochSecond(tr.getTime)
      //
      //
      //
      //        )
      //        println(s" > $transaction")
      //      Option(EthereumBlock(
      //        blockNumber,
      //        block.getHash,
      //        Some(block.getParentHash),
      //        Instant.ofEpochSecond(block.getTimestamp.longValue())
      //      ))
      val duration = Duration(System.nanoTime - startTime, TimeUnit.NANOSECONDS)

      logger.debug(s"Duration ${duration.toMillis} ms")

      Some((
        dlt.EthereumBlock(block.getNumber.intValueExact(), block.getHash, block.getParentHash, blockTime),
        List(),
        List()
      ))
    } catch {
      case NonFatal(e) => {
        logger.error(s"Cannot run readBlock($blockNumber)!", e)
        None
      }
    }
  }
}


/** Connector that handles a connection to single Ethereum node via RPC, and communicates with it. */
object EthereumRpcSingleConnector {
  @inline def apply(nodeUrl: String): EthereumRpcSingleConnector = new EthereumRpcSingleConnector(nodeUrl)

  /** Convert the web3j-provided [[TransactionReceipt]] to the [[Seq]] of [[dlt.TxLog]]. */
  def getLogsFromTransactionReceipt(trReceipt: TransactionReceipt): Seq[dlt.TxLog] = trReceipt
    .getLogs
    .asScala
    .map((l: Log) => dlt.EthereumTxLog(
      l.getLogIndex.intValueExact,
      l.getTopics.asScala.toList,
      l.getData
    )).toList
}
