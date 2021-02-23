package com.myodov.unicherrypicker.eth

import org.scalatest.FlatSpec

class EthUtilsSpec extends FlatSpec {
  "isValidHash()" should "work correctly in all scenarios" in {
    assertResult(true, "Correct hash")(
      EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
    )
    assertResult(false, "Shorter than needed")(
      EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b53", 66)
    )
    assertResult(false, "Longer than needed")(
      EthUtils.isValidHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
    )
    assertResult(false, "No leading 0x prefix")(
      EthUtils.isValidHash("7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
    )
    assertResult(false, "No leading 0x prefix (and correct length)")(
      EthUtils.isValidHash("307150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b5357", 66)
    )
    assertResult(false, "Lowercase only")(
      EthUtils.isValidHash("0x7150Afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
    )
    assertResult(false, "Even the prefix is in lowercase")(
      EthUtils.isValidHash("0X7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535", 66)
    )
  }

  "isValidBlockHash()" should "work correctly for block hashes" in {
    assertResult(true)(
      EthUtils.isValidBlockHash("0x7150afaf4bdc777e4c75de0bc15c34a23f394f8bf391cfcb7dd6309f1b14b535")
    )
  }

  "isValidTransactionHash()" should "work correctly for transaction hashes" in {
    assertResult(true)(
      EthUtils.isValidTransactionHash("0x86e671138ae2d393ee0041358ded3b0f843352ce4d7191d627452d946f3e28b2")
    )
  }

  "isValidAddressHash()" should "work correctly for address hashes" in {
    assertResult(true)(
      EthUtils.isValidAddressHash("0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24")
    )
    assertResult(false, "Lowercase only")(
      EthUtils.isValidAddressHash("0xd701eDF8f9C5d834Bcb9Add73ddefF2D6B9C3D24")
    )
  }
}
