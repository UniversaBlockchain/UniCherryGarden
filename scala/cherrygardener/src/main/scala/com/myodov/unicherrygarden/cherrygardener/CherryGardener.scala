package com.myodov.unicherrygarden.cherrygardener

import java.util.UUID

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.myodov.unicherrygarden.api.DBStorageAPI
import com.myodov.unicherrygarden.api.GardenMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.api.types.dlt.Currency
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload
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

  private def launch(): Behavior[CherryGardenerRequest] =
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
          val msgName = "Ping"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[Ping.Response](
              msgName,
              message.replyTo,
              msg => Ping.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () => handlePing() },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case message: GetCurrencies.Request => {
          val msgName = "GetCurrencies"
          context.spawn(
            CherryGardenComponent.requestHandlerActor[GetCurrencies.Response](
              msgName,
              message.replyTo,
              msg => GetCurrencies.Response.fromCommonFailure(new FailurePayload.UnspecifiedFailure(msg))
            ) { () =>
              handleGetCurrencies(
                Option(message.payload.filterCurrencyKeys).map(_.asScala.toSet),
                message.payload.getVerified,
                message.payload.getUnverified
              )
            },
            s"$msgName-${UUID.randomUUID}"
          )
          Behaviors.same
        }
        case unknown => {
          context.log.error(s"Unknown CherryGardener message: $unknown")
          Behaviors.same
        }
      }
    }

  /** Reply to [[Ping]] request. */
  private[this] def handlePing(): Ping.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      CherryGardenComponent.whenStateAndProgressAllow[Ping.Response](
        state.ethereumStatus,
        dbStorage.progress.getProgress,
        "Ping",
        Ping.Response.fromCommonFailure(FailurePayload.CHERRY_GARDEN_NOT_READY)
      ) { (ethereumNodeStatus, progress) =>
        new Ping.Response(
          new PingRequestResultPayload(
            CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
            realm,
            chainId,
            propVersionStr,
            propBuildTimestampStr
          )
        )
      }
    }

  /** Reply to [[GetCurrencies]] request. */
  private[this] def handleGetCurrencies(
                                         filterCurrencyKeys: Option[Set[String]],
                                         getVerified: Boolean,
                                         getUnverified: Boolean
                                       ): GetCurrencies.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>
      CherryGardenComponent.whenStateAndProgressAllow[GetCurrencies.Response](
        state.ethereumStatus,
        dbStorage.progress.getProgress,
        "GetCurrencies",
        GetCurrencies.Response.fromCommonFailure(FailurePayload.CHERRY_GARDEN_NOT_READY)
      ) { (ethereumNodeStatus, progress) =>
        // Real use-case handling
        val result: List[Currency] = dbStorage.currencies.getCurrencies(filterCurrencyKeys, getVerified, getUnverified).map(_.asCurrency)
        new GetCurrencies.Response(
          new CurrenciesRequestResultPayload(
            CherryGardenComponent.buildSystemSyncStatus(ethereumNodeStatus, progress),
            result.asJava
          )
        )
      }
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
