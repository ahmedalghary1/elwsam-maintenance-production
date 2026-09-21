package com.maintenance.supervisor.domain.repository

import com.maintenance.supervisor.domain.model.DailyMaintenance
import com.maintenance.supervisor.domain.model.EmergencyMaintenance
import com.maintenance.supervisor.domain.model.Factory
import com.maintenance.supervisor.domain.model.MaintenanceAnswer
import com.maintenance.supervisor.domain.model.MaintenanceReport
import com.maintenance.supervisor.domain.model.User
import kotlinx.coroutines.flow.Flow

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
}
data class HomeSnapshot(val user: User?, val factory: Factory?, val daily: DailyMaintenance?, val lastSync: String?, val availableAssets: List<com.maintenance.supervisor.domain.model.Asset> = emptyList(), val isMaintenanceDay: Boolean = true, val selectionMode: String = "automatic")

interface AuthRepository {
    suspend fun hasSession(): Boolean
    suspend fun login(phone: String, password: String): AppResult<Unit>
    suspend fun logout()
}
interface MaintenanceRepository {
    fun observeHome(): Flow<HomeSnapshot>
    suspend fun bootstrap(): AppResult<Unit>
    suspend fun startOrLoadToday(): AppResult<MaintenanceReport>
    suspend fun selectCurrentAsset(assetId: Int): AppResult<Unit>
    suspend fun saveAnswer(reportId: String, answer: MaintenanceAnswer)
    suspend fun saveEmergencyItems(reportId: String, items: List<EmergencyMaintenance>)
    suspend fun savePersonnel(reportId: String, cleaner: String, mechanical: String, electrical: String, manager: String)
    suspend fun completeReport(
        reportId: String,
        answers: List<MaintenanceAnswer>,
        emergencyItems: List<EmergencyMaintenance> = emptyList(),
        cleaner: String = "",
        mechanical: String = "",
        electrical: String = "",
        manager: String = ""
    ): AppResult<Unit>
    suspend fun sync(): AppResult<Unit>
    suspend fun resetTodayReport(): AppResult<Unit>
}
