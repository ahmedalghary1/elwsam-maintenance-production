package com.production.supervisor.ui.screens.home

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity

/** An invalid edited row must not silently fall back to its previously saved value. */
fun resolveReportEntries(saved: Map<Int, MachineEntryEntity>, drafts: Map<Int, MachineEntryEntity?>): Map<Int, MachineEntryEntity?> = saved + drafts

fun isMachineRecorded(entry: MachineEntryEntity?, stoppages: List<StoppageEntity>): Boolean {
    if (entry == null) return false
    return entry.finalProductionQuantity > 0 || entry.finalProductionWeightKg > 0 ||
        stoppages.any { it.assetId == entry.assetId && it.durationMinutes > 0 }
}

/** A machine is accounted for if it has production/stoppage recorded OR if it was explicitly marked as stopped. */
fun isMachineAccountedFor(
    assetId: Int,
    entry: MachineEntryEntity?,
    stoppages: List<StoppageEntity>,
    stoppedAssetIds: Set<Int>
): Boolean {
    if (stoppedAssetIds.contains(assetId)) return true
    return isMachineRecorded(entry, stoppages)
}
