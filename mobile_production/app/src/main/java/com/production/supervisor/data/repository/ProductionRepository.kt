package com.production.supervisor.data.repository

import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.entity.*
import com.production.supervisor.data.remote.api.ProductionApiService
import com.production.supervisor.data.remote.dto.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductionRepository @Inject constructor(
    private val api: ProductionApiService,
    private val dao: ProductionDao,
    private val tokenManager: com.production.supervisor.data.local.datastore.TokenManager
) {

    fun getAssetsFlow(): Flow<List<AssetEntity>> = dao.getAssetsFlow()
    fun getProductsFlow(): Flow<List<ProductEntity>> = dao.getProductsFlow()
    fun getOperatorsFlow(): Flow<List<OperatorEntity>> = dao.getOperatorsFlow()
    fun getEntriesFlow(reportId: String): Flow<List<MachineEntryEntity>> = dao.getEntriesForReportFlow(reportId)
    fun getStoppagesFlow(reportId: String): Flow<List<StoppageEntity>> = dao.getStoppagesForReportFlow(reportId)
    fun getShiftReportFlow(reportId: String): Flow<ShiftReportEntity?> = dao.getShiftReportFlow(reportId)

    suspend fun refreshBootstrap(factoryId: String? = null): Result<ProductionBootstrapDto> {
        return try {
            val targetFactoryId = factoryId ?: tokenManager.getSelectedFactoryId()
            val response = api.getBootstrap(targetFactoryId)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // Save selected factory
                tokenManager.saveSelectedFactory(data.factory.id.toString(), data.factory.name)

                // Build lookup maps for safe resolution
                val productMap = data.products.associateBy { it.id }
                val operatorMap = data.operators.associateBy { it.id }

                // Map and insert Assets with fallback resolution
                val assetEntities = data.assets.map { dto ->
                    val defaultProdId = dto.productionDefault?.defaultProduct
                    val resolvedProdName = dto.productionDefault?.defaultProductName?.takeIf { it.isNotBlank() }
                        ?: defaultProdId?.let { productMap[it]?.name }
                    val defaultOpId = dto.productionDefault?.defaultOperator
                    val resolvedOpName = dto.productionDefault?.defaultOperatorName?.takeIf { it.isNotBlank() }
                        ?: defaultOpId?.let { operatorMap[it]?.name }

                    AssetEntity(
                        id = dto.id,
                        assetCode = dto.assetCode,
                        assetType = dto.assetType,
                        assetTypeDisplay = dto.assetTypeDisplay,
                        maintenanceTitle = dto.productionTitle ?: dto.maintenanceTitle,
                        sequenceOrder = dto.sequenceOrder,
                        defaultProductId = defaultProdId,
                        defaultProductName = resolvedProdName,
                        defaultOperatorId = defaultOpId,
                        defaultOperatorName = resolvedOpName,
                        originalCavities = dto.productionDefault?.originalCavities ?: 1,
                        coolingTimeSeconds = dto.productionDefault?.coolingTimeSeconds ?: 0.0,
                        cycleTimeSeconds = dto.productionDefault?.cycleTimeSeconds ?: 0.0,
                        targetCycleProduction = dto.productionDefault?.targetCycleProduction ?: 0.0,
                        isActive = dto.isActive
                    )
                }
                dao.deleteAllAssets()
                dao.insertAssets(assetEntities)

                // Map and insert Products
                val productEntities = data.products.map { dto ->
                    ProductEntity(
                        id = dto.id,
                        name = dto.name,
                        code = dto.code,
                        weightPerPieceGrams = dto.weightPerPieceGrams,
                        isActive = dto.isActive
                    )
                }
                dao.deleteAllProducts()
                dao.insertProducts(productEntities)

                // Map and insert Operators
                val operatorEntities = data.operators.map { dto ->
                    OperatorEntity(
                        id = dto.id,
                        name = dto.name,
                        phone = dto.phone,
                        isActive = dto.isActive
                    )
                }
                dao.deleteAllOperators()
                dao.insertOperators(operatorEntities)

                Result.success(data)
            } else {
                val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "فشل في تحميل البيانات الأساسية (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateShiftReport(date: String, shift: String): ShiftReportEntity {
        val existing = dao.findShiftReport(date, shift)
        if (existing != null) {
            return existing
        }
        val newReport = ShiftReportEntity(
            clientReportId = UUID.randomUUID().toString(),
            shift = shift,
            reportDate = date,
            startedAtDevice = "${date}T${LocalTime.now()}",
            status = "PENDING_HANDOVER",
            isSynced = false
        )
        dao.upsertShiftReport(newReport)
        return newReport
    }

    suspend fun saveMachineEntry(entry: MachineEntryEntity) {
        dao.upsertMachineEntry(entry)
        // Mark report as unsynced
        dao.updateReportSyncStatus(entry.clientReportId, synced = false, status = "PENDING_HANDOVER")
        // Trigger background sync
        syncReportNow(entry.clientReportId)
    }

    suspend fun addStoppage(stoppage: StoppageEntity) {
        dao.insertStoppage(stoppage)
        dao.updateReportSyncStatus(stoppage.clientReportId, synced = false, status = "PENDING_HANDOVER")
        syncReportNow(stoppage.clientReportId)
    }

    suspend fun addFridayPrayerStoppage(clientReportId: String) {
        val prayerStoppage = StoppageEntity(
            clientReportId = clientReportId,
            assetId = null,
            assetCode = "كافة الماكينات",
            stoppageType = "FRIDAY_PRAYER",
            stoppageTypeDisplay = "صلاة الجمعة",
            description = "إيقاف صلاة الجمعة لجميع الماكينات بالمصنع",
            durationMinutes = 60,
            actionTaken = "إيقاف مؤقت واستئناف الإنتاج بعد انتهاء الصلاة"
        )
        addStoppage(prayerStoppage)
    }

    suspend fun finishShift(clientReportId: String, notes: String): Result<Unit> {
        return try {
            val existing = dao.getShiftReport(clientReportId)
            val nowTime = "${LocalDate.now()}T${LocalTime.now()}"
            val reportToSave = existing?.copy(
                completedAtDevice = nowTime,
                status = "PENDING_HANDOVER",
                generalNotes = notes,
                isSynced = false
            ) ?: ShiftReportEntity(
                clientReportId = clientReportId,
                shift = "FIRST",
                reportDate = LocalDate.now().toString(),
                startedAtDevice = nowTime,
                completedAtDevice = nowTime,
                status = "PENDING_HANDOVER",
                generalNotes = notes,
                isSynced = false
            )
            dao.upsertShiftReport(reportToSave)
            syncReportNow(clientReportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmHandover(clientReportId: String, notes: String): Result<Unit> {
        return try {
            val res = api.confirmHandover(ConfirmHandoverInputDto(clientReportId, notes))
            if (res.isSuccessful) {
                dao.updateReportSyncStatus(clientReportId, synced = true, status = "CONFIRMED")
                Result.success(Unit)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "فشل في تأكيد الاستلام"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncReportNow(clientReportId: String): Result<Unit> {
        return try {
            val entries = dao.getEntriesForReport(clientReportId)
            val stoppages = dao.getStoppagesForReport(clientReportId)

            val inputDto = SyncShiftReportInputDto(
                clientReportId = clientReportId,
                shift = "FIRST",
                reportDate = LocalDate.now().toString(),
                status = "PENDING_HANDOVER",
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
                        rawMaterial = it.rawMaterial,
                        finalProductionWeightKg = it.finalProductionWeightKg,
                        targetCycleProduction = it.targetCycleProduction,
                        packagingType = it.packagingType,
                        notes = it.notes
                    )
                },
                stoppages = stoppages.map {
                    StoppageInputDto(
                        assetId = it.assetId,
                        stoppageType = it.stoppageType,
                        description = it.description,
                        durationMinutes = it.durationMinutes,
                        actionTaken = it.actionTaken
                    )
                }
            )

            val response = api.syncShiftReport(inputDto)
            if (response.isSuccessful) {
                dao.updateReportSyncStatus(clientReportId, synced = true, status = response.body()?.status ?: "PENDING_HANDOVER")
                Result.success(Unit)
            } else {
                Result.failure(Exception("تعذر إرسال التقرير للسيرفر"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
