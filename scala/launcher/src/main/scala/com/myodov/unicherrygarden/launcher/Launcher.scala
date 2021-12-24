package com.myodov.unicherrygarden.launcher

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.typed.{Cluster, Subscribe}
import com.myodov.unicherrygarden.cherrygardener.CherryGardener
import com.myodov.unicherrygarden.connectors.{AbstractEthereumNodeConnector, EthereumSingleNodeGraphQLConnector, Web3ReadOperations}
import com.myodov.unicherrygarden.messages.{CherryGardenerRequest, CherryGardenerResponse, CherryPickerRequest, CherryPlanterRequest}
import com.myodov.unicherrygarden.storages.PostgreSQLStorage
import com.myodov.unicherrygarden.{CherryPicker, CherryPlanter, LoggingConfigurator, UnicherrygardenVersion}
import com.typesafe.config.{ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import scopt.OParser

import scala.jdk.CollectionConverters._

object CLIMode extends Enumeration {
  type CLIMode = Value
  val Init, Wipe, LaunchGardenWatcher, LaunchGardener, LaunchGardenerGardenWatcher = Value
}

/** The primary launcher for '''UniCherryGarden''' (and all its major subsystems). */
object LauncherApp extends App with LazyLogging {
  lazy val config = ConfigFactory.load()

  /** Command line arguments. */
  sealed case class CLIConfig(mode: CLIMode.CLIMode = null)

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
        cmd("wipe").text(
          "Initializes UniCherryGarden database, wiping it in the process " +
            "(DO NOT EVER USE IN PRODUCTION!)")
          .action((_, c) => c.copy(mode = CLIMode.Wipe)),
        cmd("launch-gardenwatcher").text("Launches GardenWatcher watchdog.")
          .action((_, c) => c.copy(mode = CLIMode.LaunchGardenWatcher)),
        cmd("launch-gardener").text(
          "Launches CherryGardener (including CherryPicker and CherryPlanter) " +
            "processes altogether.")
          .action((_, c) => c.copy(mode = CLIMode.LaunchGardener)),
        cmd("launch-gardener-gardenwatcher").text(
          "Launches CherryGardener (including CherryPicker and CherryPlanter) " +
            "and GardenWatcher processes altogether. Should not be used in production!")
          .action((_, c) => c.copy(mode = CLIMode.LaunchGardenerGardenWatcher)),
        note(
          """
You can choose a different HOCON configuration file instead of the regular application.conf using the standard approach, passing the following argument to VM:
-Dconfig.file=path/to/config-file""")
      )
    }
    OParser.parse(parser, args, CLIConfig())
  }

  private[this] def getDbStorage(wipe: Boolean): PostgreSQLStorage = {
    val jdbcUrl = config.getString("db.jdbc_url")
    val dbUser = config.getString("db.user")
    val dbPassword = config.getString("db.password")
    // This setting contains the optional database migrations, but may be omitted
    val dbMigrations: List[String] = try {
      config.getStringList("db.migrations").asScala.toList
    } catch {
      case _: ConfigException.Missing => List.empty[String]
    }
    PostgreSQLStorage(jdbcUrl, dbUser, dbPassword, wipe, dbMigrations)
  }

  private[this] def getEthereumConnector(): AbstractEthereumNodeConnector with Web3ReadOperations = {
    val nodeUrls = config.getStringList("ethereum.rpc_servers")
    if (nodeUrls.size > 1) {
      logger.warn(s"There are ${nodeUrls.size} Ethereum node URLs listed; only 1 is supported yet")
    } else if (nodeUrls.size == 0) {
      logger.error("No Ethereum nodes listed! Cannot proceed")
      throw new RuntimeException("No Ethereum nodes")
    }

    val nodeUrl = nodeUrls.get(0)
    logger.debug(s"Using Ethereum node at $nodeUrl")
    EthereumSingleNodeGraphQLConnector(nodeUrl, actorSystem)
    //    EthereumSingleNodeJsonRpcConnector(nodeUrl)
  }

  def init(wipe: Boolean): Unit = {
    logger.info("Done!\nMultiline launch message.\nInitializing...")
    val rpcServers = config.getList("ethereum.rpc_servers")

    val dbStorage = getDbStorage(wipe)

    dbStorage.State.setSyncState("Launched, using SQL")
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
    actorSystem ! LauncherActor.LaunchCherryGardener(getDbStorage(wipe = false), getEthereumConnector)
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
          case CLIMode.LaunchGardenerGardenWatcher => {
            launchWatcher
            launchGardener
          }
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

  /** Akka message to launch GardenWatcher. */
  final case class LaunchGardenWatcher() extends LaunchComponent

  /** Akka message to launch CherryGardener. */
  final case class LaunchCherryGardener(dbStorage: PostgreSQLStorage,
                                        ethereumConnector: AbstractEthereumNodeConnector with Web3ReadOperations) extends LaunchComponent

  def apply(): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case LauncherActor.LaunchGardenWatcher() =>
          logger.info(s"Launching GardenWatcher (pure launcher, no actor): launcher v. $propVersionStr, built at $propBuildTimestampStr")
        case LauncherActor.LaunchCherryGardener(dbStorage, ethereumConnector) =>
          logger.debug(s"Launching sub-actor CherryPicker ($dbStorage, $ethereumConnector)")
          val cherryPicker: ActorRef[CherryPickerRequest] =
            context.spawn(CherryPicker(dbStorage, ethereumConnector), "CherryPicker")

          logger.debug(s"Launching sub-actor CherryPlanter")
          val cherryPlanter: ActorRef[CherryPlanterRequest] =
            context.spawn(CherryPlanter(dbStorage, ethereumConnector), "CherryPlanter")

          logger.info(s"Launched CherryGardener (which now knows about CherryPicker and CherryPlanter)")
          val cherryGardener: ActorRef[CherryGardenerRequest] =
            context.spawn(
              CherryGardener(dbStorage, ethereumConnector, Some(cherryPicker), Some(cherryPlanter)),
              "CherryGardener")
          //          cherryGardener ! new Balances.GetBalance(context.self, "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")

          val clusterSubscriber = context.spawn(ClusterSubscriber(), "ClusterSubscriber")

          val cluster = Cluster(context.system)
          cluster.subscriptions ! Subscribe(clusterSubscriber: ActorRef[MemberEvent], classOf[MemberEvent])

        // CherryGardenerResponse and further are Java interfaces, so we cannot use convenient
        // unapply-based pattern matching.
        case response: CherryGardenerResponse =>
          response match {
            //            case getBalanceResp: Balances.GetBalanceResp => {
            //              logger.debug(s"Received GetBalanceResponse: $getBalanceResp")
            //            }
            case unknownResponse =>
              logger.error(s"Unexpected CherryGardener response: $unknownResponse")
          }
        case unexpectedComponent =>
          logger.error(s"Unexpected component to launch: $unexpectedComponent")
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
