package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector
import com.myodov.unicherrygarden.messages.CherryPlanterRequest
import com.myodov.unicherrygarden.storages.api.DBStorageAPI
import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

/** "Cherry planter": the "CherryGarden" subsystem to create and inject new Ethereum transactions
 * into the Ethereum blockchain. */
class CherryPlanter(private val dbStorage: DBStorageAPI,
                    private val ethereumConnector: AbstractEthereumNodeConnector) extends LazyLogging {
}


/** Akka actor to run CherryPlanter operations. */
object CherryPlanter extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherryplanter.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  /** A message informing you need to run a next iteration */
  final case class Iterate() extends CherryPlanterRequest

  def apply(dbStorage: DBStorageAPI,
            ethereumConnector: AbstractEthereumNodeConnector): Behavior[CherryPlanterRequest] = {

    val planter = new CherryPlanter(dbStorage, ethereumConnector)

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
