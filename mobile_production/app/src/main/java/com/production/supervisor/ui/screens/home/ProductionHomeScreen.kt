package com.production.supervisor.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.production.supervisor.data.local.entity.AssetEntity

import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.ui.screens.entry.MachineEntrySheet
import com.production.supervisor.ui.screens.handover.HandoverDialog
import com.production.supervisor.ui.screens.stoppage.StoppageDialog
import com.production.supervisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHomeScreen(
    onLogout: () -> Unit,
    viewModel: ProductionHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFinishShiftDialog by remember { mutableStateOf(false) }
    var finishShiftNotes by remember { mutableStateOf("") }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "نظام الإنتاج والأعطال",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = uiState.currentDate,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "تسجيل خروج", tint = Color(0xFFFCA5A5))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FactoryNavy)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = FactorySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.openStoppageDialog() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryOrange),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(Icons.Outlined.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل عطل", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }

                    Button(
                        onClick = { showFinishShiftDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryNavy),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(Icons.Outlined.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسليم الوردية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(FactoryBg)
                .padding(padding)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Handover Alert Banner if pending
            uiState.pendingHandover?.let { pending ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openHandoverDialog() },
                        colors = CardDefaults.cardColors(containerColor = FactoryLightOrange),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FactoryOrange.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Box(
                                  modifier = Modifier
                                      .size(42.dp)
                                      .background(FactoryOrange.copy(alpha = 0.2f), CircleShape),
                                  contentAlignment = Alignment.Center
                             ) {
                                 Icon(
                                     Icons.Outlined.Handshake,
                                     contentDescription = null,
                                     tint = FactoryOrange,
                                     modifier = Modifier.size(24.dp)
                                 )
                             }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "وردية بانتظار تأكيد استلامك!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = FactoryDark
                                )
                                Text(
                                    text = "المسلّم: ${pending.supervisorName} (${pending.shiftDisplay})",
                                    fontSize = 12.sp,
                                    color = FactoryTextSecondary
                                )
                            }
                            Button(
                                onClick = { viewModel.openHandoverDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text("استلام", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. Shift Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FactorySurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "اختر الوردية الحالية:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FactoryTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val shifts = listOf(
                                "FIRST" to "الوردية 1 (صباحية)",
                                "SECOND" to "الوردية 2 (مسائية)",
                                "THIRD" to "الوردية 3 (ليلية)"
                            )
                            shifts.forEach { (shiftKey, shiftName) ->
                                val isSelected = uiState.selectedShift == shiftKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) FactoryNavy else FactorySurfaceVariant,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) FactoryNavy else FactoryCardBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setShift(shiftKey) }
                                        .padding(vertical = 11.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shiftName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else FactoryTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Action: Friday Prayer Button
            item {
                Button(
                    onClick = { viewModel.addFridayPrayerStoppage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FactoryTeal),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(Icons.Outlined.PauseCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إيقاف صلاة الجمعة لجميع الماكينات (60 دقيقة)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }

            // 4. Statistics Banner
            item {
                val recordedCount = uiState.entries.size
                val totalMachines = uiState.assets.size
                val totalWeight = uiState.entries.values.sumOf { it.finalProductionWeightKg }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FactorySurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المسجل: $recordedCount من $totalMachines ماكينة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FactoryTextPrimary
                        )
                        Text(
                            text = "إجمالي الوزن: $totalWeight كجم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = FactoryGreen
                        )
                    }
                }
            }

            // 5. Machines List Header
            item {
                Text(
                    text = "الماكينات (اضغط على الماكينة لتسجيل الإنتاج):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = FactoryDark
                )
            }

            // 6. Machines List Items
            items(uiState.assets, key = { it.id }) { asset ->
                val entry = uiState.entries[asset.id]
                val isRecorded = entry != null
                val hasStoppage = uiState.stoppages.any { it.assetId == asset.id }

                MachineCard(
                    asset = asset,
                    entry = entry,
                    hasStoppage = hasStoppage,
                    onClick = { viewModel.openMachineEntry(asset) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Machine Entry Sheet
    uiState.selectedAssetForEntry?.let { asset ->
        val existingEntry = uiState.entries[asset.id]
        key(asset.id) {
            MachineEntrySheet(
                asset = asset,
                currentReportId = uiState.currentReportId,
                existingEntry = existingEntry,
                operators = uiState.operators,
                products = uiState.products,
                onDismiss = { viewModel.closeMachineEntry() },
                onSave = { entry -> viewModel.saveMachineEntry(entry) }
            )
        }
    }

    // Stoppage Dialog
    if (uiState.isStoppageDialogVisible) {
        StoppageDialog(
            assets = uiState.assets,
            currentReportId = uiState.currentReportId,
            onDismiss = { viewModel.closeStoppageDialog() },
            onConfirm = { stoppage -> viewModel.addStoppage(stoppage) }
        )
    }

    // Handover Dialog
    if (uiState.isHandoverDialogVisible && uiState.pendingHandover != null) {
        HandoverDialog(
            pendingReport = uiState.pendingHandover!!,
            onDismiss = { viewModel.closeHandoverDialog() },
            onConfirm = { notes -> viewModel.confirmHandover(notes) }
        )
    }

    // Finish Shift Dialog
    if (showFinishShiftDialog) {
        AlertDialog(
            onDismissRequest = { showFinishShiftDialog = false },
            title = { Text("تسليم الوردية للمشرف التالي", fontWeight = FontWeight.Bold, color = FactoryNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("سيتم حفظ كافة البيانات وتجهيز التقرير للمشرف القادم في الوردية التالية ليقوم بتأكيد الاستلام.")
                    OutlinedTextField(
                        value = finishShiftNotes,
                        onValueChange = { finishShiftNotes = it },
                        label = { Text("ملاحظات عامة للوردية القادمة (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishShift(finishShiftNotes)
                        showFinishShiftDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسليم الوردية الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishShiftDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun MachineCard(
    asset: AssetEntity,
    entry: MachineEntryEntity?,
    hasStoppage: Boolean,
    onClick: () -> Unit
) {
    val isRecorded = entry != null

    val borderColor = when {
        hasStoppage -> FactoryOrange
        isRecorded -> FactoryGreen.copy(alpha = 0.6f)
        else -> FactoryCardBorder
    }

    val statusBg = when {
        hasStoppage -> FactoryLightOrange
        isRecorded -> FactoryLightGreen
        else -> FactorySurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FactorySurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Code and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isRecorded) FactoryGreen else FactoryNavy, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = asset.assetCode,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = asset.productionTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FactoryDark
                        )
                        Text(
                            text = asset.assetTypeDisplay,
                            fontSize = 11.sp,
                            color = FactoryTextMuted
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when {
                            hasStoppage -> Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = FactoryOrangeDark
                            )
                            isRecorded -> Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = FactoryGreenDark
                            )
                            else -> Icon(
                                Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = FactoryTextMuted
                            )
                        }
                        Text(
                            text = when {
                                hasStoppage -> "يوجد توقف"
                                isRecorded -> "تم التسجيل"
                                else -> "لم يسجل"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                hasStoppage -> FactoryOrangeDark
                                isRecorded -> FactoryGreenDark
                                else -> FactoryTextMuted
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Details
            if (entry != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = FactoryTextMuted)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${entry.operatorName.ifBlank { "غير محدد" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = FactoryTextSecondary
                            )
                            if (entry.operatorChanged) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(FactoryLightOrange, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("تغيير عامل!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FactoryOrangeDark)
                                }
                            }
                        }
                        if (entry.operatorChanged && entry.originalOperatorName.isNotBlank()) {
                            Text(
                                text = "كان: ${entry.originalOperatorName} ➔ ${entry.operatorName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = FactoryOrangeDark
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(13.dp), tint = FactoryNavy)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${entry.productName.ifBlank { "غير محدد" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (entry.productChanged) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(FactoryLightOrange, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("تغيير منتج!", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = FactoryOrangeDark)
                                }
                            }
                        }
                        if (entry.productChanged && entry.originalProductName.isNotBlank()) {
                            Text(
                                text = "كان: ${entry.originalProductName} ➔ ${entry.productName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = FactoryOrangeDark
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Scale, contentDescription = null, modifier = Modifier.size(13.dp), tint = FactoryGreenDark)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${entry.finalProductionWeightKg} كجم",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryGreenDark
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.GridOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = FactoryTextMuted)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${entry.currentCavities}/${entry.originalCavities} | ${if (entry.operationMode == "AUTO") "أوتو" else "يدوي"}",
                                fontSize = 11.sp,
                                color = FactoryTextMuted
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "المنتج المعتاد: ${asset.defaultProductName ?: "غير محدد"}",
                            fontSize = 12.sp,
                            color = FactoryTextMuted
                        )
                        if (!asset.defaultOperatorName.isNullOrBlank()) {
                            Text(
                                text = "العامل المعتاد: ${asset.defaultOperatorName}",
                                fontSize = 11.sp,
                                color = FactoryTextMuted
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "اضغط للتسجيل",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryOrangeDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = FactoryOrangeDark
                        )
                    }
                }
            }
        }
    }
}
