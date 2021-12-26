package com.myodov.unicherrygarden.connectors

import java.time.Instant

import com.myodov.unicherrygarden.api.dlt.{EthereumBlock, EthereumMinedTransaction, EthereumTxLog}
import com.myodov.unicherrygarden.connectors.AbstractEthereumNodeConnector.SyncingStatus
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.SortedMap

abstract class AbstractEthereumNodeConnectorSpec extends AnyFlatSpec {
  val sharedConnector: AbstractEthereumNodeConnector with Web3ReadOperations

  lazy val config = ConfigFactory.load()

  "ethSyncingBlockNumber()" should "return some valid number" in {
    val blockNumberResult: Option[SyncingStatus] = sharedConnector.ethSyncingBlockNumber
    assert(
      blockNumberResult match {
        case Some(SyncingStatus(current, highest)) =>
          (current > 11500000) && (highest > 11500000)
      },
      blockNumberResult
    )
  }

  "readBlock(0)" should "read and parse an origin block" in {
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
      sharedConnector.readBlock(
        blockNumber = 0,
        addressesOfInterest = Set.empty)
    )
  }

  "readBlock(1)" should "read and parse very first block" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            1,
            hash = "0x88e96d4537bea4d9c05d12549907b32561d3bf31f45aae734cdc119f13406cb6",
            parentHash = Some("0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3"),
            Instant.parse("2015-07-30T15:26:28Z")
          ),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 1,
        addressesOfInterest = Set.empty)
    )
  }

  "readBlock()" should "read and parse some block (empty filters)" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            11906373,
            hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
            parentHash = Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
            Instant.parse("2021-02-22T10:50:22Z")
          ),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 11906373,
        addressesOfInterest = Set.empty)
    )
  }

  // Blocks 1264, 120522 and 120545 are the only blocks for address 0x90d331f19e4ef54c4dc2710087ebd8536084a85a
  // In block 1264, it mined some ETH; in 120522 and 120545 it moved out some ETH
  // 60003 block has 4 transactions
  "readBlock(120522) - pre-Byzantium" should "parse block and find some single ETH transfer from address" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = Some("0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003"),
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(
            EthereumMinedTransaction(
              txhash = "0x8fda3564427d18f119aa2309306babc7cd137893bd32260e4c75b9f74d3eeff6",
              from = "0x90d331f19e4ef54c4dc2710087ebd8536084a85a",
              to = Some("0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"),
              nonce = 0,
              value = BigInt(1000000000000000000L),
              status = None,
              blockNumber = BigInt(120522),
              gas = BigInt(90000),
              gasPrice = BigInt(59920793400L),
              transactionIndex = 0,
              gasUsed = BigInt(21000),
              effectiveGasPrice = BigInt(59920793400L),
              cumulativeGasUsed = BigInt(21000)
            )
          )
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        addressesOfInterest = Set("0x90d331f19e4ef54c4dc2710087ebd8536084a85a"))
    )
  }

  it should "parse block and find some single ETH transfer to address" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = Some("0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003"),
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(
            EthereumMinedTransaction(
              txhash = "0x8fda3564427d18f119aa2309306babc7cd137893bd32260e4c75b9f74d3eeff6",
              from = "0x90d331f19e4ef54c4dc2710087ebd8536084a85a",
              to = Some("0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"),
              nonce = 0,
              value = BigInt(1000000000000000000L),
              status = None,
              blockNumber = BigInt(120522),
              gas = BigInt(90000),
              gasPrice = BigInt(59920793400L),
              transactionIndex = 0,
              gasUsed = BigInt(21000),
              effectiveGasPrice = BigInt(59920793400L),
              cumulativeGasUsed = BigInt(21000)
            )
          )
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        addressesOfInterest = Set("0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"))
    )
  }

  it should "parse block and find some single ETH transfer when both `from` and `to` address are defined" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            120522,
            hash = "0xace30c8603317cff2dd4ba7ddfce8e67bc94ea74ec5479ef1c802985551a1662",
            parentHash = Some("0xd9d68173bb59563f20ec99fcce92dbc33c25160a1c77dd1257243b1fcf723003"),
            Instant.parse("2015-08-21T12:48:31Z")
          ),
          List(
            EthereumMinedTransaction(
              txhash = "0x8fda3564427d18f119aa2309306babc7cd137893bd32260e4c75b9f74d3eeff6",
              from = "0x90d331f19e4ef54c4dc2710087ebd8536084a85a",
              to = Some("0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"),
              nonce = 0,
              value = BigInt(1000000000000000000L),
              status = None,
              blockNumber = BigInt(120522),
              gas = BigInt(90000),
              gasPrice = BigInt(59920793400L),
              transactionIndex = 0,
              gasUsed = BigInt(21000),
              effectiveGasPrice = BigInt(59920793400L),
              cumulativeGasUsed = BigInt(21000)
            )
          )
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 120522,
        addressesOfInterest = Set("0x90d331f19e4ef54c4dc2710087ebd8536084a85a", "0x8f22398f1567cddaba1b6bb1973e62b4992d5c9c"))
    )
  }


  // 60003 block has 4 transactions
  "readBlock(60003) - pre-Byzantium" should "read and parse block (with some filters)" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            60003,
            hash = "0xaf0615219cf8b66cabdd0ca559cc27dfc070740489f5f83fd7afcdf717d00ee4",
            parentHash = Some("0xb76c54058fb9eb9590cf1140f8f4df429d2632fb2196b7b551bc1cbad84bcbb8"),
            Instant.parse("2015-08-09T19:09:21Z")
          ),
          List()
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 60003,
        addressesOfInterest = Set("0x28bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15"))
    )
  }

  "readBlock(11906373) - post-Byzantium" should "read and parse some heavily used block (with some filters)" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            11_906_373,
            hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
            parentHash = Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
            Instant.parse("2021-02-22T10:50:22Z")
          ),
          List(
            EthereumMinedTransaction(
              txhash = "0xc6d5fdb07ae326de2e8a3e154655c820852a447c3d562000b95f87d95e007bc8",
              from = "0x28bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15",
              to = Some("0x26a5b7c23e86f237ff14b9992ce5dafd72057267"),
              nonce = 1,
              value = BigInt(3513761340000000000L),
              gas = BigInt(27881),
              gasPrice = BigInt(140000000000L),
              status = Some(1),
              blockNumber = BigInt(11906373),
              transactionIndex = 244,
              gasUsed = BigInt(23234),
              effectiveGasPrice = BigInt(140000000000L),
              cumulativeGasUsed = BigInt(12429673L),
              txLogs = List(
                EthereumTxLog(
                  logIndex = 245,
                  address = "0x26a5b7c23e86f237ff14b9992ce5dafd72057267",
                  topics = List(
                    "0x606834f57405380c4fb88d1f4850326ad3885f014bab3b568dfbf7a041eef738",
                    "0x00000000000000000000000000000000000000000000000030c3635270335800",
                    "0x00000000000000000000000028bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15"
                  ),
                  data = "0x00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000"
                )
              )
            )
          )
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 11_906_373,
        addressesOfInterest = Set("0x28bacfa4fc8b8ed6f50cbd0eb1d58bc508eb8e15"))
    )
  }

  "readBlock(10381084) - post-Byzantium" should "find a transaction generating (5) ERC20 Transfer events from smart contract" in {
    assertResult(
      Some( // Option of Tuple
        (
          EthereumBlock(
            10_381_084,
            hash = "0x703aaebacb5abb97043cf22d806e0ff1b947b8f09673e8539b1abb78f737b707",
            parentHash = Some("0x3cee0f907ad3d303a32759d513eb538d58a56e2eb78985edb81841e707b1bde2"),
            Instant.parse("2020-07-02T16:30:14Z")
          ),
          List(
            EthereumMinedTransaction(
              txhash = "0xe522cdccd9c83e49a6d2b5f08b02be9c79e057b3deb827eba8944ad44f4da277",
              from = "0xff571a46c8f4e740952b38b3cd82443ed7b28624",
              to = Some("0x3452519f4711703e13ea0863487eb8401bd6ae57"),
              nonce = 119,
              value = 0,
              status = Some(1),
              blockNumber = BigInt(10_381_084),
              gas = BigInt(5000000),
              gasPrice = BigInt(43000000000L),
              transactionIndex = 38,
              gasUsed = BigInt(154501),
              effectiveGasPrice = BigInt(43000000000L),
              cumulativeGasUsed = BigInt(3121857),
              txLogs = List(
                EthereumTxLog(
                  logIndex = 82,
                  address = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                  topics = List(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                    "0x0000000000000000000000003452519f4711703e13ea0863487eb8401bd6ae57",
                    "0x000000000000000000000000c5d799861ad5f86d11ec88f96628cc0fffc6d913"
                  ),
                  data = "0x0000000000000000000000000000000000000000000a3e2bb9929af503de0000"
                ),
                EthereumTxLog(
                  logIndex = 83,
                  address = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                  topics = List(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                    "0x0000000000000000000000003452519f4711703e13ea0863487eb8401bd6ae57",
                    "0x0000000000000000000000006a227ac2bbadb8c13876c013af13af418b783b35"
                  ),
                  data = "0x0000000000000000000000000000000000000000000002a8563cab4010580000"
                ),
                EthereumTxLog(
                  logIndex = 84,
                  address = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                  topics = List(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                    "0x0000000000000000000000003452519f4711703e13ea0863487eb8401bd6ae57",
                    "0x0000000000000000000000001750abab0d4b10f3d522823df7f5f7d53db21071"
                  ),
                  data = "0x000000000000000000000000000000000000000000000002b5e3af16b1880000"
                ),
                EthereumTxLog(
                  logIndex = 85,
                  address = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                  topics = List(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                    "0x0000000000000000000000003452519f4711703e13ea0863487eb8401bd6ae57",
                    "0x00000000000000000000000093fa98f55a6bd5582f8a2d37bd04de01cb8f5a9f"
                  ),
                  data = "0x000000000000000000000000000000000000000000000002b5e3af16b1880000"
                ),
                EthereumTxLog(
                  logIndex = 86,
                  address = "0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7",
                  topics = List(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
                    "0x0000000000000000000000003452519f4711703e13ea0863487eb8401bd6ae57",
                    "0x00000000000000000000000093fa98f55a6bd5582f8a2d37bd04de01cb8f5a9f"
                  ),
                  data = "0x000000000000000000000000000000000000000000000002b5e3af16b1880000"
                )
              )
            )
          )
        )
      )
    )(
      sharedConnector.readBlock(
        blockNumber = 10_381_084,
        addressesOfInterest = Set("0x3452519f4711703e13ea0863487eb8401bd6ae57"))
    )
  }

  "readBlockHashes(6329969, 6329988)" should "read multiple hashes (20) at once" in {
    assertResult(
      Some(SortedMap(
        6329969 -> "0xf202ba73499ecc7e142f682c80244e26d43cee0c0e7b817e3bba70d825bfdfb8",
        6329970 -> "0x0d391629e5c365e7c1024242a6f6a78db957991db9777a7a5d4ae2265ffde00b",
        6329971 -> "0xa69bd41f550360e7e046f060801d863d3a4c9660c9621a2c5446c87d6b4578ae",
        6329972 -> "0x8f341345b90dfbbc9b460761362d65a00c7bfbe816a5336dace6bba165fd32bc",
        6329973 -> "0x055e89ad4fa4ff8f2e08d0ba5acef4b9d947b721d08029172ed7095f6daba7da",
        6329974 -> "0x0de0e100310684dcfb4fea85e5369b17721741530d09c93085590a9bfe79714d",
        6329975 -> "0x3978e688897aaa56c57572f76827941d2f95e8c9a3afd4a6e0adf87cfe2e34e4",
        6329976 -> "0x20aa98269681aa97a511e5b166e2284665ed5c23ac294675ad21efe4744fa3a0",
        6329977 -> "0x6375d484e111d8a79b7c832d8f653d928318b17759162fa2225d69eff6b2061a",
        6329978 -> "0xcd0df7c0e6a547c79261e8eec604efc9b2a239a02cdc5ffdfe59c6f071535217",
        6329979 -> "0x72872b6e0f520a9844fb2ce7ec1409829fb40fd23e17ef5b835e0d774c250b66",
        6329980 -> "0x0c1de31211b58b2aec7dbcf01ab22aa5a3d130683193afc0211651c7be16e7ab",
        6329981 -> "0x4f963c09b13b4d50990327f0be519292524ec104e6654d50a1501617d1645894",
        6329982 -> "0x400acfd71062e74ab474e9b2f877e387878fe9a82f1ad4c7fbc3394ff47754a7",
        6329983 -> "0x770d57125f188bd3d07ce26241205592bcfe90e6506e99684423dd315e818b38",
        6329984 -> "0x8d7ec43d9cde9134ce349e6269e6a2f5ac3ce60842a7ec4c2bcc44920003acae",
        6329985 -> "0x6e56ac89871b5ddea797d9e50270c6a50e9c22118c8569400d12f27250bb15db",
        6329986 -> "0xd3e24981669833d3f2459c401188d47fd8f988eb4b74cb0fc6cf6ab6bd8c05f6",
        6329987 -> "0x1a803a6b5db77095c60a5e3a682be45c1a7eea19857bc2defb93d176dd8c4d14",
        6329988 -> "0xeadebddc389c99454945e6a5ecd4a4f5164a8bcb1ba9f18a9a86c7de1f93c58b",
      ))
    )(
      sharedConnector.readBlockHashes(6329969 to 6329988)
    )
  }
}
