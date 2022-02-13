package com.myodov.unicherrygarden.cherrygardener

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.UnicherrygardenVersion
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.messages.cherrygardener.{GetCurrencies, PingCherryGardener}
import com.myodov.unicherrygarden.messages.{CherryGardenerRequest, CherryPickerRequest, CherryPlanterRequest}
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import com.typesafe.scalalogging.LazyLogging

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

/** "Cherry Gardener": the high-level interface to Ethereum blockchain. */
class CherryGardener(private val dbStorage: DBStorageAPI) extends LazyLogging {

  /** Reply to [[GetCurrencies]] request. */
  def getCurrencies(
                     filterCurrencyKeys: Option[Set[String]],
                     getVerified: Boolean,
                     getUnverified: Boolean
                   ): GetCurrencies.Response = {
    val result: List[Currency] = dbStorage.currencies.getCurrencies(filterCurrencyKeys, getVerified, getUnverified).map(_.asCurrency)
    new GetCurrencies.Response(new GetCurrencies.CurrenciesRequestResultPayload(result.asJava))
  }
}


/** Akka actor to run CherryGardener operations. */
object CherryGardener extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrygardener.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  def apply(dbStorage: DBStorageAPI,
            cherryPickerOpt: Option[ActorRef[CherryPickerRequest]],
            cherryPlanterOpt: Option[ActorRef[CherryPlanterRequest]]
           ): Behavior[CherryGardenerRequest] = {

    logger.debug(s"Setting up CherryGardener: picker $cherryPickerOpt, planter $cherryPlanterOpt")

    val gardener = new CherryGardener(dbStorage)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryGardener: v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(PingCherryGardener.SERVICE_KEY, context.self)
      context.system.receptionist ! Receptionist.Register(GetCurrencies.SERVICE_KEY, context.self)

      Behaviors.receiveMessage {
        case message: GetCurrencies.Request => {
          logger.debug(s"Received GetCurrencies($message) command")

          val response = gardener.getCurrencies(
            Option(message.payload.filterCurrencyKeys).map(_.asScala.toSet),
            message.payload.getVerified,
            message.payload.getUnverified)
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
