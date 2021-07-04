package com.myodov.unicherrygarden.api

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

class CherryPickerSpec extends AnyFlatSpec with BeforeAndAfter {
  before {
    println(s"Before!")
  }

  "API" should "return 7" in {
    assert(7 == 7, "At least it works")
  }

  "API" should "return 9" in {
    assert(9 == 9, "At least it works")
  }

  after {
    println(s"After!")
  }
}
