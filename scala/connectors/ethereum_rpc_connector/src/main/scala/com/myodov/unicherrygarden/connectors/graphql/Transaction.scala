package com.myodov.unicherrygarden.connectors.graphql

import caliban.Geth.{Bytes32, Transaction}
import caliban.client.SelectionBuilder

/** For a Transaction, get just its hash (for referential integrity only). */
case class TransactionMinimalView(hash: String)

object TransactionMinimal {
  /** A shorthand method to select the minimal block data to query. */
  lazy val view: SelectionBuilder[Transaction, TransactionMinimalView] = {
    Transaction.hash
  }.map(TransactionMinimalView)
}

/** For a Transaction, get most of its data. */
case class TransactionFullView(
                                // *** Before-mined transaction ***
                                hash: Bytes32,
                                from: AccountMinimalView,
                                to: Option[AccountMinimalView],
                                gas: Long,
                                gasPrice: BigInt,
                                nonce: Long,
                                value: BigInt,
                                // *** Mined transaction ***
                                status: Option[Long],
                                block: Option[BlockMinimalView],
                                index: Option[Int],
                                gasUsed: Option[Long],
                                effectiveGasPrice: Option[BigInt],
                                cumulativeGasUsed: Option[Long],
                                logs: Option[List[LogFullView]]
                              )

object TransactionFull {
  /** A shorthand method to select the most transaction data to query. */
  lazy val view = {
    // *** Before-mined transaction ***
    Transaction.hash ~
      Transaction.from() {
        AccountMinimal.view
      } ~
      Transaction.to() {
        AccountMinimal.view
      } ~
      Transaction.gas ~
      Transaction.gasPrice ~
      Transaction.nonce ~
      Transaction.value ~
      // *** Mined transaction ***
      // "status" – EIP 658, since Byzantium fork
      Transaction.status ~
      Transaction.block {
        BlockMinimal.view // for validation only!
      } ~
      Transaction.index ~
      Transaction.gasUsed ~
      Transaction.effectiveGasPrice ~
      Transaction.cumulativeGasUsed ~
      Transaction.logs {
        LogFull.view
      }
  }.mapN(TransactionFullView)
}
