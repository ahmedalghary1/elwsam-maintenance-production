package com.production.supervisor.data.local.dao

import androidx.room.*
import com.production.supervisor.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class ReportSnapshot(
    val report: ShiftReportEntity,
    val entries: List<MachineEntryEntity>,
    val stoppages: List<StoppageEntity>
)

@Dao
interface ProductionDao {

    @Query("SELECT * FROM assets WHERE isActive = 1 ORDER BY sequenceOrder ASC, id ASC")
    fun getAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isActive = 1 ORDER BY sequenceOrder ASC, id ASC")
    suspend fun getAssets(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Query("DELETE FROM assets")
    suspend fun deleteAllAssets()

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("SELECT * FROM operators WHERE isActive = 1 ORDER BY name ASC")
    fun getOperatorsFlow(): Flow<List<OperatorEntity>>

    @Query("SELECT * FROM operators WHERE isActive = 1 ORDER BY name ASC")
    suspend fun getOperators(): List<OperatorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperators(operators: List<OperatorEntity>)

    @Query("DELETE FROM operators")
    suspend fun deleteAllOperators()

    @Query("SELECT * FROM production_options ORDER BY category ASC, name ASC")
    fun getProductionOptionsFlow(): Flow<List<ProductionOptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductionOptions(options: List<ProductionOptionEntity>)

    @Query("DELETE FROM production_options")
    suspend fun deleteAllProductionOptions()

    @Query("SELECT * FROM shift_reports WHERE clientReportId = :reportId")
    fun getShiftReportFlow(reportId: String): Flow<ShiftReportEntity?>

    @Query("SELECT * FROM shift_reports WHERE clientReportId = :reportId LIMIT 1")
    suspend fun getShiftReport(reportId: String): ShiftReportEntity?

    @Query("UPDATE shift_reports SET factoryId = :factoryId WHERE factoryId IS NULL")
    suspend fun assignLegacyReportFactory(factoryId: Int)

    @Query("SELECT * FROM shift_reports WHERE reportDate = :date AND shift = :shift AND factoryId = :factoryId AND ownerUserId = :ownerUserId ORDER BY startedAtDevice DESC LIMIT 1")
    suspend fun findShiftReport(date: String, shift: String, factoryId: Int, ownerUserId: Int): ShiftReportEntity?

    @Query("SELECT * FROM shift_reports WHERE isSynced = 0 AND ownerUserId = :ownerUserId")
    suspend fun getUnsyncedReports(ownerUserId: Int): List<ShiftReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShiftReport(report: ShiftReportEntity)

    @Update
    suspend fun updateShiftReport(report: ShiftReportEntity)

    @Query("DELETE FROM shift_reports WHERE clientReportId = :reportId AND isSynced = 1")
    suspend fun deleteSyncedReport(reportId: String)

    @Query("UPDATE shift_reports SET status = :status, generalNotes = :notes, completedAtDevice = :completedAt, isSynced = 0, localRevision = localRevision + 1 WHERE clientReportId = :reportId")
    suspend fun markReportFinished(reportId: String, status: String, notes: String, completedAt: String)

    @Query("UPDATE shift_reports SET isSynced = :synced, status = :status WHERE clientReportId = :reportId")
    suspend fun updateReportSyncStatus(reportId: String, synced: Boolean, status: String)

    @Query("UPDATE shift_reports SET isSynced = 0, localRevision = localRevision + 1 WHERE clientReportId = :reportId")
    suspend fun markReportDirty(reportId: String)

    @Query("UPDATE shift_reports SET isSynced = 1, status = :status WHERE clientReportId = :reportId AND localRevision = :revision")
    suspend fun markSnapshotSynced(reportId: String, revision: Long, status: String): Int

    @Transaction
    suspend fun getReportSnapshot(reportId: String): ReportSnapshot? {
        val report = getShiftReport(reportId) ?: return null
        return ReportSnapshot(report, getEntriesForReport(reportId), getStoppagesForReport(reportId))
    }

    @Transaction
    suspend fun getOrInsertShiftReport(report: ShiftReportEntity): ShiftReportEntity {
        return findShiftReport(
            report.reportDate,
            report.shift,
            requireNotNull(report.factoryId),
            requireNotNull(report.ownerUserId)
        )
            ?: report.also { upsertShiftReport(it) }
    }

    suspend fun requireEditableReport(reportId: String): ShiftReportEntity {
        val report = getShiftReport(reportId)
            ?: throw IllegalStateException("التقرير غير موجود. أعد فتح الوردية.")
        check(report.completedAtDevice == null && report.status != "CONFIRMED") {
            "تم إنهاء هذا التقرير ولا يمكن تعديله."
        }
        return report
    }

    @Transaction
    suspend fun saveEntryLocally(entry: MachineEntryEntity) {
        requireEditableReport(entry.clientReportId)
        upsertMachineEntry(entry)
        markReportDirty(entry.clientReportId)
    }

    @Transaction
    suspend fun saveStoppageLocally(stoppage: StoppageEntity) {
        requireEditableReport(stoppage.clientReportId)
        insertStoppage(stoppage)
        markReportDirty(stoppage.clientReportId)
    }

    @Transaction
    suspend fun saveStoppedMachineLocally(entry: MachineEntryEntity, stoppage: StoppageEntity) {
        require(entry.clientReportId == stoppage.clientReportId && entry.assetId == stoppage.assetId)
        requireEditableReport(entry.clientReportId)
        upsertMachineEntry(entry)
        insertStoppage(stoppage)
        markReportDirty(entry.clientReportId)
    }

    @Transaction
    suspend fun saveEntriesAndFinish(reportId: String, entries: List<MachineEntryEntity>, notes: String, completedAt: String) {
        requireEditableReport(reportId)
        require(entries.isNotEmpty() && entries.all { it.clientReportId == reportId })
        entries.forEach { upsertMachineEntry(it) }
        markReportFinished(reportId, "PENDING_HANDOVER", notes, completedAt)
    }

    @Transaction
    suspend fun finishReportLocally(reportId: String, notes: String, completedAt: String) {
        val report = getShiftReport(reportId)
            ?: throw IllegalStateException("التقرير غير موجود. أعد فتح الوردية.")
        if (report.completedAtDevice != null || report.status == "CONFIRMED") return
        check(getEntriesForReport(reportId).isNotEmpty()) { "سجّل الماكينات قبل إنهاء التقرير." }
        markReportFinished(reportId, "PENDING_HANDOVER", notes, completedAt)
    }

    @Transaction
    suspend fun replaceBootstrapCache(
        assets: List<AssetEntity>,
        products: List<ProductEntity>,
        operators: List<OperatorEntity>,
        options: List<ProductionOptionEntity>
    ) {
        deleteAllAssets()
        insertAssets(assets)
        deleteAllProducts()
        insertProducts(products)
        deleteAllOperators()
        insertOperators(operators)
        deleteAllProductionOptions()
        insertProductionOptions(options)
    }

    @Transaction
    suspend fun restoreServerReports(reports: List<ReportSnapshot>) {
        reports.forEach { remote ->
            val local = findShiftReport(
                remote.report.reportDate,
                remote.report.shift,
                requireNotNull(remote.report.factoryId),
                requireNotNull(remote.report.ownerUserId)
            )
            val sameIdLocal = getShiftReport(remote.report.clientReportId)
            if (local?.isSynced == false || sameIdLocal?.isSynced == false) return@forEach
            if (local != null && local.clientReportId != remote.report.clientReportId) {
                deleteEntriesForReport(local.clientReportId)
                deleteStoppagesForReport(local.clientReportId)
                deleteSyncedReport(local.clientReportId)
            }
            upsertShiftReport(remote.report)
            deleteEntriesForReport(remote.report.clientReportId)
            deleteStoppagesForReport(remote.report.clientReportId)
            remote.entries.forEach { upsertMachineEntry(it.copy(id = 0)) }
            remote.stoppages.forEach { insertStoppage(it.copy(id = 0)) }
        }
    }

    @Query("SELECT * FROM machine_entries WHERE clientReportId = :reportId ORDER BY assetId ASC")
    fun getEntriesForReportFlow(reportId: String): Flow<List<MachineEntryEntity>>

    @Query("SELECT * FROM machine_entries WHERE clientReportId = :reportId ORDER BY assetId ASC")
    suspend fun getEntriesForReport(reportId: String): List<MachineEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMachineEntry(entry: MachineEntryEntity)

    @Query("DELETE FROM machine_entries WHERE clientReportId = :reportId")
    suspend fun deleteEntriesForReport(reportId: String)

    @Query("SELECT * FROM stoppages WHERE clientReportId = :reportId ORDER BY id DESC")
    fun getStoppagesForReportFlow(reportId: String): Flow<List<StoppageEntity>>

    @Query("SELECT * FROM stoppages WHERE clientReportId = :reportId ORDER BY id DESC")
    suspend fun getStoppagesForReport(reportId: String): List<StoppageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoppage(stoppage: StoppageEntity)

    @Query("DELETE FROM stoppages WHERE id = :id")
    suspend fun deleteStoppage(id: Long)

    @Query("DELETE FROM stoppages WHERE clientReportId = :reportId")
    suspend fun deleteStoppagesForReport(reportId: String)
}
