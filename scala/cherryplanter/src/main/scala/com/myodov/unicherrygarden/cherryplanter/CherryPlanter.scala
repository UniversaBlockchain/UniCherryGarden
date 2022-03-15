package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.myodov.unicherrygarden.api.DBStorageAPI
import com.myodov.unicherrygarden.api.GardenMessages.EthereumNodeStatus
import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.api.types.responseresult.FailurePayload
import com.myodov.unicherrygarden.messages.CherryPlanterRequest
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction
import com.myodov.unicherrygarden.messages.cherryplanter.PlantTransaction.{PlantTransactionRequestResultFailure, PlantTransactionRequestResultPayload}
import com.typesafe.scalalogging.LazyLogging
import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import scalikejdbc.DB

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
          // TODO: probably, should happen in a child Actor, similarly to `requestHandlerActor`
          val response = handlePlantTransaction(message.payload)
          context.log.debug(s"Replying with $response")
          message.replyTo ! response
          Behaviors.same
        case message: CherryPlanterRequest =>
          logger.debug(s"Receiving CherryPlanter message: $message")
          Behaviors.same
      }
    }

  /** Reply to [[PlantTransaction]] request. */
  private[this] def handlePlantTransaction(payload: PlantTransaction.PTRequestPayload): PlantTransaction.Response =
  // Construct all the response in a single atomic readonly DB transaction
    DB readOnly { implicit session =>

      CherryGardenComponent.whenStateAndProgressAllow[PlantTransaction.Response](
        state.ethereumStatus,
        dbStorage.progress.getProgress,
        "PlantTransaction",
        PlantTransaction.Response.fromCommonFailure(FailurePayload.CHERRY_GARDEN_NOT_READY)
      ) { (ethereumNodeStatus, progress) =>
        // Real use-case handling

        val expectedTxhash = Numeric.toHexString(Hash.sha3(payload.bytes))
        logger.debug(s"Planting tx $expectedTxhash with bytes ${payload.bytes}")

        // 1. Add the record to the DB.
        // Switch into read-write transaction for this.
        val (newlyPlanted, plantKey) = DB localTx { implicit session =>
          dbStorage.plants.addTransactionToPlant(expectedTxhash, payload.bytes)
        }

        // 2. Try planting it into blockchain
        ethereumConnector.ethSendRawTransaction(payload.bytes) match {
          case Left(errorMessage) =>
            // 3.1. Error happened: update the planting status after the first attempt.
            // Switch into read-write transaction for this.
            DB localTx { implicit session =>
              logger.debug(s"Failed to plant: $errorMessage")
            }
            new PlantTransaction.Response(
              new PlantTransactionRequestResultFailure(errorMessage)
            )
          case Right(txhash) =>
            // 3.2. Successful attempt to plant: update the planting status after the first attempt.
            // Switch into read-write transaction for this.
            if (txhash != expectedTxhash) {
              logger.warn(s"When planting, expected txhash \"$expectedTxhash\" but result was \"$txhash\"")
            }

            DB localTx { implicit session =>
              logger.debug(s"Planting attempt success for $txhash");
            }

            new PlantTransaction.Response(
              new PlantTransactionRequestResultPayload(newlyPlanted, plantKey)
            )
        }
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
