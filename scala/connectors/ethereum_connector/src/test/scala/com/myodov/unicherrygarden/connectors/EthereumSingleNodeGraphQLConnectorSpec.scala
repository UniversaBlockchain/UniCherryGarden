package com.myodov.unicherrygarden.connectors

class EthereumSingleNodeGraphQLConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeGraphQLConnector(config.getStringList("ethereum.rpc_servers").get(0))
}
