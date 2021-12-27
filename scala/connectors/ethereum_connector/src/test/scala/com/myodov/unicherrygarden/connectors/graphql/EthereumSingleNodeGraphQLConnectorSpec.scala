package com.myodov.unicherrygarden.connectors.graphql

import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnectorSpec

class EthereumSingleNodeGraphQLConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeGraphQLConnector(config.getStringList("ethereum.rpc_servers").get(0))
}
