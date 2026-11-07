package com.maintenance.supervisor.data.repository

import androidx.room.withTransaction
import com.maintenance.supervisor.data.local.*
import com.maintenance.supervisor.data.remote.*
import com.maintenance.supervisor.domain.model.*
import com.maintenance.supervisor.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.maintenance.supervisor.data.security.MaintenanceSession
import com.maintenance.supervisor.data.security.TokenStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val api: MaintenanceApi,
    private val clock: Clock,
    private val session: MaintenanceSession,
    private val tokens: TokenStore
) : MaintenanceRepository {
    private val dao = db.maintenanceDao()
    private val cairo = ZoneId.of("Africa/Cairo")
    private val reportWriteMutex get() = session.mutex

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        dao.observeUser(), dao.observeFactory(), dao.observeAssets(), dao.observeCurrent(), dao.observeReports(), dao.observeMetadata("last_sync"), dao.observeMetadata("selection_mode"),
        tokens.userId, dao.observeMetadata("can_submit_report"), dao.observeMetadata("server_report_id")
    ) { values ->
        val user = values[0] as UserEntity?
        val factory = values[1] as FactoryEntity?
        val assets = values[2] as List<AssetEntity>
        val current = values[3] as CurrentMaintenanceEntity?
        val reports = values[4] as List<ReportWithAnswers>
        val lastSync = values[5] as String?
        val selectionMode = values[6] as String? ?: "automatic"
        val activeUserId = values[7] as Int?
        if (user == null || user.id != activeUserId) return@combine HomeSnapshot(null, null, null, null)
        val canSubmit = values[8] == "true"
        val serverReportId = (values[9] as String?)?.takeIf { it.isNotBlank() }
        val today = LocalDate.now(clock.withZone(cairo))
        val isMaintenanceDay = today.dayOfWeek != DayOfWeek.FRIDAY

        // We only consider reports for today.
        val todayReports = reports.filter { LocalDate.parse(it.report.reportDate) == today }
        val completedToday = todayReports
            .filter { it.report.completedAtDevice != null }
            .maxByOrNull { it.report.lastModifiedAtDevice }
        val draftToday = todayReports
            .filter { it.report.completedAtDevice == null }
            .maxByOrNull { it.report.lastModifiedAtDevice }

        val baseIndex = assets.indexOfFirst { it.id == current?.assetId }
        val isManual = selectionMode == "manual"
        val advances = if (!isManual && current != null && LocalDate.parse(current.reportDate) < today) {
            reports.count {
                it.report.completedAtDevice != null &&
                LocalDate.parse(it.report.reportDate) < today &&
                !LocalDate.parse(it.report.reportDate).isBefore(LocalDate.parse(current.reportDate))
            }
        } else 0

        // If a report was completed for today, keep it.
        // If an uncompleted local draft exists for today, keep it ONLY if it matches the current asset.
        // If the server changed the current asset (e.g. from dashboard), the draft for the old asset is superseded.
        val chosenReport = when {
            serverReportId != null -> todayReports.firstOrNull { it.report.clientReportId == serverReportId } ?: completedToday ?: draftToday
            completedToday != null -> completedToday
            draftToday != null && (current == null || current.assetId == draftToday.report.assetId) -> draftToday
            else -> null
        }

        val asset = chosenReport?.let { r -> assets.firstOrNull { it.id == r.report.assetId } }
            ?: current?.takeIf { baseIndex >= 0 }?.let {
                if (isManual) assets[baseIndex] else MaintenanceCycle.assetAt(assets, baseIndex, advances)
            }
        val daily = if (!isMaintenanceDay) null else asset?.let {
            val sections = it.checklistTemplateId?.let { id -> dao.checklist(id) }.orEmpty().map { relation ->
                ChecklistSection(relation.section.id, relation.section.title, relation.section.sequence,
                    relation.items.sortedBy { item -> item.sequence }.map { item -> ChecklistItem(item.id, item.sectionId, item.text, item.sequence) })
            }
            val report = chosenReport?.toDomain(today, !canSubmit || !morningWindowOpen() || chosenReport.report.ownerUserId != activeUserId)?.let { saved ->
                val savedAnswers = saved.answers.associateBy { answer -> answer.checklistItemId }
                saved.copy(answers = sections.flatMap { section -> section.items }.map { item ->
                    savedAnswers[item.id] ?: MaintenanceAnswer(item.id, false, "")
                })
            }
            val total = current?.total?.takeIf { count -> count > 0 } ?: assets.size
            val position = if (isManual && baseIndex >= 0) {
                baseIndex + 1
            } else {
                current?.position?.let { start -> if (total > 0) ((start - 1 + advances).mod(total)) + 1 else start } ?: (assets.indexOf(it) + 1)
            }
            DailyMaintenance(it.toDomain(), chosenReport?.report?.reportDate?.let(LocalDate::parse) ?: today,
                position, total, sections, report)
        }
        HomeSnapshot(user?.let { u -> factory?.let { u.toDomain(it) } }, factory?.toDomain(), daily, lastSync, assets.map { it.toDomain() }, isMaintenanceDay, selectionMode)
    }

    override suspend fun bootstrap(): AppResult<Unit> = session.mutex.withLock { bootstrapInternal() }

    private suspend fun bootstrapInternal(): AppResult<Unit> {
        return try {
        // Don't skip bootstrap if there's a pending local draft — we still need fresh data
        if (!tokens.hasSession()) return AppResult.Error("سجّل الدخول لتحميل بيانات الصيانة.")
        val value = api.bootstrap()
        val localDraft = dao.openReport(value.user.id)
        db.withTransaction {
            dao.clearUsers(); dao.clearFactories()
            dao.upsertFactory(FactoryEntity(value.factory.id, value.factory.name, value.factory.code))
            dao.upsertUser(UserEntity(value.user.id, value.user.phone, value.user.role, value.user.factory.id, value.user.name))
            dao.clearItems(); dao.clearSections(); dao.clearTemplates(); dao.clearAssets()
            dao.upsertTemplates(value.checklistTemplates.map { ChecklistTemplateEntity(it.id, it.name) })
            dao.upsertSections(value.checklistTemplates.flatMap { t -> t.sections.map { ChecklistSectionEntity(it.id, t.id, it.title, it.sequence) } })
            dao.upsertItems(value.checklistTemplates.flatMap { it.sections }.flatMap { s -> s.items.map { ChecklistItemEntity(it.id, s.id, it.text, it.sequence) } })

            val templateCodes = value.checklistTemplates.associate { it.code to it.id }
            fun getTemplateId(type: String): Int? {
                val code = when (type) {
                    "REGULAR_MACHINE", "PRESS" -> "STANDARD"
                    "SPRING_MACHINE" -> "SPRING"
                    else -> "STANDARD"
                }
                return templateCodes[code]
            }

            // The server list is already in canonical cycle order. Its
            // sequence_order repeats for each asset type, so use list order.
            dao.upsertAssets(value.assets.mapIndexed { index, it ->
                AssetEntity(it.id, it.code, it.name, it.typeName, index + 1, it.checklistTemplateId ?: getTemplateId(it.assetType), it.isActive)
            })

            // Extract current asset info from the bootstrap response
            val currentAssetId = value.asset?.id
            val currentReportDate = value.serverDate
            if (currentAssetId != null && currentReportDate != null) {
                dao.upsertCurrent(CurrentMaintenanceEntity(assetId = currentAssetId, reportDate = currentReportDate, position = value.cyclePosition.takeIf { it > 0 } ?: 1, total = value.cycleTotal.takeIf { it > 0 } ?: value.assets.size))
            } else dao.clearCurrent()

            // If a local draft exists for today, but the server assigned a different asset (e.g. changed via dashboard):
            // The uncompleted draft for the old asset must be deleted so the newly assigned asset is active.
            if (localDraft != null && localDraft.completedAtDevice == null && currentAssetId != null && currentReportDate != null) {
                if (localDraft.reportDate == currentReportDate && localDraft.assetId != currentAssetId) {
                    dao.deleteReport(localDraft.clientReportId)
                }
            }

            // Save server report for today if it exists and there's no local draft for same day
            val serverReport = value.report
            dao.putMetadata(MetadataEntity("server_report_id", serverReport?.clientReportId.orEmpty()))
            val localServerCopy = serverReport?.let { dao.report(it.clientReportId)?.report }
            // Preserve the current owner's unsent revision, but refresh synced server data.
            if (serverReport != null && (localServerCopy == null || localServerCopy.syncStatus == SyncStatus.SYNCED.name)) {
                val today = LocalDate.now(clock.withZone(cairo))
                val reportDate = LocalDate.parse(serverReport.reportDate)
                val isLocked = serverReport.isLocked || reportDate < today

                dao.upsertReport(MaintenanceReportEntity(
                    clientReportId = serverReport.clientReportId,
                    ownerUserId = serverReport.supervisorId ?: value.user.id,
                    serverId = serverReport.id,
                    assetId = serverReport.asset?.id ?: currentAssetId ?: 0,
                    reportDate = serverReport.reportDate,
                    startedAtDevice = serverReport.startedAtDevice,
                    completedAtDevice = serverReport.completedAtDevice,
                    lastModifiedAtDevice = serverReport.lastModifiedAtDevice,
                    syncStatus = SyncStatus.SYNCED.name,
                    isLocked = isLocked,
                    cleanerName = serverReport.cleanerName,
                    mechanicalTechnician = serverReport.mechanicalTechnician,
                    electricalTechnician = serverReport.electricalTechnician,
                    maintenanceManager = serverReport.maintenanceManager
                ))
                // Save the answers from server report
                if (serverReport.answers.isNotEmpty()) {
                    dao.upsertAnswers(serverReport.answers.map {
                        MaintenanceAnswerEntity(serverReport.clientReportId, it.checklistItemId, it.checked, it.note)
                    })
                }
                // Save emergency items from server report
                dao.deleteEmergencyForReport(serverReport.clientReportId)
                if (serverReport.emergencyItems.isNotEmpty()) {
                    dao.upsertEmergencyMaintenances(serverReport.emergencyItems.map {
                        EmergencyMaintenanceEntity(
                            clientReportId = serverReport.clientReportId,
                            assetId = it.asset?.id ?: it.assetId ?: 0,
                            issueDescription = it.issueDescription,
                            responsiblePerson = it.responsiblePerson,
                            notes = it.notes
                        )
                    })
                }
            }

            dao.putMetadata(MetadataEntity("last_sync", OffsetDateTime.now(clock).toString()))
            dao.putMetadata(MetadataEntity("selection_mode", value.selectionMode))
            dao.putMetadata(MetadataEntity("can_submit_report", value.canSubmitReport.toString()))
        }
        tokens.bindUser(value.user.id)
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        if (t is retrofit2.HttpException && t.code() == 403) {
            AppResult.Error("لا توجد صلاحية للوصول إلى صيانة هذا المصنع.", t)
        } else AppResult.Error("تعذر تحميل بيانات المصنع. يمكنك متابعة العمل بالبيانات المحفوظة.", t)
    }

    }

    override suspend fun startOrLoadToday(): AppResult<MaintenanceReport> = session.mutex.withLock { startOrLoadTodayInternal() }

    private suspend fun startOrLoadTodayInternal(): AppResult<MaintenanceReport> {
        return try {
        val today = LocalDate.now(clock.withZone(cairo))
        if (today.dayOfWeek == DayOfWeek.FRIDAY) return AppResult.Error("الجمعة عطلة الصيانة الأسبوعية. ستظهر الماكينة نفسها في يوم العمل التالي.")
        val todayStr = today.toString()
        val home = observeHome().first()
        val snapshot = home.daily ?: return AppResult.Error("قم بمزامنة بيانات الحساب الحالي أولًا.")

        // First: check if there's already a report for today (draft or synced)
        val existingReport = dao.todayReport(todayStr)
        // Opening a saved report is always allowed; editability is computed separately.
        if (existingReport != null && (existingReport.report.completedAtDevice != null || !canEdit(existingReport.report))) {
            return AppResult.Success(existingReport.toDomain(today, !canEdit(existingReport.report)))
        }
        if (!morningWindowOpen() || dao.observeMetadata("can_submit_report").first() != "true") {
            return AppResult.Error("تسجيل الصيانة متاح لمشرف الوردية الصباحية وحتى ساعتين بعد نهايتها فقط.")
        }
        if (existingReport != null) {
            // If the report was already completed, return it
            if (existingReport.report.completedAtDevice != null) {
                return AppResult.Success(existingReport.toDomain(today))
            }
            // If it's an uncompleted draft matching the current snapshot asset, resume it
            if (existingReport.report.assetId == snapshot.asset.id) {
                return AppResult.Success(existingReport.toDomain(today))
            }
            // Obsolete draft for a previous asset that was changed: delete and start fresh
            dao.deleteReport(existingReport.report.clientReportId)
        }

        // Create a new draft for the current snapshot asset
        if (snapshot.sections.isEmpty() || snapshot.sections.all { it.items.isEmpty() }) {
            return AppResult.Error("قائمة الفحص غير متاحة لهذه الماكينة. قم بالمزامنة ثم أعد المحاولة.")
        }
        val now = OffsetDateTime.now(clock)
        val ownerId = home.user?.id ?: return AppResult.Error("أعد تسجيل الدخول.")
        val report = MaintenanceReportEntity(UUID.randomUUID().toString(), ownerId, null, snapshot.asset.id, todayStr, now.toString(), null, now.toString(), SyncStatus.LOCAL_DRAFT.name)
        val answers = snapshot.sections.flatMap { it.items }.map { MaintenanceAnswerEntity(report.clientReportId, it.id, false, "") }
        db.withTransaction { dao.upsertReport(report); dao.upsertAnswers(answers) }
        AppResult.Success(dao.report(report.clientReportId)!!.toDomain(today))
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Error("لا توجد بيانات صيانة جاهزة. قم بالمزامنة أولًا.", t) }

    }

    override suspend fun selectCurrentAsset(assetId: Int): AppResult<Unit> = session.mutex.withLock { selectCurrentAssetInternal(assetId) }

    private suspend fun selectCurrentAssetInternal(assetId: Int): AppResult<Unit> {
        return try {
            val today = LocalDate.now(clock.withZone(cairo))
            val todayStr = today.toString()
            val existing = dao.todayReport(todayStr)
            if (today.dayOfWeek == DayOfWeek.FRIDAY) {
                AppResult.Error("لا يمكن تغيير ماكينة الصيانة يوم الجمعة لأنه عطلة أسبوعية.")
            } else if (existing?.report?.completedAtDevice != null) {
                AppResult.Error("تم إكمال تقرير اليوم بالفعل. لا يمكن تغيير الماكينة بعد إرسال الفحص.")
            } else {
                api.selectCurrentAsset(CurrentAssetSelectionRequest(assetId))
                // Only discard the superseded draft once the server accepted the selection.
                if (existing != null) dao.deleteReport(existing.report.clientReportId)
                bootstrapInternal()
            }
        } catch (t: Throwable) {
        if (t is CancellationException) throw t
            val raw = (t as? retrofit2.HttpException)?.response()?.errorBody()?.string()
            val message = raw?.let { runCatching { JSONObject(it).optString("detail") }.getOrNull() }?.takeIf { it.isNotBlank() }
            AppResult.Error(message ?: "تعذر تغيير الماكينة. تحقق من الاتصال وحاول مرة أخرى.", t)
        }
    }

    override suspend fun saveAnswer(reportId: String, answer: MaintenanceAnswer) {
        reportWriteMutex.withLock { db.withTransaction {
            val existing = dao.report(reportId)?.report ?: return@withTransaction
            // Don't allow saving if report is locked
            if (!canEdit(existing)) return@withTransaction
            dao.upsertAnswers(listOf(MaintenanceAnswerEntity(reportId, answer.checklistItemId, answer.checked, answer.note)))
            dao.updateReport(existing.copy(lastModifiedAtDevice = OffsetDateTime.now(clock).toString(), syncStatus = SyncStatus.LOCAL_DRAFT.name, lastError = null))
        } }
    }

    override suspend fun saveEmergencyItems(reportId: String, items: List<EmergencyMaintenance>) {
        reportWriteMutex.withLock { db.withTransaction {
            val existing = dao.report(reportId)?.report ?: return@withTransaction
            if (!canEdit(existing)) return@withTransaction
            dao.deleteEmergencyForReport(reportId)
            if (items.isNotEmpty()) {
                dao.upsertEmergencyMaintenances(items.map {
                    EmergencyMaintenanceEntity(
                        clientReportId = reportId,
                        assetId = it.assetId,
                        issueDescription = it.issueDescription,
                        responsiblePerson = it.responsiblePerson,
                        notes = it.notes
                    )
                })
            }
            dao.updateReport(existing.copy(lastModifiedAtDevice = OffsetDateTime.now(clock).toString(), syncStatus = SyncStatus.LOCAL_DRAFT.name, lastError = null))
        } }
    }

    override suspend fun savePersonnel(reportId: String, cleaner: String, mechanical: String, electrical: String, manager: String) {
        reportWriteMutex.withLock { db.withTransaction {
            val existing = dao.report(reportId)?.report ?: return@withTransaction
            if (!canEdit(existing)) return@withTransaction
            val now = OffsetDateTime.now(clock).toString()
            dao.updateReport(existing.copy(
                cleanerName = cleaner.trim(),
                mechanicalTechnician = mechanical.trim(),
                electricalTechnician = electrical.trim(),
                maintenanceManager = manager.trim(),
                lastModifiedAtDevice = now,
                syncStatus = SyncStatus.LOCAL_DRAFT.name,
                lastError = null
            ))
        } }
    }

    override suspend fun completeReport(
        reportId: String,
        answers: List<MaintenanceAnswer>,
        emergencyItems: List<EmergencyMaintenance>,
        cleaner: String,
        mechanical: String,
        electrical: String,
        manager: String
    ): AppResult<Unit> = if (!morningWindowOpen()) {
        AppResult.Error("انتهى وقت تقرير الصيانة. المهلة ساعتان بعد الوردية الصباحية فقط.")
    } else try {
        reportWriteMutex.withLock { db.withTransaction {
            val report = dao.report(reportId)?.report ?: error("missing report")
            // Don't allow completing a locked report
            if (!canEdit(report)) error("التقرير للعرض فقط ولا يمكن تعديله بهذا الحساب أو في هذا الوقت.")
            val now = OffsetDateTime.now(clock).toString()
            dao.updateReport(report.copy(
                completedAtDevice = now,
                lastModifiedAtDevice = now,
                syncStatus = SyncStatus.PENDING_SYNC.name,
                lastError = null,
                cleanerName = cleaner.trim(),
                mechanicalTechnician = mechanical.trim(),
                electricalTechnician = electrical.trim(),
                maintenanceManager = manager.trim()
            ))
            // Flush the latest in-memory form snapshot atomically before sync.
            dao.upsertAnswers(answers.map {
                MaintenanceAnswerEntity(reportId, it.checklistItemId, it.checked, it.note)
            })
            dao.deleteEmergencyForReport(reportId)
            if (emergencyItems.isNotEmpty()) {
                dao.upsertEmergencyMaintenances(emergencyItems.map {
                    EmergencyMaintenanceEntity(
                        clientReportId = reportId,
                        assetId = it.assetId,
                        issueDescription = it.issueDescription,
                        responsiblePerson = it.responsiblePerson,
                        notes = it.notes
                    )
                })
            }
            dao.enqueue(SyncQueueEntity(clientReportId = reportId, enqueuedAt = now))
        } }
        AppResult.Success(Unit)
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Error("تعذر حفظ التقرير على الهاتف.", t) }

    private fun morningWindowOpen(): Boolean {
        val local = java.time.LocalTime.now(clock.withZone(cairo))
        return !local.isBefore(java.time.LocalTime.of(7, 0)) &&
            !local.isAfter(java.time.LocalTime.of(17, 0))
    }

    override suspend fun sync(): AppResult<Unit> = session.mutex.withLock { syncInternal() }

    private suspend fun syncInternal(): AppResult<Unit> {
        return try {
        if (!tokens.hasSession()) return AppResult.Success(Unit)
        val cachedUser = dao.observeUser().first()
        val sessionUserId = tokens.userId.first()
        // Never upload a cached user's reports under another account's tokens.
        if (sessionUserId == null || cachedUser?.id != sessionUserId) {
            val loaded = bootstrapInternal()
            if (loaded is AppResult.Error) return loaded
        }
        var retryableFailure: Throwable? = null
        val pending = dao.pendingReports()
        if (pending.isNotEmpty()) {
            pending.forEach { local ->
                dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCING.name)
                try {
                    val response = api.syncReports(BatchSyncRequest(listOf(local.toInput())))
                    val result = response.reports.firstOrNull { it.clientReportId == local.report.clientReportId }
                    when {
                        result == null -> {
                            val error = IllegalStateException("لم يؤكد الخادم استلام التقرير")
                            retryableFailure = error
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = error.message)
                        }
                        result.status.lowercase() in setOf("accepted", "created", "updated", "synced", "already_synced", "success") -> {
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCED.name, result.id)
                            dao.dequeue(local.report.clientReportId)
                        }
                        result.status.lowercase() in setOf("conflict") -> {
                            // A conflict is not a successful upload. Preserve the
                            // local answers and show the server explanation.
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, result.id, result.detail ?: "تعارض التقرير مع بيانات الخادم")
                            dao.dequeue(local.report.clientReportId)
                        }
                        result.status.lowercase() == "rejected" -> {
                            // Rejected — keep the error so user knows
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = result.detail ?: "تم رفض التقرير من الخادم")
                            dao.dequeue(local.report.clientReportId)
                        }
                        else -> dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = result.detail ?: result.status)
                    }
                } catch (t: retrofit2.HttpException) {
                    if (t.code() == 409) {
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "تعارض التقرير مع بيانات الخادم")
                        dao.dequeue(local.report.clientReportId)
                    } else if (t.code() == 400) {
                        val errorBody = t.response()?.errorBody()?.string() ?: "بيانات غير صالحة"
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "خطأ في البيانات: $errorBody")
                        dao.dequeue(local.report.clientReportId)
                    } else {
                        retryableFailure = t
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "تعذر الاتصال بالخادم (HTTP ${t.code()})")
                    }
                } catch (t: kotlinx.serialization.SerializationException) {
                    retryableFailure = t
                    dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "خطأ في تحليل الاستجابة")
                } catch (t: Throwable) {
        if (t is CancellationException) throw t
                    retryableFailure = t
                    dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "تعذر الاتصال بالخادم. سيُعاد الإرسال تلقائيًا.")
                }
            }
        }
        when (val pulled = bootstrapInternal()) {
            is AppResult.Error -> pulled
            is AppResult.Success -> retryableFailure?.let {
                AppResult.Error("تعذر إرسال التقرير الآن، وسيتم تكرار المحاولة تلقائيًا.", it)
            } ?: AppResult.Success(Unit)
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Error("تعذر الإرسال الآن، وسيتم المحاولة تلقائيًا.", t)
    }

    }

    override suspend fun resetTodayReport(): AppResult<Unit> = session.mutex.withLock { resetTodayReportInternal() }

    private suspend fun resetTodayReportInternal(): AppResult<Unit> = try {
        val today = LocalDate.now(clock.withZone(cairo)).toString()
        val existing = dao.todayReport(today)
        if (existing != null && !canEdit(existing.report)) {
            AppResult.Error("لا يمكن إعادة تعيين هذا التقرير لأنه مقفل.")
        } else {
            db.withTransaction {
                    if (existing != null) {
                        dao.deleteReport(existing.report.clientReportId)
                        dao.deleteEmergencyForReport(existing.report.clientReportId)
                        dao.dequeue(existing.report.clientReportId)
                    }
                    dao.deleteUnlockedReportsForDate(today)
            }
            bootstrapInternal()
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        AppResult.Error("تعذر إعادة التعيين: ${t.message}", t)
    }

    private suspend fun canEdit(report: MaintenanceReportEntity): Boolean =
        tokens.hasSession() && report.ownerUserId == tokens.userId.first() &&
            dao.observeUser().first()?.id == report.ownerUserId &&
            !report.isLocked && report.reportDate == LocalDate.now(clock.withZone(cairo)).toString() &&
            morningWindowOpen() && dao.observeMetadata("can_submit_report").first() == "true"
}

