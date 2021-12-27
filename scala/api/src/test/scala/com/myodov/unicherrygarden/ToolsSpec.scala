package com.myodov.unicherrygarden

import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.SortedMap

class ToolsSpec extends AnyFlatSpec {

  import Tools.Implicits._
  import Tools.{forAllPairs, reduceOptionSeq, seqIsIncreasing, seqIsIncrementing}

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

  "forAllPairs" should "work with various predicates" in {
    assertResult(true)(
      forAllPairs((_: Int) < (_: Int))(Seq(6, 7, 8, 9))
    )
    assertResult(false)(
      forAllPairs((_: Int) < (_: Int))(Seq(6, 78, 7, 9))
    )
    assertResult(true)(
      forAllPairs((_: Int) * 2 + 1 == (_: Int))(Seq(1, 3, 7, 15, 31, 63))
    )
    assertResult(false)(
      forAllPairs((_: Int) * 2 + 1 == (_: Int))(Seq(1, 3, 7, 14, 31, 63))
    )
  }

  "Iterable.forAllPairs" should "work with various predicates as a method of Iterable" in {

    assertResult(true)(
      Seq(6, 7, 8, 9).forAllPairs((_: Int) < (_: Int))
    )
    assertResult(false)(
      Seq(6, 78, 7, 9).forAllPairs((_: Int) < (_: Int))
    )
    assertResult(true)(
      Seq(1, 3, 7, 15, 31, 63).forAllPairs((_: Int) * 2 + 1 == (_: Int))
    )
    assertResult(false)(
      Seq(1, 3, 7, 14, 31, 63).forAllPairs((_: Int) * 2 + 1 == (_: Int))
    )
  }

  "seqIsIncreasing" should "handle ordered sequences as true" in {
    assert(seqIsIncreasing(Seq.empty[Int]))
    assert(seqIsIncreasing(Seq(5)))
    assert(seqIsIncreasing(Seq(5, 6, 7, 8)))
    assert(seqIsIncreasing(Seq(5, 6, 9, 2000)))
    assert(seqIsIncreasing(Seq(5L, 6L, 2000L)))
    assert(seqIsIncreasing(Seq("a", "b", "ba", "bb", "c")))
  }

  it should "handle unordered sequences as false" in {
    assert(seqIsIncreasing(Seq(5, 7, 8, 6)) == false)
    assert(seqIsIncreasing(Seq(5, 6, 2000, 9)) == false)
    assert(seqIsIncreasing(Seq(5L, 2000L, 6L)) == false)
    assert(seqIsIncreasing(Seq("a", "ba", "b", "bb", "c")) == false)
  }

  it should "handle the keys of SortedMap" in {
    assertResult(
      true
    )(
      seqIsIncreasing(SortedMap(
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
      ).keys)
    )
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

  it should "handle the keys of SortedMap" in {
    assertResult(
      true
    )(
      seqIsIncrementing(SortedMap(
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
      ).keys)
    )
  }
}
