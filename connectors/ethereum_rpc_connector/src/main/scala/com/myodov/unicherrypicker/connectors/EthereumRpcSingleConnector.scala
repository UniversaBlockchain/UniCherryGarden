package com.myodov.unicherrypicker.connectors

import java.time.Instant

import com.myodov.unicherrypicker.api.dlt.{Block, EthereumBlock}
import com.typesafe.scalalogging.Logger
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.methods.response.{EthBlock, Transaction}
import org.web3j.protocol.core.methods.response.EthBlock.TransactionObject
import org.web3j.protocol.http.HttpService

import scala.jdk.CollectionConverters._

/** Connector that handles a connection to single Ethereum node via RPC, and communicates with it.
 * */
class EthereumRpcSingleConnector(private[this] val nodeUrl: String) {
  private[this] lazy val logger = Logger[EthereumRpcSingleConnector.type]

  override def toString: String = s"EthereumRpcSingleConnector(${nodeUrl})"

  private[this] var web3j: Web3j = rebuildWeb3j()

  private[this] def reconnect(): Unit = {
    web3j.shutdown()
    web3j = rebuildWeb3j()
  }

  private[this] def rebuildWeb3j(): Web3j = {
    Web3j.build(new HttpService(nodeUrl))
  }

  /** Get the number of the last block synced by this Ethereum node (`eth.blockNumber`). */
  private[connectors] def latestSyncedBlockNumber: Option[BigInt] = {
    try {
      Some(web3j.ethBlockNumber.send.getBlockNumber)
    } catch {
      case e: Throwable => {
        logger.error(s"Cannot run latestSyncedBlockNumber()!", e)
        None
      }
    }
  }

  /** Read the block from Ethereum node (by the block number). */
  private[connectors] def readBlock(blockNumber: BigInt): Option[Block] = {
    try {
      val block: EthBlock.Block = web3j.ethGetBlockByNumber(new DefaultBlockParameterNumber(blockNumber.bigInteger), true).send.getBlock
      assert(blockNumber == BigInt(block.getNumber))

      val transactions: List[EthBlock.TransactionResult[_]] = block.getTransactions.asScala.toList
      for (trr <- transactions) {
        val tr: Transaction = trr.asInstanceOf[TransactionObject].get
        println(s"TR: Hash ${tr.getHash}")
      }
      Option(EthereumBlock(
        blockNumber,
        block.getHash,
        Some(block.getParentHash),
        Instant.ofEpochSecond(block.getTimestamp.longValue())
      ))
    } catch {
      case e: Throwable => {
        logger.error(s"Cannot run readBlock($blockNumber)!", e)
        None
      }
    }
  }
}

object EthereumRpcSingleConnector {
  def apply(nodeUrl: String): EthereumRpcSingleConnector = new EthereumRpcSingleConnector(nodeUrl)
}
