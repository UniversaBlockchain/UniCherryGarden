package com.myodov.unicherrygarden

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
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

  trait CherryPlanterMessage

  /** A message informing you need to run a next iteration */
  final case class Iterate() extends CherryPlanterMessage

  def apply(pgStorage: PostgreSQLStorage,
            ethereumConnector: EthereumRpcSingleConnector): Behavior[CherryPlanterMessage] = {

    val planter = new CherryPlanter(pgStorage, ethereumConnector)

    Behaviors.receiveMessage {
      (message: CherryPlanterMessage) => {
        logger.debug(s"Receiving CherryPlanter message: $message")
        Behaviors.same
      }
    }
  }
}
