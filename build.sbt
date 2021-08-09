import sbt.Compile
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

sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeCredentialHost := "s01.oss.sonatype.org"
publishTo := sonatypePublishToBundle.value

val projectVersion = "0.1.1"
name := "unicherrygarden"
version := projectVersion

// Configure settings for all (!) subprojects
inThisBuild(List(
  organization := "com.myodov.unicherrygarden",
  version := projectVersion,
  scalaVersion := "2.13.0",
  organizationName := "UniCherryGarden",
  organizationHomepage := Some(url("https://github.com/UniversaBlockchain/UniCherryGarden")),
  homepage := Some(url("https://github.com/UniversaBlockchain/UniCherryGarden")),
  startYear := Some(2021),
  description := "UniCherryGarden: universal “data gardening” solution for Ethereum blockchain data",
  scmInfo := Some(ScmInfo(
    url("https://github.com/UniversaBlockchain/UniCherryGarden"),
    "scm:git@github.com:UniversaBlockchain/UniCherryGarden.git"
  )),
  licenses := Seq("MIT License" -> url("https://github.com/UniversaBlockchain/UniCherryGarden/blob/master/LICENSE")),
  developers := List(
    Developer(
      id = "amyodov",
      name = "Alex Myodov",
      email = "amyodov@gmail.com",
      url = url("https://myodov.com/")
    )
  )
))

usePgpKeyHex("BE53ACD082329B6231C5D4F41B6C3A2684CA4538")


lazy val commonSettings = Seq(
  // We don't want Javadoc generated, as it issues too many problems with “wrong” Javadoc tags;
  // and also causes many warnings for bad Javadoc.
  sources in(Compile, doc) := Seq.empty,
  publishTo := sonatypePublishToBundle.value,
)

lazy val commonJavaSettings = Seq(
  libraryDependencies ++= Seq(
    // For logging
    "org.slf4j" % "slf4j-api" % javaSlf4jVersion,
    // For Unit tests
    "junit" % "junit" % javaJunitVersion % Test,
    // So the test scripts will have its data logged to STDOUT
    "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
    // Type annotations
    "org.checkerframework" % "checker-qual" % javaCheckerVersion,
  ),
  // Java-only artifacts shouldn't have the Scala-version postfix in their names.
  crossPaths := false,
)

lazy val commonScalaSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    // So the test scripts will have its data logged to STDOUT
    "ch.qos.logback" % "logback-classic" % logbackVersion % Test,
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

// (Java-based) a virtual “project” that contains all settings/data common for all Java projects.
lazy val commonJava = (project in file("java/common_java"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "common_java",
  )

// (Java-based) Low-level Akka interoperability layer (messages) to communicate
// between CherryGarden Akka remote actors and Client Connector.
lazy val ethUtils = (project in file("java/ethutils"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "ethutils",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
    ),
  )
  .dependsOn(commonJava)

// (Java-based) Low-level Akka interoperability layer (messages) to communicate
// between CherryGarden Akka remote actors and Client Connector.
lazy val cherryGardenerInterop = (project in file("java/cherrygardener_interop"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "cherrygardener_interop",
    libraryDependencies ++= Seq(
      // Akka
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      //      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test, // or should not use "% Test"?
    ),
  )
  .dependsOn(commonJava, ethUtils)

// (Java-based) Client connector (API library) to CherryGardener;
// allows external clients to use high-level CherryGardener interface
// to Ethereum blockchain.
lazy val cherryGardenerConnector = (project in file("java/cherrygardener_connector"))
  .settings(
    commonSettings,
    commonJavaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrygardener_connector",
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
  .dependsOn(commonJava, cherryGardenerInterop)

lazy val cherryGardenerConnectorCLI = (project in file("java/cherrygardener_connector_cli"))
  .settings(
    commonSettings,
    commonJavaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrygardener_connector_cli",
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % javaCommonsCLIVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    ),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(commonJava, cherryGardenerConnector)


//
// Scala components
//

// a virtual “project” that contains all settings/data common for all Scala projects.
lazy val commonScala = (project in file("scala/common_scala"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "common_scala",
  )

// Common modules (for all Scala components).
lazy val api = (project in file("scala/api"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "api",
  )
  .dependsOn(commonScala, ethUtils)

// Separate module to handle reading the HOCON conf files.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val confreader = (project in file("scala/confreader"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "confreader",
    libraryDependencies ++= Seq(
      // Parse HOCON config files
      "com.typesafe" % "config" % configLibVersion,
    ),
  )
  .dependsOn(commonScala)

// Separate module to handle logging configuration.
// Used from CLI launcher, and from "test" targets of other modules.
lazy val logging = (project in file("scala/logging"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "logging",
    libraryDependencies ++= Seq(
      // Default logging output
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Support for logging output to syslog - disabled for now, as there is no publicly hosted artifact
      //      "com.github.serioussam" % "syslogappender" % syslogAppenderVersion,
    ),
  )
  .dependsOn(commonScala)

// RDBMS Storage
lazy val db_postgresql_storage = (project in file("scala/storages/db_postgresql_storage"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "db_postgresql_storage",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "org.flywaydb" % "flyway-core" % flywayDbVersion,
      // Convenient Scala-like interface to SQL queries
      "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
      "org.scalikejdbc" %% "scalikejdbc-test" % scalikeJdbcVersion % "test",
    ),
  )
  .dependsOn(commonScala, api)

// Interface to Web3 Ethereum nodes via RPC.
lazy val ethereum_rpc_connector = (project in file("scala/connectors/ethereum_rpc_connector"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "ethereum_rpc_connector",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
      "org.web3j" % "contracts" % web3jVersion,
    ),
  )
  .dependsOn(commonScala, api, confreader % "test->compile", logging % "test->compile")

// CherryPicker: investigates the Ethereum blockchain and cherry-picks
// the information about the Ether/ERC20 transfers.
lazy val cherrypicker = (project in file("scala/cherrypicker"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrypicker",
  )
  .dependsOn(commonScala, api, db_postgresql_storage, ethereum_rpc_connector)

// CherryPlanter: sends the transactions to the Ethereum blockchain.
lazy val cherryplanter = (project in file("scala/cherryplanter"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherryplanter",
  )
  .dependsOn(commonScala, api, db_postgresql_storage, ethereum_rpc_connector)

// CherryGardener: provides high-level convenient front-end to the .
lazy val cherrygardener = (project in file("scala/cherrygardener"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrygardener",
  )
  .dependsOn(
    commonScala, api,
    db_postgresql_storage, ethereum_rpc_connector,
    cherryGardenerInterop, cherrypicker, cherryplanter
  )

// Launcher; launches the CherryPicker/CherryPlanter/CherryGardener daemons.
lazy val launcher = (project in file("scala/launcher"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "launcher",
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
    commonScala, api, confreader, db_postgresql_storage, logging,
    cherrypicker, cherryplanter, cherrygardener
  )

publishArtifact := false
