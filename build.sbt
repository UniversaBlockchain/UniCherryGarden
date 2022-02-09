import java.time.Instant

import sbt.Compile
// Versions

val akkaVersion = "2.6.17"
val akkaHttpVersion = "10.2.7"
val calibanVersion = "1.3.0"
val commonsIoVersion = "2.6"
val configLibVersion = "1.4.1"
val flywayDbVersion = "7.15.0"
val h2DatabaseVersion = "1.4.199"
val logbackVersion = "1.2.3"
val postgresqlVersion = "42.2.24"
val scalaLoggingVersion = "3.9.2"
val scalaParallelCollectionsVersion = "1.0.0"
val scalaTestVersion = "3.1.0"
val scalikeJdbcVersion = "4.0.0"
val scoptVersion = "4.0.0"
val sttpClient3Version = "3.3.18"
val web3jVersion = "4.8.8"
val jacksonCoreVersion = "2.11.4" // same version as used by akka-serialization-jackson for Scala 2.13

val javaSlf4jVersion = "1.7.30"
val javaJunitVersion = "4.13.2"
val javaCheckerVersion = "3.14.0"
val javaCommonsCLIVersion = "1.4"


// Helper functions

def versionFileTask(filename: String) = Def.task {
  val file = (Compile / resourceManaged).value / "unicherrygarden" / filename
  val contents =
    s"""version=${version.value}
       |build_timestamp=${unicherryGardenBuildTimestamp}"""
      .stripMargin
  IO.write(file, contents)
  Seq(file)
}


// Common settings

sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeCredentialHost := "s01.oss.sonatype.org"
publishTo := sonatypePublishToBundle.value

val projectVersion = "0.9.1"
name := "unicherrygarden"
version := projectVersion

val unicherryGardenBuildTimestamp = Instant.now


// Configure settings for all (!) subprojects

inThisBuild(List(
  organization := "com.myodov.unicherrygarden",
  version := projectVersion,
  scalaVersion := "2.13.8",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-parameters", "-Xlint:unchecked"),
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
  ),
  scalacOptions ++= Seq(
    // display deprecations, warnings and feature warnings on compilations
    "-unchecked", "-deprecation", "-feature")
    ++ Seq("-Xelide-below", sys.props.getOrElse("elide.below", "0"))
    ++ Seq(sys.props.getOrElse("scalacopt", "-opt:l:default")),
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

/** Settings common to any component that uses Akka for microservice/actor architecture. */
lazy val commonAkkaMicroserviceSettings = Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  ),
)

/** Settings common to anything that uses JavaAppPackaging to deploy a binary runnable package. */
lazy val commonJavaAppPackagingSettings = Seq(
  Universal / javaOptions ++= Seq(
    "-Dlog4j2.formatMsgNoLookups=true",
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
    description := "UniCherryGarden: the contents common for all Java-language components",
  )

// (Java-based) Low-level Akka interoperability layer (messages) to communicate
// between CherryGarden Akka remote actors and Client Connector.
lazy val ethUtils = (project in file("java/ethutils"))
  .settings(
    commonSettings,
    commonJavaSettings,
    name := "ethutils",
    description := "UniCherryGarden: Java-based classes and helpers to work with Ethereum blockchain/data structures",
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
    description := "UniCherryGarden: “interop” classes defining the Akka communication messages between Java and Scala code",
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
    description := "UniCherryGarden: primary Java API to access the UniCherryGarden system from Java/Scala/Kotlin code",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
      //      "org.web3j" % "contracts" % web3jVersion,
      // Jackson used to parse JSON structures
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonCoreVersion,
      "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonCoreVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonCoreVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonCoreVersion,
      // Parse HOCON config files
      "com.typesafe" % "config" % configLibVersion,
    ),
    Compile / resourceGenerators += versionFileTask("cherrygardener_connector.properties").taskValue,
  )
  .dependsOn(commonJava, cherryGardenerInterop)


lazy val cherryGardenerConnectorCLI = (project in file("java/cherrygardener_connector_cli"))
  .settings(
    commonSettings,
    commonJavaSettings,
    commonAkkaMicroserviceSettings,
    commonJavaAppPackagingSettings,
    name := "cherrygardener_connector_cli",
    description := "UniCherryGarden: CLI tool to access the CherryGardener Connector features from command line",
    libraryDependencies ++= Seq(
      "commons-cli" % "commons-cli" % javaCommonsCLIVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Parse HOCON config files - to read the CLI config file
      "com.typesafe" % "config" % configLibVersion,
    ),
    Compile / resourceGenerators += versionFileTask("cherrygardener_connector_cli.properties").taskValue,
    mainClass in Compile := Some("com.myodov.unicherrygarden.cherrygardener.CherryGardenerCLI"),
  )
  .dependsOn(commonJava, cherryGardenerConnector)
  .enablePlugins(JavaAppPackaging)


//
// Scala components
//

