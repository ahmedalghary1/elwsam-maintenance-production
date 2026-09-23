package com.production.supervisor.data.local.dao

import androidx.room.*
import com.production.supervisor.data.local.entity.*
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM shift_reports WHERE clientReportId = :reportId")
    fun getShiftReportFlow(reportId: String): Flow<ShiftReportEntity?>

    @Query("SELECT * FROM shift_reports WHERE clientReportId = :reportId LIMIT 1")
    suspend fun getShiftReport(reportId: String): ShiftReportEntity?

    @Query("SELECT * FROM shift_reports WHERE reportDate = :date AND shift = :shift LIMIT 1")
    suspend fun findShiftReport(date: String, shift: String): ShiftReportEntity?

    @Query("SELECT * FROM shift_reports WHERE isSynced = 0")
    suspend fun getUnsyncedReports(): List<ShiftReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShiftReport(report: ShiftReportEntity)

    @Query("UPDATE shift_reports SET isSynced = :synced, status = :status WHERE clientReportId = :reportId")
    suspend fun updateReportSyncStatus(reportId: String, synced: Boolean, status: String)

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
