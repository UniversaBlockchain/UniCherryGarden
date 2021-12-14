package com.myodov.unicherrygarden.connectors.graphql

import caliban.Geth.Block
import caliban.client.SelectionBuilder

/** For a Block, get just its number and hash.
 *
 * Hash is selected for referential integrity only.
 */
case class BlockMinimalView(number: Long,
                            hash: String)

object BlockMinimal {
  /** A shorthand method to select the minimal block data to query. */
  lazy val view: SelectionBuilder[Block, BlockMinimalView] = {
    Block.number ~
      Block.hash
  }.mapN(BlockMinimalView)
}

/** For a Block, select most of the information needed for our processing. */
case class BlockBasicView(number: Long,
                          hash: String,
                          parent: Option[BlockMinimalView],
                          timestamp: Long,
                          transactions: Option[List[TransactionFullView]]
                         ) {
  lazy val asMinimalBlock: BlockMinimalView = BlockMinimalView(number, hash)
}

object BlockBasic {
  /** A shorthand method to select the basic block data. */
  lazy val view: SelectionBuilder[Block, BlockBasicView] = {
    Block.number ~
      Block.hash ~
      Block.parent {
        BlockMinimal.view
      } ~
      Block.timestamp ~
      Block.transactions {
        TransactionFull.view
      }
  }.mapN(BlockBasicView)
}
