package com.myodov.unicherrygarden.connectors.graphql.types

import caliban.Geth.{Account, Address}
import caliban.client.SelectionBuilder

/** For an Account, get just its address. */
case class AccountMinimalView(address: Address)

object AccountMinimal {
  /** A shorthand method to select the minimal account data to query. */
  lazy val view: SelectionBuilder[Account, AccountMinimalView] = {
    Account.address
  }.map(AccountMinimalView)
}
