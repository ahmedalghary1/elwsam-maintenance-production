package com.maintenance.supervisor.domain.model

object MaintenanceCycle {
    fun <T> assetAt(orderedAssets: List<T>, baseIndex: Int, completedPreviousDays: Int): T? {
        if (orderedAssets.isEmpty()) return null
        return orderedAssets[(baseIndex.coerceAtLeast(0) + completedPreviousDays).mod(orderedAssets.size)]
    }
}
