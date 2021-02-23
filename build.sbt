// Versions

val commonsIoVersion = "2.6"
val configLibVersion = "1.4.1"
val scoptVersion = "4.0.0"
val h2DatabaseVersion = "1.4.199"
val flywayDbVersion = "7.5.3"
val logbackVersion = "1.2.3"
val postgresqlVersion = "42.2.5"
val scalaLoggingVersion = "3.9.2"
val scalaTestVersion = "3.0.8"
val scalikeJdbcVersion = "3.3.5"
val syslogAppenderVersion = "1.0.0"
val web3jVersion = "4.8.4"

// Common settings

lazy val commonSettings = Seq(
  organization := "com.myodov",
  maintainer := "Alex Myodov <amyodov@gmail.com>",
  version := "0.1.2",
  scalaVersion := "2.13.0",
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  ),
)

// Common modules

lazy val api = (project in file("api"))
  .settings(
    commonSettings,
    name := "unicherrypicker__api",
  )

// Separate module to handle reading the HOCON conf files.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val confreader = (project in file("confreader"))
  .settings(
    commonSettings,
    name := "unicherrypicker__confreader",
    libraryDependencies ++= Seq(
      // Parse HOCON config files
      "com.typesafe" % "config" % configLibVersion,
    ),
  )

// Separate module to handle logging configuration.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val logging = (project in file("logging"))
  .settings(
    commonSettings,
    name := "unicherrypicker__logging",
    libraryDependencies ++= Seq(
      // Default logging output
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Support for logging output to syslog
      "com.github.serioussam" % "syslogappender" % syslogAppenderVersion,
    ),
  )

lazy val cherrypicker = (project in file("cherrypicker"))
  .settings(
    commonSettings,
    name := "unicherrypicker__picker",
  )
  .dependsOn(api)

// Storages

lazy val db_postgresql_storage = (project in file("storages/db_postgresql_storage"))
  .settings(
    commonSettings,
    name := "unicherrypicker__db_postgresql_storage",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "org.flywaydb" % "flyway-core" % flywayDbVersion,
    ),
  )
  .dependsOn(api)

lazy val ethereum_rpc_connector = (project in file("connectors/ethereum_rpc_connector"))
  .settings(
    commonSettings,
    name := "unicherrypicker__ethereum_rpc_connector",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
    ),
  )
  .dependsOn(api, confreader % "test->compile", logging % "test->compile")

// Launcher

lazy val launcher = (project in file("launcher"))
  .settings(
    commonSettings,
    name := "unicherrypicker",
    resolvers += Resolver.bintrayRepo("serioussam", "oss"),
    libraryDependencies ++= Seq(
      // Parse command line arguments
      "com.github.scopt" %% "scopt" % scoptVersion,
    ),
    // Define the launch option for sbt-native-packager
    //    mainClass in Compile := Some("com.myodov.unicherrypicker.launcher.Launcher"),
    //    discoveredMainClasses in Compile := Seq(),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api, confreader, logging, cherrypicker, db_postgresql_storage, ethereum_rpc_connector)
