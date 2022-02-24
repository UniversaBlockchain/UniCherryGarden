package com.myodov.unicherrygarden.cherrygardener

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.DBStorageAPI
import com.myodov.unicherrygarden.api.GardenMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.messages.cherrygardener.GetCurrencies.CurrenciesRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrygardener.Ping.PingRequestResultPayload
import com.myodov.unicherrygarden.messages.cherrygardener.{GetCurrencies, Ping}
import com.myodov.unicherrygarden.messages.{CherryGardenerRequest, CherryPickerRequest, CherryPlanterRequest}
import com.myodov.unicherrygarden.{CherryGardenComponent, CherryPicker, UnicherrygardenVersion}
import com.typesafe.scalalogging.LazyLogging
import scalikejdbc.DB

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

/** "Cherry Gardener": the high-level interface to Ethereum blockchain. */
class CherryGardener(
                      // CherryGardenComponent-specific
                      realm: String,
                      chainId: Long,
                      dbStorage: DBStorageAPI,
                      // CherryGardener-specific
                      protected[this] val state: CherryGardener.State = CherryGardener.State()
                    ) extends CherryGardenComponent(realm, dbStorage) {

  import CherryPicker.{logger => _, _}

  private def launch(): Behavior[CherryGardenerRequest] = {
    Behaviors.setup { context: ActorContext[CherryGardenerRequest] =>
      context.log.info(
        s"Launching CherryGardener in realm \"$realm\" for Chain ID $chainId: " +
        s"v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(Ping.makeServiceKey(realm), context.self)
      context.system.receptionist ! Receptionist.Register(GetCurrencies.makeServiceKey(realm), context.self)

      Behaviors.receiveMessage {
        case message@EthereumNodeStatus(status) =>
          context.log.debug(s"CherryGardener received Ethereum node syncing status: $message")
          state.ethereumStatus = Some(status)
          Behaviors.same
        case message: Ping.Request => {
          context.log.debug(s"Received Ping($message) command")

          val response = new Ping.Response(
            CherryGardenComponent.whenStateAndProgressAllow[PingRequestResultPayload](
              state.ethereumStatus,
              dbStorage.progress.getProgress,
              "Ping",
              null
            ) { (ethereumNodeStatus, progress) =>
              new PingRequestResultPayload(
                CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
                realm,
                chainId,
                propVersionStr,
                propBuildTimestampStr
              )
            }
          )
          context.log.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        }
        case message: GetCurrencies.Request => {
          context.log.debug(s"Received GetCurrencies($message) command")

          val response = getCurrencies(
            Option(message.payload.filterCurrencyKeys).map(_.asScala.toSet),
            message.payload.getVerified,
            message.payload.getUnverified)
          context.log.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        }
        case unknown => {
          context.log.error(s"Unknown CherryGardener message: $unknown")
          Behaviors.same
        }
      }
    }
  }

  /** Reply to [[GetCurrencies]] request. */
  def getCurrencies(
                     filterCurrencyKeys: Option[Set[String]],
                     getVerified: Boolean,
                     getUnverified: Boolean
                   ): GetCurrencies.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      new GetCurrencies.Response(
        CherryGardenComponent.whenStateAndProgressAllow[CurrenciesRequestResultPayload](
          state.ethereumStatus,
          dbStorage.progress.getProgress,
          "GetCurrencies",
          null
        ) { (ethereumNodeStatus, progress) =>
          // Real use-case handling
          val result: List[Currency] = dbStorage.currencies.getCurrencies(filterCurrencyKeys, getVerified, getUnverified).map(_.asCurrency)
          new CurrenciesRequestResultPayload(
            CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
            result.asJava
          )
        }
      )
    }
}


/** Akka actor to run CherryGardener operations. */
object CherryGardener extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherrygardener.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  protected final case class State(@volatile var ethereumStatus: Option[SystemStatus.Blockchain] = None)

  /** Main constructor. */
  @inline final def apply(realm: String,
                          chainId: Long,
                          dbStorage: DBStorageAPI,
                          cherryPickerOpt: Option[ActorRef[CherryPickerRequest]],
                          cherryPlanterOpt: Option[ActorRef[CherryPlanterRequest]]
                         ): Behavior[CherryGardenerRequest] =
    new CherryGardener(
      realm,
      chainId,
      dbStorage,
      state = CherryGardener.State()
    ).launch()
}
