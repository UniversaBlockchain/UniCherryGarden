package com.myodov.unicherrygarden.connectors

import java.math.BigInteger

import org.web3j.abi.datatypes.{Address, Type}
import org.web3j.abi.{EventEncoder, FunctionReturnDecoder}
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.protocol.core.methods.response.Log

import scala.jdk.CollectionConverters._


/** ERC20 “Transfer” event data. */
class Erc20TransferEvent(val from: String, val to: String, val tokenAddress: String, val value: BigInt) {
  override def toString: String = s"Erc20TransferEvent(from=$from, to=$to, tokenAddress=$tokenAddress, value=$value)"
}


/** Utility object that assists in analyzing Ethereum ERC20 “Transfer” events from the block logs. */
object Erc20TransferEvent {
  private val transferEvent = ERC20.TRANSFER_EVENT

  val eventIndexedParametersJava = ERC20.TRANSFER_EVENT.getIndexedParameters
  val eventIndexedParameters = eventIndexedParametersJava.asScala.toList
  val eventNonIndexedParametersJava = ERC20.TRANSFER_EVENT.getNonIndexedParameters
  val eventNonIndexedParameters = eventNonIndexedParametersJava.asScala.toList

  /** The Transfer event signature usable to match the logs. */
  val signature: String = EventEncoder.encode(transferEvent)

  final case class TopicParseResult(from: String, to: String)

  /** Analyze the `topics` of the Ethereum log; if they are for ERC20 “Transfer” */
  private[connectors] def parseTopics(topics: List[String]): Option[TopicParseResult] = {
    topics match {
//    if signatureTopic == Erc20TransferEvent.signature
      case Erc20TransferEvent.signature :: indexedParameters => {
        assert(indexedParameters.length == 2)
        val argFrom: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(0), eventIndexedParametersJava.get(0)).asInstanceOf[Type[Address]]
        val argTo: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(1), eventIndexedParametersJava.get(1)).asInstanceOf[Type[Address]]
        Option(TopicParseResult(argFrom.toString, argTo.toString))
      }
      case _ => Option.empty
    }
  }

  def getEventFromEthereumLog(log: Log): Option[Erc20TransferEvent] = {
    val topics: List[String] = log.getTopics.asScala.toList

    parseTopics(topics).map(topicsParseResult => {
      val resultList = FunctionReturnDecoder.decode(log.getData, Erc20TransferEvent.eventNonIndexedParametersJava).asScala.toList
      assert(resultList.length == 1)

      Erc20TransferEvent(
        topicsParseResult.from,
        topicsParseResult.to,
        log.getAddress,
        resultList(0).getValue.asInstanceOf[BigInteger]
      )
    })

    //    lazy val logData: String = log.getData

//    topics match {
//      case signatureTopic :: indexedParameters if signatureTopic == Erc20TransferEvent.signature => {
//        //        assert(indexedParameters.length == 2)
//        //        val argFrom: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(0), eventIndexedParametersJava.get(0)).asInstanceOf[Type[Address]]
//        //        val argTo: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(1), eventIndexedParametersJava.get(1)).asInstanceOf[Type[Address]]
//
//        val resultList = FunctionReturnDecoder.decode(logData, Erc20TransferEvent.eventNonIndexedParametersJava).asScala.toList
//        assert(resultList.length == 1)
//
//        val transferAmountBigInteger: BigInt = resultList(0).getValue.asInstanceOf[BigInteger]
//        Option(Erc20TransferEvent(
//          argFrom.toString,
//          argTo.toString,
//          log.getAddress,
//          transferAmountBigInteger
//        ))
//      }
//      case _ => Option.empty
//    }
  }

  @inline def apply(from: String, to: String, tokenAddress: String, value: BigInt): Erc20TransferEvent =
    new Erc20TransferEvent(from, to, tokenAddress, value)
}
