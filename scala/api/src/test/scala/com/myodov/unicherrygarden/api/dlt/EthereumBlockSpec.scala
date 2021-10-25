package com.myodov.unicherrygarden.api.dlt

import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec

class EthereumBlockSpec extends AnyFlatSpec {
  "EthereumBlock constructor" should "support correct data" in {
    assert(
      EthereumBlock(
        11906373,
        hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Just a regular EthereumBlock"
    )
    assert(
      EthereumBlock(
        11906373,
        hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        parentHash = "0x0000000000000000000000000000000000000000000000000000000000000000",
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Parent hash may be missing"
    )
  }

  it should "fail with null arguments" in {
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        hash = null,
        parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Fail with null hash"
    )
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        null,
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Fail with null parentHash"
    )
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
        null
      ).isInstanceOf[EthereumBlock],
      "Fail with null timestamp"
    )
  }

  it should "validate hash arguments" in {
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94eg",
        parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9",
        Instant.parse("2021-02-22T10:50:22Z")
      )
    )
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        hash = "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        parentHash = "0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692dG",
        Instant.parse("2021-02-22T10:50:22Z")
      )
    )
  }
}
