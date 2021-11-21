package com.myodov.unicherrygarden.storages.types

import com.myodov.unicherrygarden.api.dlt

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
                                                    override val status: Option[Int],
                                                    override val blockNumber: BigInt,
                                                    override val transactionIndex: Int,
                                                    override val gasUsed: BigInt = 0,
                                                    override val effectiveGasPrice: BigInt = 0,
                                                    override val cumulativeGasUsed: BigInt = 0,
                                                    override val txLogs: Seq[dlt.EthereumTxLog] = Seq(),
                                                    // Commented
                                                    comment: Option[String] = None
                                                  ) extends dlt.EthereumMinedTransaction(
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
  cumulativeGasUsed,
  txLogs
) with Commented
