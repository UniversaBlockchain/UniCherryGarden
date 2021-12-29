package com.myodov.unicherrygarden.connectors.graphql

import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorSystem => TypedActorSystem}
import akka.actor.{ActorSystem => ClassicActorSystem}
import caliban.client.Operations.RootQuery
import caliban.client.{CalibanClientError, SelectionBuilder}
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector.{SingleBlockData, SyncingStatus}
import com.myodov.unicherrygarden.connectors.graphql.types.{BlockBasic, BlockMinimal, TransactionFullView}
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.typesafe.scalalogging.LazyLogging
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
    with LazyLogging {

  override def toString: String = s"EthereumSingleNodeGraphQLConnector($nodeUrl)"

  protected val graphQLUri: Uri = uri"$nodeUrl/graphql"

  /** Backend used for sending out queries. */
  protected val sttpBackend: SttpBackend[Future, AkkaStreams with capabilities.WebSockets] =
    (preferredActorSystem: @switch) match {
      case None => AkkaHttpBackend()
      case Some(actorSystem: ClassicActorSystem) => AkkaHttpBackend.usingActorSystem(actorSystem)
    }

  /** Perform a GraphQL query with all the necessary error handling.
   *
   * `QV` - either `(Option[SyncState.SyncStateView], Option[BlockMinimalView])` or List[BlockBasicView]`,
   * or something similar..
   **/
  private[this] def queryGraphQL[QV](query: SelectionBuilder[RootQuery, QV],
                                     argHint: String): Option[QV] = {
    val rq: Request[Either[CalibanClientError, QV], Any] = query.toRequest(graphQLUri)
    try {
      val value = Await.result(rq.send(sttpBackend), AbstractEthereumNodeConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          logger.error(s"Error for GraphQL querying $argHint", err)
          None
        case Right(res) =>
          // Received a valid response
          Some(res)
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

  override def ethSyncingBlockNumber: Option[SyncingStatus] = {
    import caliban.Geth._

    // Either we have some data in `syncing`; or we must get the most recent block as just `block {number hash}`
    val query =
      Query.syncing {
        SyncState.view
      } ~ Query.block() {
        BlockMinimal.view
      }

    // Received a valid response; do something with both paths:
    queryGraphQL(query, argHint = "ethSyncingBlockNumber").flatMap(_ match {
      case (Some(syncing), _) =>
        // The node is still syncing
        logger.debug(s"The node is still syncing: $syncing")
        Some(SyncingStatus(
          currentBlock = Math.toIntExact(syncing.currentBlock),
          highestBlock = Math.toIntExact(syncing.highestBlock)
        ))
      case (None, Some(latestBlock)) =>
        logger.debug(s"The node is fully synced, most recent block is $latestBlock")
        // Not syncing already; but does the block number really makes sense?
        if (latestBlock.number > 0) // This is a sensible block
          Some(SyncingStatus(
            currentBlock = Math.toIntExact(latestBlock.number),
            highestBlock = Math.toIntExact(latestBlock.number)
          ))
        else None // not really synced
      case other =>
        logger.debug(s"The node is in incomprehensible state of syncing: $other")
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
    require(range.head <= range.end, range)

    import caliban.Geth._

    val query =
      Query.blocks(from = Some(range.head), to = Some(range.end)) {
        BlockBasic.view
      }

    val queryStartTime = System.nanoTime

    // This is a legit response; but it may have no contents.
    // For None, return None; for Some return a result,... hey it’s a map!
    queryGraphQL(query, argHint = s"readBlocks($range)").flatMap { blocks =>
      val queryDuration = Duration(System.nanoTime - queryStartTime, TimeUnit.NANOSECONDS)
      logger.debug(s"Querying for blocks $range (${range.size} blocks) took ${queryDuration.toMillis} ms.")

      if (!BlockBasic.validateBlocks(blocks)) {
        None // validation failed, let’s consider reading failed too
      } else {
        val resultSeq = blocks.map { blockBasic =>
          val blockNumber = Math.toIntExact(blockBasic.number)

          val block = dlt.EthereumBlock(
            number = blockNumber,
            hash = blockBasic.hash,
            parentHash = blockBasic.parent match {
              // We need some custom handling of parent
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
                // "status" – EIP 658, since Byzantium fork
                // using Option(nullable).
                // But there seems to be a bug in GraphQL handling pre-Byzantium statuses,
                // (https://github.com/ethereum/go-ethereum/issues/24124)
                // So need to handle this manually.
                status = blockNumber match {
                  case preByzantium if preByzantium < EthUtils.BYZANTIUM_FIRST_BLOCK =>
                    None
                  case byzantiumAndNewer =>
                    tr.status.map(Math.toIntExact) // Option[Long] to Option[Int]
                },
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
    require(range.head <= range.end, range)

    import caliban.Geth._

    val query =
      Query.blocks(from = Some(range.head), to = Some(range.end)) {
        BlockMinimal.view
      }

    queryGraphQL(query, "readBlockHashes($range)").map {
      // If result is present, convert the result list to result map
      _.map(bl => Math.toIntExact(bl.number) -> bl.hash)
        .to(SortedMap)
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
