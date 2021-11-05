package com.myodov.unicherrygarden.storages.types

import java.time.Instant

import com.myodov.unicherrygarden.api.dlt.EthereumBlock

/** Information for any block in Ethereum blockchain; stored in `ucg_block` table. */
final case class CommentedEthereumBlock(
                                   override val number: Int,
                                   override val hash: String,
                                   override val parentHash: Option[String],
                                   override val timestamp: Instant,
                                   comment: Option[String] = None
                                 ) extends EthereumBlock(number, hash, parentHash, timestamp) with Commented
