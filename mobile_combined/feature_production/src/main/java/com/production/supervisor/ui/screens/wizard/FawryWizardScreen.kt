package com.production.supervisor.ui.screens.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.data.local.entity.AssetEntity
import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.ProductionOptionEntity
import com.production.supervisor.data.local.entity.StoppageEntity
import com.production.supervisor.ui.components.ProductionOptionSelector
import com.production.supervisor.ui.screens.handover.HandoverDialog
import com.production.supervisor.ui.screens.home.ProductionHomeUiState
import com.production.supervisor.ui.screens.home.ProductionHomeViewModel
import com.production.supervisor.ui.screens.home.WizardSubStep
import com.production.supervisor.ui.screens.home.isMachineRecorded
import com.production.supervisor.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.math.BigDecimal

private fun number(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

private fun numericText(value: String): String = buildString {
    value.forEach { char ->
        when {
            char in '0'..'9' -> append(char)
            char in '٠'..'٩' -> append('0' + (char - '٠'))
            char in '۰'..'۹' -> append('0' + (char - '۰'))
            char in listOf('.', '٫', ',') -> append('.')
        }
    }
}

private fun ProductionHomeUiState.isRecorded(asset: AssetEntity): Boolean {
    return stoppedMachineAssetIds.contains(asset.id) || isMachineRecorded(entries[asset.id], stoppages)
}

private fun quantitySummary(entries: Collection<MachineEntryEntity>): String = entries
    .filter { it.finalProductionQuantity > 0 }
    .groupBy { it.finalProductionUnitName.ifBlank { "كجم" } }
    .map { (unit, rows) -> "${number(rows.sumOf { it.finalProductionQuantity })} $unit" }
    .joinToString(" • ").ifBlank { "لا يوجد إنتاج مسجل" }

private fun shiftName(shift: String): String = when (shift) {
    "FIRST" -> "الوردية الأولى"
    "SECOND" -> "الوردية الثانية"
    "THIRD" -> "الوردية الثالثة"
    else -> shift
}

private data class WorkingInput(
    val quantity: Double,
    val unit: ProductionOptionEntity?,
    val currentCavities: Int,
    val originalCavities: Int,
    val cooling: Double,
    val cycle: Double,
    val material: ProductionOptionEntity?,
    val packaging: ProductionOptionEntity?,
    val mode: String,
    val operatorId: Int?,
    val productId: Int?,
    val notes: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FawryWizardScreen(viewModel: ProductionHomeViewModel, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    if (!uiState.handoverChecked || uiState.pendingHandover != null) {
        com.production.supervisor.ui.screens.handover.HandoverGate(uiState, viewModel)
        return
    }
    val snackbar = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    BackHandler(enabled = uiState.wizardStarted && !uiState.isSavingEntry && !uiState.isFinishingShift) {
        when {
            uiState.wizardSubStep == WizardSubStep.STOPPAGE -> viewModel.setWizardSubStep(WizardSubStep.STATUS)
            uiState.wizardSubStep == WizardSubStep.SUMMARY -> viewModel.exitWizardToWelcome()
            else -> viewModel.previousWizardMachine()
        }
    }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            viewModel.clearMessage()
            snackbar.showSnackbar(message)
        }
    }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("تقرير الإنتاج", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("${uiState.factoryName} • ${shiftName(uiState.selectedShift)}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "خيارات التقرير") }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("تحديث البيانات") }, onClick = { showMenu = false; viewModel.loadData() }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                                DropdownMenuItem(text = { Text("إرسال البيانات المحفوظة") }, onClick = { showMenu = false; viewModel.retrySync() }, leadingIcon = { Icon(Icons.Default.Sync, null) })
                                DropdownMenuItem(text = { Text("عرض كل الماكينات") }, onClick = { showMenu = false; viewModel.toggleFawryMode(false) }, leadingIcon = { Icon(Icons.Default.List, null) })
                                DropdownMenuItem(text = { Text("تسجيل الخروج") }, onClick = { showMenu = false; viewModel.logout(onLogout) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) })
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FactoryNavy, titleContentColor = Color.White, actionIconContentColor = Color.White)
                )
                if (uiState.wizardStarted && uiState.assets.isNotEmpty()) MachineProgress(uiState,
                    onSelect = { viewModel.setWizardAssetIndex(it) }, onReview = { viewModel.goToWizardSummary() })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = FactoryBg
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.assets.isEmpty() -> {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("جاري تحميل الماكينات")
                    }
                }
                !uiState.wizardStarted -> WelcomeView(uiState,
                    onStart = { viewModel.startWizard() }, onReview = { viewModel.goToWizardSummary() },
                    onHandover = { viewModel.openHandoverDialog() }, onFridayPrayer = { viewModel.addFridayPrayerStoppage() })
                uiState.wizardSubStep == WizardSubStep.SUMMARY -> SummaryView(uiState,
                    onEdit = { viewModel.setWizardAssetIndex(it) }, onFinish = { viewModel.finishShift(it) }, onRetry = { viewModel.retrySync() })
                else -> {
                    val asset = uiState.assets.getOrNull(uiState.wizardAssetIndex)
                    if (asset != null) key(uiState.currentReportId, asset.id, uiState.wizardSubStep == WizardSubStep.STOPPAGE) {
                        val entry = uiState.entries[asset.id]
                        val stoppage = uiState.stoppages.firstOrNull { it.assetId == asset.id }
                        if (uiState.wizardSubStep == WizardSubStep.STOPPAGE) {
                            StoppedMachineView(asset, entry, stoppage, uiState,
                                onBack = { viewModel.setWizardSubStep(WizardSubStep.STATUS) },
                                onSave = { reason, duration, notes, partialKg, current, original, cooling, cycle ->
                                    viewModel.saveFawryStoppage(asset = asset, stoppageType = reason.first,
                                        stoppageTypeDisplay = reason.second, durationMinutes = duration, notes = notes,
                                        partialWeightKg = partialKg, currentCavities = current, originalCavities = original,
                                        coolingTimeSeconds = cooling, cycleTimeSeconds = cycle)
                                })
                        } else {
                            WorkingMachineView(asset, entry, stoppage, uiState,
                                onStopped = { viewModel.setWizardSubStep(WizardSubStep.STOPPAGE) },
                                onPrevious = { viewModel.previousWizardMachine() },
                                onSave = { input ->
                                    viewModel.saveFawryWorkingEntry(asset = asset, quantity = input.quantity,
                                        finalUnit = input.unit, currentCavities = input.currentCavities,
                                        rawMaterial = input.material, packaging = input.packaging,
                                        coolingTimeSeconds = input.cooling, cycleTimeSeconds = input.cycle,
                                        operationMode = input.mode, customOperatorId = input.operatorId,
                                        customProductId = input.productId, clearOperator = input.operatorId == null,
                                        clearProduct = input.productId == null, notes = input.notes,
                                        originalCavities = input.originalCavities)
                                })
                        }
                    } else Text("لا توجد ماكينات في هذا المصنع", Modifier.align(Alignment.Center), color = FactoryTextSecondary)
                }
            }
        }
    }
    if (uiState.isHandoverDialogVisible && uiState.pendingHandover != null) {
        HandoverDialog(
            pendingReport = uiState.pendingHandover!!,
            onDismiss = { viewModel.closeHandoverDialog() },
            onConfirm = { viewModel.confirmHandover(it) },
            onReject = { viewModel.confirmHandover(it, "REJECT") },
            busy = uiState.isLoading,
            error = uiState.message
        )
    }
}

