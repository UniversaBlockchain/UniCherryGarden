package com.myodov.unicherrygarden.launcher

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.typed.{Cluster, Subscribe}
import com.myodov.unicherrygarden.cherrygardener.messages.{CherryGardenerRequest, CherryGardenerResponse}
import com.myodov.unicherrygarden.connectors.EthereumRpcSingleConnector
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.myodov.unicherrygarden._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scopt.OParser

import scala.jdk.CollectionConverters._

object CLIMode extends Enumeration {
  type CLIMode = Value
  val Init, Wipe, LaunchGardenWatcher, LaunchGardener = Value
}

/** The primary launcher for '''UniCherryGarden''' (and all its major subsystems). */
object LauncherApp extends App with LazyLogging {
  lazy val config = ConfigFactory.load()

  /** Command line arguments. */
  case class CLIConfig(mode: CLIMode.CLIMode = null)

  def handleCLI(args: Array[String]): Option[CLIConfig] = {
    val builder = OParser.builder[CLIConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("UniCherryGarden"),
        head("UniCherryGarden", "0.0.5"),
        help('h', "help").text("print help"),
        version('v', "version").text("print version"),
        cmd("init").text("Initializes UniCherryGarden database.")
          .action((_, c) => c.copy(mode = CLIMode.Init)),
        cmd("wipe").text("Initializes UniCherryGarden database, wiping it in the process (DO NOT EVER USE IN PRODUCTION!)")
          .action((_, c) => c.copy(mode = CLIMode.Wipe)),
        cmd("launch-gardenwatcher").text("Launches GardenWatcher watchdog.")
          .action((_, c) => c.copy(mode = CLIMode.LaunchGardenWatcher)),
        cmd("launch-gardener").text("Launches CherryGardener (including CherryPicker and CherryPlanter) processes altogether.")
          .action((_, c) => c.copy(mode = CLIMode.LaunchGardener)),
        note(
          """
You can choose a different HOCON configuration file instead of the regular application.conf using the standard approach, passing the following argument to VM:
-Dconfig.file=path/to/config-file""")
      )
    }
    OParser.parse(parser, args, CLIConfig())
  }

  private[this] def getPgStorage(wipe: Boolean): PostgreSQLStorage = {
    val jdbcUrl = config.getString("db.jdbc_url")
    val dbUser = config.getString("db.user")
    val dbPassword = config.getString("db.password")
    val dbMigrations = config.getStringList("db.migrations").asScala.toList
    PostgreSQLStorage(jdbcUrl, dbUser, dbPassword, wipe, dbMigrations)
  }

  private[this] def getEthereumConnector(): EthereumRpcSingleConnector = {
    val nodeUrls = config.getStringList("ethereum.rpc_servers")
    if (nodeUrls.size > 1) {
      logger.warn(s"There are ${nodeUrls.size} Ethereum node URLs listed; only 1 is supported yet")
    } else if (nodeUrls.size == 0) {
      logger.error("No Ethereum nodes listed! Cannot proceed")
      throw new RuntimeException("No Ethereum nodes")
    }

    val nodeUrl = nodeUrls.get(0)
    logger.debug(s"Using Ethereum node at $nodeUrl")
    EthereumRpcSingleConnector(nodeUrl)
  }

  def init(wipe: Boolean): Unit = {
    //    parseConfFile(confFile)
    logger.info("Done!\nMultiline launch message.\nInitializing...")
    val rpcServers = config.getList("ethereum.rpc_servers")

    val pgStorage = getPgStorage(wipe)
    //    implicit val dbSession: DBSession = pgStorage.makeSession

    pgStorage.state.setSyncState("Launched, using SQL")
  }

  lazy val actorSystem: ActorSystem[LauncherActor.LaunchComponent] =
    ActorSystem(LauncherActor(), "CherryGarden")

  /** Launches the GardenWatcher watchdog (usually executed in a standalone process). */
  private[this] def launchWatcher(): Unit = {
    actorSystem ! LauncherActor.LaunchGardenWatcher()
  }

  /** Launches the CherryGardener (and, inside it, CherryPicker and CherryPlanter) Akka actors
   * (which are usually launcher together at the moment). */
  private[this] def launchGardener(): Unit = {
    val pgStorage = getPgStorage(wipe = false)
    val ethereumConnector = getEthereumConnector

    //    actorSystem ! LauncherActor.LaunchCherryPicker(pgStorage, ethereumConnector)
    //    actorSystem ! LauncherActor.LaunchCherryPlanter(pgStorage, ethereumConnector)
    actorSystem ! LauncherActor.LaunchCherryGardener(pgStorage, ethereumConnector)
  }

  private[this] def mainLaunch(args: Array[String]): Unit = {
    LoggingConfigurator.configure()

    handleCLI(args) match {
      case Some(cliConfig) =>
        // Config parsed successfully
        cliConfig.mode match {
          case CLIMode.Init => init(wipe = false)
          case CLIMode.Wipe => init(wipe = true)
          case CLIMode.LaunchGardenWatcher => launchWatcher
          case CLIMode.LaunchGardener => launchGardener
          case unhandledMode => println(s"Unhandled mode $unhandledMode!")
        }
      case _ =>
      // Wrong arguments; error has been displayed already
    }
  }

  // Main
  mainLaunch(args)
}

