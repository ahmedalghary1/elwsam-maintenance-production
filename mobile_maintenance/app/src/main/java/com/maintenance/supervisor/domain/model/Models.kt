package com.maintenance.supervisor.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime

enum class SyncStatus { LOCAL_DRAFT, PENDING_SYNC, SYNCING, SYNCED, SYNC_ERROR }

data class Factory(val id: Int, val name: String, val code: String)

data class User(
    val id: Int,
    val phone: String,
    val role: String,
    val factory: Factory,
    val name: String = ""
) {
    val displayName: String get() = name.ifBlank { phone }
}
data class Asset(val id: Int, val code: String, val name: String, val typeName: String, val sequence: Int) {
    val normalizedTypeName: String
        get() = normalizeAssetTypeName(typeName.ifBlank { name })

    val maintenanceTitle: String
        get() = formatMaintenanceTitle(normalizedTypeName, code)
}

fun normalizeAssetTypeName(raw: String): String {
    val trimmed = raw.trim()
    return when {
        trimmed.contains("حقن") -> "ماكينة حقن"
        trimmed.contains("عادية") || trimmed.contains("عاديه") -> "ماكينة حقن"
        trimmed.contains("نفخ") -> "ماكينة نفخ"
        trimmed.contains("سوست") -> "ماكينة نفخ"
        trimmed.contains("مكبس") -> "مكبس"
        trimmed.isNotBlank() -> trimmed
        else -> "ماكينة"
    }
}

fun formatMaintenanceTitle(normalizedType: String, code: String): String {
    val cleanCode = code.trim()
    return when {
        normalizedType.contains("مكبس") -> "الصيانة الدورية لمكبس $cleanCode"
        normalizedType.startsWith("ماكينة") -> "الصيانة الدورية ل$normalizedType $cleanCode"
        else -> "الصيانة الدورية لماكينة $normalizedType $cleanCode"
    }
}

data class ChecklistItem(val id: Int, val sectionId: Int, val text: String, val sequence: Int)
data class ChecklistSection(val id: Int, val title: String, val sequence: Int, val items: List<ChecklistItem>)
data class DailyMaintenance(
    val asset: Asset,
    val reportDate: LocalDate,
    val position: Int,
    val total: Int,
    val sections: List<ChecklistSection>,
    val report: MaintenanceReport? = null
)
data class EmergencyMaintenance(
    val id: Long = 0,
    val assetId: Int,
    val issueDescription: String,
    val responsiblePerson: String,
    val notes: String = ""
)
data class MaintenanceAnswer(val checklistItemId: Int, val checked: Boolean, val note: String)
data class MaintenanceReport(
    val clientReportId: String,
    val serverId: Int?,
    val assetId: Int,
    val reportDate: LocalDate,
    val startedAt: OffsetDateTime,
    val completedAt: OffsetDateTime?,
    val lastModifiedAt: OffsetDateTime,
    val status: SyncStatus,
    val answers: List<MaintenanceAnswer>,
    val isLocked: Boolean = false,
    val lastError: String? = null,
    val emergencyMaintenances: List<EmergencyMaintenance> = emptyList(),
    val cleanerName: String = "",
    val mechanicalTechnician: String = "",
    val electricalTechnician: String = "",
    val maintenanceManager: String = ""
)

