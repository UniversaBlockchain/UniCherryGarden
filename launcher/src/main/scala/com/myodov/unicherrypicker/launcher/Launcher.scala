package com.myodov.unicherrypicker.launcher

import com.myodov.unicherrypicker.storages.PostgreSQLStorage
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import scopt.OParser

object CLIMode extends Enumeration {
  type CLIMode = Value
  val Init, Launch = Value
}

/** The primary launcher for '''UniCherrypicker'''. */
object Launcher {
  lazy val logger = Logger[Launcher.type]

  lazy val config = ConfigFactory.load()

  implicit val weekDaysRead: scopt.Read[CLIMode.Value] =
    scopt.Read.reads(CLIMode withName _)

  /** Command line arguments. */
  case class CLIConfig(mode: CLIMode.CLIMode = null)

  def handleCLI(args: Array[String]): Option[CLIConfig] = {
    val builder = OParser.builder[CLIConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("UniCherrypicker"),
        head("UniCherrypicker", "0.0.5"),
        help('h', "help").text("print help"),
        version('v', "version").text("print version"),
        // option -c, --conf
//        opt[File]('c', "conf")
//          .required()
//          .action((x: File, c: CLIConfig) => c.copy(confFile = x))
//          .text("UniCherrypicker configuration file (in HOCON format)"),
        cmd("init").text("Initializes UniCherrypicker database.")
          .action((_, c) => c.copy(mode = CLIMode.Init)),
        cmd("launch").text("Launches UniCherrypicker process.")
          .action((_, c) => c.copy(mode = CLIMode.Launch)),
        note("""
You can choose a different configuration file using the standard approach, passing the following argument to VM:
-Dconfig.file=path/to/config-file""")
      )
    }
    OParser.parse(parser, args, CLIConfig())
  }

  def init(): Unit = {
    println(s"Running init")
//    parseConfFile(confFile)
    logger.info("Done!\nSecond line of done\nthird line of done")
    val rpcServers = config.getList("ethereum.rpc_servers")

    val jdbcUrl = config.getString("db.jdbc_url")
    val dbUser = config.getString("db.user")
    val dbPassword = config.getString("db.password")
    println(s"Conf RPC servers $rpcServers")
    val pgStorage = PostgreSQLStorage(jdbcUrl, dbUser, dbPassword)
    println("PGStorage created!")
  }

  def launch(): Unit = {
    println(s"Running launch")
    logger.info("Done!\nSecond line of done\nthird line of done")
  }

  def main(args: Array[String]): Unit = {
    LoggingConfigurator.configure()

    handleCLI(args) match {
      case Some(cliConfig) =>
        // Config parsed successfully
        cliConfig.mode match {
          case CLIMode.Init => init
          case CLIMode.Launch => launch
          case unhandledMode => println(s"Unhandled mode $unhandledMode!")
        }
      case _ =>
      // Wrong arguments; error has been displayed already
    }
  }
}
