package com.myodov.unicherrygarden.api.dlt

///** This is a single “currency operation” in a blockchain, typically
//  * causing some amount of currency to be changed on some address (and optionally, on some other addresses too).
//  *
//  * @param address      : Base address of operation.
//  * @param balanceDelta : If positive, the balance for `address` has increased;
//  *                     if negative, the balance for `address` has decreased.
//  */
//class Operation(val address: String, val balanceDelta: BigDecimal) {
//
//  override def toString: String = s"Operation($address : $balanceDelta)"
//
//  def primitiveOps: Set[PrimitiveOp] = {
//    Set(new PrimitiveOp(address, balanceDelta))
//  }
//}

///** The currency operation of moving some amount from `address` to `receiverAddress`.
//  * `balanceDelta` should be negative assuming that the currency has been moved from address``
//  * (decreasing its balance) to `receiverAddress` (increasing its balance).
//  */
//class Transfer(val senderAddress: String, val receiverAddress: String, balanceDelta: BigDecimal)
//  extends Operation(senderAddress, -balanceDelta) {
//
//  override def toString: String = s"Transfer($senderAddress -> $receiverAddress: $balanceDelta)"
//
//  override def primitiveOps = {
//    val value: Set[PrimitiveOp] = super.primitiveOps
//    value + new PrimitiveOp(receiverAddress, balanceDelta)
//
//    //      super.primitiveOps() + new PrimitiveOp(receiverAddress, -balanceDelta)
//  }
//}
//
//protected class PrimitiveOp(val address: String, val balanceDelta: BigDecimal) {}

/** A transfer from `from` to `to`. */
final case class Transfer(from: Option[String],
                          to: Option[String],
                          currency: Asset,
                          amount: BigDecimal,
                          tr: Transaction) {
  require(from != null, from)
  require(to != null, to)
  require(from.nonEmpty || to.nonEmpty, "At least fromHash or toHash must be non-empty!")

  require(currency != null, currency)
  require(amount != null && amount >= 0, amount)

  require(tr != null, tr)

  lazy val isMint: Boolean = from.isEmpty

  lazy val isBurn: Boolean = to.isEmpty
}
