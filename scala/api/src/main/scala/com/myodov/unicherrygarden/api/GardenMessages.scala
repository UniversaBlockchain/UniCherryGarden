package com.myodov.unicherrygarden.api

import com.myodov.unicherrygarden.api.types.SystemStatus
import com.myodov.unicherrygarden.messages.{CherryGardenerRequest, CherryPickerRequest}

object GardenMessages {

  // Sealed to make message matches exhaustive
  sealed trait Message

  sealed trait SyncerMessage
    extends Message

  sealed trait HeadSyncerMessage
    extends SyncerMessage

  sealed trait TailSyncerMessage
    extends SyncerMessage

  /** The message to inform about the new Ethereum node sync status (equivalent to `eth.syncing`). */
  final case class EthereumNodeStatus(nodeStatus: SystemStatus.Blockchain)
    extends HeadSyncerMessage with TailSyncerMessage with CherryPickerRequest with CherryGardenerRequest

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
