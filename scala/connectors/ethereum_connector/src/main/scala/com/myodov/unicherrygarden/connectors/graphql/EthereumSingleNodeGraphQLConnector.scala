package com.myodov.unicherrygarden.connectors.graphql

import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorSystem => TypedActorSystem}
import akka.actor.{ActorSystem => ClassicActorSystem}
import caliban.client.Operations.{RootMutation, RootQuery}
import caliban.client.{CalibanClientError, SelectionBuilder}
import com.myodov.unicherrygarden.AbstractEthereumNodeConnector.SingleBlockData
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.connectors.graphql.types._
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.myodov.unicherrygarden.{AbstractEthereumNodeConnector, Web3ReadOperations, Web3WriteOperations}
import com.typesafe.scalalogging.LazyLogging
import org.bouncycastle.util.encoders.Hex
import sttp.capabilities
import sttp.capabilities.akka.AkkaStreams
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.{Request, SttpBackend, UriContext}
import sttp.model.Uri

import scala.annotation.switch
import scala.collection.immutable.SortedMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

/** Connector that communicates with a single Ethereum node using GraphQL (via Caliban library). */
class EthereumSingleNodeGraphQLConnector(nodeUrl: String,
                                         preferredActorSystem: Option[ClassicActorSystem])
  extends AbstractEthereumNodeConnector(nodeUrl)
    with Web3ReadOperations
    with Web3WriteOperations
    with LazyLogging {

  override def toString: String = s"EthereumSingleNodeGraphQLConnector($nodeUrl)"

  protected val graphQLUri: Uri = uri"$nodeUrl/graphql"

  /** Backend used for sending out queries. */
  protected val sttpBackend: SttpBackend[Future, AkkaStreams with capabilities.WebSockets] =
    (preferredActorSystem: @switch) match {
      case None => AkkaHttpBackend()
      case Some(actorSystem: ClassicActorSystem) => AkkaHttpBackend.usingActorSystem(actorSystem)
    }

  /** Execute a GraphQL mutation, with the GraphQL error handling passed to the user
   * (some errors are still handled automatically). Good for mutations.
   *
   * If some low-level error happened, returns `None` (and the error is logged).
   * If some GraphQL error was returned (e.g. from mutation), returns `Some(Left(String))`,
   * where String is the error message.
   * If no GraphQL error happened, returns `Some(Right(QV))`.
   *
   * `QV` - either `(Option[SyncState.SyncStateView], Option[BlockMinimalView])` or `List[BlockBasicView]`,
   * or something similar ??? the actual result of mutation.
   */
  private[this] def sendGraphQLMutation[QV](query: SelectionBuilder[RootMutation, QV],
                                            argHint: String): Option[Either[String, QV]] = {
    val rq: Request[Either[CalibanClientError, QV], Any] = query.toRequest(graphQLUri)
    try {
      val value = Await.result(rq.send(sttpBackend), AbstractEthereumNodeConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          // Received a valid/but unsuccessful response, may return it
          Some(Left(err.getMessage))
        case Right(res) =>
          // Received a valid/successful response, may return it
          Some(Right(res))
        case other =>
          logger.error(s"Unhandled GraphQL response for GraphQL querying $argHint: $other")
          None
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Some nonfatal error happened during GraphQL querying $argHint", e)
        None
    }
  }


  /** Execute a GraphQL query, with the GraphQL error handling passed to the user
   * (some errors are still handled automatically). Good for mutations.
   *
   * If some low-level error happened, returns `None` (and the error is logged).
   * If some GraphQL error was returned (e.g. from mutation), returns `Some(Left(String))`,
   * where String is the error message.
   * If no GraphQL error happened, returns `Some(Right(QV))`.
   *
   * `QV` - either `(Option[SyncState.SyncStateView], Option[BlockMinimalView])` or `List[BlockBasicView]`,
   * or something similar ??? the actual result of query.
   */
  private[this] def sendGraphQLQuery[QV](query: SelectionBuilder[RootQuery, QV],
                                         argHint: String): Option[Either[String, QV]] = {
    val rq: Request[Either[CalibanClientError, QV], Any] = query.toRequest(graphQLUri)
    try {
      val value = Await.result(rq.send(sttpBackend), AbstractEthereumNodeConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          // Received a valid/but unsuccessful response, may return it
          Some(Left(err.getMessage))
        case Right(res) =>
          // Received a valid/successful response, may return it
          Some(Right(res))
        case other =>
          logger.error(s"Unhandled GraphQL response for GraphQL querying $argHint: $other")
          None
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Some nonfatal error happened during GraphQL querying $argHint", e)
        None
    }
  }

  /** Execute a GraphQL query with all the necessary error handling being automatic.
   *
   * `QV` - either `(Option[SyncState.SyncStateView], Option[BlockMinimalView])` or `List[BlockBasicView]`,
   * or something similar.
   */
  private[this] def sendGraphQLQueryHandleErrors[QV](query: SelectionBuilder[RootQuery, QV],
                                                     argHint: String): Option[QV] =
    sendGraphQLQuery(query, argHint) match {
      case None =>
        // Error logged already, do nothing
        None
      case Some(Left(message)) =>
        // Log it ourselves, treat as None
        logger.error(s"Error for GraphQL querying $argHint: $message")
        None
      case Some(Right(qv)) =>
        // Treat as okay
        Some(qv)
    }

  override def ethBlockchainStatus: Option[SystemStatus.Blockchain] = {
    import caliban.Geth._

    // Either we have some data in `syncing`; or we must get the most recent block as just `block {number hash}`
    val query =
      Query.syncing {
        SyncState.view
      } ~ Query.block() {
        BlockLatest.view
      } ~ Query.maxPriorityFeePerGas

    // Received a valid response; do something with both paths:
    sendGraphQLQueryHandleErrors(query, argHint = "ethBlockchainStatus").flatMap(_ match {
      case ((Some(syncState), Some(nonlatestBlock)), maxPriorityFeePerGas) =>
        // The node is still syncing
        logger.debug(s"The node is still syncing: $syncState")
        Some(SystemStatus.Blockchain.create(
          SystemStatus.Blockchain.SyncingData.create(
            Math.toIntExact(syncState.currentBlock),
            Math.toIntExact(syncState.highestBlock)
          ),
          // This is really not a latest block; but we use what we've received
          nonlatestBlock.asLatestBlock,
          maxPriorityFeePerGas.bigInteger
        ))
      case ((None, Some(latestBlock)), maxPriorityFeePerGas) =>
        // Not syncing already.
        logger.debug(s"The node is fully synced, most recent block is $latestBlock")
        // But does the block number really makes sense?
        val latestBlockNumber = Math.toIntExact(latestBlock.number)
        if (latestBlockNumber > 0) // This is a sensible block
          Some(SystemStatus.Blockchain.create(
            SystemStatus.Blockchain.SyncingData.create(
              latestBlockNumber,
              latestBlockNumber
            ),
            latestBlock.asLatestBlock,
            maxPriorityFeePerGas.bigInteger
          ))
        else None // not really synced
      case other =>
        logger.error(s"The node is in incomprehensible and unexpected state of syncing: $other")
        None
    })
  }

  override def readBlock(blockNumber: BigInt): Option[SingleBlockData] = {
    // Comparing to the default Web3ReadOperations implementation, we use an inverted implementation here:
    // `readBlocks` is implemented in details, `readBlock` just calls it.

    // Option of Seq can be easily converted to a single Option with just flatMap
    readBlocks(blockNumber.intValue to blockNumber.intValue).flatMap {
      _ match {
        case emptySeq if emptySeq.isEmpty => None
        case Seq(singleItem) => Some(singleItem)
        case tooLargeSeq => None
      }
    }
  }

  override def readBlocks(range: dlt.EthereumBlock.BlockNumberRange): Option[Seq[SingleBlockData]] = {
    require(range.head <= range.last, range)

    import caliban.Geth._

    val query =
      Query.blocks(from = Some(range.head), to = Some(range.last)) {
        BlockBasic.view
      }

    val queryStartTime = System.nanoTime

    // This is a legit response; but it may have no contents.
    // For None, return None; for Some return a result,... hey it???s a map!
    sendGraphQLQueryHandleErrors(query, argHint = s"readBlocks($range)").flatMap { blocks =>
      val queryDuration = Duration(System.nanoTime - queryStartTime, TimeUnit.NANOSECONDS)
      logger.debug(s"Querying for blocks $range (${range.size} blocks) " +
        s"took ${queryDuration.toMillis} ms")

      blocks match {
        case invalidResult if !BlockBasic.validateBlocks(invalidResult) =>
          logger.error(s"Queried $range (${range.size} blocks) " +
            "returned invalid result")
          None // validation failed, let???s consider reading failed too
        case emptyResult@Seq() =>
          logger.debug(s"Querying for blocks $range (${range.size} blocks) " +
            "returned empty result")
          None
        case nonEmptyResults =>
          val resultSeq = blocks.map { blockBasic =>
            val blockNumber = Math.toIntExact(blockBasic.number)

            val block = dlt.EthereumBlock(
              number = blockNumber,
              hash = blockBasic.hash,
              parentHash = blockBasic.parent match {
                // We need some custom handling of empty parent
                // to make it compatible with RPC/block explorers
                case None => Some("0x0000000000000000000000000000000000000000000000000000000000000000")
                case Some(parent) => Some(parent.hash)
              },
              timestamp = Instant.ofEpochSecond(blockBasic.timestamp)
            )
            val transactions = blockBasic.transactions match {
              case None => Seq()
              case Some(transactions) => transactions.map { (tr: TransactionFullView) =>
                dlt.EthereumMinedTransaction(
                  // *** Before-mined transaction ***
                  txhash = tr.hash,
                  from = tr.from.address,
                  to = tr.to.map(_.address), // Option(nullable)
                  gas = tr.gas,
                  gasPrice = tr.gasPrice,
                  nonce = Math.toIntExact(tr.nonce),
                  value = tr.value,
                  // *** Mined transaction ***
                  // "status" ??? EIP 658, since Byzantium fork
                  status = tr.status.map(Math.toIntExact), // Option[Long] to Option[Int]
                  blockNumber = tr.block.get.number, // block must exist!
                  transactionIndex = tr.index.get, // transaction must exist!
                  gasUsed = tr.gasUsed.get, // presumed non-null if mined
                  effectiveGasPrice = tr.effectiveGasPrice.get, // presumed non-null if mined
                  cumulativeGasUsed = tr.cumulativeGasUsed.get, // presumed non-null if mined
                  txLogs = tr.logs match {
                    case None => Seq.empty
                    case Some(logs) => logs.map { log =>
                      dlt.EthereumTxLog(
                        logIndex = log.index,
                        address = log.account.address,
                        topics = log.topics,
                        data = log.data
                      )
                    }
                  }
                )
              }
            }
            (block, transactions)
          }
          Some(resultSeq)
      }
    }
  }

  override def readBlockHashes(range: dlt.EthereumBlock.BlockNumberRange): Option[SortedMap[Int, String]] = {
    require(range.head <= range.last, range)

    import caliban.Geth._

    val query =
      Query.blocks(from = Some(range.head), to = Some(range.last)) {
        BlockMinimal.view
      }

    sendGraphQLQueryHandleErrors(query, argHint = s"readBlockHashes($range)").map {
      // If result is present, convert the result list to result map
      _.map(bl => Math.toIntExact(bl.number) -> bl.hash)
        .to(SortedMap)
    }
  }

  override def getAddressNonces(address: String): Option[(Int, Option[Int])] = {
    require(EthUtils.Addresses.isValidLowercasedAddress(address), address)

    import caliban.Geth._

    val query =
      Query.block() {
        Block.account(address = address) {
          Account.transactionCount
        }
      } ~ Query.pending {
        Pending.account(address = address) {
          Account.transactionCount
        }
      }

    // Nonces are assumed `Int` here
    sendGraphQLQueryHandleErrors(query, argHint = s"getAddressNonces($address)").flatMap {
      case (None, pendingNonce) =>
        logger.error(s"in getAddressNonces($address), received only pendingNonce $pendingNonce!")
        None
      case (Some(blockNonce), pendingNonce) if pendingNonce < blockNonce =>
        logger.error(s"in getAddressNonces($address), pendingNonce $pendingNonce < blockNonce $blockNonce!")
        None
      case (Some(blockNonce), pendingNonce) =>
        require(pendingNonce < Int.MaxValue, pendingNonce)
        if (pendingNonce == blockNonce) {
          // Pending pool nonce is equal to blockchain nonce; this means there is nothing special in pending pool
          Some((blockNonce.toInt, None))
        } else { // if pendingNonce > blockNonce
          // Pending pool nonce > blockchain nonce; there is something in pending pool!
          Some((blockNonce.toInt, Some(pendingNonce.toInt)))
        }
    }
  }

  override def ethSendRawTransaction(bytes: Array[Byte]): Either[String, String] = {
    require(bytes.size > 0, bytes)

    import caliban.Geth._

    val bytesStr = "0x" + Hex.toHexString(bytes)

    val mutation: SelectionBuilder[RootMutation, Bytes32] = Mutation.sendRawTransaction(data = bytesStr)

    sendGraphQLMutation(mutation, argHint = s"ethSendRawTransaction($bytesStr)") match {
      case None =>
        Left("Unknown error")
      case Some(either: Either[String, String]) =>
        either
    }
  }
}

/** Connector that handles a connection to single Ethereum node via RPC, and communicates with it. */
object EthereumSingleNodeGraphQLConnector {
  @inline def apply(nodeUrl: String): EthereumSingleNodeGraphQLConnector =
    new EthereumSingleNodeGraphQLConnector(nodeUrl, None)

  @inline def apply(nodeUrl: String,
                    preferredActorSystem: ClassicActorSystem): EthereumSingleNodeGraphQLConnector =
    new EthereumSingleNodeGraphQLConnector(nodeUrl, Some(preferredActorSystem))

  @inline def apply[T](nodeUrl: String,
                       preferredActorSystem: TypedActorSystem[T]): EthereumSingleNodeGraphQLConnector =
    new EthereumSingleNodeGraphQLConnector(nodeUrl, Some(preferredActorSystem.classicSystem))
}
