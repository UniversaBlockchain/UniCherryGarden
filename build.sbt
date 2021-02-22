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
val slf4jVersion = "1.7.26"
val syslogAppenderVersion = "1.0.0"
val web3jVersion = "4.8.4"

// Common settings

lazy val commonSettings = Seq(
  organization := "com.myodov",
  maintainer := "Alex Myodov <amyodov@gmail.com>",
  version := "0.1.2",
  scalaVersion := "2.13.0",
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    "com.typesafe" % "config" % configLibVersion % Test, // so the specs can read the settings from conf files
  ),
)

// Common modules

lazy val api = (project in file("api"))
  .settings(commonSettings: _*)
  .settings(
    name := "uni_cherrypicker__api",
  )

lazy val cherrypicker = (project in file("cherrypicker"))
  .settings(commonSettings: _*)
  .settings(
    name := "uni_cherrypicker__picker",
  )
  .dependsOn(api)

// Storages

lazy val db_postgresql_storage = (project in file("storages/db_postgresql_storage"))
  .settings(commonSettings: _*)
  .settings(
    name := "uni_cherrypicker__db_postgresql_storage",
    libraryDependencies ++= Seq(
      "org.postgresql" % "postgresql" % postgresqlVersion,
      "org.flywaydb" % "flyway-core" % flywayDbVersion,
    ),
  )
  .dependsOn(api)

lazy val ethereum_rpc_well = (project in file("connectors/ethereum_rpc_connector"))
  .settings(commonSettings: _*)
  .settings(
    name := "uni_cherrypicker__ethereum_rpc_well",
    libraryDependencies ++= Seq(
      "org.web3j" % "core" % web3jVersion,
    ),
  )
  .dependsOn(api)

// Launcher

lazy val launcher = (project in file("launcher"))
  .settings(commonSettings: _*)
  .settings(
    name := "uni_cherrypicker",
    resolvers += Resolver.bintrayRepo("serioussam", "oss"),
    libraryDependencies ++= Seq(
      // Default logging output
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      // Support for logging output to syslog
      "com.github.serioussam" % "syslogappender" % syslogAppenderVersion,
      // Parse HOCON config files
      "com.typesafe" % "config" % configLibVersion,
      // Parse command line arguments
      "com.github.scopt" %% "scopt" % scoptVersion,
    ),
    // Define the launch option for sbt-native-packager
    //    mainClass in Compile := Some("com.myodov.unicherrypicker.launcher.Launcher"),
    //    discoveredMainClasses in Compile := Seq(),
  )
  .enablePlugins(JavaAppPackaging)
  .dependsOn(api, cherrypicker, db_postgresql_storage, ethereum_rpc_well)
