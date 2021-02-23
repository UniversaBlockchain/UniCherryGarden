package com.myodov.unicherrypicker.connectors

/** Various helpers and utils to deal with Ethereum data. */
object EthUtils {
  private val BLOCK_HASH_LENGTH = 66
  private val TRANSACTION_HASH_LENGTH = 66
  private val ADDRESS_HASH_LENGTH = 42

  /** Check if the `hash` is valid for usage in Ethereum. */
  private[connectors] def isValidHash(hash: String, length: Int): Boolean = {
    assert(hash != null)
    assert(length > 2)
    (hash.length == length) && hash.startsWith("0x") && hash.matches("^0x[0-9a-f]+$")
  }

  /** Whether the argument is a valid Ethereum block hash. */
  def isValidBlockHash(hash: String): Boolean = isValidHash(hash, BLOCK_HASH_LENGTH)

  /** Whether the argument is a valid Ethereum transaction hash. */
  def isValidTransactionHash(hash: String): Boolean = isValidHash(hash, TRANSACTION_HASH_LENGTH)

  /** Whether the argument is a valid Ethereum address hash. */
  def isValidAddressHash(hash: String): Boolean = isValidHash(hash, ADDRESS_HASH_LENGTH)
}
