package com.maintenance.supervisor.data.repository

import com.maintenance.supervisor.data.local.*
import com.maintenance.supervisor.data.remote.MaintenanceApi
import com.maintenance.supervisor.data.security.MaintenanceSession
import com.maintenance.supervisor.data.security.TokenStore
import com.maintenance.supervisor.domain.model.SyncStatus
import com.maintenance.supervisor.domain.repository.AppResult
import io.mockk.*
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MaintenanceAccountTest {
    private val dao = mockk<MaintenanceDao>(relaxed = true)
    private val db = mockk<AppDatabase> { every { maintenanceDao() } returns dao }
    private val api = mockk<MaintenanceApi>()
    private val tokens = mockk<TokenStore>()
    private val activeUser = MutableStateFlow<Int?>(2)
    private val clock = Clock.fixed(Instant.parse("2026-11-02T18:00:00Z"), ZoneId.of("Africa/Cairo"))
    private val saved = ReportWithAnswers(
        MaintenanceReportEntity("saved-uuid", 2, 10, 4, "2026-11-02",
            "2026-11-02T08:00:00+02:00", "2026-11-02T09:00:00+02:00",
            "2026-11-02T09:00:00+02:00", SyncStatus.SYNCED.name),
        listOf(MaintenanceAnswerEntity("saved-uuid", 7, true, "saved answer"))
    )

    private fun repository(report: ReportWithAnswers = saved, cachedUserId: Int = 2): MaintenanceRepositoryImpl {
        every { tokens.userId } returns activeUser
        coEvery { tokens.hasSession() } returns true
        every { dao.observeUser() } returns flowOf(UserEntity(cachedUserId, "phone-$cachedUserId", "GENERAL_SUPERVISOR", 1, "User $cachedUserId"))
        every { dao.observeFactory() } returns flowOf(FactoryEntity(1, "factory", "F1"))
        every { dao.observeAssets() } returns flowOf(listOf(AssetEntity(4, "4", "machine", "machine", 1, 6, true)))
        every { dao.observeCurrent() } returns flowOf(CurrentMaintenanceEntity(assetId = 4, reportDate = "2026-11-02", position = 1, total = 1))
        every { dao.observeReports() } returns flowOf(listOf(report))
        every { dao.observeMetadata(any()) } returns flowOf(null)
        every { dao.observeMetadata("can_submit_report") } returns flowOf("true")
        every { dao.observeMetadata("server_report_id") } returns flowOf(report.report.clientReportId)
        coEvery { dao.checklist(6) } returns listOf(SectionWithItems(ChecklistSectionEntity(8, 6, "section", 1), listOf(ChecklistItemEntity(7, 8, "item", 1))))
        coEvery { dao.todayReport("2026-11-02") } returns report
        return MaintenanceRepositoryImpl(db, api, clock, MaintenanceSession(db, tokens), tokens)
    }

    @Test fun completedReportOpensAfterMorningDeadlineWithAnswersAndWithoutUpload() = runTest {
        val repo = repository()
        val result = repo.startOrLoadToday()
        assertTrue(result is AppResult.Success)
        val report = (result as AppResult.Success).value
        assertEquals("saved-uuid", report.clientReportId)
        assertTrue(report.isLocked)
        assertEquals("saved answer", report.answers.single().note)
        coVerify(exactly = 0) { api.syncReports(any()) }
        coVerify(exactly = 0) { dao.deleteReport(any()) }
    }

    @Test fun coworkerReportIsVisibleButReadOnlyInCurrentAccount() = runTest {
        val repo = repository(saved.copy(report = saved.report.copy(ownerUserId = 1)))
        val home = repo.observeHome().first()
        assertEquals("User 2", home.user?.name)
        assertEquals("saved-uuid", home.daily?.report?.clientReportId)
        assertTrue(home.daily!!.report!!.isLocked)
        assertTrue((repo.startOrLoadToday() as AppResult.Success).value.isLocked)
    }

    @Test fun oldIdentityIsHiddenAndCannotUploadWhenNewAccountBootstrapFails() = runTest {
        val repo = repository(cachedUserId = 1)
        coEvery { api.bootstrap() } throws IOException("offline")
        assertNull(repo.observeHome().first().user)
        assertNull(repo.observeHome().first().daily)
        assertTrue(repo.sync() is AppResult.Error)
        coVerify(exactly = 0) { dao.pendingReports() }
        coVerify(exactly = 0) { api.syncReports(any()) }
    }

    @Test fun loggingOutImmediatelyHidesCachedIdentity() = runTest {
        val repo = repository()
        activeUser.value = null
        assertNull(repo.observeHome().first().user)
        assertNull(repo.observeHome().first().daily)
    }
}
