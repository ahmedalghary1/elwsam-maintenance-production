package com.production.supervisor.data.repository

import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.dao.ReportSnapshot
import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.ShiftReportEntity
import com.production.supervisor.data.remote.api.ProductionApiService
import com.production.supervisor.data.remote.dto.*
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class ReportSynchronizerTest {
    private class LocalReport {
        var snapshot = ReportSnapshot(
            ShiftReportEntity("stable-uuid", "SECOND", "2026-10-11", "2026-10-11T16:00:00", factoryId = 7, localRevision = 1),
            listOf(MachineEntryEntity(clientReportId = "stable-uuid", assetId = 4, assetCode = "4", finalProductionWeightKg = 10.0)),
            emptyList()
        )
        var rejectedAcknowledgements = 0
        val dao: ProductionDao = Proxy.newProxyInstance(
            ProductionDao::class.java.classLoader, arrayOf(ProductionDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getReportSnapshot" -> snapshot
                "updateReportSyncStatus" -> {
                    snapshot = snapshot.copy(report = snapshot.report.copy(isSynced = args!![1] as Boolean, status = args[2] as String))
                    Unit
                }
                "markSnapshotSynced" -> {
                    if (snapshot.report.localRevision == args!![1] as Long) {
                        snapshot = snapshot.copy(report = snapshot.report.copy(isSynced = true, status = args[2] as String))
                        1
                    } else {
                        rejectedAcknowledgements++
                        assertFalse(snapshot.report.isSynced)
                        0
                    }
                }
                else -> error("Unexpected DAO operation ${method.name}")
            }
        } as ProductionDao
    }

    private class FakeApi : ProductionApiService {
        val uploads = mutableListOf<SyncShiftReportInputDto>()
        var send: suspend (SyncShiftReportInputDto) -> Response<ShiftReportDto> = { input ->
            Response.success(ShiftReportDto(1, input.clientReportId, input.shift, reportDate = input.reportDate, status = input.status))
        }
        override suspend fun syncShiftReport(body: SyncShiftReportInputDto): Response<ShiftReportDto> {
            uploads += body
            return send(body)
        }
        override suspend fun getBootstrap(factoryId: String?): Response<ProductionBootstrapDto> = error("Not used")
        var review: suspend (ConfirmHandoverInputDto) -> Response<ShiftReportDto> = { error("Not used") }
        override suspend fun confirmHandover(body: ConfirmHandoverInputDto): Response<ShiftReportDto> = review(body)
    }

    @Test fun rejectionSendsReasonAndPreservesServerDecision() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        api.review = { input ->
            assertEquals("REJECT", input.decision)
            assertEquals("Quantity mismatch", input.handoverNotes)
            assertEquals("stable-uuid", input.clientReportId)
            val json = Json.encodeToString(input)
            assertTrue(json.contains("handover_notes"))
            assertTrue(json.contains("REJECT"))
            Response.success(ShiftReportDto(1, input.clientReportId, "FIRST", reportDate = "2026-10-13", status = "REJECTED"))
        }
        assertTrue(ReportSynchronizer(api, local.dao).confirmHandover("stable-uuid", "Quantity mismatch", "REJECT").isSuccess)
        assertEquals("REJECTED", local.snapshot.report.status)
    }

    @Test fun failedReviewDoesNotMarkReportReviewed() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        api.review = { throw java.io.IOException("offline") }
        assertTrue(ReportSynchronizer(api, local.dao).confirmHandover("stable-uuid", "", "ACCEPT").isFailure)
        assertEquals("DRAFT", local.snapshot.report.status)
        assertFalse(local.snapshot.report.isSynced)
    }

    @Test fun changeDuringUploadStaysDirtyUntilItsOwnSnapshotIsSent() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        api.send = { input ->
            if (api.uploads.size == 1) {
                local.snapshot = local.snapshot.copy(
                    report = local.snapshot.report.copy(localRevision = 2),
                    entries = local.snapshot.entries.map { it.copy(finalProductionWeightKg = 35.0) }
                )
            }
            Response.success(ShiftReportDto(1, input.clientReportId, input.shift, reportDate = input.reportDate, status = input.status))
        }
        assertTrue(ReportSynchronizer(api, local.dao).syncReportNow("stable-uuid").isSuccess)
        assertEquals(listOf(10.0, 35.0), api.uploads.map { it.entries.single().finalProductionWeightKg })
        assertEquals(1, local.rejectedAcknowledgements)
        assertTrue(local.snapshot.report.isSynced)
        assertTrue(api.uploads.all { it.clientReportId == "stable-uuid" && it.factoryId == 7 })
    }

    @Test fun simultaneousRequestsCoalesceAndNeverUploadConcurrently() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        var active = 0
        var peak = 0
        api.send = { input ->
            active++
            peak = maxOf(peak, active)
            delay(100)
            active--
            Response.success(ShiftReportDto(1, input.clientReportId, input.shift, reportDate = input.reportDate, status = input.status))
        }
        val synchronizer = ReportSynchronizer(api, local.dao)
        val results = List(5) { async { synchronizer.syncReportNow("stable-uuid") } }.awaitAll()
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, api.uploads.size)
        assertEquals(1, peak)
    }

    @Test fun failedUploadKeepsDataAndUuidForRetry() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        api.send = { throw java.io.IOException("offline") }
        val synchronizer = ReportSynchronizer(api, local.dao)
        assertTrue(synchronizer.syncReportNow("stable-uuid").isFailure)
        assertFalse(local.snapshot.report.isSynced)
        assertEquals(10.0, local.snapshot.entries.single().finalProductionWeightKg, 0.0)
        api.send = { input ->
            Response.success(ShiftReportDto(1, input.clientReportId, input.shift, reportDate = input.reportDate, status = input.status))
        }
        assertTrue(synchronizer.syncReportNow("stable-uuid").isSuccess)
        assertEquals(listOf("stable-uuid", "stable-uuid"), api.uploads.map { it.clientReportId })
    }

    @Test fun cancellationPropagatesAndDoesNotAcknowledgeUpload() = runTest {
        val local = LocalReport()
        val api = FakeApi()
        val waiting = CompletableDeferred<Response<ShiftReportDto>>()
        api.send = { waiting.await() }
        val synchronizer = ReportSynchronizer(api, local.dao)
        val upload = async { synchronizer.syncReportNow("stable-uuid") }
        runCurrent()
        upload.cancel()
        try {
            upload.await()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertFalse(local.snapshot.report.isSynced)
        }
    }

    @Test fun payloadPreservesIndependentOriginalAndCurrentSettingsIncludingZero() {
        val local = LocalReport()
        local.snapshot = local.snapshot.copy(entries = local.snapshot.entries.map {
            it.copy(originalCavities = 12, currentCavities = 8, originalCoolingTimeSeconds = 4.5,
                coolingTimeSeconds = 0.0, originalCycleTimeSeconds = 19.0, cycleTimeSeconds = 15.25)
        })
        val input = local.snapshot.toInput().entries.single()
        assertEquals(12, input.originalCavities)
        assertEquals(8, input.currentCavities)
        assertEquals(4.5, input.originalCoolingTimeSeconds, 0.0)
        assertEquals(0.0, input.coolingTimeSeconds, 0.0)
        assertEquals(19.0, input.originalCycleTimeSeconds, 0.0)
        assertEquals(15.25, input.cycleTimeSeconds, 0.0)
        val json = Json { encodeDefaults = true }.parseToJsonElement(Json { encodeDefaults = true }.encodeToString(input)).jsonObject
        assertEquals("4.5", json["original_cooling_time_seconds"].toString())
        assertEquals("19.0", json["original_cycle_time_seconds"].toString())
        assertEquals("0.0", json["cooling_time_seconds"].toString())
    }
}
