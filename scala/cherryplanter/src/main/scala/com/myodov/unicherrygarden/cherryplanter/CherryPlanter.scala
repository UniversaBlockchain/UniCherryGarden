package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.cherrygardener.messages.CherryPlanterRequest
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

/** "Cherry planter": the "CherryGarden" subsystem to create and inject new Ethereum transactions
 * into the Ethereum blockchain. */
class CherryPlanter(private val pgStorage: PostgreSQLStorage,
                    private val ethereumConnector: EthereumRpcSingleConnector) extends LazyLogging {
}


/** Akka actor to run CherryPlanter operations. */
object CherryPlanter extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherryplanter.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  /** A message informing you need to run a next iteration */
  final case class Iterate() extends CherryPlanterRequest

  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector): Behavior[CherryPlanterRequest] = {

    val planter = new CherryPlanter(pgStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryPlanter: v. $propVersionStr, built at $propBuildTimestampStr")

      Behaviors.receiveMessage {
        (message: CherryPlanterRequest) => {
          logger.debug(s"Receiving CherryPlanter message: $message")
          Behaviors.same
        }
      }
    }
  }
}
