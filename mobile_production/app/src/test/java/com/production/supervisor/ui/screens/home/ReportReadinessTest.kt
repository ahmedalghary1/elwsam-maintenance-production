package com.production.supervisor.ui.screens.home

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportReadinessTest {
    private val entry = MachineEntryEntity(clientReportId = "report", assetId = 4, assetCode = "M4")

    @Test fun submissionIncludesUnsavedMachinesAndLatestEdits() {
        val old = entry.copy(finalProductionQuantity = 5.0)
        val edited = old.copy(finalProductionQuantity = 12.0)
        val next = entry.copy(assetId = 5, finalProductionQuantity = 9.0)
        val result = resolveReportEntries(mapOf(4 to old), mapOf(4 to edited, 5 to next))
        assertEquals(edited, result[4])
        assertEquals(next, result[5])
        assertTrue(result.values.all { isMachineRecorded(it, emptyList()) })
    }

    @Test fun invalidUnsavedEditBlocksPreviouslyCompletedMachine() {
        val saved = entry.copy(finalProductionQuantity = 5.0)
        val result = resolveReportEntries(mapOf(4 to saved), mapOf(4 to null))
        assertNull(result[4])
        assertFalse(isMachineRecorded(result[4], emptyList()))
        assertEquals(saved, resolveReportEntries(mapOf(4 to saved), emptyMap())[4])
    }

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

    @Test fun machineMarkedAsStoppedIsAccountedForWithoutEntry() {
        // Machine 4 has no entry at all, but is marked as stopped
        assertTrue(isMachineAccountedFor(4, null, emptyList(), setOf(4)))
        // When toggle is removed, it is no longer accounted for
        assertFalse(isMachineAccountedFor(4, null, emptyList(), emptySet()))
    }

    @Test fun normalRecordedMachineIsAccountedForRegardlessOfStoppedSet() {
        val recorded = entry.copy(finalProductionQuantity = 25.0)
        assertTrue(isMachineAccountedFor(4, recorded, emptyList(), emptySet()))
        assertTrue(isMachineAccountedFor(4, recorded, emptyList(), setOf(4)))
    }

    @Test fun allPackagingUnitDropdownOptionsCompleteMachine() {
        val packagingOptions = listOf("كرتونة", "كيلو", "شيكاره", "قطعة")
        for (unit in packagingOptions) {
            val machineEntry = entry.copy(finalProductionQuantity = 10.0, finalProductionUnitName = unit)
            assertTrue("Unit $unit should complete the machine", isMachineRecorded(machineEntry, emptyList()))
        }
    }

    @Test fun machineWithReasonStoppageAccountedFor() {
        val stoppageWithReason = StoppageEntity(
            clientReportId = "report",
            assetId = 4,
            assetCode = "M4",
            stoppageType = "MACHINE_BREAKDOWN",
            stoppageTypeDisplay = "عطل ماكينة",
            description = "عطل كهربائي في الموتور",
            durationMinutes = 480
        )
        assertTrue(isMachineRecorded(entry, listOf(stoppageWithReason)))
        assertTrue(isMachineAccountedFor(4, null, listOf(stoppageWithReason), setOf(4)))
    }
}