@Composable
private fun MachineProgress(uiState: ProductionHomeUiState, onSelect: (Int) -> Unit, onReview: () -> Unit) {
    val count = uiState.assets.count { uiState.isRecorded(it) }
    Column(Modifier.fillMaxWidth().background(FactorySurface).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("تم تسجيل $count من ${uiState.assets.size}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = onReview, enabled = !uiState.isSavingEntry) { Text("مراجعة التقرير") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 4.dp)) {
            itemsIndexed(uiState.assets, key = { _, asset -> asset.id }) { index, asset ->
                FilterChip(
                    selected = uiState.wizardSubStep != WizardSubStep.SUMMARY && index == uiState.wizardAssetIndex,
                    onClick = { onSelect(index) }, enabled = !uiState.isSavingEntry && !uiState.isReportFinished && !uiState.isFinishingShift,
                    label = { Text(asset.assetCode, fontSize = 14.sp) },
                    leadingIcon = if (uiState.isRecorded(asset)) { { Icon(Icons.Default.CheckCircle, null, tint = FactoryGreenDark, modifier = Modifier.size(18.dp)) } } else null
                )
            }
        }
    }
}

@Composable
private fun WelcomeView(uiState: ProductionHomeUiState, onStart: () -> Unit, onReview: () -> Unit, onHandover: () -> Unit, onFridayPrayer: () -> Unit) {
    val count = uiState.assets.count { uiState.isRecorded(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(uiState.currentDate, fontSize = 16.sp, color = FactoryTextSecondary)
        uiState.pendingHandover?.let { report ->
            Card(colors = CardDefaults.cardColors(containerColor = FactoryLightOrange)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("وردية سابقة بانتظار الاستلام", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${report.supervisorName} • ${report.shiftDisplay}")
                    OutlinedButton(onClick = onHandover, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("مراجعة واستلام الوردية") }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = FactorySurface), border = BorderStroke(1.dp, FactoryCardBorder)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (uiState.isReportFinished) "تم حفظ تقرير الوردية" else "تسجيل إنتاج الوردية", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                uiState.currentUser?.name?.takeIf { it.isNotBlank() }?.let { Text("المشرف: $it", color = FactoryTextSecondary) }
                Text("اكتب كمية إنتاج الماكينة ثم اضغط حفظ والتالي. إعدادات الماكينة جاهزة تلقائياً.", fontSize = 18.sp, color = FactoryTextSecondary)
                Text("$count من ${uiState.assets.size} ماكينة مسجلة", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Button(
                    onClick = if (uiState.isReportFinished) onReview else onStart,
                    enabled = uiState.assets.isNotEmpty() && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 62.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FactoryNavy,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (uiState.isReportFinished) "عرض التقرير" else if (count > 0) "استكمال التقرير" else "بدء التقرير",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (count > 0 && !uiState.isReportFinished) OutlinedButton(onClick = onReview, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("مراجعة الماكينات المسجلة", fontSize = 17.sp) }
            }
        }
        val isFriday = runCatching { LocalDate.parse(uiState.currentDate).dayOfWeek == DayOfWeek.FRIDAY }.getOrDefault(false)
        if (isFriday && !uiState.isReportFinished) OutlinedButton(onClick = onFridayPrayer, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("تسجيل توقف صلاة الجمعة: 60 دقيقة", fontSize = 16.sp) }
        if (uiState.assets.isEmpty() && !uiState.isLoading) Text("لا توجد ماكينات متاحة. استخدم تحديث البيانات من قائمة الخيارات.", color = FactoryTextSecondary)
    }
}

@Composable
private fun MachineHeading(asset: AssetEntity, uiState: ProductionHomeUiState) {
    Text(asset.productionTitle, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FactoryNavy)
    Text("الماكينة ${uiState.wizardAssetIndex + 1} من ${uiState.assets.size} • ${asset.assetCode}", fontSize = 15.sp, color = FactoryTextSecondary)
}

@Composable
private fun WorkingMachineView(asset: AssetEntity, initial: MachineEntryEntity?, stoppage: StoppageEntity?, uiState: ProductionHomeUiState,
    onStopped: () -> Unit, onPrevious: () -> Unit, onSave: (WorkingInput) -> Unit) {
    fun options(category: String, savedId: Int?, savedName: String, kgPerUnit: Double? = null): List<ProductionOptionEntity> {
        val available = uiState.productionOptions.filter { it.category == category }
        return if (savedId != null && savedName.isNotBlank() && available.none { it.id == savedId })
            available + ProductionOptionEntity(savedId, category, savedName, kgPerUnit) else available
    }
    val units = options("FINAL_UNIT", initial?.finalProductionUnitId, initial?.finalProductionUnitName.orEmpty(),
        initial?.let { if (it.finalProductionQuantity > 0 && it.finalProductionWeightKg > 0) it.finalProductionWeightKg / it.finalProductionQuantity else null })
    val materials = options("RAW_MATERIAL", initial?.rawMaterialOptionId, initial?.rawMaterial.orEmpty())
    val packagingOptions = options("PACKAGING", initial?.packagingOptionId, initial?.packagingType.orEmpty())
    var quantity by rememberSaveable { mutableStateOf(initial?.let { number(it.finalProductionQuantity.takeIf { q -> q > 0 } ?: it.finalProductionWeightKg) }?.takeIf { it != "0" } ?: "") }
    var unit by remember { mutableStateOf(units.find { it.id == (initial?.finalProductionUnitId ?: asset.defaultFinalUnitId) } ?: units.find { it.name == (initial?.finalProductionUnitName ?: asset.defaultFinalUnitName ?: "كجم") } ?: units.firstOrNull()) }
    var material by remember { mutableStateOf(materials.find { it.id == (initial?.rawMaterialOptionId ?: asset.defaultRawMaterialId) }) }
    var packaging by remember { mutableStateOf(packagingOptions.find { it.id == (initial?.packagingOptionId ?: asset.defaultPackagingId) }) }
    var current by rememberSaveable { mutableStateOf((initial?.currentCavities ?: asset.originalCavities).toString()) }
    var original by rememberSaveable { mutableStateOf((initial?.originalCavities ?: asset.originalCavities).toString()) }
    var cooling by rememberSaveable { mutableStateOf(number(initial?.coolingTimeSeconds ?: asset.coolingTimeSeconds)) }
    var cycle by rememberSaveable { mutableStateOf(number(initial?.cycleTimeSeconds ?: asset.cycleTimeSeconds)) }
    var mode by rememberSaveable { mutableStateOf(initial?.operationMode ?: "AUTO") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var operatorId by rememberSaveable { mutableStateOf(if (initial != null) initial.operatorId else asset.defaultOperatorId) }
    var productId by rememberSaveable { mutableStateOf(if (initial != null) initial.productId else asset.defaultProductId) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var exceptionVisible by remember { mutableStateOf(false) }
    val unitName = unit?.name ?: initial?.finalProductionUnitName ?: asset.defaultFinalUnitName ?: "كجم"
    val validSettings = current.toIntOrNull()?.let { it > 0 } == true && original.toIntOrNull()?.let { it > 0 } == true &&
        cooling.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true && cycle.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true
    val validQuantity = quantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MachineHeading(asset, uiState)
            OutlinedButton(onClick = onStopped, enabled = !uiState.isSavingEntry, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.PauseCircle, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(if (stoppage != null) "تعديل التوقف المسجل: ${stoppage.durationMinutes} دقيقة" else "الماكينة توقفت؟ تسجيل توقف", fontSize = 16.sp)
            }
            Text("كمية الإنتاج ($unitName)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AtmNumpad(quantity, { quantity = it }, unitName, enabled = !uiState.isSavingEntry)
            Card(colors = CardDefaults.cardColors(containerColor = FactorySurface), border = BorderStroke(1.dp, FactoryCardBorder)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("اللقم الحالية: $current  •  الأصلية: $original", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("الدورة: $cycle ثانية  •  التبريد: $cooling ثانية", fontSize = 15.sp, color = FactoryTextSecondary)
                    val productName = uiState.products.find { it.id == productId }?.name ?: if (productId == initial?.productId) initial?.productName else asset.defaultProductName
                    val operatorName = uiState.operators.find { it.id == operatorId }?.name ?: if (operatorId == initial?.operatorId) initial?.operatorName else asset.defaultOperatorName
                    Text("المنتج: ${productName?.ifBlank { "غير محدد" } ?: "غير محدد"} • العامل: ${operatorName?.ifBlank { "غير محدد" } ?: "غير محدد"}", fontSize = 15.sp, color = FactoryTextSecondary)
                    TextButton(onClick = { expanded = !expanded }, enabled = !uiState.isSavingEntry, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.Tune, null)
                        Spacer(Modifier.width(8.dp)); Text(if (expanded) "إخفاء الإعدادات" else "تعديل إعدادات الماكينة", fontSize = 17.sp)
                    }
                    if (expanded) {
                        TechnicalFields(asset, current, { current = it }, original, { original = it }, cooling, { cooling = it }, cycle, { cycle = it },
                            defaultCooling = initial?.originalCoolingTimeSeconds ?: asset.coolingTimeSeconds,
                            defaultCycle = initial?.originalCycleTimeSeconds ?: asset.cycleTimeSeconds)
                        OutlinedButton(onClick = { exceptionVisible = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("تغيير العامل أو المنتج", fontSize = 17.sp) }
                        if (units.isNotEmpty()) ProductionOptionSelector("وحدة الإنتاج", units, unit, { unit = it }, allowNone = false)
                        ProductionOptionSelector("الخامة", materials, material, { material = it }, allowNone = false)
                        ProductionOptionSelector("العبوة", packagingOptions, packaging, { packaging = it }, allowNone = false)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وضع التشغيل:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            FilterChip(
                                selected = mode == "AUTO",
                                onClick = { mode = "AUTO" },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                leadingIcon = if (mode == "AUTO") {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                label = {
                                    Text(
                                        text = "أوتو",
                                        fontSize = 15.sp,
                                        fontWeight = if (mode == "AUTO") FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF64748B),
                                    selectedContainerColor = Color(0xFF15803D),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = mode == "AUTO",
                                    borderColor = Color(0xFFCBD5E1),
                                    selectedBorderColor = Color(0xFF14532D),
                                    borderWidth = if (mode == "AUTO") 2.dp else 1.dp
                                )
                            )
                            FilterChip(
                                selected = mode == "MANUAL",
                                onClick = { mode = "MANUAL" },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                leadingIcon = if (mode == "MANUAL") {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                label = {
                                    Text(
                                        text = "يدوي",
                                        fontSize = 15.sp,
                                        fontWeight = if (mode == "MANUAL") FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF64748B),
                                    selectedContainerColor = Color(0xFFEA580C),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = mode == "MANUAL",
                                    borderColor = Color(0xFFCBD5E1),
                                    selectedBorderColor = Color(0xFF9A3412),
                                    borderWidth = if (mode == "MANUAL") 2.dp else 1.dp
                                )
                            )
                        }
                        OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات (اختياري)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    }
                }
            }
            if (!validSettings) Text("راجع اللقم والدورة والتبريد. اللقم أكبر من صفر والأزمنة صفر أو أكثر.", color = FactoryRed, fontSize = 15.sp)
        }
        SaveFooter(
            label = if (uiState.wizardAssetIndex == uiState.assets.lastIndex) "حفظ ومراجعة التقرير" else "حفظ والتالي",
            enabled = validQuantity && validSettings && !uiState.isSavingEntry,
            saving = uiState.isSavingEntry,
            onSave = { onSave(WorkingInput(quantity.toDouble(), unit, current.toInt(), original.toInt(), cooling.toDouble(), cycle.toDouble(), material, packaging, mode, operatorId, productId, notes)) },
            previousEnabled = uiState.wizardAssetIndex > 0 && !uiState.isSavingEntry, onPrevious = onPrevious
        )
    }
    if (exceptionVisible) MachineExceptionDialog(asset = asset, operators = uiState.operators, products = uiState.products,
        currentOperatorId = operatorId, currentProductId = productId, currentCavities = current.toIntOrNull() ?: asset.originalCavities,
        onDismiss = { exceptionVisible = false }, onConfirm = { op, product, cavities ->
            operatorId = op; productId = product; current = cavities.toString(); exceptionVisible = false
        })
}

@Composable
private fun TechnicalFields(asset: AssetEntity, current: String, onCurrent: (String) -> Unit, original: String, onOriginal: (String) -> Unit,
    cooling: String, onCooling: (String) -> Unit, cycle: String, onCycle: (String) -> Unit,
    defaultCooling: Double = asset.coolingTimeSeconds, defaultCycle: Double = asset.cycleTimeSeconds) {
    Text("القيم الافتراضية: ${asset.originalCavities} لقمة • دورة ${number(defaultCycle)} ث • تبريد ${number(defaultCooling)} ث", fontSize = 14.sp, color = FactoryTextSecondary)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumericField("اللقم الحالية", current, onCurrent, Modifier.weight(1f), integer = true, positive = true)
        NumericField("اللقم الأصلية", original, onOriginal, Modifier.weight(1f), integer = true, positive = true)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NumericField("الدورة (ثانية)", cycle, onCycle, Modifier.weight(1f))
        NumericField("التبريد (ثانية)", cooling, onCooling, Modifier.weight(1f))
    }
    TextButton(onClick = { onCurrent(asset.originalCavities.toString()); onOriginal(asset.originalCavities.toString()); onCooling(number(defaultCooling)); onCycle(number(defaultCycle)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("إعادة القيم الافتراضية", fontSize = 16.sp)
    }
}

@Composable
private fun NumericField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, integer: Boolean = false, positive: Boolean = false) {
    val numeric = if (integer) value.toIntOrNull()?.toDouble() else value.toDoubleOrNull()
    val valid = numeric != null && numeric.isFinite() && if (positive) numeric > 0 else numeric >= 0
    OutlinedTextField(value = value, onValueChange = { text -> onChange(numericText(text).let { if (integer) it.filter(Char::isDigit) else it }) },
        label = { Text(label, fontSize = 14.sp) }, modifier = modifier, singleLine = true, isError = !valid,
        keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal), shape = RoundedCornerShape(10.dp))
}

@Composable
private fun SaveFooter(label: String, enabled: Boolean, saving: Boolean, onSave: () -> Unit, previousEnabled: Boolean = false, onPrevious: (() -> Unit)? = null) {
    Surface(color = FactorySurface, shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onPrevious != null && previousEnabled) OutlinedButton(onClick = onPrevious, modifier = Modifier.heightIn(min = 58.dp), shape = RoundedCornerShape(12.dp)) { Text("السابق", fontSize = 16.sp) }
            Button(
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FactoryNavy,
                    contentColor = Color.White
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    if (saving) "جاري الحفظ" else label,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StoppedMachineView(asset: AssetEntity, initial: MachineEntryEntity?, stoppage: StoppageEntity?, uiState: ProductionHomeUiState,
    onBack: () -> Unit, onSave: (Pair<String, String>, Int, String, Double, Int, Int, Double, Double) -> Unit) {
    val reasons = buildList {
        add("MACHINE_BREAKDOWN" to "عطل ماكينة")
        add("POWER_OUTAGE" to "انقطاع كهرباء")
        add("COOLING_HEATING" to "تبريد أو حرارة")
        add("MOLD_CHANGE" to "تغيير اسطمبة")
        add("MAINTENANCE" to "صيانة")
        add("RAW_MATERIAL" to "نقص خامات")
        if (runCatching { LocalDate.parse(uiState.currentDate).dayOfWeek == DayOfWeek.FRIDAY }.getOrDefault(false)) add("FRIDAY_PRAYER" to "صلاة الجمعة")
        add("OTHER" to "سبب آخر")
    }
    var reasonCode by rememberSaveable { mutableStateOf(stoppage?.stoppageType ?: "MACHINE_BREAKDOWN") }
    var duration by rememberSaveable { mutableStateOf((stoppage?.durationMinutes ?: 60).toString()) }
    var notes by rememberSaveable { mutableStateOf(stoppage?.description ?: "") }
    var partial by rememberSaveable { mutableStateOf(initial?.finalProductionWeightKg?.takeIf { it > 0 }?.let { number(it) } ?: "") }
    var hasProduction by rememberSaveable { mutableStateOf((initial?.finalProductionWeightKg ?: 0.0) > 0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var current by rememberSaveable { mutableStateOf((initial?.currentCavities ?: asset.originalCavities).toString()) }
    var original by rememberSaveable { mutableStateOf((initial?.originalCavities ?: asset.originalCavities).toString()) }
    var cooling by rememberSaveable { mutableStateOf(number(initial?.coolingTimeSeconds ?: asset.coolingTimeSeconds)) }
    var cycle by rememberSaveable { mutableStateOf(number(initial?.cycleTimeSeconds ?: asset.cycleTimeSeconds)) }
    val validSettings = current.toIntOrNull()?.let { it > 0 } == true && original.toIntOrNull()?.let { it > 0 } == true &&
        cooling.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true && cycle.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } == true
    val validPartial = !hasProduction || partial.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MachineHeading(asset, uiState)
            OutlinedButton(onClick = onBack, enabled = !uiState.isSavingEntry, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("العودة لتسجيل الإنتاج", fontSize = 16.sp) }
            Text("سبب التوقف", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            reasons.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { reason ->
                        FilterChip(selected = reasonCode == reason.first, onClick = { reasonCode = reason.first },
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp), label = { Text(reason.second, fontSize = 16.sp) })
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text("مدة التوقف", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15 دقيقة", 30 to "30 دقيقة", 60 to "ساعة").forEach { (minutes, text) ->
                    FilterChip(selected = duration == minutes.toString(), onClick = { duration = minutes.toString() }, label = { Text(text) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(120 to "ساعتان", 240 to "4 ساعات", 480 to "8 ساعات").forEach { (minutes, text) ->
                    FilterChip(selected = duration == minutes.toString(), onClick = { duration = minutes.toString() }, label = { Text(text) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                }
            }
            NumericField("مدة التوقف بالدقائق", duration, { duration = it }, Modifier.fillMaxWidth(), integer = true, positive = true)
            if ((duration.toIntOrNull() ?: 0) > 1440) Text("مدة التوقف لا تزيد عن 24 ساعة (1440 دقيقة)", color = FactoryRed)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("يوجد إنتاج قبل التوقف", fontSize = 18.sp, modifier = Modifier.weight(1f))
                Switch(checked = hasProduction, onCheckedChange = { hasProduction = it })
            }
            if (hasProduction) {
                Text("وزن الإنتاج قبل التوقف (كجم)", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                AtmNumpad(partial, { partial = it }, enabled = !uiState.isSavingEntry)
            }
            TextButton(onClick = { settings = !settings }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (settings) "إخفاء الإعدادات" else "تعديل اللقم والدورة والتبريد", fontSize = 17.sp) }
            if (settings) TechnicalFields(asset, current, { current = it }, original, { original = it }, cooling, { cooling = it }, cycle, { cycle = it },
                defaultCooling = initial?.originalCoolingTimeSeconds ?: asset.coolingTimeSeconds,
                defaultCycle = initial?.originalCycleTimeSeconds ?: asset.cycleTimeSeconds)
            OutlinedTextField(notes, { notes = it }, label = { Text("ملاحظات التوقف (اختياري)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            if (!validSettings) Text("راجع قيم اللقم والدورة والتبريد", color = FactoryRed)
        }
        SaveFooter(label = if (uiState.wizardAssetIndex == uiState.assets.lastIndex) "حفظ ومراجعة التقرير" else "حفظ والتالي",
            enabled = duration.toIntOrNull()?.let { it > 0 && it <= 1440 } == true && validSettings && validPartial && !uiState.isSavingEntry,
            saving = uiState.isSavingEntry,
            onSave = { onSave(reasons.find { it.first == reasonCode } ?: (reasonCode to (stoppage?.stoppageTypeDisplay ?: "سبب آخر")), duration.toInt(), notes,
                if (hasProduction) partial.toDouble() else 0.0, current.toInt(), original.toInt(), cooling.toDouble(), cycle.toDouble()) })
    }
}

@Composable
private fun SummaryView(uiState: ProductionHomeUiState, onEdit: (Int) -> Unit, onFinish: (String) -> Unit, onRetry: () -> Unit) {
    val count = uiState.assets.count { uiState.isRecorded(it) }
    val incomplete = uiState.assets.indices.filter { !uiState.isRecorded(uiState.assets[it]) }
    var notes by rememberSaveable(uiState.currentReportId) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            item {
                Text(if (uiState.isReportFinished) "تم حفظ التقرير" else "مراجعة التقرير", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("${uiState.currentDate} • ${shiftName(uiState.selectedShift)}", fontSize = 16.sp, color = FactoryTextSecondary)
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = FactorySurface), border = BorderStroke(1.dp, FactoryCardBorder)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الماكينات المسجلة: $count من ${uiState.assets.size}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("الإنتاج: ${quantitySummary(uiState.entries.values)}", fontSize = 17.sp, color = FactoryGreenDark)
                        if (uiState.isReportFinished) {
                            Text(if (uiState.isReportSynced) "تم إرسال التقرير إلى الإدارة" else "محفوظ على الهاتف. بانتظار إرسال التقرير عند توفر الاتصال.", fontSize = 17.sp, color = if (uiState.isReportSynced) FactoryGreenDark else FactoryOrangeDark)
                            Text(if (uiState.reportStatus == "CONFIRMED") "تم اعتماد استلام الوردية" else "بانتظار استلام الوردية التالية", fontSize = 16.sp, color = FactoryTextSecondary)
                            if (!uiState.isReportSynced) OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("إعادة إرسال التقرير", fontSize = 17.sp) }
                        } else if (incomplete.isNotEmpty()) Text("متبقي ${incomplete.size} ماكينة. سجل الإنتاج أو التوقف قبل إنهاء التقرير.", fontSize = 17.sp, color = FactoryOrangeDark)
                    }
                }
            }
            itemsIndexed(uiState.assets, key = { _, asset -> asset.id }) { index, asset ->
                val entry = uiState.entries[asset.id]
                val stops = uiState.stoppages.filter { it.assetId == asset.id }
                OutlinedCard(onClick = { onEdit(index) }, enabled = !uiState.isReportFinished && !uiState.isFinishingShift && !uiState.isSavingEntry, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(asset.productionTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (!uiState.isReportFinished) Text(if (uiState.isRecorded(asset)) "تعديل" else "تسجيل", fontSize = 16.sp, color = FactoryPrimaryBlue)
                        }
                        if (entry != null && entry.finalProductionQuantity > 0) Text("الإنتاج: ${number(entry.finalProductionQuantity)} ${entry.finalProductionUnitName}", fontSize = 17.sp, color = FactoryGreenDark)
                        if (stops.isNotEmpty()) Text("توقف: ${stops.joinToString("، ") { "${it.stoppageTypeDisplay} (${it.durationMinutes} دقيقة)" }}", fontSize = 16.sp, color = FactoryOrangeDark)
                        if (!uiState.isRecorded(asset)) Text("لم يتم التسجيل", fontSize = 16.sp, color = FactoryTextMuted)
                        if (entry != null) Text("اللقم الحالية: ${entry.currentCavities} • الأصلية: ${entry.originalCavities}\nالدورة: ${number(entry.cycleTimeSeconds)} ث • التبريد: ${number(entry.coolingTimeSeconds)} ث", fontSize = 15.sp, color = FactoryTextSecondary)
                    }
                }
            }
            if (!uiState.isReportFinished) item {
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات تسليم الوردية (اختياري)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
        if (!uiState.isReportFinished) {
            if (incomplete.isNotEmpty()) SaveFooter("استكمال الماكينات المتبقية", !uiState.isSavingEntry, false, { onEdit(incomplete.first()) })
            else SaveFooter("حفظ التقرير وتسليم الوردية", uiState.assets.isNotEmpty() && !uiState.isSavingEntry && !uiState.isFinishingShift, uiState.isFinishingShift, { onFinish(notes) })
        }
    }
}
