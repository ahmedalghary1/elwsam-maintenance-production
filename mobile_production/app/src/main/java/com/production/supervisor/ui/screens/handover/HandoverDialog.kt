package com.production.supervisor.ui.screens.handover

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.production.supervisor.data.remote.dto.ShiftReportDto

@Composable
fun HandoverDialog(
    pendingReport: ShiftReportDto,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit = {},
    busy: Boolean = false,
    error: String? = null
) {
    var notes by rememberSaveable(pendingReport.clientReportId) { mutableStateOf("") }
    var rejecting by rememberSaveable(pendingReport.clientReportId) { mutableStateOf(false) }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Text("مراجعة تقرير الإنتاج والأعطال", style = MaterialTheme.typography.titleLarge)
                Text("المشرف: ${pendingReport.supervisorName}", fontWeight = FontWeight.Bold)
                Text("${pendingReport.reportDate} • ${pendingReport.shiftDisplay}")
                Text("التقرير للعرض فقط. اختر الموافقة أو غير مقبول قبل كتابة تقريرك.")
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("البداية: ${pendingReport.startedAtDevice ?: "?"}\nالتسليم: ${pendingReport.completedAtDevice ?: "?"}")
                    Text("ملاحظات التقرير: ${pendingReport.generalNotes.ifBlank { "لا توجد" }}")
                    Text("إنتاج الماكينات (${pendingReport.machineEntries.size})", fontWeight = FontWeight.Bold)
                    pendingReport.machineEntries.forEach { e ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الماكينة: ${e.assetCode}", fontWeight = FontWeight.Bold)
                            Text("العامل: ${e.operatorName} • السابق: ${e.originalOperatorName}")
                            Text("المنتج: ${e.productName} • السابق: ${e.originalProductName}")
                            Text("اللقم: ${e.currentCavities} / الأصلية: ${e.originalCavities}")
                            Text("التشغيل: ${if (e.operationMode == "AUTO") "أوتو" else "يدوي"}")
                            Text("التبريد: ${e.coolingTimeSeconds} ث • الأصلي: ${e.originalCoolingTimeSeconds ?: "?"}")
                            Text("الدورة: ${e.cycleTimeSeconds} ث • الأصلية: ${e.originalCycleTimeSeconds ?: "?"}")
                            Text("الخامة: ${e.rawMaterial} • العبوة: ${e.packagingType}")
                            Text("الإنتاج: ${e.finalProductionQuantity ?: e.finalProductionWeightKg} ${e.finalProductionUnitName}")
                            Text("المستهدف: ${e.targetCycleProduction} ${e.targetCycleUnitName}")
                            Text("ملاحظات: ${e.notes.ifBlank { "لا توجد" }}")
                        } }
                    }
                    Text("الأعطال والتوقفات (${pendingReport.stoppages.size})", fontWeight = FontWeight.Bold)
                    if (pendingReport.stoppages.isEmpty()) Text("لا توجد أعطال مسجلة")
                    pendingReport.stoppages.forEach { s ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                            Text("${s.assetCode.ifBlank { "المصنع بالكامل" }} • ${s.stoppageTypeDisplay}")
                            Text(s.description)
                            Text("المدة: ${s.durationMinutes} دقيقة")
                            Text("الإجراء: ${s.actionTaken.ifBlank { "لا توجد" }}")
                        } }
                    }
                    OutlinedTextField(notes, { notes = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                        label = { Text(if (rejecting) "سبب عدم القبول (إلزامي)" else "ملاحظات المراجعة") },
                        isError = rejecting && notes.isBlank())
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onConfirm(notes.trim()) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("الموافقة") }
                    OutlinedButton(onClick = { if (rejecting && notes.isNotBlank()) onReject(notes.trim()) else rejecting = true },
                        enabled = !busy && (!rejecting || notes.isNotBlank()), modifier = Modifier.weight(1f)) {
                        Text(if (rejecting) "تأكيد عدم القبول" else "غير مقبول")
                    }
                }
            }
        }
    }
}
