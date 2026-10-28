package com.production.supervisor.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.mapSaver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.production.supervisor.data.local.entity.*
import com.production.supervisor.ui.screens.handover.HandoverDialog
import com.production.supervisor.ui.screens.stoppage.StoppageDialog
import com.production.supervisor.ui.screens.stoppage.MachineStopReasonDialog
import org.json.JSONObject
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionTableScreen(viewModel: ProductionHomeViewModel, onLogout: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF2563EB), onPrimary = Color.White,
        secondary = Color(0xFF475569), onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9), onSecondaryContainer = Color(0xFF0F172A),
        background = Color.White, onBackground = Color(0xFF0F172A),
        surface = Color.White, onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF8FAFC), onSurfaceVariant = Color(0xFF475569),
        surfaceContainer = Color.White, surfaceContainerLow = Color.White,
        surfaceContainerHigh = Color(0xFFF1F5F9), surfaceTint = Color.Transparent,
        outline = Color(0xFF94A3B8), outlineVariant = Color(0xFFE2E8F0)
    )) {
        ProductionTableContent(viewModel, onLogout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductionTableContent(viewModel: ProductionHomeViewModel, onLogout: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, state.selectedFactoryId) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(30_000)
                viewModel.refreshProductionOptions()
            }
        }
    }
    if (!state.handoverChecked || state.pendingHandover != null) {
        com.production.supervisor.ui.screens.handover.HandoverGate(state, viewModel)
        return
    }
    val snackbar = remember { SnackbarHostState() }
    var showFaults by rememberSaveable { mutableStateOf(false) }
    var faults by rememberSaveable(state.currentReportId) { mutableStateOf("") }
    var finish by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var notes by rememberSaveable(state.currentReportId) { mutableStateOf("") }
    var machineForStoppage by remember { mutableStateOf<AssetEntity?>(null) }
    val dirty = rememberSaveable(state.currentReportId, saver = listSaver<SnapshotStateMap<Int, Boolean>, Int>(
        save = { map -> map.filterValues { it }.keys.toList() },
        restore = { ids -> mutableStateMapOf<Int, Boolean>().apply { ids.forEach { put(it, true) } } }
    )) { mutableStateMapOf<Int, Boolean>() }
    val editable = !state.isReportFinished && !state.isLoading && !state.isSavingEntry && !state.isFinishingShift
    val drafts = rememberSaveable(state.currentReportId, saver = mapSaver(
        save = { map: SnapshotStateMap<Int, MachineEntryEntity?> -> map.entries.associate { it.key.toString() to Json.encodeToString(it.value) } },
        restore = { values -> mutableStateMapOf<Int, MachineEntryEntity?>().apply {
            values.forEach { (id, value) -> put(id.toInt(), Json.decodeFromString<MachineEntryEntity?>(value as String)) }
        } }
    )) { mutableStateMapOf<Int, MachineEntryEntity?>() }
    val reportEntries = resolveReportEntries(state.entries, drafts)
    LaunchedEffect(state.message) {
        state.message?.let { viewModel.clearMessage(); snackbar.showSnackbar(it) }
    }
    val allAccountedFor = editable && state.assets.isNotEmpty() && state.assets.all {
        isMachineAccountedFor(it.id, reportEntries[it.id], state.stoppages, state.stoppedMachineAssetIds)
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                TextButton(onClick = { showActions = true }) { Text("المزيد") }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text(if (state.isReportSynced) "تم إرسال البيانات" else "بانتظار المزامنة") }, onClick = {}, enabled = false)
                    if (state.isReportFinished) DropdownMenuItem(text = { Text("تم تسليم الوردية • عرض فقط") }, onClick = {}, enabled = false)
                    DropdownMenuItem(text = { Text("توقف ماكينة / مدة التوقف") }, enabled = editable,
                        onClick = { showActions = false; viewModel.openStoppageDialog() })
                    DropdownMenuItem(text = { Text("إعادة المزامنة") }, onClick = { showActions = false; viewModel.retrySync() })
                    DropdownMenuItem(text = { Text("تحديث البيانات") }, enabled = editable && dirty.values.none { it },
                        onClick = { showActions = false; viewModel.loadData() })
                    if (state.pendingHandover != null) DropdownMenuItem(text = { Text("استلام الوردية السابقة") },
                        onClick = { showActions = false; viewModel.openHandoverDialog() })
                    state.availableFactories.takeIf { it.size > 1 }?.forEach { factory ->
                        DropdownMenuItem(text = { Text(factory.name) }, enabled = editable && dirty.values.none { it },
                            onClick = { showActions = false; viewModel.selectFactory(factory) })
                    }
                    if (java.time.LocalDate.now(java.time.ZoneId.of("Africa/Cairo")).dayOfWeek == java.time.DayOfWeek.FRIDAY) {
                        DropdownMenuItem(text = { Text("توقف صلاة الجمعة (60 دقيقة)") }, enabled = editable,
                            onClick = { showActions = false; viewModel.addFridayPrayerStoppage() })
                    }
                    DropdownMenuItem(text = { Text("خروج") }, enabled = dirty.values.none { it },
                        onClick = { showActions = false; viewModel.logout(onLogout) })
                }
            }
            OutlinedButton(onClick = { showFaults = true }, enabled = editable, modifier = Modifier.weight(1f)) {
                Text("إضافة أعطال", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { finish = true },
                modifier = Modifier.weight(1.3f),
                enabled = allAccountedFor,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE2E8F0),
                    disabledContentColor = Color(0xFF94A3B8)
                )
            ) {
                Text(
                    "تسليم الوردية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (allAccountedFor) Color.White else Color(0xFF94A3B8)
                )
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val completedCount = state.assets.count { isMachineAccountedFor(it.id, reportEntries[it.id], state.stoppages, state.stoppedMachineAssetIds) }
                    val stoppedCount = state.assets.count { state.stoppedMachineAssetIds.contains(it.id) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("التاريخ: ${state.currentDate}", fontSize = 13.sp)
                        Text(
                            if (stoppedCount > 0)
                                "المكتمل: $completedCount / ${state.assets.size} ($stoppedCount متوقفة)"
                            else
                                "المكتمل: $completedCount / ${state.assets.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (completedCount == state.assets.size && state.assets.isNotEmpty()) Color(0xFF16A34A) else Color(0xFF0F172A)
                        )
                    }
                    Text("المشرف: ${state.currentUser?.name?.takeIf { it.isNotBlank() } ?: state.cachedSupervisorName.ifBlank { "—" }}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("التسليم إلى: ${state.currentUser?.nextShiftSupervisor?.name ?: "لم يتم تحديد المستلم من الداشبورد"}", fontSize = 13.sp)
                }
            }
            if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.assets.isEmpty() && !state.isLoading) item {
                Text("لا توجد ماكينات. حدّث البيانات للتحقق من المصنع.")
            }
            items(if (state.isLoading || state.currentReportId.isBlank()) emptyList() else state.assets, key = { "${state.currentReportId}:${it.id}:${state.entries[it.id]?.id ?: 0}" }) { asset ->
                MachineTable(
                    asset = asset,
                    entry = state.entries[asset.id],
                    state = state,
                    enabled = editable,
                    isStopped = state.stoppedMachineAssetIds.contains(asset.id),
                    onToggleStopped = {
                        if (state.stoppedMachineAssetIds.contains(asset.id)) {
                            viewModel.cancelMachineStopped(asset.id)
                        } else {
                            machineForStoppage = asset
                        }
                    },
                    onEditStopReason = { machineForStoppage = asset },
                    onDirty = { dirty[asset.id] = it },
                    onDraft = { changed, entry ->
                        if (changed) { if (!drafts.containsKey(asset.id) || drafts[asset.id] != entry) drafts[asset.id] = entry }
                        else drafts.remove(asset.id)
                    },
                    onSave = viewModel::saveMachineEntry
                )
            }
            items(state.stoppages, key = { "fault:${it.id}" }) { fault ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(if (fault.assetId == null) "أعطال عامة" else "توقف ${fault.assetCode}", fontWeight = FontWeight.Bold)
                    Text(fault.description)
                    if (fault.durationMinutes > 0) Text("${fault.durationMinutes} دقيقة", color = Color(0xFFD97706), fontWeight = FontWeight.Medium)
                } }
            }
        }
    }
    if (showFaults) AlertDialog(
        onDismissRequest = { showFaults = false },
        title = { Text("جميع أعطال الوردية", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(faults, { faults = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("اكتب الأعطال وأرقام الماكينات والإجراءات يدويًا") })
        },
        confirmButton = {
            Button(
                onClick = { viewModel.addGeneralFaults(faults) { faults = ""; showFaults = false } },
                enabled = faults.isNotBlank() && editable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White)
            ) {
                Text("حفظ الأعطال", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { showFaults = false }) { Text("رجوع") }
        }
    )
    if (finish) AlertDialog(
        onDismissRequest = { finish = false },
        title = { Text("تسليم الوردية", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val stoppedCount = state.assets.count { state.stoppedMachineAssetIds.contains(it.id) }
                if (stoppedCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "تنبيه: تم تحديد $stoppedCount ماكينة كمتوقفة وسيتم تسليم التقرير بدون بيانات إنتاج لها بشكل طبيعي.",
                            modifier = Modifier.padding(10.dp),
                            fontSize = 12.5.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 17.sp
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات التسليم (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.finishShift(notes, drafts.toMap()); finish = false },
                enabled = editable,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White)
            ) {
                Text("تأكيد وتسليم الوردية", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { finish = false }) { Text("رجوع") }
        }
    )
    if (state.isStoppageDialogVisible) StoppageDialog(state.assets, state.currentReportId, { viewModel.closeStoppageDialog() }, { viewModel.addStoppage(it) })
    if (machineForStoppage != null) {
        val assetToStop = machineForStoppage!!
        val currentStoppage = state.stoppages.findLast { it.assetId == assetToStop.id && it.clientReportId == state.currentReportId }
        MachineStopReasonDialog(
            asset = assetToStop,
            initialReason = currentStoppage?.description ?: "",
            initialDuration = currentStoppage?.durationMinutes?.takeIf { it > 0 } ?: 480,
            onDismiss = { machineForStoppage = null },
            onConfirm = { reason, duration, stoppageType ->
                viewModel.setMachineStoppedWithReason(assetToStop.id, reason, duration, stoppageType)
                machineForStoppage = null
            }
        )
    }
    if (state.isHandoverDialogVisible && state.pendingHandover != null) {
        HandoverDialog(
            pendingReport = state.pendingHandover!!,
            onDismiss = { viewModel.closeHandoverDialog() },
            onConfirm = { viewModel.confirmHandover(it) },
            onReject = { viewModel.confirmHandover(it, "REJECT") },
            busy = state.isLoading,
            error = state.message
        )
    }
}

@Composable
private fun MachineTable(
    asset: AssetEntity,
    entry: MachineEntryEntity?,
    state: ProductionHomeUiState,
    enabled: Boolean,
    isStopped: Boolean,
    onToggleStopped: () -> Unit,
    onEditStopReason: () -> Unit,
    onDirty: (Boolean) -> Unit,
    onDraft: (Boolean, MachineEntryEntity?) -> Unit,
    onSave: (MachineEntryEntity) -> Unit
) {
    val baseline = rememberSaveable { entry?.dashboardDefaults?.takeIf { it.isNotBlank() } ?: JSONObject(mapOf(
        "operator_name" to asset.defaultOperatorName.orEmpty(), "product_name" to asset.defaultProductName.orEmpty(),
        "current_cavities" to asset.originalCavities.toString(), "original_cavities" to asset.originalCavities.toString(),
        "cooling_time_seconds" to asset.coolingTimeSeconds.toString(), "cycle_time_seconds" to asset.cycleTimeSeconds.toString(),
        "raw_material" to asset.defaultRawMaterialName.orEmpty(), "packaging_type" to asset.defaultPackagingName.orEmpty(),
        "final_production_unit_name" to (asset.defaultFinalUnitName ?: "كجم"), "operation_mode" to "AUTO",
        "target_cycle_production" to asset.targetCycleProduction.toString(), "target_cycle_unit_name" to asset.targetCycleUnitName.orEmpty()
    )).toString() }
    val defaults = remember(baseline) { JSONObject(baseline) }
    val materialOptions = state.productionOptions.filter { it.category == "RAW_MATERIAL" }.map { it.name }
    val packagingOptions = state.productionOptions.filter { it.category == "PACKAGING" }.map { it.name }
    val unitOptions = state.productionOptions.filter { it.category == "FINAL_UNIT" }.map { it.name }
    val initialUnit = entry?.finalProductionUnitName?.ifBlank { null }
        ?: defaults.optString("final_production_unit_name").ifBlank { null }
        ?: unitOptions.firstOrNull().orEmpty()
    var worker by rememberSaveable { mutableStateOf(entry?.operatorName ?: defaults.optString("operator_name")) }
    var product by rememberSaveable { mutableStateOf(entry?.productName ?: defaults.optString("product_name")) }
    var cavities by rememberSaveable { mutableStateOf((entry?.currentCavities ?: asset.originalCavities).toString()) }
    var original by rememberSaveable { mutableStateOf((entry?.originalCavities ?: asset.originalCavities).toString()) }
    var cooling by rememberSaveable { mutableStateOf((entry?.coolingTimeSeconds ?: asset.coolingTimeSeconds).toString()) }
    var cycle by rememberSaveable { mutableStateOf((entry?.cycleTimeSeconds ?: asset.cycleTimeSeconds).toString()) }
    var material by rememberSaveable { mutableStateOf(entry?.rawMaterial ?: defaults.optString("raw_material")) }
    var packaging by rememberSaveable { mutableStateOf(entry?.packagingType ?: defaults.optString("packaging_type")) }
    var quantity by rememberSaveable { mutableStateOf(entry?.finalProductionQuantity?.toString() ?: "") }
    var unit by rememberSaveable { mutableStateOf(initialUnit) }
    var mode by rememberSaveable { mutableStateOf(entry?.operationMode ?: "AUTO") }
    var notes by rememberSaveable { mutableStateOf(entry?.notes ?: "") }
    var changed by rememberSaveable { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        pending?.let { saved -> if (entry?.copy(id = 0).toString() == saved) { changed = false; pending = null } }
    }
    LaunchedEffect(changed) { onDirty(changed) }
    val selectedUnit = state.productionOptions.find { it.category == "FINAL_UNIT" && it.name == unit.trim() }
    val selectedMaterial = state.productionOptions.find { it.category == "RAW_MATERIAL" && it.name == material.trim() }
    val selectedPackaging = state.productionOptions.find { it.category == "PACKAGING" && it.name == packaging.trim() }
    val valid = quantity.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true &&
        cavities.toIntOrNull()?.let { it > 0 } == true && original.toIntOrNull()?.let { it > 0 } == true &&
        cooling.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true && cycle.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true &&
        selectedUnit != null
    val draftEntry = if (valid && selectedUnit != null && !isStopped) {
                val q = quantity.toDouble()
                val workerId = state.operators.find { it.name == worker.trim() }?.id
                val productId = state.products.find { it.name == product.trim() }?.id
                val unitKgFactor = selectedUnit.kgPerUnit ?: 0.0
                MachineEntryEntity(id = entry?.id ?: 0, clientReportId = state.currentReportId, assetId = asset.id, assetCode = asset.assetCode,
                    operatorId = workerId, operatorName = worker.trim(), originalOperatorId = if (entry != null) entry.originalOperatorId else asset.defaultOperatorId,
                    originalOperatorName = defaults.optString("operator_name"), operatorChanged = worker.trim() != defaults.optString("operator_name"),
                    productId = productId, productName = product.trim(), originalProductId = if (entry != null) entry.originalProductId else asset.defaultProductId,
                    originalProductName = defaults.optString("product_name"), productChanged = product.trim() != defaults.optString("product_name"),
                    originalCavities = original.toInt(), currentCavities = cavities.toInt(), coolingTimeSeconds = cooling.toDouble(), cycleTimeSeconds = cycle.toDouble(),
                    originalCoolingTimeSeconds = defaults.optString("cooling_time_seconds").toDoubleOrNull() ?: 0.0,
                    originalCycleTimeSeconds = defaults.optString("cycle_time_seconds").toDoubleOrNull() ?: 0.0,
                    operationMode = mode, rawMaterial = material.trim(), rawMaterialOptionId = selectedMaterial?.id,
                    packagingType = packaging.trim(), packagingOptionId = selectedPackaging?.id, finalProductionQuantity = q,
                    finalProductionUnitId = selectedUnit.id, finalProductionUnitName = selectedUnit.name, finalProductionWeightKg = q * unitKgFactor,
                    targetCycleProduction = defaults.optString("target_cycle_production").toDoubleOrNull() ?: 0.0,
                    targetCycleUnitName = defaults.optString("target_cycle_unit_name"), notes = notes, dashboardDefaults = baseline)
    } else null
    SideEffect { onDraft(changed && !isStopped, draftEntry) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isStopped) Color(0xFFFFFBEB) else Color.White),
        border = BorderStroke(if (isStopped) 1.5.dp else 1.dp, if (isStopped) Color(0xFFF59E0B) else Color(0xFFE2E8F0))) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${asset.assetCode} • ${asset.productionTitle}", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(
                    if (isStopped) "متوقفة" else if (changed) "غير محفوظ" else if (entry != null) "محفوظ" else "جديد",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isStopped) Color(0xFFDC2626) else if (changed) Color(0xFFD97706) else if (entry != null) Color(0xFF16A34A) else Color(0xFF64748B)
                )
            }

            // Dedicated toggle bar for "الماكينة متوقفة"
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isStopped) Color(0xFFFEF3C7) else Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isStopped) "حالة الماكينة: متوقفة بالوردية" else "هل الماكينة متوقفة؟",
                    fontSize = 12.sp,
                    fontWeight = if (isStopped) FontWeight.Bold else FontWeight.Medium,
                    color = if (isStopped) Color(0xFFB45309) else Color(0xFF475569)
                )
                FilterChip(
                    selected = isStopped,
                    onClick = onToggleStopped,
                    enabled = enabled,
                    label = {
                        Text(
                            if (isStopped) "✓ الماكينة متوقفة (إلغاء)" else "الماكينة متوقفة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStopped) Color(0xFFDC2626) else Color(0xFF334155)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEE2E2),
                        selectedLabelColor = Color(0xFFDC2626),
                        containerColor = Color.White,
                        labelColor = Color(0xFF334155)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = enabled,
                        selected = isStopped,
                        borderColor = if (isStopped) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                        borderWidth = 1.dp
                    )
                )
            }

            if (isStopped) {
                val machineStoppage = state.stoppages.findLast { it.assetId == asset.id && it.clientReportId == state.currentReportId }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(6.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⛔ الماكينة متوقفة بالوردية", fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), fontSize = 13.sp)
                        if (enabled) {
                            TextButton(
                                onClick = onEditStopReason,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("تعديل السبب", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                    if (machineStoppage != null && machineStoppage.description.isNotBlank()) {
                        Text("سبب التوقف: ${machineStoppage.description}", fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309), fontSize = 12.5.sp)
                        if (machineStoppage.durationMinutes > 0) {
                            Text("المدة المسجلة: ${machineStoppage.durationMinutes} دقيقة", color = Color(0xFF64748B), fontSize = 11.5.sp)
                        }
                    }
                    Text(
                        "تم تسجيل سبب التوقف؛ ويمكن تسليم التقرير بدون بيانات إنتاج لهذه الماكينة. (لإلغاء التوقف اضغط على «✓ الماكينة متوقفة (إلغاء)» أعلاه).",
                        fontSize = 11.5.sp,
                        color = Color(0xFF7F1D1D),
                        lineHeight = 15.sp
                    )
                }
            } else {
                Row(Modifier.fillMaxWidth().background(Color(0xFFF3F3F3)).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text("البيان / الافتراضي", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("القيمة الفعلية", Modifier.weight(1.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                TableCell("العامل", defaults.optString("operator_name"), worker, enabled) { worker = it; changed = true }
                TableCell("المنتج", defaults.optString("product_name"), product, enabled) { product = it; changed = true }
                TableCell("اللقم الأصلية", defaults.optString("original_cavities"), original, enabled, true) { original = it; changed = true }
                TableCell("اللقم الحالية", defaults.optString("current_cavities"), cavities, enabled, true) { cavities = it; changed = true }
                TableCell("التبريد (ث)", defaults.optString("cooling_time_seconds"), cooling, enabled, true) { cooling = it; changed = true }
                TableCell("الدورة (ث)", defaults.optString("cycle_time_seconds"), cycle, enabled, true) { cycle = it; changed = true }
                TableDropdown("الخامة", defaults.optString("raw_material"), material,
                    materialOptions, enabled) { material = it; changed = true }
                TableDropdown("العبوة", defaults.optString("packaging_type"), packaging,
                    packagingOptions, enabled) { packaging = it; changed = true }
                HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("وضع التشغيل:", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    FilterChip(
                        selected = mode == "AUTO",
                        onClick = { mode = "AUTO"; changed = true },
                        enabled = enabled,
                        leadingIcon = if (mode == "AUTO") {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = "أوتو",
                                fontSize = 13.sp,
                                fontWeight = if (mode == "AUTO") FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF64748B),
                            selectedContainerColor = Color(0xFF15803D),
                            selectedLabelColor = Color.White,
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledLabelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = mode == "AUTO",
                            borderColor = Color(0xFFCBD5E1),
                            selectedBorderColor = Color(0xFF14532D),
                            borderWidth = if (mode == "AUTO") 2.dp else 1.dp
                        )
                    )
                    FilterChip(
                        selected = mode == "MANUAL",
                        onClick = { mode = "MANUAL"; changed = true },
                        enabled = enabled,
                        leadingIcon = if (mode == "MANUAL") {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = "يدوي",
                                fontSize = 13.sp,
                                fontWeight = if (mode == "MANUAL") FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFFF1F5F9),
                            labelColor = Color(0xFF64748B),
                            selectedContainerColor = Color(0xFFEA580C),
                            selectedLabelColor = Color.White,
                            disabledContainerColor = Color(0xFFF1F5F9),
                            disabledLabelColor = Color(0xFF94A3B8)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = mode == "MANUAL",
                            borderColor = Color(0xFFCBD5E1),
                            selectedBorderColor = Color(0xFF9A3412),
                            borderWidth = if (mode == "MANUAL") 2.dp else 1.dp
                        )
                    )
                }
                TableCell("كمية الإنتاج", "", quantity, enabled, true) { quantity = it; changed = true }
                TableDropdown("وحدة الإنتاج", defaults.optString("final_production_unit_name"), unit, unitOptions, enabled) { unit = it; changed = true }
                Text("المستهدف: ${defaults.optString("target_cycle_production")} ${defaults.optString("target_cycle_unit_name")}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp)
                TableCell("ملاحظات", "", notes, enabled) { notes = it; changed = true }
                if (!valid && changed) Text("راجع الأرقام والبيانات المدخلة.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                Button(
                    onClick = {
                        val saved = draftEntry ?: return@Button
                        pending = saved.copy(id = 0).toString()
                        onSave(saved)
                    },
                    enabled = enabled && valid,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCBD5E1),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        "حفظ بيانات الماكينة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled && valid) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}


@Composable
private fun TableDropdown(label: String, default: String, value: String, options: List<String>, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(label, fontSize = 13.sp, lineHeight = 17.sp)
            if (default.isNotBlank()) Text("الافتراضي: $default", fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFF666666))
        }
        VerticalDivider(Modifier.fillMaxHeight())
        Box(Modifier.weight(1.6f)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled) { expanded = true }
                .semantics { contentDescription = label }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value.ifBlank { "اختر $label" }, Modifier.weight(1f), fontSize = 14.sp)
                Text("▾", fontSize = 16.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false }) }
            }
        }
    }
}

@Composable
private fun TableCell(label: String, default: String, value: String, enabled: Boolean, numeric: Boolean = false, onChange: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(label, fontSize = 13.sp, lineHeight = 17.sp)
            if (default.isNotBlank()) Text("الافتراضي: $default", fontSize = 11.sp, lineHeight = 14.sp, color = Color(0xFF666666))
        }
        VerticalDivider(Modifier.fillMaxHeight())
        BasicTextField(value, { text -> onChange(if (numeric) text.map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else if (c == '٫') '.' else c }.joinToString("") else text) },
            modifier = Modifier.weight(1.6f).heightIn(min = 48.dp).fillMaxHeight()
                .onFocusChanged { focused = it.isFocused }
                .background(if (focused) Color(0xFFF0F0F0) else Color.White)
                .semantics { contentDescription = label }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            enabled = enabled, textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp, color = Color(0xFF171717)),
            cursorBrush = SolidColor(Color.Black),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
            singleLine = numeric,
            decorationBox = { innerTextField -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && !focused) Text("—", color = Color(0xFF888888), fontSize = 14.sp)
                innerTextField()
            } })
    }
}
