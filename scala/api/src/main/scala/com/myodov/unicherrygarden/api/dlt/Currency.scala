package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.ethereum.EthUtils

/** Some cryptocurrency (or probably a token) in Ethereum blockchain. */
trait Currency {
  def isToken: Boolean
}

/** Some cryptocurrency (or probably a token) for which we know name/“symbol” (ticker code). */
trait NamedCurrency extends Currency {
  /** E.g. "Ether" */
  def name: String

  require(name != null)

  /** E.g. "ETH" or "UTNP" */
  def symbol: String

  require(symbol != null)
}

trait Token extends Currency {
  /** Some ID of the token, uniquely identifying it in the base blockchain.
   *
   * E.g., for Ethereum, it is the Ethereum address of the token contract.
   * */
  def uid: String

  require(uid != null && EthUtils.Addresses.isValidAddress(uid))
}

//class DummyCurrency(val name: String, val network: Network, val isToken: Boolean)
//  extends Currency
//
//class DummyCoin(name: String, network: Network)
//  extends DummyCurrency(name, network, false)
//
//class DummyToken(val uid: String, name: String, network: Network)
//  extends DummyCurrency(name, network, true) with Token

case class Ether(name: String = "Ether",
                 symbol: String = "ETH",
                 isToken: Boolean = false) extends NamedCurrency

object Ether {
  val ETHER = Ether()
}

case class ERC20Token(uid: String,
                      isToken: Boolean = true) extends Token {
  @inline def dappAddress: String = uid
}
