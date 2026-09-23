package com.production.supervisor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "shift_reports",
    indices = [Index(value = ["reportDate", "shift"])]
)
data class ShiftReportEntity(
    @PrimaryKey val clientReportId: String,
    val shift: String, // FIRST, SECOND, THIRD
    val reportDate: String, // YYYY-MM-DD
    val startedAtDevice: String,
    val completedAtDevice: String? = null,
    val status: String = "PENDING_HANDOVER", // PENDING_HANDOVER, CONFIRMED
    val generalNotes: String = "",
    val isSynced: Boolean = false,
    val handoverConfirmedAt: String? = null,
    val handoverToSupervisorName: String? = null,
    val handoverNotes: String? = null
)

@Entity(
    tableName = "machine_entries",
    indices = [
        Index(value = ["clientReportId", "assetId"], unique = true),
        Index(value = ["clientReportId"])
    ]
)
data class MachineEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientReportId: String,
    val assetId: Int,
    val assetCode: String,
    val operatorId: Int? = null,
    val operatorName: String = "",
    val originalOperatorId: Int? = null,
    val originalOperatorName: String = "",
    val operatorChanged: Boolean = false,
    val productId: Int? = null,
    val productName: String = "",
    val originalProductId: Int? = null,
    val originalProductName: String = "",
    val productChanged: Boolean = false,
    val originalCavities: Int = 1,
    val currentCavities: Int = 1,
    val operationMode: String = "AUTO", // AUTO, MANUAL
    val coolingTimeSeconds: Double = 0.0,
    val cycleTimeSeconds: Double = 0.0,
    val rawMaterial: String = "",
    val finalProductionWeightKg: Double = 0.0,
    val targetCycleProduction: Double = 0.0,
    val packagingType: String = "", // كراتين، براميل، شكاير...
    val notes: String = ""
)

@Entity(
    tableName = "stoppages",
    indices = [Index(value = ["clientReportId"])]
)
data class StoppageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientReportId: String,
    val assetId: Int? = null, // null means general stoppage
    val assetCode: String = "",
    val stoppageType: String = "MACHINE_BREAKDOWN", // MACHINE_BREAKDOWN, FRIDAY_PRAYER, POWER_OUTAGE, MOLD_CHANGE, MAINTENANCE, OTHER
    val stoppageTypeDisplay: String = "عطل ماكينة",
    val description: String = "",
    val durationMinutes: Int = 0,
    val actionTaken: String = ""
)
