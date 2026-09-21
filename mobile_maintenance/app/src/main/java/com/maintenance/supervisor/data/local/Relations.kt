package com.maintenance.supervisor.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SectionWithItems(@Embedded val section: ChecklistSectionEntity, @Relation(parentColumn = "id", entityColumn = "sectionId") val items: List<ChecklistItemEntity>)
data class ReportWithAnswers(
    @Embedded val report: MaintenanceReportEntity,
    @Relation(parentColumn = "clientReportId", entityColumn = "clientReportId") val answers: List<MaintenanceAnswerEntity>,
    @Relation(parentColumn = "clientReportId", entityColumn = "clientReportId") val emergencyMaintenances: List<EmergencyMaintenanceEntity> = emptyList()
)
