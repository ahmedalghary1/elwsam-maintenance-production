package com.maintenance.supervisor.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val phone: String, val password: String)
@Serializable data class RefreshRequest(val refresh: String)
@Serializable data class TokenResponse(val access: String, val refresh: String? = null)
@Serializable data class FactoryDto(val id: Int, val name: String, val code: String = "")

@Serializable data class UserDto(val id: Int, val phone: String, val role: String, val factory: FactoryDto, val name: String = "")
@Serializable data class AssetDto(
    val id: Int,
    @SerialName("asset_code") val code: String = "",
    val name: String = "",
    @SerialName("asset_type_display") val typeName: String = "",
    @SerialName("sequence_order") val sequence: Int = 0,
    @SerialName("checklist_template_id") val checklistTemplateId: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("asset_type") val assetType: String = ""
)
@Serializable data class ChecklistItemDto(val id: Int, @SerialName("section_id") val sectionId: Int = 0, val text: String, @SerialName("sequence_order") val sequence: Int = 0)
@Serializable data class ChecklistSectionDto(val id: Int, @SerialName("template_id") val templateId: Int = 0, @SerialName("name") val title: String = "", @SerialName("sequence_order") val sequence: Int = 0, val items: List<ChecklistItemDto> = emptyList())
@Serializable data class ChecklistTemplateDto(val id: Int, val name: String = "", val code: String = "", val sections: List<ChecklistSectionDto> = emptyList())

// --- DTOs matching actual server responses ---

// Server report answer as returned from ReportSerializer
@Serializable data class ServerAnswerDto(
    @SerialName("checklist_item_id") val checklistItemId: Int,
    val text: String = "",
    val checked: Boolean = false,
    val note: String = ""
)

@Serializable data class ServerEmergencyItemDto(
    val id: Int? = null,
    val asset: AssetDto? = null,
    @SerialName("asset_id") val assetId: Int? = null,
    @SerialName("issue_description") val issueDescription: String = "",
    @SerialName("responsible_person") val responsiblePerson: String = "",
    val notes: String = ""
)

// Server report as returned from ReportSerializer
@Serializable data class ServerReportDto(
    val id: Int,
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("factory_id") val factoryId: Int? = null,
    val asset: AssetDto? = null,
    @SerialName("supervisor_id") val supervisorId: Int? = null,
    @SerialName("report_date") val reportDate: String = "",
    @SerialName("started_at_device") val startedAtDevice: String = "",
    @SerialName("completed_at_device") val completedAtDevice: String = "",
    @SerialName("last_modified_at_device") val lastModifiedAtDevice: String = "",
    @SerialName("cleaner_name") val cleanerName: String = "",
    @SerialName("mechanical_technician") val mechanicalTechnician: String = "",
    @SerialName("electrical_technician") val electricalTechnician: String = "",
    @SerialName("maintenance_manager") val maintenanceManager: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("completed_at") val completedAt: String = "",
    @SerialName("is_locked") val isLocked: Boolean = false,
    val answers: List<ServerAnswerDto> = emptyList(),
    @SerialName("emergency_items") val emergencyItems: List<ServerEmergencyItemDto> = emptyList()
)

// Bootstrap response matches BootstrapView.get() output
@Serializable data class BootstrapDto(
    val user: UserDto,
    val factory: FactoryDto = user.factory,
    @SerialName("active_assets") val assets: List<AssetDto> = emptyList(),
    @SerialName("checklist_templates") val checklistTemplates: List<ChecklistTemplateDto> = emptyList(),
    val asset: AssetDto? = null,
    @SerialName("checklist_template") val checklistTemplate: ChecklistTemplateDto? = null,
    @SerialName("server_date") val serverDate: String? = null,
    @SerialName("is_reported") val isReported: Boolean = false,
    val report: ServerReportDto? = null,
    @SerialName("order_version") val orderVersion: Int = 1,
    @SerialName("is_maintenance_day") val isMaintenanceDay: Boolean = true,
    @SerialName("selection_mode") val selectionMode: String = "automatic",
    @SerialName("cycle_position") val cyclePosition: Int = 0,
    @SerialName("cycle_total") val cycleTotal: Int = 0
)

@Serializable data class CurrentAssetSelectionRequest(@SerialName("asset_id") val assetId: Int)

// Input DTOs (sent TO server)
@Serializable data class AnswerInput(
    @SerialName("checklist_item_id") val checklistItemId: Int,
    val checked: Boolean,
    val note: String = ""
)
@Serializable data class EmergencyMaintenanceInput(
    @SerialName("asset_id") val assetId: Int,
    @SerialName("issue_description") val issueDescription: String,
    @SerialName("responsible_person") val responsiblePerson: String,
    val notes: String = ""
)
@Serializable data class ReportInput(
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("asset_id") val assetId: Int,
    @SerialName("report_date") val reportDate: String,
    @SerialName("started_at_device") val startedAtDevice: String,
    @SerialName("completed_at_device") val completedAtDevice: String,
    @SerialName("last_modified_at_device") val lastModifiedAtDevice: String,
    val answers: List<AnswerInput>,
    @SerialName("emergency_items") val emergencyItems: List<EmergencyMaintenanceInput> = emptyList(),
    @SerialName("cleaner_name") val cleanerName: String = "",
    @SerialName("mechanical_technician") val mechanicalTechnician: String = "",
    @SerialName("electrical_technician") val electricalTechnician: String = "",
    @SerialName("maintenance_manager") val maintenanceManager: String = ""
)
@Serializable data class BatchSyncRequest(val reports: List<ReportInput>)

// Response from POST/PUT /api/v1/mobile/reports/ and /reports/<id>/
@Serializable data class ReportResponseDto(
    val status: String = "",
    val reason: String = "",
    val report: ServerReportDto? = null
)

// Individual sync result in batch response
@Serializable data class ReportResultDto(
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("report_id") val id: Int? = null,
    val status: String = "accepted",
    @SerialName("reason") val detail: String? = null
)

// Response from POST /api/v1/mobile/sync/reports/
@Serializable data class BatchSyncResponse(
    @SerialName("results") val reports: List<ReportResultDto> = emptyList(),
    @SerialName("current_maintenance") val currentMaintenance: BootstrapCurrentDto? = null,
    @SerialName("server_date") val serverDate: String? = null
)

// The current_maintenance object returned inside batch sync response
@Serializable data class BootstrapCurrentDto(
    @SerialName("server_date") val serverDate: String? = null,
    val asset: AssetDto? = null,
    @SerialName("checklist_template") val checklistTemplate: ChecklistTemplateDto? = null,
    @SerialName("is_reported") val isReported: Boolean = false,
    val report: ServerReportDto? = null,
    @SerialName("order_version") val orderVersion: Int = 1,
    @SerialName("is_maintenance_day") val isMaintenanceDay: Boolean = true,
    @SerialName("selection_mode") val selectionMode: String = "automatic",
    @SerialName("cycle_position") val cyclePosition: Int = 0,
    @SerialName("cycle_total") val cycleTotal: Int = 0
)
