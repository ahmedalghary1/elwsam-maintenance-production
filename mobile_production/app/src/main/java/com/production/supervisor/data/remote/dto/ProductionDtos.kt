package com.production.supervisor.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductionBootstrapDto(
    @SerialName("server_date") val serverDate: String,
    val timezone: String,
    val factory: FactoryDto,
    val factories: List<FactoryDto> = emptyList(),
    val user: UserDto,
    val assets: List<ProductionAssetDto>,
    val products: List<ProductDto>,
    val operators: List<MachineOperatorDto>,
    @SerialName("pending_handover") val pendingHandover: ShiftReportDto? = null,
    @SerialName("today_reports") val todayReports: List<ShiftReportDto> = emptyList()
)

@Serializable
data class RefreshTokenRequestDto(
    val refresh: String
)

@Serializable
data class RefreshTokenResponseDto(
    val access: String,
    val refresh: String? = null
)

@Serializable
data class FactoryDto(
    val id: Int,
    val name: String,
    val code: String
)

@Serializable
data class MachineProductionDefaultDto(
    @SerialName("default_product") val defaultProduct: Int? = null,
    @SerialName("default_product_name") val defaultProductName: String? = null,
    @SerialName("default_operator") val defaultOperator: Int? = null,
    @SerialName("default_operator_name") val defaultOperatorName: String? = null,
    @SerialName("original_cavities") val originalCavities: Int = 1,
    @SerialName("cooling_time_seconds") val coolingTimeSeconds: Double = 0.0,
    @SerialName("cycle_time_seconds") val cycleTimeSeconds: Double = 0.0,
    @SerialName("target_cycle_production") val targetCycleProduction: Double = 0.0
)

@Serializable
data class ProductionAssetDto(
    val id: Int,
    @SerialName("asset_code") val assetCode: String,
    @SerialName("asset_type") val assetType: String,
    @SerialName("asset_type_display") val assetTypeDisplay: String,
    @SerialName("sequence_order") val sequenceOrder: Int = 1,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("maintenance_title") val maintenanceTitle: String = "",
    @SerialName("production_title") val productionTitle: String? = null,
    @SerialName("production_default") val productionDefault: MachineProductionDefaultDto? = null
)

@Serializable
data class ProductDto(
    val id: Int,
    val name: String,
    val code: String = "",
    @SerialName("weight_per_piece_grams") val weightPerPieceGrams: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class MachineOperatorDto(
    val id: Int,
    val name: String,
    val phone: String = "",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class SyncShiftReportInputDto(
    @SerialName("client_report_id") val clientReportId: String,
    val shift: String,
    @SerialName("report_date") val reportDate: String,
    @SerialName("started_at_device") val startedAtDevice: String? = null,
    @SerialName("completed_at_device") val completedAtDevice: String? = null,
    val status: String = "PENDING_HANDOVER",
    @SerialName("general_notes") val generalNotes: String = "",
    val entries: List<MachineEntryInputDto> = emptyList(),
    val stoppages: List<StoppageInputDto> = emptyList()
)

@Serializable
data class MachineEntryInputDto(
    @SerialName("asset_id") val assetId: Int,
    @SerialName("operator_id") val operatorId: Int? = null,
    @SerialName("operator_name") val operatorName: String = "",
    @SerialName("original_operator_id") val originalOperatorId: Int? = null,
    @SerialName("original_operator_name") val originalOperatorName: String = "",
    @SerialName("operator_changed") val operatorChanged: Boolean = false,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("product_name") val productName: String = "",
    @SerialName("original_product_id") val originalProductId: Int? = null,
    @SerialName("original_product_name") val originalProductName: String = "",
    @SerialName("product_changed") val productChanged: Boolean = false,
    @SerialName("original_cavities") val originalCavities: Int = 1,
    @SerialName("current_cavities") val currentCavities: Int = 1,
    @SerialName("operation_mode") val operationMode: String = "AUTO",
    @SerialName("cooling_time_seconds") val coolingTimeSeconds: Double = 0.0,
    @SerialName("cycle_time_seconds") val cycleTimeSeconds: Double = 0.0,
    @SerialName("raw_material") val rawMaterial: String = "",
    @SerialName("final_production_weight_kg") val finalProductionWeightKg: Double = 0.0,
    @SerialName("target_cycle_production") val targetCycleProduction: Double = 0.0,
    @SerialName("packaging_type") val packagingType: String = "",
    val notes: String = ""
)

@Serializable
data class StoppageInputDto(
    @SerialName("asset_id") val assetId: Int? = null,
    @SerialName("stoppage_type") val stoppageType: String = "MACHINE_BREAKDOWN",
    val description: String = "",
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("action_taken") val actionTaken: String = ""
)

@Serializable
data class ConfirmHandoverInputDto(
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("handover_notes") val handoverNotes: String = ""
)

@Serializable
data class ShiftReportDto(
    val id: Int,
    @SerialName("client_report_id") val clientReportId: String,
    val shift: String,
    @SerialName("shift_display") val shiftDisplay: String = "",
    @SerialName("report_date") val reportDate: String,
    val status: String,
    @SerialName("status_display") val statusDisplay: String = "",
    @SerialName("supervisor_name") val supervisorName: String = "",
    @SerialName("handover_to_supervisor_name") val handoverToSupervisorName: String? = null,
    @SerialName("handover_confirmed_at") val handoverConfirmedAt: String? = null,
    @SerialName("handover_notes") val handoverNotes: String? = null,
    @SerialName("general_notes") val generalNotes: String = ""
)
