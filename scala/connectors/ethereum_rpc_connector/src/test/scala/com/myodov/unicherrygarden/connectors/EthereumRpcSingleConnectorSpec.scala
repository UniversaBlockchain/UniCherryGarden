package com.myodov.unicherrygarden.connectors

import java.time.Instant

import com.myodov.unicherrygarden.api.dlt.EthereumBlock
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec

class EthereumRpcSingleConnectorSpec extends AnyFlatSpec {
  lazy val config = ConfigFactory.load()
  lazy val sharedConnector = EthereumRpcSingleConnector(config.getStringList("ethereum.rpc_servers").get(0))

  "latestSyncedBlockNumber()" should "return some valid number" in {
    assert(
      sharedConnector.latestSyncedBlockNumber.get > 11500000
    )
  }

  "readBlock(0)" should "read and parse an origin block" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            0,
            hash = "0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3",
            parentHash = "0x0000000000000000000000000000000000000000000000000000000000000000",
            Instant.parse("1970-01-01T00:00:00Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 0,
        filterAddresses = Set.empty,
        filterCurrencies = Set.empty)
    )
  }

  "readBlock(1)" should "read and parse very first block" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            1,
            hash = "0x88e96d4537bea4d9c05d12549907b32561d3bf31f45aae734cdc119f13406cb6",
            parentHash = "0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3",
            Instant.parse("2015-07-30T15:26:28Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 1,
        filterAddresses = Set.empty,
        filterCurrencies = Set.empty)
    )
  }

  "readBlock()" should "read and parse some block (empty filters)" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            11906373,
            hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
            parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
            Instant.parse("2021-02-22T10:50:22Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 11906373,
        filterAddresses = Set.empty,
        filterCurrencies = Set.empty)
    )
  }

  // Blocks 1264, 120522 and 120545 are the only blocks for address 0x90d331f19e4ef54c4dc2710087ebd8536084a85a
  // In block 1264, it mined some ETH; in 120522 and 120545 it moved out some ETH
  // 60003 block has 4 transactions
  "readBlock(120522)" should "parse block and find some single ETH transfer from address" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = "0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003",
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        filterAddresses = Set("0x90d331f19e4ef54c4dc2710087ebd8536084a85a"),
        filterCurrencies = Set.empty)
    )
  }

  it should "parse block and find some single ETH transfer to address" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = "0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003",
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        filterAddresses = Set("0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"),
        filterCurrencies = Set.empty)
    )
  }

  it should "parse block and find some single ETH transfer when both `from` and `to` address are defined" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = "0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003",
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        filterAddresses = Set("0x90d331f19e4ef54c4dc2710087ebd8536084a85a", "0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c" ),
        filterCurrencies = Set.empty)
    )
  }


  // 60003 block has 4 transactions
  "readBlock(60003)" should "read and parse block (with some filters)" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            60003,
            hash = "0xaf0615219cf8b66cabdd0ca559cc27dfc070740489f5f83fd7afcdf717d00ee4",
            parentHash = "0xb76c54058fb9eb9590cf1140f8f4df429d2632fb2196b7b551bc1cbad84bcbb8",
            Instant.parse("2015-08-09T19:09:21Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 60003,
        filterAddresses = Set("0x28bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15"),
        filterCurrencies = Set.empty)
    )
  }


  it should "read and parse some heavily used block (with some filters)" in {
    assertResult(
      Option( // Option of Tuple
        (
          EthereumBlock(
            11906373,
            hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
            parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
            Instant.parse("2021-02-22T10:50:22Z")
          ),
          List(),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 11906373,
        filterAddresses = Set("0x28bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15"),
        filterCurrencies = Set.empty)
    )
  }
}
