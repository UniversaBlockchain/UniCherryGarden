package com.myodov.unicherrygarden.connectors.graphql

import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnectorSpec

class EthereumSingleNodeGraphQLConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeGraphQLConnector(config.getStringList("ethereum.rpc_servers").get(0))

  "readBlocks(20 blocks)" should "work well" in {
    assert(
      sharedConnector.readBlocks(12_129_000 to 12_129_019).get.size == 20
    )
  }

  "readBlocks(100 blocks)" should "work well" in {
    assert(
      sharedConnector.readBlocks(12_329_000 to 12_329_999).get.size == 100
    )
  }
}
