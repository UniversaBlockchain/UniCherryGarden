package com.myodov.unicherrygarden

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.cherrygardener.messages.{CherryGardenerRequest, GetCurrenciesList, PingCherryGardener}
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
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrygardener.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector,
            cherryPickerOpt: Option[ActorRef[CherryPicker.CherryPickerMessage]],
            cherryPlanterOpt: Option[ActorRef[CherryPlanter.CherryPlanterMessage]]
           ): Behavior[CherryGardenerRequest] = {

    logger.debug(s"Setting up CherryGardener: picker $cherryPickerOpt, planter $cherryPlanterOpt")

    //    val gardener = new CherryGardener(pgStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryGardener: v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(GetCurrenciesList.SERVICE_KEY, context.self)
      context.system.receptionist ! Receptionist.Register(PingCherryGardener.SERVICE_KEY, context.self)

      Behaviors.receiveMessage {
        case message: GetCurrenciesList.Request => {
          logger.debug(s"Received GetCurrenciesList($message) command")
          message.replyTo ! new GetCurrenciesList.Response("This is the result")
          Behaviors.same
        }
        case unknown => {
          logger.error(s"Unknown CherryGardener message: $unknown")
          Behaviors.same
        }
      }
    }
  }
}
