package com.production.supervisor.ui.screens.home

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity

/** An invalid edited row must not silently fall back to its previously saved value. */
fun resolveReportEntries(saved: Map<Int, MachineEntryEntity>, drafts: Map<Int, MachineEntryEntity?>): Map<Int, MachineEntryEntity?> = saved + drafts

/** A zero-quantity settings edit or a factory-wide stoppage does not complete a machine. */
fun isMachineRecorded(entry: MachineEntryEntity?, stoppages: List<StoppageEntity>): Boolean {
    if (entry == null) return false
    return entry.finalProductionQuantity > 0 || entry.finalProductionWeightKg > 0 ||
        stoppages.any { it.assetId == entry.assetId && it.durationMinutes > 0 }
}
