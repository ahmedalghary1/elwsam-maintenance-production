package com.production.supervisor.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
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
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionTableScreen(viewModel: ProductionHomeViewModel, onLogout: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF171717), onPrimary = Color.White,
        secondary = Color(0xFF404040), onSecondary = Color.White,
        secondaryContainer = Color(0xFFEAEAEA), onSecondaryContainer = Color.Black,
        background = Color.White, onBackground = Color(0xFF171717),
        surface = Color.White, onSurface = Color(0xFF171717),
        surfaceVariant = Color(0xFFF5F5F5), onSurfaceVariant = Color(0xFF555555),
        surfaceContainer = Color.White, surfaceContainerLow = Color.White,
        surfaceContainerHigh = Color(0xFFF5F5F5), surfaceTint = Color.Transparent,
        outline = Color(0xFF999999), outlineVariant = Color(0xFFD8D8D8)
    )) {
        ProductionTableContent(viewModel, onLogout)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductionTableContent(viewModel: ProductionHomeViewModel, onLogout: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showFaults by rememberSaveable { mutableStateOf(false) }
    var faults by rememberSaveable(state.currentReportId) { mutableStateOf("") }
    var finish by remember { mutableStateOf(false) }
    var notes by rememberSaveable(state.currentReportId) { mutableStateOf("") }
    val dirty = rememberSaveable(state.currentReportId, saver = listSaver<SnapshotStateMap<Int, Boolean>, Int>(
        save = { map -> map.filterValues { it }.keys.toList() },
        restore = { ids -> mutableStateMapOf<Int, Boolean>().apply { ids.forEach { put(it, true) } } }
    )) { mutableStateMapOf<Int, Boolean>() }
    val editable = !state.isReportFinished && !state.isLoading && !state.isSavingEntry && !state.isFinishingShift
    LaunchedEffect(state.message) {
        state.message?.let { viewModel.clearMessage(); snackbar.showSnackbar(it) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("جدول الإنتاج • ${state.factoryName}", fontSize = 16.sp) }, actions = {
            TextButton(onClick = { viewModel.loadData() }, enabled = editable && dirty.values.none { it }) { Text("تحديث") }
            TextButton(onClick = { viewModel.logout(onLogout) }, enabled = dirty.values.none { it }) { Text("خروج") }
        })
    }, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showFaults = true }, enabled = editable, modifier = Modifier.weight(1f)) { Text("إضافة أعطال") }
            Button(onClick = { finish = true }, modifier = Modifier.weight(1f), enabled = editable &&
                dirty.values.none { it } && state.assets.isNotEmpty() && state.assets.all { isMachineRecorded(state.entries[it.id], state.stoppages) }) { Text("تسليم الوردية") }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("${state.currentDate} • ${when(state.selectedShift) { "FIRST" -> "الوردية الأولى"; "SECOND" -> "الوردية الثانية"; else -> "الوردية الثالثة" }}")
                Text("اكتب داخل خلايا الجدول واحفظ الماكينة، ثم مرّر لأسفل للماكينة التالية.")
                Text("المكتمل: ${state.assets.count { isMachineRecorded(state.entries[it.id], state.stoppages) }} من ${state.assets.size} ماكينة")
                Text("للماكينة المتوقفة: احفظ كمية صفر وسجّل توقف الماكينة ومدته.", style = MaterialTheme.typography.bodySmall)
                Text(if (state.isReportSynced) "تم إرسال البيانات إلى الموقع" else "البيانات المحفوظة على الهاتف بانتظار المزامنة")
                TextButton(onClick = viewModel::retrySync) { Text("إعادة المزامنة") }
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.isReportFinished) Text("تم تسليم الوردية • عرض فقط")
                state.availableFactories.takeIf { it.size > 1 }?.let { factories ->
                    factories.forEach { factory ->
                        TextButton(onClick = { viewModel.selectFactory(factory) }, enabled = editable && dirty.values.none { it }) { Text(factory.name) }
                    }
                }
                if (state.pendingHandover != null) Button(onClick = viewModel::openHandoverDialog) { Text("استلام الوردية السابقة") }
                OutlinedButton(onClick = viewModel::openStoppageDialog, enabled = editable) { Text("توقف ماكينة / مدة التوقف") }
                if (java.time.LocalDate.now(java.time.ZoneId.of("Africa/Cairo")).dayOfWeek == java.time.DayOfWeek.FRIDAY) {
                    TextButton(onClick = viewModel::addFridayPrayerStoppage, enabled = editable) { Text("توقف صلاة الجمعة للجميع (60 دقيقة)") }
                }
                if (state.assets.isEmpty() && !state.isLoading) Text("لا توجد ماكينات. حدّث البيانات للتحقق من المصنع.")
            }
            items(if (state.isLoading || state.currentReportId.isBlank()) emptyList() else state.assets, key = { "${state.currentReportId}:${it.id}:${state.entries[it.id]?.id ?: 0}" }) { asset ->
                MachineTable(asset, state.entries[asset.id], state, editable,
                    onDirty = { dirty[asset.id] = it }, onSave = viewModel::saveMachineEntry)
            }
            items(state.stoppages, key = { "fault:${it.id}" }) { fault ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(if (fault.assetId == null) "أعطال عامة" else "توقف ${fault.assetCode}")
                    Text(fault.description)
                    if (fault.durationMinutes > 0) Text("${fault.durationMinutes} دقيقة")
                } }
            }
        }
    }
    if (showFaults) AlertDialog(onDismissRequest = { showFaults = false }, title = { Text("جميع أعطال الوردية") }, text = {
        OutlinedTextField(faults, { faults = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp), label = { Text("اكتب الأعطال وأرقام الماكينات والإجراءات يدويًا") })
    }, confirmButton = { TextButton(onClick = {
        viewModel.addGeneralFaults(faults) { faults = ""; showFaults = false }
    }, enabled = faults.isNotBlank() && editable) { Text("حفظ الأعطال") } }, dismissButton = { TextButton(onClick = { showFaults = false }) { Text("رجوع") } })
    if (finish) AlertDialog(onDismissRequest = { finish = false }, title = { Text("تسليم الوردية") }, text = {
        OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات التسليم") })
    }, confirmButton = { TextButton(onClick = { viewModel.finishShift(notes); finish = false }) { Text("تأكيد التسليم") } }, dismissButton = { TextButton(onClick = { finish = false }) { Text("رجوع") } })
    if (state.isStoppageDialogVisible) StoppageDialog(state.assets, state.currentReportId, { viewModel.closeStoppageDialog() }, { viewModel.addStoppage(it) })
    if (state.isHandoverDialogVisible && state.pendingHandover != null) HandoverDialog(state.pendingHandover!!, { viewModel.closeHandoverDialog() }, { viewModel.confirmHandover(it) })
}

