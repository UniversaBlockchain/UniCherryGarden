package com.myodov.unicherrygarden.connectors.graphql

import caliban.Geth.Block
import caliban.client.SelectionBuilder
import com.typesafe.scalalogging.LazyLogging

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

object BlockBasic extends LazyLogging {
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

  /** Check if a single block is well-formed. */
  def validateBlock(block: BlockBasicView): Boolean = {
    // Different validations depending on whether parent is Some(block) or None:
    // “parent is absent” may happen only on the block 0;
    // “parent is not absent” implies the parent block has number lower by one.
    val parentIsConsistent: Boolean = block.parent match {
      case None => block.number == 0
      case Some(parentBlock) => parentBlock.number == block.number - 1
    }

    val transactionsAreConsistent: Boolean = block.transactions match {
        // If the transactions are not available at all – that’s legit
        case None => true
        // If the transactions are available - all of them must refer to the same block
        case Some(trs) => TransactionFull.validateTransactions(trs, block.asMinimalBlock)
      }

    if (!parentIsConsistent) {
      logger.error(s"For the following block, parent is inconsistent: $block")
      false
    } else if (!transactionsAreConsistent) {
      logger.error(s"For the following block, transactions are inconsistent: $block")
      false
    } else {
      true
    }
  }

  /** Check if a sequence of blocks is well-formed. */
  def validateBlocks(blocks: Seq[BlockBasicView]): Boolean = {
    blocks.forall(validateBlock)
  }
}
