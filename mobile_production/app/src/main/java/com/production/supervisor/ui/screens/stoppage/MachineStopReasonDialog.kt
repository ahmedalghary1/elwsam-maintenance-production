package com.production.supervisor.ui.screens.stoppage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.data.local.entity.AssetEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachineStopReasonDialog(
    asset: AssetEntity,
    initialReason: String = "",
    initialDuration: Int = 480,
    onDismiss: () -> Unit,
    onConfirm: (reason: String, durationMinutes: Int, stoppageType: String) -> Unit
) {
    var reason by rememberSaveable(asset.id) { mutableStateOf(initialReason) }
    var durationText by rememberSaveable(asset.id) { mutableStateOf(initialDuration.toString()) }
    var stoppageType by rememberSaveable(asset.id) { mutableStateOf("MACHINE_BREAKDOWN") }

    val commonReasons = listOf(
        "عطل ميكانيكي بالماكينة" to "MACHINE_BREAKDOWN",
        "عطل كهربائي بالماكينة" to "MACHINE_BREAKDOWN",
        "تغيير اسطمبة" to "MOLD_CHANGE",
        "صيانة دورية / طارئة" to "MAINTENANCE",
        "عدم وجود خامة" to "OTHER",
        "عدم وجود عامل" to "OTHER",
        "انقطاع التيار الكهربائي" to "POWER_OUTAGE",
        "أخرى" to "OTHER"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⛔ ", fontSize = 18.sp)
                Text(
                    text = "سبب توقف ${asset.assetCode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFFDC2626)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "حدد سبب التوقف لتأكيد إيقاف الماكينة (${asset.productionTitle}):",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )

                Text(
                    text = "أسباب شائعة (اضغط للاختيار السريع):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonReasons.forEach { (label, type) ->
                        FilterChip(
                            selected = reason == label,
                            onClick = {
                                reason = label
                                stoppageType = type
                            },
                            label = { Text(label, fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFEE2E2),
                                selectedLabelColor = Color(0xFFDC2626)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب التوقف * (مطلوب)") },
                    placeholder = { Text("اكتب سبب التوقف هنا...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = reason.isBlank(),
                    supportingText = {
                        if (reason.isBlank()) {
                            Text(
                                "يجب إضافة سبب التوقف لتأكيد الإيقاف",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { text ->
                        durationText = text.filter { it.isDigit() }
                    },
                    label = { Text("مدة التوقف (بالدقائق)") },
                    placeholder = { Text("480") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        Text(
                            "الافتراضي: 480 دقيقة (كامل الوردية)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 480
                    onConfirm(reason.trim(), duration, stoppageType)
                },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "تأكيد إيقاف الماكينة",
                    fontWeight = FontWeight.Bold,
                    color = if (reason.isNotBlank()) Color.White else Color(0xFF94A3B8)
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("إلغاء")
            }
        }
    )
}
