package com.myodov.unicherrygarden

import java.time.Instant

import com.myodov.unicherrygarden.api.DBStorage.Progress
import com.myodov.unicherrygarden.api.types.SystemStatus

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Any actor/component of the cluster, such as: CherryGardener, CherryPicker, CherryPlanter.
 */
trait CherryGardenComponent {
  @inline
  final def buildSystemSyncStatus(ethereumNodeStatusOpt: Option[SystemStatus.Blockchain],
                                  progressOpt: Option[Progress.ProgressData]
                                 ): SystemStatus =
    new SystemStatus(
      Instant.now,
      ethereumNodeStatusOpt.orNull,
      progressOpt
        .flatMap(pr => (pr.blocks.to, pr.perCurrencyTrackedAddresses.maxTo, pr.perCurrencyTrackedAddresses.minTo) match {
          case (Some(blocksTo), Some(partiallySynced), Some(fullySynced)) =>
            Some(SystemStatus.CherryPicker.create(blocksTo, partiallySynced, fullySynced))
          case other =>
            None
        })
        .orNull
    )
}


object CherryGardenComponent {
  val BLOCK_ITERATION_PERIOD = 10 seconds // Each block is generated about once per 13 seconds, let’s be safe
}
