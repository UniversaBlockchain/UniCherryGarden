package com.myodov.unicherrygarden

import org.scalatest.flatspec.AnyFlatSpec

class ToolsSpec extends AnyFlatSpec {
  import Tools.reduceOptionSeq

  "reduceOptionSeq" should "handle sequences without None's as Some(sequences)" in {
    assert(
      reduceOptionSeq(Seq(Option(5), Option(6), Option(7))) ==
        Some(Seq(5, 6, 7))
    )
    assert(
      reduceOptionSeq(Seq(Option("A"), Option("BC"), Option("DEF"), Option(""))) ==
        Some(Seq("A", "BC", "DEF", ""))
    )
    assert(
      reduceOptionSeq(Seq(Option(1.23), Option(0.45), Option(3.78), Option(Math.PI))) ==
        Some(Seq(1.23, 0.45, 3.78, Math.PI))
    )
  }

  "reduceOptionSeq" should "handle sequences a None somewhere as a None(sequence)" in {
    assert(
      reduceOptionSeq(Seq(None, Option(5), Option(6), Option(7))).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(Option("A"), Option("BC"), None, Option("DEF"), Option(""))).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(Option(1.23), Option(0.45), Option(3.78), Option(Math.PI), None)).isEmpty
    )
  }


  "reduceOptionSeq" should "handle sequences with multiple None's somewhere as a None(sequence)" in {
    assert(
      reduceOptionSeq(Seq(None, Option(5), None, Option(6), Option(7))).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(Option("A"), Option("BC"), None, Option("DEF"), Option(""), None)).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(None, Option("A"), Option("BC"), None, Option("DEF"), None, Option(""))).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(Option(1.23), Option(0.45), Option(3.78), Option(Math.PI), None, None)).isEmpty
    )
    assert(
      reduceOptionSeq(Seq(None, Option(1.23), Option(0.45), Option(3.78), Option(Math.PI), None, None)).isEmpty
    )
  }
}
