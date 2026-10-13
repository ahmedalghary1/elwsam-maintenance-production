package com.production.supervisor.ui.screens.handover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val totalProduction = pendingReport.machineEntries.sumOf { it.finalProductionQuantity ?: it.finalProductionWeightKg }
    val primaryUnit = pendingReport.machineEntries.firstOrNull()?.finalProductionUnitName?.ifBlank { "كجم" } ?: "كجم"
    val totalStoppageMinutes = pendingReport.stoppages.sumOf { it.durationMinutes }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Text(
                    "مراجعة تقرير إنتاج وأعطال الوردية السابقة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "المشرف المسلّم: ${pendingReport.supervisorName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "التاريخ: ${pendingReport.reportDate} • ${pendingReport.shiftDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // Read-only notice banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 التقرير معروض بالكامل للمراجعة فقط بدون إمكانية التعديل عليه. يجب اختيار (الموافقة) أو (غير مقبول مع كتابة السبب) قبل أن تتمكن من كتابة تقريرك الجديد.",
                        color = Color(0xFF92400E),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Summary metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي الإنتاج", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "%.1f %s".format(totalProduction, primaryUnit),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الماكينات", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${pendingReport.machineEntries.size} ماكينة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("الأعطال", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "${pendingReport.stoppages.size} (${totalStoppageMinutes} د)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Times & Notes
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("توقيت بدء الوردية: ${pendingReport.startedAtDevice ?: "غير محدد"}", style = MaterialTheme.typography.bodySmall)
                            Text("توقيت تسليم الوردية: ${pendingReport.completedAtDevice ?: "غير محدد"}", style = MaterialTheme.typography.bodySmall)
                            if (pendingReport.generalNotes.isNotBlank()) {
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                Text("ملاحظات المشرف المسلّم: ${pendingReport.generalNotes}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Machine Entries List
                    Text(
                        "بيانات إنتاج وتشغيل الماكينات (${pendingReport.machineEntries.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    pendingReport.machineEntries.forEach { e ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ماكينة: ${e.assetCode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Surface(
                                        color = if (e.operationMode == "AUTO") Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            if (e.operationMode == "AUTO") "أوتو" else "يدوي",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (e.operationMode == "AUTO") Color(0xFF166534) else Color(0xFF92400E)
                                        )
                                    }
                                }

                                Text("العامل: ${e.operatorName}${if (e.originalOperatorName.isNotBlank() && e.operatorName != e.originalOperatorName) " (الأصلي: ${e.originalOperatorName})" else ""}")
                                Text("المنتج: ${e.productName}${if (e.originalProductName.isNotBlank() && e.productName != e.originalProductName) " (الأصلي: ${e.originalProductName})" else ""}")
                                Text("اللقم: ${e.currentCavities} / الأصلية: ${e.originalCavities}")
                                Text("التبريد: ${e.coolingTimeSeconds} ث • الدورة: ${e.cycleTimeSeconds} ث")
                                if (e.rawMaterial.isNotBlank() || e.packagingType.isNotBlank()) {
                                    Text("الخامة: ${e.rawMaterial.ifBlank { "-" }} • العبوة: ${e.packagingType.ifBlank { "-" }}")
                                }
                                Text(
                                    "الإنتاج الفعلي: ${e.finalProductionQuantity ?: e.finalProductionWeightKg} ${e.finalProductionUnitName}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (e.targetCycleProduction > 0) {
                                    Text("مستهدف الدورة: ${e.targetCycleProduction} ${e.targetCycleUnitName}")
                                }
                                if (e.notes.isNotBlank()) {
                                    Text("ملاحظات الماكينة: ${e.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Stoppages List
                    Text(
                        "الأعطال والتوقفات (${pendingReport.stoppages.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (pendingReport.stoppages.isEmpty()) {
                        Text("لا توجد أعطال مسجلة في هذه الوردية", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF16A34A))
                    } else {
                        pendingReport.stoppages.forEach { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            s.assetCode.ifBlank { "المصنع بالكامل" },
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B)
                                        )
                                        Text("${s.durationMinutes} دقيقة", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                    Text("نوع العطل: ${s.stoppageTypeDisplay}", style = MaterialTheme.typography.bodySmall)
                                    Text("السبب / الوصف: ${s.description}", fontWeight = FontWeight.Medium)
                                    if (s.actionTaken.isNotBlank()) {
                                        Text("الإجراء المتخذ: ${s.actionTaken}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Rejection reason or approval notes input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        label = {
                            Text(if (rejecting) "سبب عدم القبول (إلزامي)*" else "ملاحظات الاستلام (اختياري)")
                        },
                        placeholder = {
                            Text(if (rejecting) "اكتب تفاصيل سبب عدم القبول..." else "اكتب أي ملاحظات على الاستلام...")
                        },
                        supportingText = {
                            if (rejecting) {
                                Text("يجب كتابة سبب عدم القبول لتأكيد القرار", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = rejecting && notes.isBlank()
                    )

                    if (rejecting) {
                        TextButton(
                            onClick = { rejecting = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("إلغاء وضع عدم القبول والعودة للموافقة")
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }

                Spacer(Modifier.height(8.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Approve Button
                    Button(
                        onClick = { onConfirm(notes.trim()) },
                        enabled = !busy && !rejecting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF16A34A),
                            contentColor = Color.White
                        )
                    ) {
                        Text("✓ الموافقة على التقرير", fontWeight = FontWeight.Bold)
                    }

                    // Reject Button
                    if (rejecting) {
                        Button(
                            onClick = {
                                if (notes.isNotBlank()) {
                                    onReject(notes.trim())
                                }
                            },
                            enabled = !busy && notes.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            )
                        ) {
                            Text("تأكيد عدم القبول", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { rejecting = true },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            )
                        ) {
                            Text("✕ غير مقبول", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}
