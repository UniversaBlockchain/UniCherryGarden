package com.myodov.unicherrygarden.connectors.jsonrpc

import com.myodov.unicherrygarden.connectors.jsonrpc.Erc20TransferEventParser.TopicParseResult
import org.scalatest.flatspec.AnyFlatSpec

class Erc20TransferEventParserSpec extends AnyFlatSpec {
  "Signature" should "be invariant" in {
    assertResult(
      "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
    )(
      Erc20TransferEventParser.signature
    )
  }

  "Log topics" should "be parseable when contain actual Transfer event" in {
    assertResult(
      Some(TopicParseResult(
        from = "0x5041ed759dd4afc3a72b8192c143f72f4724081a",
        to = "0xa34e0bbc51fdfe5e7bfc0544769d8072533600cf")),
      "Actual transfer event"
    )(
      Erc20TransferEventParser.parseTopics(List(
        "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
        "0x0000000000000000000000005041ed759dd4afc3a72b8192c143f72f4724081a",
        "0x000000000000000000000000a34e0bbc51fdfe5e7bfc0544769d8072533600cf"
      ))
    )

    assertResult(
      Some(TopicParseResult(
        from = "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
        to = "0x618a2750e5adaa56aa42db9f1c9af3c03675eaf0")),
      "Another real transfer event"
    )(
      Erc20TransferEventParser.parseTopics(List(
        "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
        "0x000000000000000000000000d701edf8f9c5d834bcb9add73ddeff2d6b9c3d24",
        "0x000000000000000000000000618a2750e5adaa56aa42db9f1c9af3c03675eaf0"
      ))
    )
  }

  they should "not be parseable in non-Transfer event" in {
    assertResult(
      None,
      "Some Uniswap Deposit event"
    )(
      Erc20TransferEventParser.parseTopics(List(
        "0xe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c",
        "0x0000000000000000000000007a250d5630b4cf539739df2c5dacb4c659f2488d"
      ))
    )

    assertResult(
      None,
      "Some Uniswap Sync event"
    )(
      Erc20TransferEventParser.parseTopics(List(
        "0x1c411e9a96e071241c2f21f7726b17ae89e3cab4c78be50e062b03a9fffbbad1"
      ))
    )

    assertResult(
      None,
      "Some Uniswap Mint event"
    )(
      Erc20TransferEventParser.parseTopics(List(
        "0x4c209b5fc8ad50758f13e2e1088ba56a560dff690a1c6fef26394f4c03821c4f",
        "0x0000000000000000000000007a250d5630b4cf539739df2c5dacb4c659f2488d"
      ))
    )
  }

}
