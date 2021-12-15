package com.myodov.unicherrygarden.connectors

class EthereumSingleNodeGraphQLConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeJsonRpcConnector(config.getStringList("ethereum.rpc_servers").get(0))
}
