package com.myodov.unicherrypicker.storages

import com.typesafe.scalalogging.Logger
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.{CleanResult, MigrateResult}

/** Stores the blockchain information in PostgreSQL database. */
class PostgreSQLStorage(jdbcUrl: String, dbUser: String, dbPassword: String) {
  private[this] lazy val logger = Logger[PostgreSQLStorage.type]

  private[this] val flw: Flyway = {
    Flyway.configure.dataSource(jdbcUrl, dbUser, dbPassword)
      .table("flyway_schema_history") // Default in modern Flyway
      .baselineOnMigrate(true)
      .locations("classpath:com/myodov/unicherrypicker/db/migration")
      .load
  }

  private[this]  val clean: CleanResult = flw.clean
  println(s"Cleaned Flyway: cleaned ${clean.schemasCleaned}, dropped ${clean.schemasDropped}");
  private[this]  val migrate: MigrateResult = flw.migrate
  println(s"Migrated Flyway: ${migrate.targetSchemaVersion}");
  //    .migrate()
}

object PostgreSQLStorage {
  def apply(jdbcUrl: String, dbUser: String, dbPassword: String): PostgreSQLStorage =
    new PostgreSQLStorage(jdbcUrl, dbUser, dbPassword)
}