/** The Akka guardian actor that launches all necessary components of CherryGarden. */
object LauncherActor extends LazyLogging {
  lazy val props = UnicherrygardenVersion.loadPropsFromNamedResource("unicherrygarden_launcher.properties")
  lazy val propVersionStr = props.getProperty("version", "N/A");
  lazy val propBuildTimestampStr = props.getProperty("build_timestamp", "");

  trait Message {}

  trait LaunchComponent extends Message {}

  /** Messages supported by this actor. */
  //  final case class LaunchCherryPicker(pgStorage: PostgreSQLStorage,
  //                                      ethereumConnector: EthereumRpcSingleConnector) extends LaunchComponent
  //
  //  final case class LaunchCherryPlanter(pgStorage: PostgreSQLStorage,
  //                                       ethereumConnector: EthereumRpcSingleConnector) extends LaunchComponent

  /** Akka message to launch GardenWatcher. */
  final case class LaunchGardenWatcher() extends LaunchComponent

  /** Akka message to launch CherryGardener. */
  final case class LaunchCherryGardener(pgStorage: PostgreSQLStorage,
                                        ethereumConnector: EthereumRpcSingleConnector) extends LaunchComponent

  def apply(): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        //        case LauncherActor.LaunchCherryPicker(pgStorage, ethereumConnector) => {
        //          logger.info(s"Launching CherryPicker ($pgStorage, $ethereumConnector)")
        //          val cherryPicker: ActorRef[CherryPicker.Iterate] = context.spawn(CherryPicker(pgStorage, ethereumConnector), "CherryPicker")
        //        }
        //        case LauncherActor.LaunchCherryPlanter(pgStorage, ethereumConnector) => {
        //          logger.info(s"Launching CherryPlanter")
        //          val cherryPlanter: ActorRef[CherryPlanter.Iterate] = context.spawn(CherryPlanter(pgStorage, ethereumConnector), "CherryPlanter")
        //        }
        case LauncherActor.LaunchGardenWatcher() => {
          logger.info(s"Launching GardenWatcher (pure launcher, no actor): launcher v. $propVersionStr, built at $propBuildTimestampStr")
        }

        case LauncherActor.LaunchCherryGardener(pgStorage, ethereumConnector) => {
          logger.debug(s"Launching sub-actor CherryPicker ($pgStorage, $ethereumConnector)")
          val cherryPicker: ActorRef[CherryPicker.CherryPickerMessage] =
            context.spawn(CherryPicker(pgStorage, ethereumConnector), "CherryPicker")

          logger.debug(s"Launching sub-actor CherryPlanter")
          val cherryPlanter: ActorRef[CherryPlanter.CherryPlanterMessage] =
            context.spawn(CherryPlanter(pgStorage, ethereumConnector), "CherryPlanter")

          logger.info(s"Launched CherryGardener (which now knows about CherryPicker and CherryPlanter)")
          val cherryGardener: ActorRef[CherryGardenerRequest] =
            context.spawn(
              CherryGardener(pgStorage, ethereumConnector, Option(cherryPicker), Option(cherryPlanter)),
              "CherryGardener")
          //          cherryGardener ! new Balances.GetBalance(context.self, "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")

          val clusterSubscriber = context.spawn(ClusterSubscriber(), "ClusterSubscriber")

          val cluster = Cluster(context.system)
          cluster.subscriptions ! Subscribe(clusterSubscriber: ActorRef[MemberEvent], classOf[MemberEvent])
        }
        // CherryGardenerResponse and further are Java interfaces, so we cannot use convenient
        // unapply-based pattern matching.
        case response: CherryGardenerResponse => {
          response match {
            //            case getBalanceResp: Balances.GetBalanceResp => {
            //              logger.debug(s"Received GetBalanceResponse: $getBalanceResp")
            //            }
            case unknownResponse =>
              logger.error(s"Unexpected CherryGardener response: $unknownResponse")
          }
        }
        case unexpectedComponent => {
          logger.error(s"Unexpected component to launch: $unexpectedComponent")
        }
      }

      Behaviors.same
    }
}

/** Tiny Akka actor to notice and log all the changes in Akka cluster subscriptions. */
object ClusterSubscriber extends LazyLogging {
  def apply(): Behavior[MemberEvent] = Behaviors.receiveMessage {
    (message: MemberEvent) => {
      logger.info(s"Cluster subscription change: $message")
      Behaviors.same
    }
  }
}