@Composable
private fun MachineTable(asset: AssetEntity, entry: MachineEntryEntity?, state: ProductionHomeUiState,
                         enabled: Boolean, onDirty: (Boolean) -> Unit, onSave: (MachineEntryEntity) -> Unit) {
    val baseline = rememberSaveable { entry?.dashboardDefaults?.takeIf { it.isNotBlank() } ?: JSONObject(mapOf(
        "operator_name" to asset.defaultOperatorName.orEmpty(), "product_name" to asset.defaultProductName.orEmpty(),
        "current_cavities" to asset.originalCavities.toString(), "original_cavities" to asset.originalCavities.toString(),
        "cooling_time_seconds" to asset.coolingTimeSeconds.toString(), "cycle_time_seconds" to asset.cycleTimeSeconds.toString(),
        "raw_material" to asset.defaultRawMaterialName.orEmpty(), "packaging_type" to asset.defaultPackagingName.orEmpty(),
        "final_production_unit_name" to (asset.defaultFinalUnitName ?: "كجم"), "operation_mode" to "AUTO",
        "target_cycle_production" to asset.targetCycleProduction.toString(), "target_cycle_unit_name" to asset.targetCycleUnitName.orEmpty()
    )).toString() }
    val defaults = remember(baseline) { JSONObject(baseline) }
    var worker by rememberSaveable { mutableStateOf(entry?.operatorName ?: defaults.optString("operator_name")) }
    var product by rememberSaveable { mutableStateOf(entry?.productName ?: defaults.optString("product_name")) }
    var cavities by rememberSaveable { mutableStateOf((entry?.currentCavities ?: asset.originalCavities).toString()) }
    var original by rememberSaveable { mutableStateOf((entry?.originalCavities ?: asset.originalCavities).toString()) }
    var cooling by rememberSaveable { mutableStateOf((entry?.coolingTimeSeconds ?: asset.coolingTimeSeconds).toString()) }
    var cycle by rememberSaveable { mutableStateOf((entry?.cycleTimeSeconds ?: asset.cycleTimeSeconds).toString()) }
    var material by rememberSaveable { mutableStateOf(entry?.rawMaterial ?: defaults.optString("raw_material")) }
    var packaging by rememberSaveable { mutableStateOf(entry?.packagingType ?: defaults.optString("packaging_type")) }
    var quantity by rememberSaveable { mutableStateOf(entry?.finalProductionQuantity?.toString() ?: "") }
    var unit by rememberSaveable { mutableStateOf(entry?.finalProductionUnitName ?: defaults.optString("final_production_unit_name")) }
    var mode by rememberSaveable { mutableStateOf(entry?.operationMode ?: "AUTO") }
    var notes by rememberSaveable { mutableStateOf(entry?.notes ?: "") }
    var changed by rememberSaveable { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(entry) {
        pending?.let { saved -> if (entry?.copy(id = 0).toString() == saved) { changed = false; pending = null } }
    }
    LaunchedEffect(changed) { onDirty(changed) }
    val selectedUnit = state.productionOptions.find { it.category == "FINAL_UNIT" && it.name == unit.trim() }
    val valid = quantity.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true &&
        cavities.toIntOrNull()?.let { it > 0 } == true && original.toIntOrNull()?.let { it > 0 } == true &&
        cooling.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true && cycle.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true && selectedUnit != null
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${asset.assetCode} • ${asset.productionTitle}", Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (changed) "غير محفوظ" else if (entry != null) "محفوظ" else "جديد", fontSize = 11.sp, color = Color(0xFF555555))
            }
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
            TableCell("الخامة", defaults.optString("raw_material"), material, enabled) { material = it; changed = true }
            TableCell("العبوة", defaults.optString("packaging_type"), packaging, enabled) { packaging = it; changed = true }
            HorizontalDivider()
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("التشغيل", Modifier.weight(1f), fontSize = 13.sp)
                FilterChip(mode == "AUTO", { mode = "AUTO"; changed = true }, { Text("أوتو", fontSize = 13.sp) }, enabled = enabled)
                FilterChip(mode == "MANUAL", { mode = "MANUAL"; changed = true }, { Text("يدوي", fontSize = 13.sp) }, enabled = enabled)
            }
            TableCell("كمية الإنتاج", "", quantity, enabled, true) { quantity = it; changed = true }
            TableCell("وحدة الإنتاج", defaults.optString("final_production_unit_name"), unit, enabled) { unit = it; changed = true }
            Text("الوحدات: " + state.productionOptions.filter { it.category == "FINAL_UNIT" }.joinToString { it.name }, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 11.sp, color = Color(0xFF555555))
            Text("المستهدف: ${defaults.optString("target_cycle_production")} ${defaults.optString("target_cycle_unit_name")}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp)
            TableCell("ملاحظات", "", notes, enabled) { notes = it; changed = true }
            if (!valid && changed) Text("راجع الأرقام واختر اسم وحدة من الوحدات المتاحة.", color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                val q = quantity.toDouble()
                val workerId = state.operators.find { it.name == worker.trim() }?.id
                val productId = state.products.find { it.name == product.trim() }?.id
                val saved = MachineEntryEntity(id = entry?.id ?: 0, clientReportId = state.currentReportId, assetId = asset.id, assetCode = asset.assetCode,
                    operatorId = workerId, operatorName = worker.trim(), originalOperatorId = if (entry != null) entry.originalOperatorId else asset.defaultOperatorId,
                    originalOperatorName = defaults.optString("operator_name"), operatorChanged = worker.trim() != defaults.optString("operator_name"),
                    productId = productId, productName = product.trim(), originalProductId = if (entry != null) entry.originalProductId else asset.defaultProductId,
                    originalProductName = defaults.optString("product_name"), productChanged = product.trim() != defaults.optString("product_name"),
                    originalCavities = original.toInt(), currentCavities = cavities.toInt(), coolingTimeSeconds = cooling.toDouble(), cycleTimeSeconds = cycle.toDouble(),
                    originalCoolingTimeSeconds = defaults.optString("cooling_time_seconds").toDoubleOrNull() ?: 0.0,
                    originalCycleTimeSeconds = defaults.optString("cycle_time_seconds").toDoubleOrNull() ?: 0.0,
                    operationMode = mode, rawMaterial = material.trim(), packagingType = packaging.trim(), finalProductionQuantity = q,
                    finalProductionUnitId = selectedUnit!!.id, finalProductionUnitName = selectedUnit.name, finalProductionWeightKg = q * (selectedUnit.kgPerUnit ?: 0.0),
                    targetCycleProduction = defaults.optString("target_cycle_production").toDoubleOrNull() ?: 0.0,
                    targetCycleUnitName = defaults.optString("target_cycle_unit_name"), notes = notes, dashboardDefaults = baseline)
                pending = saved.copy(id = 0).toString(); onSave(saved)
            }, enabled = enabled && valid, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(4.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("حفظ الماكينة", fontSize = 13.sp) }
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
