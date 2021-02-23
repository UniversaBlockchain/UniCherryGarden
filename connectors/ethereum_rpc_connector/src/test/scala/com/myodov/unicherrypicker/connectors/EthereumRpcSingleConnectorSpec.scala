package com.myodov.unicherrypicker.connectors

import java.time.Instant

import com.myodov.unicherrypicker.api.dlt.EthereumBlock
import com.typesafe.config.ConfigFactory
import org.scalatest.FlatSpec

class EthereumRpcSingleConnectorSpec extends FlatSpec {
  lazy val config = ConfigFactory.load()
  lazy val sharedConnector = EthereumRpcSingleConnector(config.getStringList("ethereum.rpc_servers").get(0))

  "latestSyncedBlockNumber()" should "return some valid number" in {
    assert(
      sharedConnector.latestSyncedBlockNumber.get > 11500000
    )
  }

  "readBlock()" should "read and parse a block from Ethereum blockchain" in {
    assertResult(
      Some(EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
        Instant.parse("2021-02-22T10:50:22Z")
      ))
    )(
      sharedConnector.readBlock(11906373)
    )
  }
}
