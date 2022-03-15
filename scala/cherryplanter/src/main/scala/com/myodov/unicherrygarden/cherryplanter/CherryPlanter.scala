package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.myodov.unicherrygarden.api.DBStorageAPI
import com.myodov.unicherrygarden.api.GardenMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.messages.CherryPlanterRequest
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction.PlantTransactionRequestResultPayload
import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

/** "Cherry planter": the "CherryGarden" subsystem to create and inject new Ethereum transactions
 * into the Ethereum blockchain. */
class CherryPlanter(
                     // CherryGardenComponent-specific
                     realm: String,
                     dbStorage: DBStorageAPI,
                     // CherryPlanter-specific
                     protected[this] val ethereumConnector: AbstractEthereumNodeConnector with Web3WriteOperations,
                     protected[this] val state: CherryPlanter.State = CherryPlanter.State()
                   )
  extends CherryGardenComponent(realm, dbStorage) with LazyLogging {

  import CherryPlanter.{logger => _, _}

  private def launch(): Behavior[CherryPlanterRequest] =
    Behaviors.setup { context: ActorContext[CherryPlanterRequest] =>
      context.log.info(
        s"Launching CherryPlanter in realm \"$realm\": " +
          s"v. $propVersionStr, built at $propBuildTimestampStr")

      context.system.receptionist ! Receptionist.Register(PlantTransaction.makeServiceKey(realm), context.self)

      Behaviors.receiveMessage {
        case message@EthereumNodeStatus(status) =>
          logger.debug(s"CherryPlanter received Ethereum node syncing status: $message")
          // Store the status message for our own usage
          state.ethereumStatus = Some(status)
          Behaviors.same
        case message: PlantTransaction.Request =>
          context.log.debug(s"Received PlantTransaction($message) command")

          val plantKey = 42

          val response = new PlantTransaction.Response(
            CherryGardenComponent.whenStateAndProgressAllow[PlantTransactionRequestResultPayload](
              state.ethereumStatus,
              dbStorage.progress.getProgress,
              "PlantTransaction",
              null
            ) { (ethereumNodeStatus, progress) =>
              new PlantTransactionRequestResultPayload(plantKey)
            }
          )
          context.log.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        case message: CherryPlanterRequest =>
          logger.debug(s"Receiving CherryPlanter message: $message")
          Behaviors.same
      }
    }
}


/** Akka actor to run CherryPlanter operations. */
object CherryPlanter extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherryplanter.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  protected final case class State(@volatile var ethereumStatus: Option[SystemStatus.Blockchain] = None)

  /** Main constructor. */
  @inline final def apply(realm: String,
                          dbStorage: DBStorageAPI,
                          ethereumConnector: AbstractEthereumNodeConnector with Web3WriteOperations
                         ): Behavior[CherryPlanterRequest] =
    new CherryPlanter(
      realm,
      dbStorage,
      ethereumConnector,
      state = CherryPlanter.State()
    ).launch()
}
