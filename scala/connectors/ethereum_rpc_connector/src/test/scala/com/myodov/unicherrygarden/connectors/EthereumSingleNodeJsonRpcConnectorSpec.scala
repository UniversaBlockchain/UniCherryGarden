package com.myodov.unicherrygarden.connectors

import java.time.Instant

import com.myodov.unicherrygarden.api.dlt.EthereumBlock

class EthereumSingleNodeJsonRpcConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeJsonRpcConnector(config.getStringList("ethereum.rpc_servers").get(0))

  "readBlockGraphQL(12345)" should "read and parse a block" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            0,
            hash = "0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3",
            parentHash = Some("0x0000000000000000000000000000000000000000000000000000000000000000"),
            Instant.parse("1970-01-01T00:00:00Z")
          ),
          List()
        )
      )
    )(
      sharedConnector.readBlockGraphQL(
        blockNumber = 10381084,
        addressesOfInterest = Set.empty)
    )
  }
}
