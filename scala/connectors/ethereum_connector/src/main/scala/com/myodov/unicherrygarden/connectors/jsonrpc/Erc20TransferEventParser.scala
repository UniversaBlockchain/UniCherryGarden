package com.myodov.unicherrygarden.connectors.jsonrpc

import java.math.BigInteger

import com.myodov.unicherrygarden.api.dlt.events.Erc20TransferEvent
import org.web3j.abi.datatypes.{Address, Type}
import org.web3j.abi.{EventEncoder, FunctionReturnDecoder}
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.protocol.core.methods.response.Log

import scala.jdk.CollectionConverters._


/** Utility object that assists in analyzing Ethereum ERC20 “Transfer” events from the block logs. */
object Erc20TransferEventParser {
  private val transferEvent = ERC20.TRANSFER_EVENT

  val eventIndexedParametersJava = ERC20.TRANSFER_EVENT.getIndexedParameters
  val eventIndexedParameters = eventIndexedParametersJava.asScala.toList
  val eventNonIndexedParametersJava = ERC20.TRANSFER_EVENT.getNonIndexedParameters
  val eventNonIndexedParameters = eventNonIndexedParametersJava.asScala.toList

  /** The Transfer event signature usable to match the logs. */
  val signature: String = EventEncoder.encode(transferEvent)

  final case class TopicParseResult(from: String, to: String)

  /** Analyze the `topics` of the Ethereum log; if they are for ERC20 “Transfer” event. */
  private[connectors] def parseTopics(topics: List[String]): Option[TopicParseResult] = {
    topics match {
      case Erc20TransferEventParser.signature :: indexedParameters => {
        assert(indexedParameters.length == 2)
        val argFrom: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(0), eventIndexedParametersJava.get(0)).asInstanceOf[Type[Address]]
        val argTo: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(1), eventIndexedParametersJava.get(1)).asInstanceOf[Type[Address]]
        Some(TopicParseResult(argFrom.toString, argTo.toString))
      }
      case _ => None
    }
  }

  def getEventFromEthereumLog(log: Log): Option[Erc20TransferEvent] = {
    val topics: List[String] = log.getTopics.asScala.toList

    parseTopics(topics).map(topicsParseResult => {
      val resultList = FunctionReturnDecoder.decode(log.getData, Erc20TransferEventParser.eventNonIndexedParametersJava).asScala.toList
      assert(resultList.length == 1)

      Erc20TransferEvent(
        topicsParseResult.from,
        topicsParseResult.to,
        resultList(0).getValue.asInstanceOf[BigInteger]
      )
    })

    //    lazy val logData: String = log.getData

    //    topics match {
    //      case signatureTopic :: indexedParameters if signatureTopic == Erc20TransferEventParser.signature => {
    //        //        assert(indexedParameters.length == 2)
    //        //        val argFrom: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(0), eventIndexedParametersJava.get(0)).asInstanceOf[Type[Address]]
    //        //        val argTo: Type[Address] = FunctionReturnDecoder.decodeIndexedValue(indexedParameters(1), eventIndexedParametersJava.get(1)).asInstanceOf[Type[Address]]
    //
    //        val resultList = FunctionReturnDecoder.decode(logData, Erc20TransferEvent.eventNonIndexedParametersJava).asScala.toList
    //        assert(resultList.length == 1)
    //
    //        val transferAmountBigInteger: BigInt = resultList(0).getValue.asInstanceOf[BigInteger]
    //        Some(Erc20TransferEvent(
    //          argFrom.toString,
    //          argTo.toString,
    //          log.getAddress,
    //          transferAmountBigInteger
    //        ))
    //      }
    //      case _ => None
    //    }
  }
}
