package com.myodov.unicherrygarden.api.dlt.events

/**
 * ERC20 “Transfer” event, usually defined as `Transfer(address,address,uint256)`.
 *
 * @see [[https://eips.ethereum.org/EIPS/eip-20 ERC20/EIP-20 specification]].
 */
case class Erc20TransferEvent(from: String, to: String, value: BigInt)
