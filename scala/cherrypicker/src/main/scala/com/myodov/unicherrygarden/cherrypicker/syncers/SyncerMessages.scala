package com.myodov.unicherrygarden.cherrypicker.syncers

import com.myodov.unicherrygarden.api.dlt

object SyncerMessages {

  // Sealed to make message matches exhaustive
  sealed trait Message

  sealed trait HeadSyncerMessage
    extends Message

  sealed trait TailSyncerMessage
    extends Message

  /** The message to inform about the new Ethereum node sync status (equivalent to `eth.syncing`). */
  final case class EthereumNodeStatus(currentBlock: Int,
                                      highestBlock: Int)
    extends HeadSyncerMessage with TailSyncerMessage

  sealed trait IterateSyncer[M <: Message] extends Message

  /** The message to run the next iteration (likely after some delay). */
  final case class IterateHeadSyncer()
    extends HeadSyncerMessage with IterateSyncer[HeadSyncerMessage]

  final case class IterateTailSyncer()
    extends TailSyncerMessage with IterateSyncer[TailSyncerMessage]

  /** The message from TailSyncer to HeadSyncer, notifying about what range HeadSyncer is going to sync. */
  final case class GoingToTailSync(range: dlt.EthereumBlock.BlockNumberRange) extends HeadSyncerMessage

}