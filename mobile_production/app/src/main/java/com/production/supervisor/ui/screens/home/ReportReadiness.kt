package com.production.supervisor.ui.screens.home

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity

/** A zero-quantity settings edit or a factory-wide stoppage does not complete a machine. */
fun isMachineRecorded(entry: MachineEntryEntity?, stoppages: List<StoppageEntity>): Boolean {
    if (entry == null) return false
    return entry.finalProductionQuantity > 0 || entry.finalProductionWeightKg > 0 ||
        stoppages.any { it.assetId == entry.assetId && it.durationMinutes > 0 }
}
