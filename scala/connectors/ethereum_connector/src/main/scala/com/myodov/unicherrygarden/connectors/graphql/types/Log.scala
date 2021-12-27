package com.myodov.unicherrygarden.connectors.graphql.types

import caliban.Geth.{Bytes, Bytes32, Log}

/** For a TX Log, get all of its data. */
case class LogFullView(transaction: TransactionMinimalView,
                       index: Int,
                       account: AccountMinimalView,
                       topics: List[Bytes32],
                       data: Bytes)

object LogFull {
  /** A shorthand method to select the full log data to query. */
  lazy val view = {
    Log.transaction { // for validation only!
      TransactionMinimal.view
    } ~
      Log.index ~
      Log.account() {
        AccountMinimal.view
      } ~
      Log.topics ~
      Log.data
  }.mapN(LogFullView)
}
