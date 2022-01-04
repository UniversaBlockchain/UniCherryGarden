package com.myodov.unicherrygarden.cherrypicker.syncers

import com.myodov.unicherrygarden.api.dlt
import com.myodov.unicherrygarden.api.types.SystemSyncStatus
import com.myodov.unicherrygarden.messages.CherryPickerRequest

object SyncerMessages {

  // Sealed to make message matches exhaustive
  sealed trait Message

  sealed trait HeadSyncerMessage
    extends Message

  sealed trait TailSyncerMessage
    extends Message

  /** The message to inform about the new Ethereum node sync status (equivalent to `eth.syncing`). */
  final case class EthereumNodeStatus(nodeStatus: SystemSyncStatus.Blockchain)
    extends HeadSyncerMessage with TailSyncerMessage with CherryPickerRequest

  sealed trait IterateSyncer[M <: Message] extends Message

  /** The message to run the next iteration (likely after some delay). */
  final case class IterateHeadSyncer()
    extends HeadSyncerMessage with IterateSyncer[HeadSyncerMessage]

  final case class IterateTailSyncer()
    extends TailSyncerMessage with IterateSyncer[TailSyncerMessage]

  /** The message from TailSyncer to HeadSyncer, notifying about what range HeadSyncer is tail syncing.
   * If `Option` is `None`, it means TailSyncer is not going to sync anything.
   */
  final case class TailSyncing(range: Option[dlt.EthereumBlock.BlockNumberRange]) extends HeadSyncerMessage

}
