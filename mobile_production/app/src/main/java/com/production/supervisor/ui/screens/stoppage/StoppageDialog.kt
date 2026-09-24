package com.production.supervisor.ui.screens.stoppage

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.data.local.entity.AssetEntity
import com.production.supervisor.data.local.entity.StoppageEntity
import com.production.supervisor.ui.theme.*

@Composable
fun StoppageDialog(
    assets: List<AssetEntity>,
    currentReportId: String,
    onDismiss: () -> Unit,
    onConfirm: (StoppageEntity) -> Unit
) {
    var selectedAsset by remember { mutableStateOf<AssetEntity?>(null) } // null = general
    var stoppageType by remember { mutableStateOf("MACHINE_BREAKDOWN") }
    var durationMinutesText by remember { mutableStateOf("30") }
    var description by remember { mutableStateOf("") }
    var actionTaken by remember { mutableStateOf("") }

    var showAssetMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    val isFriday = try {
        java.time.LocalDate.now().dayOfWeek == java.time.DayOfWeek.FRIDAY
    } catch (_: Exception) {
        false
    }

    val stoppageTypes = buildList {
        add("MACHINE_BREAKDOWN" to "عطل ميكانيكي / كهربائي بالماكينة")
        if (isFriday) {
            add("FRIDAY_PRAYER" to "إيقاف صلاة الجمعة")
        }
        add("POWER_OUTAGE" to "انقطاع التيار الكهربائي")
        add("MOLD_CHANGE" to "تغيير اسطمبة")
        add("MAINTENANCE" to "صيانة دورية / طارئة")
        add("OTHER" to "أخرى")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تسجيل عطل أو توقف",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = FactoryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scope: General or specific asset
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PrecisionManufacturing, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نطاق التوقف:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FactoryTextSecondary)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FactoryCardBorder, RoundedCornerShape(10.dp))
                        .clickable { showAssetMenu = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedAsset?.productionTitle ?: "عام لكافة ماكينات المصنع",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedAsset == null) FactoryOrangeDark else FactoryTextPrimary
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = FactoryTextMuted)
                    }
                    DropdownMenu(expanded = showAssetMenu, onDismissRequest = { showAssetMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("عام لكافة ماكينات المصنع (مثل صلاة الجمعة / كهرباء)", color = FactoryOrangeDark, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedAsset = null
                                showAssetMenu = false
                            }
                        )
                        assets.forEach { asset ->
                            DropdownMenuItem(
                                text = { Text(asset.productionTitle, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedAsset = asset
                                    showAssetMenu = false
                                }
                            )
                        }
                    }
                }

                // Stoppage Type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Category, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نوع التوقف:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, FactoryCardBorder, RoundedCornerShape(10.dp))
                        .clickable { showTypeMenu = true }
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stoppageTypes.find { it.first == stoppageType }?.second ?: stoppageType,
                            fontSize = 13.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        stoppageTypes.forEach { (typeKey, typeLabel) ->
                            DropdownMenuItem(
                                text = { Text(typeLabel) },
                                onClick = {
                                    stoppageType = typeKey
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Duration in minutes
                OutlinedTextField(
                    value = durationMinutesText,
                    onValueChange = { durationMinutesText = it },
                    label = { Text("مدة التوقف (بالدقائق)") },
                    leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null, tint = FactoryNavy) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف العطل أو التوقف") },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, tint = FactoryNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Action Taken
                OutlinedTextField(
                    value = actionTaken,
                    onValueChange = { actionTaken = it },
                    label = { Text("الإجراء المتخذ (اختياري)") },
                    leadingIcon = { Icon(Icons.Outlined.Build, contentDescription = null, tint = FactoryNavy) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationMinutesText.toIntOrNull() ?: 0
                    val typeLabel = stoppageTypes.find { it.first == stoppageType }?.second ?: stoppageType
                    val stoppage = StoppageEntity(
                        clientReportId = currentReportId,
                        assetId = selectedAsset?.id,
                        assetCode = selectedAsset?.assetCode ?: "عام لكافة الماكينات",
                        stoppageType = stoppageType,
                        stoppageTypeDisplay = typeLabel,
                        description = description,
                        durationMinutes = duration,
                        actionTaken = actionTaken
                    )
                    onConfirm(stoppage)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FactoryOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Outlined.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تسجيل التوقف", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
