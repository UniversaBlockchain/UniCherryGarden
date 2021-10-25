package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.ethereum.EthUtils

/** Some asset – cryptocurrency, or probably a token – in Ethereum blockchain. */
trait Asset {
  def isToken: Boolean
}

/** Some asset for which we know name/“symbol” (ticker code). */
trait NamedAsset extends Asset {
  /** E.g. "Ether" */
  def name: String

  /** E.g. "ETH" or "UTNP" */
  def symbol: String
}

trait Token extends Asset {
  /** Some ID of the token, uniquely identifying it in the base blockchain.
   *
   * E.g., for Ethereum blockchain, it is the Ethereum address of the token contract.
   */
  def uid: String
}

//class DummyAsset(val name: String, val isToken: Boolean)
//  extends Asset
//
//class DummyCoin(name: String, network: Network)
//  extends DummyAsset(name, network, false)
//
//class DummyToken(val uid: String, name: String, network: Network)
//  extends DummyAsset(name, network, true) with Token

//case class Ether() extends NamedAsset {
//  override val name = "Ether"
//  override val symbol = "ETH"
//  override val isToken = false
//}

//object Ether {
//  val ETHER = Ether()
//}

object Ether extends NamedAsset {
  override val name = "Ether"
  override val symbol = "ETH"
  override val isToken = false
}

case class ERC20Token(uid: String) extends Token {
  require(uid != null && EthUtils.Addresses.isValidLowercasedAddress(uid), uid)

  override val isToken = true

  @inline val dappAddress: String = uid
}
