package com.maintenance.supervisor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetNamingTest {
    @Test
    fun renamedTypeFromDashboardIsShownWithoutLegacySubstitution() {
        val asset = Asset(7, "7", "سوستة 7", "سوستة", 1)

        assertEquals("سوستة", asset.normalizedTypeName)
        assertEquals("الصيانة الدورية لـ سوستة 7", asset.maintenanceTitle)
    }

    @Test
    fun machineTypeNameIsNotDuplicatedInTitle() {
        val asset = Asset(8, "8", "ماكينة سوستة 8", "ماكينة سوستة", 2)

        assertEquals("الصيانة الدورية لـ ماكينة سوستة 8", asset.maintenanceTitle)
    }
}
