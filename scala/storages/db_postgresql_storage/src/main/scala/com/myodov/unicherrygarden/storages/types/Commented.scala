package com.myodov.unicherrygarden.storages.types

/** Any record in the DB, which may have an optional custom comment by the admins. */
trait Commented {
  val comment: Option[String]
}
