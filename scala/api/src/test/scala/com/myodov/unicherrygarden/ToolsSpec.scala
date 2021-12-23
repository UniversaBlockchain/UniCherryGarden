package com.myodov.unicherrygarden

import org.scalatest.flatspec.AnyFlatSpec

class ToolsSpec extends AnyFlatSpec {
  import Tools.{reduceOptionSeq, seqIsIncreasing, seqIsIncrementing}

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

  it should "handle sequences a None somewhere as a None(sequence)" in {
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

  it should "handle sequences with multiple None's somewhere as a None(sequence)" in {
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

  "seqIsIncreasing" should "handle ordered sequences as true" in {
    assert(seqIsIncreasing(Seq.empty[Int]))
    assert(seqIsIncreasing(Seq(5)))
    assert(seqIsIncreasing(Seq(5, 6, 7, 8)))
    assert(seqIsIncreasing(Seq(5, 6, 9, 2000)))
    assert(seqIsIncreasing(Seq(5L, 6L, 2000L)))
    assert(seqIsIncreasing(Seq(Float.NegativeInfinity, 28.5, 29.0, 30.2, Float.PositiveInfinity)))
    assert(seqIsIncreasing(Seq("a", "b", "ba", "bb", "c")))
  }

  it should "handle unordered sequences as false" in {
    assert(seqIsIncreasing(Seq(5, 7, 8, 6)) == false)
    assert(seqIsIncreasing(Seq(5, 6, 2000, 9)) == false)
    assert(seqIsIncreasing(Seq(5L, 2000L, 6L)) == false)
    assert(seqIsIncreasing(Seq(28.5, Float.NegativeInfinity)) == false)
    assert(seqIsIncreasing(Seq("a", "ba", "b", "bb", "c")) == false)
  }

  "seqIsIncrementing" should "handle incrementing sequences as true" in {
    assert(seqIsIncrementing(Seq.empty[Int]))
    assert(seqIsIncrementing(Seq(5)))
    assert(seqIsIncrementing(Seq(5, 6, 7, 8)))
    assert(seqIsIncrementing(Seq(6, 7, 8, 9)))
    assert(seqIsIncrementing(Seq(10L, 11L, 12L)))
    assert(seqIsIncrementing(Seq(28.5, 29.5, 30.5)))
  }

  it should "handle non-incrementing sequences as false" in {
    assert(seqIsIncrementing(Seq(5, 7, 8, 6)) == false)
    assert(seqIsIncrementing(Seq(5, 6, 2000, 9)) == false)
    assert(seqIsIncrementing(Seq(5L, 2000L, 6L)) == false)
    assert(seqIsIncrementing(Seq(28.5, Float.NegativeInfinity)) == false)
  }
}
