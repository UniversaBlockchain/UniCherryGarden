package com.myodov.unicherrygarden.cherrygardener

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.UnicherrygardenVersion
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector
import com.myodov.unicherrygarden.messages.cherrygardener.{GetCurrencies, PingCherryGardener}
import com.myodov.unicherrygarden.messages.{CherryGardenerRequest, CherryPickerRequest, CherryPlanterRequest}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

/** "Cherry Gardener": the high-level interface to Ethereum blockchain. */
class CherryGardener(private val dbStorage: PostgreSQLStorage,
                     private val ethereumConnector: AbstractEthereumNodeConnector) extends LazyLogging {

  /**
   * Reply to [[GetCurrencies]] request.
   */
  def getCurrencies(getVerified: Boolean, getUnverified: Boolean): GetCurrencies.Response = {
    val result: List[Currency] = dbStorage.Currencies.getCurrencies(getVerified, getUnverified).map(
      c => new Currency(
        dbStorage.Currencies.CurrencyTypes.toInteropType(c.currencyType),
        c.dAppAddress.orNull,
        c.name.orNull,
        c.symbol.orNull,
        c.ucgComment.orNull,
        c.verified,
        c.decimals.map(Integer.valueOf).orNull
      )
    )
    new GetCurrencies.Response(result.asJava)
  }
}


/** Akka actor to run CherryGardener operations. */
object CherryGardener extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrygardener.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  def apply(dbStorage: PostgreSQLStorage,
            ethereumConnector: AbstractEthereumNodeConnector,
            cherryPickerOpt: Option[ActorRef[CherryPickerRequest]],
            cherryPlanterOpt: Option[ActorRef[CherryPlanterRequest]]
           ): Behavior[CherryGardenerRequest] = {

    logger.debug(s"Setting up CherryGardener: picker $cherryPickerOpt, planter $cherryPlanterOpt")

    val gardener = new CherryGardener(dbStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryGardener: v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(PingCherryGardener.SERVICE_KEY, context.self)
      context.system.receptionist ! Receptionist.Register(GetCurrencies.SERVICE_KEY, context.self)

      Behaviors.receiveMessage {
        case message: GetCurrencies.Request => {
          logger.debug(s"Received GetCurrencies($message) command")

          val response = gardener.getCurrencies(message.payload.getVerified, message.payload.getUnverified)
          logger.debug(s"Replying with $response")
          message.replyTo ! response
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
