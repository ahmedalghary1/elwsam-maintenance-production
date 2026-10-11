package com.production.supervisor.data.repository

import com.production.supervisor.data.local.dao.ProductionDao
import com.production.supervisor.data.local.dao.ReportSnapshot
import com.production.supervisor.data.local.entity.*
import com.production.supervisor.data.remote.api.ProductionApiService
import com.production.supervisor.data.remote.dto.*
import com.production.supervisor.data.sync.ProductionSyncScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductionRepository @Inject constructor(
    private val api: ProductionApiService,
    private val dao: ProductionDao,
    private val tokenManager: com.production.supervisor.data.local.datastore.TokenManager,
    private val synchronizer: ReportSynchronizer,
    private val syncScheduler: ProductionSyncScheduler
) {
    init {
        syncScheduler.scheduleRecovery()
    }

    suspend fun getCachedShift(): String? = tokenManager.getAssignedShift()
    suspend fun getCachedFactoryName(): String? = tokenManager.getSelectedFactoryName()
    suspend fun getCachedFactoryId(): Int? = tokenManager.getSelectedFactoryId()?.toIntOrNull()

    fun getAssetsFlow(): Flow<List<AssetEntity>> = dao.getAssetsFlow()
    fun getProductsFlow(): Flow<List<ProductEntity>> = dao.getProductsFlow()
    fun getOperatorsFlow(): Flow<List<OperatorEntity>> = dao.getOperatorsFlow()
    fun getProductionOptionsFlow(): Flow<List<ProductionOptionEntity>> = dao.getProductionOptionsFlow()
    fun getEntriesFlow(reportId: String): Flow<List<MachineEntryEntity>> = dao.getEntriesForReportFlow(reportId)
    fun getStoppagesFlow(reportId: String): Flow<List<StoppageEntity>> = dao.getStoppagesForReportFlow(reportId)
    fun getShiftReportFlow(reportId: String): Flow<ShiftReportEntity?> = dao.getShiftReportFlow(reportId)

    suspend fun refreshBootstrap(factoryId: String? = null): Result<ProductionBootstrapDto> {
        return try {
            val previousFactoryId = getCachedFactoryId()
            val targetFactoryId = factoryId ?: previousFactoryId?.toString()
            val response = api.getBootstrap(targetFactoryId)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // Legacy drafts belong to the factory selected before this bootstrap,
                // including when an administrator switches to a different factory.
                dao.assignLegacyReportFactory(previousFactoryId ?: data.factory.id)

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
                        defaultRawMaterialId = dto.productionDefault?.defaultRawMaterial,
                        defaultRawMaterialName = dto.productionDefault?.defaultRawMaterialName,
                        defaultFinalUnitId = dto.productionDefault?.defaultFinalUnit,
                        defaultFinalUnitName = dto.productionDefault?.defaultFinalUnitName,
                        targetCycleUnitId = dto.productionDefault?.targetCycleUnit,
                        targetCycleUnitName = dto.productionDefault?.targetCycleUnitName,
                        defaultPackagingId = dto.productionDefault?.defaultPackaging,
                        defaultPackagingName = dto.productionDefault?.defaultPackagingName,
                        originalCavities = dto.productionDefault?.originalCavities ?: 1,
                        coolingTimeSeconds = dto.productionDefault?.coolingTimeSeconds ?: 0.0,
                        cycleTimeSeconds = dto.productionDefault?.cycleTimeSeconds ?: 0.0,
                        targetCycleProduction = dto.productionDefault?.targetCycleProduction ?: 0.0,
                        isActive = dto.isActive
                    )
                }
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
                // Map and insert Operators
                val operatorEntities = data.operators.map { dto ->
                    OperatorEntity(
                        id = dto.id,
                        name = dto.name,
                        phone = dto.phone,
                        isActive = dto.isActive
                    )
                }
                val optionEntities = data.productionOptions.map { dto ->
                    ProductionOptionEntity(dto.id, dto.category, dto.name, dto.kgPerUnit)
                }
                dao.replaceBootstrapCache(assetEntities, productEntities, operatorEntities, optionEntities)
                tokenManager.saveSelectedFactory(data.factory.id.toString(), data.factory.name)
                tokenManager.saveAssignedShift(data.user.shift)
                dao.restoreServerReports(data.todayReports.filter {
                    it.completedAtDevice != null || it.status == "CONFIRMED" || it.supervisor == data.user.id
                }.map {
                    val report = ShiftReportEntity(
                        clientReportId = it.clientReportId,
                        factoryId = data.factory.id,
                        shift = it.shift,
                        reportDate = it.reportDate,
                        startedAtDevice = it.startedAtDevice ?: "${it.reportDate}T00:00:00",
                        completedAtDevice = it.completedAtDevice,
                        status = it.status,
                        generalNotes = it.generalNotes,
                        isSynced = true,
                        handoverConfirmedAt = it.handoverConfirmedAt,
                        handoverToSupervisorName = it.handoverToSupervisorName,
                        handoverNotes = it.handoverNotes
                    )
                    ReportSnapshot(
                        report = report,
                        entries = it.machineEntries.map { entry ->
                            MachineEntryEntity(
                                clientReportId = it.clientReportId,
                                assetId = entry.asset,
                                assetCode = entry.assetCode,
                                operatorId = entry.operator,
                                operatorName = entry.operatorName,
                                originalOperatorId = entry.originalOperator,
                                originalOperatorName = entry.originalOperatorName,
                                operatorChanged = entry.operatorChanged,
                                productId = entry.product,
                                productName = entry.productName,
                                originalProductId = entry.originalProduct,
                                originalProductName = entry.originalProductName,
                                productChanged = entry.productChanged,
                                originalCavities = entry.originalCavities,
                                currentCavities = entry.currentCavities,
                                operationMode = entry.operationMode,
                                coolingTimeSeconds = entry.coolingTimeSeconds,
                                cycleTimeSeconds = entry.cycleTimeSeconds,
                                originalCoolingTimeSeconds = entry.originalCoolingTimeSeconds ?: entry.coolingTimeSeconds,
                                originalCycleTimeSeconds = entry.originalCycleTimeSeconds ?: entry.cycleTimeSeconds,
                                rawMaterial = entry.rawMaterial,
                                rawMaterialOptionId = optionEntities.firstOrNull { option ->
                                    option.category == "RAW_MATERIAL" && option.name == entry.rawMaterial
                                }?.id,
                                finalProductionWeightKg = entry.finalProductionWeightKg,
                                finalProductionQuantity = entry.finalProductionQuantity ?: entry.finalProductionWeightKg,
                                finalProductionUnitId = entry.finalProductionUnit,
                                finalProductionUnitName = entry.finalProductionUnitName,
                                targetCycleProduction = entry.targetCycleProduction,
                                targetCycleUnitName = entry.targetCycleUnitName,
                                packagingType = entry.packagingType,
                                packagingOptionId = optionEntities.firstOrNull { option ->
                                    option.category == "PACKAGING" && option.name == entry.packagingType
                                }?.id,
                                notes = entry.notes
                            )
                        },
                        stoppages = it.stoppages.map { stoppage ->
                            StoppageEntity(
                                clientReportId = it.clientReportId,
                                assetId = stoppage.asset,
                                assetCode = stoppage.assetCode,
                                stoppageType = stoppage.stoppageType,
                                stoppageTypeDisplay = stoppage.stoppageTypeDisplay,
                                description = stoppage.description,
                                durationMinutes = stoppage.durationMinutes,
                                actionTaken = stoppage.actionTaken
                            )
                        }
                    )
                })
                requestPendingSync()
                Result.success(data)
            } else {
                val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "فشل في تحميل البيانات الأساسية (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrCreateShiftReport(date: String, shift: String): ShiftReportEntity {
        val factoryId = getCachedFactoryId()
            ?: throw IllegalStateException("اختر المصنع وحمّل بياناته قبل تسجيل التقرير.")
        dao.assignLegacyReportFactory(factoryId)
        val newReport = ShiftReportEntity(
            clientReportId = UUID.randomUUID().toString(),
            shift = shift,
            reportDate = date,
            factoryId = factoryId,
            startedAtDevice = LocalDateTime.now(ZoneId.of("Africa/Cairo")).toString(),
            status = "DRAFT",
            isSynced = false
        )
        return dao.getOrInsertShiftReport(newReport)
    }

    suspend fun saveMachineEntry(entry: MachineEntryEntity) {
        dao.saveEntryLocally(entry)
        requestSync(entry.clientReportId)
    }

    suspend fun addStoppage(stoppage: StoppageEntity) {
        dao.saveStoppageLocally(stoppage)
        requestSync(stoppage.clientReportId)
    }

    suspend fun saveStoppedMachine(entry: MachineEntryEntity, stoppage: StoppageEntity) {
        dao.saveStoppedMachineLocally(entry, stoppage)
        requestSync(entry.clientReportId)
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
            dao.finishReportLocally(
                clientReportId, notes, LocalDateTime.now(ZoneId.of("Africa/Cairo")).toString()
            )
            requestSync(clientReportId)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmHandover(clientReportId: String, notes: String): Result<Unit> =
        synchronizer.confirmHandover(clientReportId, notes)

    fun requestSync(clientReportId: String) = syncScheduler.requestSync(clientReportId)

    private suspend fun requestPendingSync() {
        dao.getUnsyncedReports().filter { it.factoryId != null }.forEach { requestSync(it.clientReportId) }
    }

    suspend fun syncReportNow(clientReportId: String): Result<Unit> = synchronizer.syncReportNow(clientReportId)

    suspend fun syncPendingReports(): Result<Unit> {
        var firstFailure: Throwable? = null
        dao.getUnsyncedReports().forEach {
            val result = syncReportNow(it.clientReportId)
            if (result.isFailure && firstFailure == null) firstFailure = result.exceptionOrNull()
        }
        return firstFailure?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
