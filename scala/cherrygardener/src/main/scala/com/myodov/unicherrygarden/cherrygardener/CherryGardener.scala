package com.myodov.unicherrygarden

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.cherrygardener.messages.Balances.GetBalance
import com.myodov.unicherrygarden.cherrygardener.messages.{Balances, CherryGardenerActorIncomingMessage, CherryGardenerActorOutgoingMessage}
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

/** "Cherry Gardener": the high-level interface to Ethereum blockchain. */
class CherryGardener(private val pgStorage: PostgreSQLStorage,
                     private val ethereumConnector: EthereumRpcSingleConnector) extends LazyLogging {
}


/** Akka actor to run CherryGardener operations. */
object CherryGardener extends LazyLogging {

  //  final case class Hi(msg: String) extends CherryGardenerMessage
  //  final case class Hello(msg: String) extends CherryGardenerMessage

  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector,
            cherryPickerOpt: Option[ActorRef[CherryPicker.CherryPickerMessage]],
            cherryPlanterOpt: Option[ActorRef[CherryPlanter.CherryPlanterMessage]]
           ): Behavior[CherryGardenerActorOutgoingMessage] = {
    logger.debug(s"Setting up CherryGardener: picker $cherryPickerOpt, planter $cherryPlanterOpt")

    val gardener = new CherryGardener(pgStorage, ethereumConnector)

    Behaviors.receive {
      (context, message: CherryGardenerActorOutgoingMessage) => {
        logger.debug(s"Received CherryGardener message: $message")
        message match {
          case messageGB: Balances.GetBalance => {
            logger.debug(s"This is getBalance for ${messageGB.address}, ${messageGB.sender}")
            messageGB.sender ! new Balances.GetBalanceResp(context.self, BigDecimal("17.142").bigDecimal)
          }
          case unknown => {
            logger.error(s"Unknown CherryGardener message: $unknown")
          }
        }
        Behaviors.same
      }
    }
  }
}
