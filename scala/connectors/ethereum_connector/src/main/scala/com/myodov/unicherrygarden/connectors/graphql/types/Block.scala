package com.myodov.unicherrygarden.connectors.graphql.types

import caliban.Geth.Block
import caliban.client.SelectionBuilder
import com.myodov.unicherrygarden.Tools.Implicits._
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
    val eachSingleBlockValid: Boolean = blocks.forall(validateBlock)
    val blocksInTotalValid: Boolean = blocks.forAllPairs { (bl1, bl2) =>
      bl2.parent match {
        case None =>
          logger.error(s"In block sequence of ${blocks.size} blocks, " +
            s"block parent is missing: $bl2")
          false
        case Some(bl2Parent) =>
          if (bl2.number != bl1.number + 1) {
            logger.error(s"In block sequence of ${blocks.size} blocks, " +
              s"these two blocks are not subsequent: $bl1 and $bl2")
            false
          } else if (bl2Parent.number != bl1.number || bl2Parent.hash != bl1.hash) {
            logger.error(s"In block sequence of ${blocks.size} blocks, " +
              s"the parent reference of second block is not the the first block: $bl1 and $bl2")
            false
          } else {
            // Everything seems fine
            true
          }
      }
    }

    if (!eachSingleBlockValid) {
      logger.error(s"In block sequence of ${blocks.size} blocks, " +
        s"some block is invalid")
      false
    } else if (!blocksInTotalValid) {
      logger.error(s"In block sequence of ${blocks.size} blocks, " +
        s"not all pairs are valid")
      false
    } else {
      // Finally, we are cool
      true
    }
  }
}
