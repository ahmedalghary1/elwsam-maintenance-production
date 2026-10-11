package com.production.supervisor.ui.screens.home

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportReadinessTest {
    private val entry = MachineEntryEntity(clientReportId = "report", assetId = 4, assetCode = "M4")

    @Test fun nonWeightUnitCompletesMachine() {
        assertTrue(isMachineRecorded(entry.copy(finalProductionQuantity = 12.0, finalProductionUnitName = "قطعة"), emptyList()))
    }

    @Test fun settingsOnlyEntryDoesNotCompleteMachine() {
        assertFalse(isMachineRecorded(entry.copy(currentCavities = 2), emptyList()))
    }

    @Test fun generalStoppageDoesNotCompleteMachine() {
        val general = StoppageEntity(clientReportId = "report", assetId = null, durationMinutes = 60)
        assertFalse(isMachineRecorded(entry, listOf(general)))
    }

    @Test fun stoppedMachineRequiresItsOwnEntryAndPositiveDuration() {
        val stop = StoppageEntity(clientReportId = "report", assetId = 4, durationMinutes = 30)
        assertFalse(isMachineRecorded(null, listOf(stop)))
        assertFalse(isMachineRecorded(entry, listOf(stop.copy(assetId = 9))))
        assertFalse(isMachineRecorded(entry, listOf(stop.copy(durationMinutes = 0))))
        assertTrue(isMachineRecorded(entry, listOf(stop)))
    }
}