// a virtual “project” that contains all settings/data common for all Scala projects.
lazy val commonScala = (project in file("scala/common_scala"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "common_scala",
    description := "UniCherryGarden: the contents common for all Scala-language components",
  )

// Common modules (for all Scala components).
lazy val api = (project in file("scala/api"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "api",
    description := "UniCherryGarden: internal Scala API commons",
  )
  .dependsOn(commonScala, cherryGardenerInterop, ethUtils)

// Separate module to handle reading the HOCON conf files.
// Used from CLI launcher of CherryGarden components, and from "test" targets of other modules.
lazy val confreader = (project in file("scala/confreader"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "confreader",
    description := "UniCherryGarden: read the HOCON conf files by any UniCherryGarden components",
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
    description := "UniCherryGarden: setup the logging subsystem by any UniCherryGarden components",
    libraryDependencies ++= Seq(
      // Default logging output
      "ch.qos.logback" % "logback-classic" % logbackVersion,
    ),
  )
  .dependsOn(commonScala)

// RDBMS Storage
lazy val db_postgresql_storage = (project in file("scala/storages/db_postgresql_storage"))
  .settings(
    commonSettings,
    commonScalaSettings,
    name := "db_postgresql_storage",
    description := "UniCherryGarden: PostgreSQL data storage available to UniCherryGarden server components",
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
lazy val ethereum_connector = (project in file("scala/connectors/ethereum_connector"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings, // for akka-http
    name := "ethereum_connector",
    description := "UniCherryGarden: connect to Web3 (geth) nodes from Scala code via JSON-RPC or GraphQL",
    libraryDependencies ++= Seq(
      // Web3/Ethereum node JSON-RPC client
      "org.web3j" % "core" % web3jVersion,
      "org.web3j" % "contracts" % web3jVersion,
      // GraphQL client
      "com.github.ghostdogpr" %% "caliban-client" % calibanVersion,
      // Used by Caliban Client for outgoing queries
      "com.softwaremill.sttp.client3" %% "core" % sttpClient3Version,
      "com.softwaremill.sttp.client3" %% "akka-http-backend" % sttpClient3Version, // backend of choice
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion, // needed for "akka-http-backend
      "com.typesafe.akka" %% "akka-stream" % akkaVersion, // needed for akka-http explicitly
    ),
    Compile / caliban / calibanSettings ++= Seq(
      calibanSetting(file("scala/connectors/ethereum_connector/src/main/graphql/Geth.graphql"))(
        cs => cs
          .packageName("caliban")
          .genView(true)
          .imports(
            "com.myodov.unicherrygarden.connectors.graphql.ScalarDecoder.long",
            "com.myodov.unicherrygarden.connectors.graphql.ScalarDecoder.bigInt")
      )
    ),
    Compile / resourceGenerators += versionFileTask("unicherrygarden_ethereum_connector.properties").taskValue,
  )
  .dependsOn(commonScala, api, confreader % "test->compile", logging % "test->compile")
  .enablePlugins(CalibanPlugin)


// CherryPicker: investigates the Ethereum blockchain and cherry-picks
// the information about the Ether/ERC20 transfers.
lazy val cherrypicker = (project in file("scala/cherrypicker"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrypicker",
    description := "UniCherryGarden: CherryPicker – the subsystem that keeps track " +
      "of Ethereum blockchain data, stores it in the DB storage and “cherry-picks” the data on the fly " +
      "(selective currencies, selective addresses to watch)",
    Compile / resourceGenerators += versionFileTask("unicherrygarden_cherrypicker.properties").taskValue,
  )
  .dependsOn(commonScala, api, db_postgresql_storage, ethereum_connector)

// CherryPlanter: sends the transactions to the Ethereum blockchain.
lazy val cherryplanter = (project in file("scala/cherryplanter"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherryplanter",
    description := "UniCherryGarden: CherryPlanter – the subsystem that registers the transactions " +
      "in the Ethereum blockchain in the safest possible manner (retries; increasing the gas price" +
      "if needed, maintaining the nonce values)",
    Compile / resourceGenerators += versionFileTask("unicherrygarden_cherryplanter.properties").taskValue,
  )
  .dependsOn(commonScala, api, db_postgresql_storage, ethereum_connector)

// CherryGardener: provides high-level convenient front-end to the .
lazy val cherrygardener = (project in file("scala/cherrygardener"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonAkkaMicroserviceSettings,
    name := "cherrygardener",
    description := "UniCherryGarden: CherryGardener – the primary frontend to all other UniCherryGarden components",
    Compile / resourceGenerators += versionFileTask("unicherrygarden_cherrygardener.properties").taskValue,
  )
  .dependsOn(
    commonScala, api,
    db_postgresql_storage, ethereum_connector,
    cherrypicker, cherryplanter
  )

// Launcher; launches the CherryPicker/CherryPlanter/CherryGardener daemons.
lazy val launcher = (project in file("scala/launcher"))
  .settings(
    commonSettings,
    commonScalaSettings,
    commonJavaAppPackagingSettings,
    name := "launcher",
    description := "UniCherryGarden: launcher CLI to execute the UniCherryGarden components",
    resolvers += Resolver.bintrayRepo("serioussam", "oss"),
    libraryDependencies ++= Seq(
      // Parse command line arguments
      "com.github.scopt" %% "scopt" % scoptVersion,
    ),
    // Define the launch option for sbt-native-packager
    //    mainClass in Compile := Some("com.myodov.unicherrygarden.launcher.Launcher"),
    //    discoveredMainClasses in Compile := Seq(),
    Compile / resourceGenerators += versionFileTask("unicherrygarden_launcher.properties").taskValue,
  )
  .dependsOn(
    commonScala, api, confreader, db_postgresql_storage, logging,
    cherrypicker, cherryplanter, cherrygardener
  )
  .enablePlugins(JavaAppPackaging)

// So the “main project” won’t build its own super-artifact; only the subprojects will do
publishArtifact := false
