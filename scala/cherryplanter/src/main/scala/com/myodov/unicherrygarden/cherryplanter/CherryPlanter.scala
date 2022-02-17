package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.myodov.unicherrygarden.api.DBStorageAPI
import com.myodov.unicherrygarden.messages.CherryPlanterRequest
import com.typesafe.scalalogging.LazyLogging

import scala.language.postfixOps

/** "Cherry planter": the "CherryGarden" subsystem to create and inject new Ethereum transactions
 * into the Ethereum blockchain. */
class CherryPlanter(
                     // CherryGardenComponent-specific
                     realm: String,
                     dbStorage: DBStorageAPI,
                     // CherryPlanter-specific
                     private val ethereumConnector: AbstractEthereumNodeConnector)
  extends CherryGardenComponent(realm, dbStorage) with LazyLogging {
}


/** Akka actor to run CherryPlanter operations. */
object CherryPlanter extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_cherryplanter.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  /** A message informing you need to run a next iteration */
  final case class Iterate() extends CherryPlanterRequest

  /** Main constructor. */
  @inline final def apply(realm: String,
                          dbStorage: DBStorageAPI,
                          ethereumConnector: AbstractEthereumNodeConnector): Behavior[CherryPlanterRequest] = {
    val planter = new CherryPlanter(realm, dbStorage, ethereumConnector)

    Behaviors.setup { context =>
      logger.info(s"Launching CherryPlanter in realm \"$realm\": v. $propVersionStr, built at $propBuildTimestampStr")

      Behaviors.receiveMessage {
        (message: CherryPlanterRequest) => {
          logger.debug(s"Receiving CherryPlanter message: $message")
          Behaviors.same
        }
      }
    }
  }
}
