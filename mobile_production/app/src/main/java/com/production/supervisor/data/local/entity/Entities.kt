package com.production.supervisor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: Int,
    val assetCode: String,
    val assetType: String,
    val assetTypeDisplay: String,
    val maintenanceTitle: String,
    val sequenceOrder: Int,
    val defaultProductId: Int?,
    val defaultProductName: String?,
    val defaultOperatorId: Int? = null,
    val defaultOperatorName: String? = null,
    val originalCavities: Int,
    val coolingTimeSeconds: Double,
    val cycleTimeSeconds: Double,
    val targetCycleProduction: Double,
    val isActive: Boolean
) {
    val productionTitle: String
        get() {
            if (maintenanceTitle.startsWith("الصيانة الدورية ل")) {
                return maintenanceTitle.replaceFirst("الصيانة الدورية ل", "إنتاج ")
            }
            if (maintenanceTitle.startsWith("الصيانة الدورية ")) {
                return maintenanceTitle.replaceFirst("الصيانة الدورية ", "إنتاج ")
            }
            if (maintenanceTitle.isNotBlank() && !maintenanceTitle.contains("الصيانة الدورية")) {
                return maintenanceTitle
            }
            val typeLabel = if (assetType.contains("BLOW", ignoreCase = true) || assetType.contains("SPRING", ignoreCase = true)) {
                "ماكينة نفخ"
            } else if (assetType.contains("PRESS", ignoreCase = true)) {
                "مكبس"
            } else {
                "ماكينة حقن"
            }
            return "إنتاج $typeLabel $assetCode"
        }
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val code: String,
    val weightPerPieceGrams: Double,
    val isActive: Boolean
)

@Entity(tableName = "operators")
data class OperatorEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val phone: String,
    val isActive: Boolean
)
