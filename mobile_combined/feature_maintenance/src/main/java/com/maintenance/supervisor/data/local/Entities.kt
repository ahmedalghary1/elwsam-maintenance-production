package com.maintenance.supervisor.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "factories") data class FactoryEntity(@PrimaryKey val id: Int, val name: String, val code: String)
@Entity(tableName = "users") data class UserEntity(@PrimaryKey val id: Int, val phone: String, val role: String, val factoryId: Int, val name: String = "")
@Entity(tableName = "assets") data class AssetEntity(@PrimaryKey val id: Int, val code: String, val name: String, val typeName: String, val sequence: Int, val checklistTemplateId: Int?, val active: Boolean)
@Entity(tableName = "checklist_templates") data class ChecklistTemplateEntity(@PrimaryKey val id: Int, val name: String)
@Entity(tableName = "checklist_sections", indices = [Index("templateId")]) data class ChecklistSectionEntity(@PrimaryKey val id: Int, val templateId: Int, val title: String, val sequence: Int)
@Entity(tableName = "checklist_items", indices = [Index("sectionId")]) data class ChecklistItemEntity(@PrimaryKey val id: Int, val sectionId: Int, val text: String, val sequence: Int)
@Entity(tableName = "current_maintenance") data class CurrentMaintenanceEntity(@PrimaryKey val singletonId: Int = 1, val assetId: Int, val reportDate: String, val position: Int, val total: Int)
@Entity(tableName = "reports", indices = [Index("reportDate"), Index("syncStatus")])
data class MaintenanceReportEntity(
    @PrimaryKey val clientReportId: String,
    val ownerUserId: Int,
    val serverId: Int?,
    val assetId: Int,
    val reportDate: String,
    val startedAtDevice: String,
    val completedAtDevice: String?,
    val lastModifiedAtDevice: String,
    val syncStatus: String,
    val lastError: String? = null,
    val isLocked: Boolean = false,
    val cleanerName: String = "",
    val mechanicalTechnician: String = "",
    val electricalTechnician: String = "",
    val maintenanceManager: String = ""
)
@Entity(
    tableName = "answers",
    primaryKeys = ["clientReportId", "checklistItemId"],
    indices = [Index("clientReportId")],
    foreignKeys = [ForeignKey(entity = MaintenanceReportEntity::class, parentColumns = ["clientReportId"], childColumns = ["clientReportId"], onDelete = ForeignKey.CASCADE)]
)
data class MaintenanceAnswerEntity(val clientReportId: String, val checklistItemId: Int, val checked: Boolean, val note: String)
@Entity(tableName = "sync_queue", indices = [Index(value = ["clientReportId"], unique = true)])
data class SyncQueueEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val clientReportId: String, val enqueuedAt: String, val attempts: Int = 0)
@Entity(tableName = "metadata") data class MetadataEntity(@PrimaryKey val key: String, val value: String)

@Entity(
    tableName = "emergency_maintenances",
    foreignKeys = [ForeignKey(entity = MaintenanceReportEntity::class, parentColumns = ["clientReportId"], childColumns = ["clientReportId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("clientReportId"), Index("assetId")]
)
data class EmergencyMaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientReportId: String,
    val assetId: Int,
    val issueDescription: String,
    val responsiblePerson: String,
    val notes: String
)