private fun FactoryEntity.toDomain() = Factory(id, name, code)
private fun UserEntity.toDomain(factory: FactoryEntity) = User(id, phone, role, factory.toDomain(), name)
private fun AssetEntity.toDomain() = Asset(id, code, name, typeName, sequence)
private fun ReportWithAnswers.toDomain(today: LocalDate = LocalDate.now(), readOnly: Boolean = false): MaintenanceReport {
    val reportDate = LocalDate.parse(report.reportDate)
    // Report is locked if server says so, or if the report date is in the past
    val locked = readOnly || report.isLocked || reportDate < today
    return MaintenanceReport(
        clientReportId = report.clientReportId,
        serverId = report.serverId,
        assetId = report.assetId,
        reportDate = reportDate,
        startedAt = OffsetDateTime.parse(report.startedAtDevice),
        completedAt = report.completedAtDevice?.let(OffsetDateTime::parse),
        lastModifiedAt = OffsetDateTime.parse(report.lastModifiedAtDevice),
        status = SyncStatus.valueOf(report.syncStatus),
        answers = answers.map { MaintenanceAnswer(it.checklistItemId, it.checked, it.note) },
        isLocked = locked,
        lastError = report.lastError,
        emergencyMaintenances = emergencyMaintenances.map { EmergencyMaintenance(it.id, it.assetId, it.issueDescription, it.responsiblePerson, it.notes) },
        cleanerName = report.cleanerName,
        mechanicalTechnician = report.mechanicalTechnician,
        electricalTechnician = report.electricalTechnician,
        maintenanceManager = report.maintenanceManager
    )
}
private fun ReportWithAnswers.toInput() = ReportInput(
    clientReportId = report.clientReportId,
    assetId = report.assetId,
    reportDate = report.reportDate,
    startedAtDevice = report.startedAtDevice,
    completedAtDevice = requireNotNull(report.completedAtDevice),
    lastModifiedAtDevice = report.lastModifiedAtDevice,
    cleanerName = report.cleanerName,
    mechanicalTechnician = report.mechanicalTechnician,
    electricalTechnician = report.electricalTechnician,
    maintenanceManager = report.maintenanceManager,
    answers = answers.map { AnswerInput(it.checklistItemId, it.checked, it.note) },
    emergencyItems = emergencyMaintenances.map { EmergencyMaintenanceInput(it.assetId, it.issueDescription, it.responsiblePerson, it.notes) }
)

