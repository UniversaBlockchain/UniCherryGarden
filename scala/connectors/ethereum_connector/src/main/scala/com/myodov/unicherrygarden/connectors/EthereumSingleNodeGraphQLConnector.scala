package com.myodov.unicherrygarden.connectors

import java.time.Instant

import akka.actor.typed.{ActorSystem => TypedActorSystem}
import akka.actor.{ActorSystem => ClassicActorSystem}
import caliban.client.CalibanClientError
import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.connectors.graphql._
import com.myodov.unicherrygarden.ethereum.EthUtils
import com.typesafe.scalalogging.LazyLogging
import sttp.capabilities
import sttp.capabilities.akka.AkkaStreams
import sttp.client3.akkahttp.AkkaHttpBackend
import sttp.client3.{Response, SttpBackend, UriContext}
import sttp.model.Uri

import scala.annotation.switch
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

  override def ethSyncingBlockNumber: Option[SyncingStatus] = {
    import caliban.Geth._

    val query = Query.syncing {
      SyncState.view
    } ~ Query.block() {
      BlockMinimal.view
    }

    val rq = query.toRequest(graphQLUri)

    try {
      val value = Await.result(rq.send(sttpBackend), AbstractEthereumNodeConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          logger.error(s"Error for GraphQL querying sync state", err)
          None
        case Right((optSyncing, optRecentBlock)) =>
          // Received a valid response; do something with both paths:
          (optSyncing, optRecentBlock) match {
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
              logger.debug(s"The node is in incomprehensible state of syncing: $optSyncing, $optRecentBlock")
              None
          }
        case other =>
          logger.error(s"Unhandled GraphQL response for GraphQL querying sync state: $other")
          None
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Some nonfatal error happened during GraphQL querying sync state", e)
        None
    }
  }

  override def readBlock(blockNumber: BigInt): Option[(dlt.EthereumBlock, Seq[dlt.EthereumMinedTransaction])] = {
    import caliban.Geth._

    val query = Query.block(number = Some(blockNumber.longValue)) {
      BlockBasic.view
    }

    val rq = query.toRequest(graphQLUri)

    try {
      val value: Response[Either[CalibanClientError, Option[BlockBasicView]]] =
        Await.result(rq.send(sttpBackend), AbstractEthereumNodeConnector.NETWORK_TIMEOUT)

      value.body match {
        case Left(err) =>
          logger.error(s"Error for GraphQL querying block $blockNumber", err)
          None
        case Right(optBlockBasic) =>
          // This is a legit response; but it may have no contents.
          // For None, return None; for Some return a result,... hey it’s a map!
          optBlockBasic.map { blockBasic =>
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
        case other =>
          logger.error(s"Unhandled GraphQL response for block $blockNumber: $other")
          None
      }
    } catch {
      case NonFatal(e) =>
        logger.error(s"Some nonfatal error happened during GraphQL query for $blockNumber", e)
        None
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
