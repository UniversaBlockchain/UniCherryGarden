package com.myodov.unicherrygarden.api.dlt

import com.myodov.unicherrygarden.api.dlt.events.Erc20TransferEvent
import org.scalatest.flatspec.AnyFlatSpec

class EthereumTxLogSpec extends AnyFlatSpec {
  "TxLog" should "validate nulls" in {
    assertThrows[NullPointerException](
      EthereumTxLog(
        0,
        null,
        ""),
      "Topics cannot be null"
    )
    assertThrows[NullPointerException](
      EthereumTxLog(
        0,
        List(null),
        ""),
      "Topics cannot contain null"
    )
    assertThrows[IllegalArgumentException](
      EthereumTxLog(
        0,
        List[Seq[Byte]](),
        null),
      "Data cannot be null"
    )
  }
  it should "validate logs of improper length" in {
    assertThrows[IllegalArgumentException](
      EthereumTxLog(
        0,
        List("0x1234"),
        ""),
      "Logs should be 66 symbols long"
    )
    assertThrows[IllegalArgumentException](
      EthereumTxLog(
        0,
        List("001234567890123456789012345678901234567890123456789012345678901234"),
        ""),
      "Logs should be 66 symbols long proper hexadecimal"
    )
  }
  it should "not fail validations for valid contents" in {
    assert(
      null !=
        EthereumTxLog(
          0,
          List(),
          ""),
      "Minimal empty log is okay"
    )
    assert(
      null !=
        EthereumTxLog(
          0,
          List(),
          "0x1234"),
      "Some data is okay"
    )
    assert(
      null !=
        EthereumTxLog(
          0,
          List("0x0000000000000000000000001df163ef8699c9b9c16236e6ff016c7834206304"),
          ""),
      "Some topics is okay"
    )
    assert(
      null !=
        EthereumTxLog(
          0,
          List("0x0000000000000000000000001df163ef8699c9b9c16236e6ff016c7834206304"),
          "0x1234"),
      "Some topics and data altogether is okay"
    )
  }
  it should "detect valid ERC20 Transfer events" in {
    assert(
      EthereumTxLog(
        0,
        List(
          "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
          "0x000000000000000000000000d701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          "0x0000000000000000000000001df163ef8699c9b9c16236e6ff016c7834206304"),
        "0x00000000000000000000000000000000000000000000054c2c9e1a40db440000"
      ).isErc20Transfer.get ==
        Erc20TransferEvent(
          from = "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
          to = "0x1df163ef8699c9b9c16236e6ff016c7834206304",
          value = BigInt("25017000000000000000000")
        ),
      "ERC20 Transfer event is parseable"
    )
    assert(
      EthereumTxLog(
        0,
        List(
          "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
          "0x0000000000000000000000004b35c092772f6187b1cd2a26c4f537292ce68b2c",
          "0x000000000000000000000000b5d85cbf7cb3ee0d56b3bb207d5fc4b82f43f511"),
        "0x000000000000000000000000000000000000000000000000000000001beb297f"
      ).isErc20Transfer.get ==
        Erc20TransferEvent(
          from = "0x4b35c092772f6187b1cd2a26c4f537292ce68b2c",
          to = "0xb5d85cbf7cb3ee0d56b3bb207d5fc4b82f43f511",
          value = BigInt(468396415)
        ),
      "Another example of valid ERC20 transfer event"
    )
  }
  it should "skip the data if it’s not a ERC20 Transfer event" in {
    assert(
      EthereumTxLog(
        0,
        List(
          "0x8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925",
          "0x000000000000000000000000cc46bd3ecd8e57edfe9019e2d0de835379424196",
          "0x000000000000000000000000e592427a0aece92de3edee1f18e0157c05861564"),
        "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
      ).isErc20Transfer.isEmpty,
      "This is actually a ERC20 Approval event, not a ERC20 Transfer"
    )
  }
}
