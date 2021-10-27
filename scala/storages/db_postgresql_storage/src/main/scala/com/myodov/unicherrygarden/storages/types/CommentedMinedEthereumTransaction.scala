package com.myodov.unicherrygarden.storages.types

import com.myodov.unicherrygarden.api.dlt.EthereumMinedTransaction

/** Information for any transaction mined (recorded) in Ethereum blockchain;
 * stored in `ucg_transaction` table.
 */
final case class CommentedMinedEthereumTransaction(
                                                    // Transaction-specific
                                                    override val txhash: String,
                                                    override val from: String,
                                                    override val to: Option[String],
                                                    override val gas: BigInt,
                                                    override val gasPrice: BigInt,
                                                    override val nonce: Int,
                                                    override val value: BigInt,
                                                    // MinedTransaction-specific
                                                    override val status: Int,
                                                    override val blockNumber: BigInt,
                                                    override val transactionIndex: Int,
                                                    override val gasUsed: BigInt,
                                                    override val effectiveGasPrice: BigInt,
                                                    override val cumulativeGasUsed: BigInt,
                                                    // Commented
                                                    comment: Option[String]
                                                  ) extends EthereumMinedTransaction(
  txhash,
  from,
  to,
  gas,
  gasPrice,
  nonce,
  value,
  status,
  blockNumber,
  transactionIndex,
  gasUsed,
  effectiveGasPrice,
  cumulativeGasUsed
) with Commented
