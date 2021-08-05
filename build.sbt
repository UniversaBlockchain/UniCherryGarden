// Versions

val akkaVersion = "2.6.15"
val commonsIoVersion = "2.6"
val configLibVersion = "1.4.1"
val scoptVersion = "4.0.0"
val h2DatabaseVersion = "1.4.199"
val flywayDbVersion = "7.5.3"
val logbackVersion = "1.2.3"
val postgresqlVersion = "42.2.5"
val scalaLoggingVersion = "3.9.2"
val scalaParallelCollectionsVersion = "1.0.0"
val scalaTestVersion = "3.1.0"
val scalikeJdbcVersion = "3.5.0"
val syslogAppenderVersion = "1.0.0"
val web3jVersion = "4.8.4"
val jacksonCoreVersion = "2.11.4" // same version as used by akka-serialization-jackson

val javaSlf4jVersion = "1.7.30"
val javaJunitVersion = "4.13.2"
val javaCheckerVersion = "3.14.0"
val javaCommonsCLIVersion = "1.4"

// Common settings

lazy val commonSettings = Seq(
  organization := "com.myodov",
  maintainer := "Alex Myodov <amyodov@gmail.com>",
  version := "0.1.2",
  scalaVersion := "2.13.0",
)

lazy val commonJavaSettings = Seq(
  libraryDependencies ++= Seq(
    // For logging
    "org.slf4j" % "slf4j-api" % javaSlf4jVersion,
    // For Unit tests
    "junit" % "junit" % javaJunitVersion % Test,
    // Type annotations
    "org.checkerframework" % "checker-qual" % javaCheckerVersion,
  ),
)

lazy val commonScalaSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.scala-lang.modules" %% "scala-parallel-collections" % scalaParallelCollectionsVersion
  ),
)

lazy val commonAkkaMicroserviceSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    //    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  ),
)


//
// Java components
//


// (Java-based) Low-level Akka interoperability layer (messages) to communicate
// between CherryGarden Akka remote actors and Client Connector.
lazy val ethUtils = (project in file("java/ethutils"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "unicherrygarden__ethutils",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
    ),
  )

// (Java-based) Low-level Akka interoperability layer (messages) to communicate
// between CherryGarden Akka remote actors and Client Connector.
lazy val cherryGardenerInterop = (project in file("java/cherrygardener_interop"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "unicherrygarden__cherrygardener_interop",
    libraryDependencies ++= Seq(
      // Akka
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      //      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test, // or should not use "% Test"?
    ),
  )
  .dependsOn(ethUtils)

// (Java-based) Client connector (API library) to CherryGardener;
// allows external clients to use high-level CherryGardener interface
// to Ethereum blockchain.
lazy val cherryGardenerConnector = (project in file("java/cherrygardener_connector"))
  .settings(
    commonSettings,
    commonJavaSettings,
    commonAkkaMicroserviceSettings,
    name := "unicherrygarden__cherrygardener_connector",
    // Java-based layer is capable to do some Ethereum-specific things (like, build transactions)
    // directly on the connector side.
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
      //      "org.web3j" % "contracts" % web3jVersion,
      // Jackson used to parse JSON structures
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonCoreVersion,
    ),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(cherryGardenerInterop)

lazy val cherryGardenerConnectorCLI = (project in file("java/cherrygardener_connector_cli"))
  .settings(
    commonSettings,
    commonJavaSettings,
    commonAkkaMicroserviceSettings,
    name := "unicherrygarden__cherrygardener_connector_cli",
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % javaCommonsCLIVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    ),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(cherryGardenerConnector)


//
// Scala components
//


// Common modules (for all Scala components).
lazy val api = (project in file("scala/api"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__api",
  )
  .dependsOn(ethUtils)

// Separate module to handle reading the HOCON conf files.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val confreader = (project in file("scala/confreader"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__confreader",
    libraryDependencies ++= Seq(
      // Parse HOCON config files
      "com.typesafe" % "config" % configLibVersion,
    ),
  )

// Separate module to handle logging configuration.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val logging = (project in file("scala/logging"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__logging",
    libraryDependencies ++= Seq(
      // Default logging output
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Support for logging output to syslog - disabled for now, as there is no publicly hosted artifact
      //      "com.github.serioussam" % "syslogappender" % syslogAppenderVersion,
    ),
  )

// RDBMS Storage
lazy val db_postgresql_storage = (project in file("scala/storages/db_postgresql_storage"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__db_postgresql_storage",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "org.flywaydb" % "flyway-core" % flywayDbVersion,
      // Convenient Scala-like interface to SQL queries
      "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikeJdbcVersion % "test",
    ),
  )
  .dependsOn(api)

// Interface to Web3 Ethereum nodes via RPC.
lazy val ethereum_rpc_connector = (project in file("scala/connectors/ethereum_rpc_connector"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__ethereum_rpc_connector",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
      "org.web3j" % "contracts" % web3jVersion,
    ),
  )
  .dependsOn(api, confreader % "test->compile", logging % "test->compile")

// CherryPicker: investigates the Ethereum blockchain and cherry-picks
// the information about the Ether/ERC20 transfers.
lazy val cherrypicker = (project in file("scala/cherrypicker"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "unicherrygarden__cherrypicker",
  )
  .dependsOn(api, db_postgresql_storage, ethereum_rpc_connector)

// CherryPlanter: sends the transactions to the Ethereum blockchain.
lazy val cherryplanter = (project in file("scala/cherryplanter"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "unicherrygarden__cherryplanter",
  )
  .dependsOn(api, db_postgresql_storage, ethereum_rpc_connector)

// CherryGardener: provides high-level convenient front-end to the .
lazy val cherrygardener = (project in file("scala/cherrygardener"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "unicherrygarden__cherrygardener",
  )
  .dependsOn(
    api, db_postgresql_storage, ethereum_rpc_connector,
    cherryGardenerInterop, cherrypicker, cherryplanter
  )

// Launcher; launches the CherryPicker/CherryPlanter/CherryGardener daemons.
lazy val launcher = (project in file("scala/launcher"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "unicherrygarden__launcher",
    resolvers += Resolver.bintrayRepo("serioussam", "oss"),
    libraryDependencies ++= Seq(
      // Parse command line arguments
      "com.github.scopt" %% "scopt" % scoptVersion,
    ),
    // Define the launch option for sbt-native-packager
    //    mainClass in Compile := Some("com.myodov.unicherrygarden.launcher.Launcher"),
    //    discoveredMainClasses in Compile := Seq(),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(
    api, confreader, db_postgresql_storage, logging,
    cherrypicker, cherryplanter, cherrygardener
  )
