package com.production.supervisor.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetTitleTest {
    @Test
    fun productionTitleUsesRenamedServerType() {
        val asset = AssetEntity(
            id = 7, assetCode = "7", assetType = "SPRING_MACHINE",
            assetTypeDisplay = "سوستة", maintenanceTitle = "إنتاج سوستة 7",
            sequenceOrder = 1, defaultProductId = null, defaultProductName = null,
            originalCavities = 1, coolingTimeSeconds = 0.0,
            cycleTimeSeconds = 0.0, targetCycleProduction = 0.0, isActive = true,
        )

        assertEquals("إنتاج سوستة 7", asset.productionTitle)
    }
}
