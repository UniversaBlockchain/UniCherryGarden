package com.myodov.unicherrygarden.connectors.jsonrpc

import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnectorSpec

class EthereumSingleNodeJsonRpcConnectorSpec extends AbstractEthereumNodeConnectorSpec {
  lazy val sharedConnector = EthereumSingleNodeJsonRpcConnector(config.getStringList("unicherrygarden.ethereum.rpc_servers").get(0))
}
