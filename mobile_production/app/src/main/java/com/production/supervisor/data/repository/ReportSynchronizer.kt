package com.production.supervisor.data.repository

import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.dao.ReportSnapshot
import com.production.supervisor.data.remote.api.ProductionApiService
import com.production.supervisor.data.remote.dto.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes uploads and only acknowledges the exact local revision that was sent. */
@Singleton
class ReportSynchronizer @Inject constructor(
    private val api: ProductionApiService,
    private val dao: ProductionDao
) {
    private val uploadMutex = Mutex()

    suspend fun syncReportNow(reportId: String): Result<Unit> = uploadMutex.withLock {
        try {
            while (true) {
                val snapshot = dao.getReportSnapshot(reportId)
                    ?: return@withLock Result.failure(IllegalStateException("التقرير غير موجود."))
                if (snapshot.report.isSynced) return@withLock Result.success(Unit)
                check(snapshot.report.factoryId != null) { "افتح بيانات المصنع قبل إرسال التقرير." }
                val response = api.syncShiftReport(snapshot.toInput())
                if (!response.isSuccessful) {
                    return@withLock Result.failure(
                        IllegalStateException("تعذر إرسال التقرير (${response.code()}). البيانات محفوظة على الهاتف.")
                    )
                }
                val acknowledged = dao.markSnapshotSynced(
                    reportId, snapshot.report.localRevision,
                    response.body()?.status ?: snapshot.report.status
                )
                // A machine can be edited during the HTTP request. Send the newer snapshot,
                // without falsely marking it synced or allowing an older request to overtake it.
                if (acknowledged > 0) return@withLock Result.success(Unit)
            }
            @Suppress("UNREACHABLE_CODE")
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun confirmHandover(reportId: String, notes: String): Result<Unit> = uploadMutex.withLock {
        try {
            val response = api.confirmHandover(ConfirmHandoverInputDto(reportId, notes))
            if (!response.isSuccessful) {
                Result.failure(IllegalStateException("تعذر تأكيد استلام الوردية. حاول مرة أخرى."))
            } else {
                dao.updateReportSyncStatus(reportId, synced = true, status = "CONFIRMED")
                Result.success(Unit)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}

internal fun ReportSnapshot.toInput() = SyncShiftReportInputDto(
    clientReportId = report.clientReportId,
    factoryId = report.factoryId,
    shift = report.shift,
    reportDate = report.reportDate,
    startedAtDevice = report.startedAtDevice,
    completedAtDevice = report.completedAtDevice,
    status = report.status,
    generalNotes = report.generalNotes,
    entries = entries.map {
        MachineEntryInputDto(
            assetId = it.assetId,
            operatorId = it.operatorId,
            operatorName = it.operatorName,
            originalOperatorId = it.originalOperatorId,
            originalOperatorName = it.originalOperatorName,
            operatorChanged = it.operatorChanged,
            productId = it.productId,
            productName = it.productName,
            originalProductId = it.originalProductId,
            originalProductName = it.originalProductName,
            productChanged = it.productChanged,
            originalCavities = it.originalCavities,
            currentCavities = it.currentCavities,
            operationMode = it.operationMode,
            coolingTimeSeconds = it.coolingTimeSeconds,
            cycleTimeSeconds = it.cycleTimeSeconds,
            originalCoolingTimeSeconds = it.originalCoolingTimeSeconds,
            originalCycleTimeSeconds = it.originalCycleTimeSeconds,
            rawMaterial = it.rawMaterial,
            rawMaterialOptionId = it.rawMaterialOptionId,
            finalProductionWeightKg = it.finalProductionWeightKg,
            finalProductionQuantity = it.finalProductionQuantity,
            finalProductionUnitId = it.finalProductionUnitId,
            targetCycleProduction = it.targetCycleProduction,
            packagingType = it.packagingType,
            packagingOptionId = it.packagingOptionId,
            notes = it.notes
        )
    },
    stoppages = stoppages.map {
        StoppageInputDto(it.assetId, it.stoppageType, it.description, it.durationMinutes, it.actionTaken)
    }
)
