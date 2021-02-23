package com.myodov.unicherrypicker.api.dlt

import java.time.Instant

import org.scalatest.FlatSpec

class EthereumBlockSpec extends FlatSpec {
  "EthereumBlock constructor" should "support correct data" in {
    assert(
      EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Just a regular EthereumBlock"
    )
    assert(
      EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        None,
        Instant.parse("2021-02-22T10:50:22Z")
      ).isInstanceOf[EthereumBlock],
      "Parent hash may be missing"
    )
  }

  it should "fail with null arguments" in {
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        null,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
        Instant.parse("2021-02-22T10:50:22Z")
      ),
      "Fail with null blockNumber"
    )
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        null,
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
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
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
        null
      ).isInstanceOf[EthereumBlock],
      "Fail with null timestamp"
    )
  }

  it should "validate hash arguments" in {
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94eg",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692d9"),
        Instant.parse("2021-02-22T10:50:22Z")
      )
    )
    assertThrows[IllegalArgumentException](
      EthereumBlock(
        11906373,
        "0x71313d0f8edb2146c071d088a7ea4f91dd6f108ee42a7b7041c95a6154ed94e8",
        Some("0x7a5412e1e68f2627ac671e33a0b8f1e0aad47231b78333328dabdaf5e1b692dG"),
        Instant.parse("2021-02-22T10:50:22Z")
      )
    )
  }
}
