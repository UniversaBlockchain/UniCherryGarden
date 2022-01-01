package com.myodov.unicherrygarden.connectors.graphql

import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnectorSpec

class EthereumSingleNodeGraphQLConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeGraphQLConnector(config.getStringList("unicherrygarden.ethereum.rpc_servers").get(0))

  "readBlocks(20 blocks)" should "work well" in {
    assert(
      sharedConnector.readBlocks(12_129_000 until 12_129_020).get.size == 20
    )
  }

  "readBlocks(50 blocks)" should "work well" in {
    assert(
      sharedConnector.readBlocks(12_329_000 until 12_329_050).get.size == 50
    )
  }
}
