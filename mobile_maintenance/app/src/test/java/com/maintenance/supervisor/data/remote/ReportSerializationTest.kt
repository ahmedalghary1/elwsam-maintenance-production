package com.maintenance.supervisor.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportSerializationTest {
    @Test fun `report uses API snake case fields`() {
        val report = ReportInput("6ba7b810-9dad-11d1-80b4-00c04fd430c8", 4, "2026-09-17", "2026-09-17T08:00:00Z", "2026-09-17T09:00:00Z", "2026-09-17T09:00:00Z", listOf(AnswerInput(7, true, "")))
        val json = Json.encodeToString(report)
        assertTrue(json.contains("\"client_report_id\"")); assertTrue(json.contains("\"checklist_item_id\""))
    }
    @Test fun `asset selection uses API asset id field`() {
        assertEquals("{\"asset_id\":42}", Json.encodeToString(CurrentAssetSelectionRequest(42)))
    }
    @Test fun `report with emergency items serializes correctly`() {
        val emergency = listOf(EmergencyMaintenanceInput(5, "عطل في الموتور", "فني الصيانة", "تم الإصلاح"))
        val report = ReportInput(
            "6ba7b810-9dad-11d1-80b4-00c04fd430c8", 4, "2026-09-17",
            "2026-09-17T08:00:00Z", "2026-09-17T09:00:00Z", "2026-09-17T09:00:00Z",
            listOf(AnswerInput(7, true, "")),
            emergency
        )
        val json = Json.encodeToString(report)
        assertTrue(json.contains("\"emergency_items\""))
        assertTrue(json.contains("\"issue_description\""))
        assertTrue(json.contains("\"responsible_person\""))
        assertTrue(json.contains("\"عطل في الموتور\""))
    }
    @Test fun `report with personnel fields serializes correctly`() {
        val report = ReportInput(
            clientReportId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
            assetId = 4,
            reportDate = "2026-09-17",
            startedAtDevice = "2026-09-17T08:00:00Z",
            completedAtDevice = "2026-09-17T09:00:00Z",
            lastModifiedAtDevice = "2026-09-17T09:00:00Z",
            answers = listOf(AnswerInput(7, true, "")),
            cleanerName = "محمد حسن",
            mechanicalTechnician = "أحمد إبراهيم",
            electricalTechnician = "محمود السيد",
            maintenanceManager = "م. خالد عبد الرحمن"
        )
        val json = Json.encodeToString(report)
        assertTrue(json.contains("\"cleaner_name\":\"محمد حسن\""))
        assertTrue(json.contains("\"mechanical_technician\":\"أحمد إبراهيم\""))
        assertTrue(json.contains("\"electrical_technician\":\"محمود السيد\""))
        assertTrue(json.contains("\"maintenance_manager\":\"م. خالد عبد الرحمن\""))
    }
}

