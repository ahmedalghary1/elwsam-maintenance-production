package com.production.supervisor.ui.screens.wizard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.data.local.entity.AssetEntity
import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.StoppageEntity
import com.production.supervisor.ui.screens.handover.HandoverDialog
import com.production.supervisor.ui.screens.home.ProductionHomeUiState
import com.production.supervisor.ui.screens.home.ProductionHomeViewModel
import com.production.supervisor.ui.screens.home.WizardSubStep
import com.production.supervisor.ui.theme.*

/**
 * الشاشة الرئيسية لنظام "ماكينة فوري / الخطوة بخطوة" (Fawry Wizard Mode)
 * تطبيق فائق البساطة ومخصص للمشرفين والعمال في بيئة المصانع:
 * 1. قاعدة "سؤال واحد وشاشة واحدة"
 * 2. الاعتماد التلقائي على إعدادات الداشبورد
 * 3. لوحة أرقام ATM ضخمة تمنع فتح كيبورد الهاتف
 * 4. ألوان وإشارات بصرية صريحة وعريضة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FawryWizardScreen(
    viewModel: ProductionHomeViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FactoryNavy)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "نظام فوري المبسط ⚡",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                if (uiState.factoryName.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = uiState.factoryName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${uiState.currentDate} • ${when (uiState.selectedShift) {
                                    "FIRST" -> "الوردية 1 (صباحية)"
                                    "SECOND" -> "الوردية 2 (مسائية)"
                                    "THIRD" -> "الوردية 3 (ليلية)"
                                    else -> uiState.selectedShift
                                }}",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    },
                    actions = {
                        // Switch to Detailed Mode Button
                        FilledTonalButton(
                            onClick = { viewModel.toggleFawryMode(false) },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الوضع التفصيلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { viewModel.loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "خروج", tint = Color(0xFFFCA5A5))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = FactoryNavy)
                )

                // Machines Step Indicator Bar (only shown when wizard is started)
                if (uiState.wizardStarted && uiState.assets.isNotEmpty()) {
                    WizardStepBar(
                        assets = uiState.assets,
                        currentIndex = uiState.wizardAssetIndex,
                        entries = uiState.entries,
                        stoppages = uiState.stoppages,
                        isSummary = uiState.wizardSubStep == WizardSubStep.SUMMARY,
                        onSelectMachine = { index -> viewModel.setWizardAssetIndex(index) },
                        onSelectSummary = { viewModel.goToWizardSummary() }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FactoryBg)
                .padding(padding)
        ) {
            if (!uiState.wizardStarted) {
                // ==========================================
                // Screen 1: Welcome / Start Shift Screen
                // ==========================================
                ShiftWelcomeView(
                    uiState = uiState,
                    onStartWizard = { viewModel.startWizard() },
                    onSetShift = { shift -> viewModel.setShift(shift) },
                    onOpenHandover = { viewModel.openHandoverDialog() },
                    onAddFridayPrayer = { viewModel.addFridayPrayerStoppage() },
                    onGoToSummary = { viewModel.goToWizardSummary() }
                )
            } else if (uiState.wizardSubStep == WizardSubStep.SUMMARY) {
                // ==========================================
                // Screen 4: Summary & Handover Screen
                // ==========================================
                ShiftSummaryView(
                    uiState = uiState,
                    onEditMachine = { index -> viewModel.setWizardAssetIndex(index) },
                    onFinishShift = { notes -> viewModel.finishShift(notes) },
                    onBackToMachines = { viewModel.previousWizardMachine() }
                )
            } else {
                // Active Machine Step
                val currentAsset = uiState.assets.getOrNull(uiState.wizardAssetIndex)
                if (currentAsset != null) {
                    val currentEntry = uiState.entries[currentAsset.id]
                    val currentStoppages = uiState.stoppages.filter { it.assetId == currentAsset.id }

                    when (uiState.wizardSubStep) {
                        WizardSubStep.STATUS -> {
                            // ==========================================
                            // Screen 2: Machine Status Question Screen
                            // ==========================================
                            MachineStatusQuestionView(
                                asset = currentAsset,
                                machineIndex = uiState.wizardAssetIndex,
                                totalMachines = uiState.assets.size,
                                existingEntry = currentEntry,
                                stoppages = currentStoppages,
                                onWorkingClick = {
                                    viewModel.setWizardSubStep(WizardSubStep.WEIGHT)
                                },
                                onStoppedClick = {
                                    viewModel.setWizardSubStep(WizardSubStep.STOPPAGE)
                                },
                                onOpenException = {
                                    viewModel.openExceptionDialog(currentAsset)
                                },
                                onPrevious = { viewModel.previousWizardMachine() },
                                onNext = { viewModel.nextWizardMachine() }
                            )
                        }
                        WizardSubStep.WEIGHT -> {
                            // ==========================================
                            // Screen 3-A: ATM Weight Entry Screen
                            // ==========================================
                            MachineWeightEntryView(
                                asset = currentAsset,
                                machineIndex = uiState.wizardAssetIndex,
                                totalMachines = uiState.assets.size,
                                initialWeight = currentEntry?.finalProductionWeightKg ?: 0.0,
                                isLastMachine = uiState.wizardAssetIndex == uiState.assets.size - 1,
                                onSaveWeight = { weight ->
                                    viewModel.saveFawryWorkingEntry(
                                        asset = currentAsset,
                                        weightKg = weight
                                    )
                                },
                                onBack = { viewModel.setWizardSubStep(WizardSubStep.STATUS) }
                            )
                        }
                        WizardSubStep.STOPPAGE -> {
                            // ==========================================
                            // Screen 3-B: Fast One-Tap Stoppage Screen
                            // ==========================================
                            MachineStoppageEntryView(
                                asset = currentAsset,
                                machineIndex = uiState.wizardAssetIndex,
                                totalMachines = uiState.assets.size,
                                isLastMachine = uiState.wizardAssetIndex == uiState.assets.size - 1,
                                onSaveStoppage = { reasonCode, reasonDisplay, durationMinutes, notes, partialWeight ->
                                    viewModel.saveFawryStoppage(
                                        asset = currentAsset,
                                        stoppageType = reasonCode,
                                        stoppageTypeDisplay = reasonDisplay,
                                        durationMinutes = durationMinutes,
                                        notes = notes,
                                        partialWeightKg = partialWeight
                                    )
                                },
                                onBack = { viewModel.setWizardSubStep(WizardSubStep.STATUS) }
                            )
                        }
                        WizardSubStep.SUMMARY -> {
                            // Handled above
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا توجد ماكينات مسجلة في هذا المصنع", color = FactoryTextMuted)
                    }
                }
            }

            // Exceptions Dialog
            uiState.exceptionAssetForEdit?.let { asset ->
                MachineExceptionDialog(
                    asset = asset,
                    operators = uiState.operators,
                    products = uiState.products,
                    currentOperatorId = uiState.entries[asset.id]?.operatorId ?: asset.defaultOperatorId,
                    currentProductId = uiState.entries[asset.id]?.productId ?: asset.defaultProductId,
                    currentCavities = uiState.entries[asset.id]?.currentCavities ?: asset.originalCavities,
                    onDismiss = { viewModel.closeExceptionDialog() },
                    onConfirm = { opId, prodId, cavs ->
                        // Save or update existing entry with exception values
                        val existingWeight = uiState.entries[asset.id]?.finalProductionWeightKg ?: 0.0
                        viewModel.saveFawryWorkingEntry(
                            asset = asset,
                            weightKg = existingWeight,
                            customOperatorId = opId,
                            customProductId = prodId,
                            customCavities = cavs
                        )
                    }
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
        }
    }
}

/**
 * شريط تتبع الماكينات في أعلى الشاشة:
 * يعرض رقماً ملوناً لكل ماكينة:
 * - أخضر: مسجلة وشغالة
 * - أحمر/برتقالي: مسجلة كعطل
 * - رمادي: لم تسجل بعد
 * مع إمكانية الضغط على أي ماكينة للانتقال إليها مباشرة
 */
@Composable
private fun WizardStepBar(
    assets: List<AssetEntity>,
    currentIndex: Int,
    entries: Map<Int, MachineEntryEntity>,
    stoppages: List<StoppageEntity>,
    isSummary: Boolean,
    onSelectMachine: (Int) -> Unit,
    onSelectSummary: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSummary) "ملخص الوردية والتسليم" else "الماكينة ${currentIndex + 1} من ${assets.size}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                val recordedCount = entries.size
                Text(
                    text = "تم تسجيل $recordedCount من ${assets.size}",
                    color = if (recordedCount == assets.size) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(assets) { index, asset ->
                    val isCurrent = !isSummary && index == currentIndex
                    val isRecorded = entries[asset.id] != null
                    val hasStoppage = stoppages.any { it.assetId == asset.id }

                    val bgColor = when {
                        isCurrent -> FactoryNavyLight
                        hasStoppage -> FactoryOrange.copy(alpha = 0.25f)
                        isRecorded -> FactoryGreen.copy(alpha = 0.25f)
                        else -> Color(0xFF1E293B)
                    }

                    val borderColor = when {
                        isCurrent -> Color.White
                        hasStoppage -> FactoryOrange
                        isRecorded -> FactoryGreen
                        else -> Color(0xFF334155)
                    }

                    val textColor = when {
                        isCurrent -> Color.White
                        hasStoppage -> FactoryOrange
                        isRecorded -> Color(0xFF34D399)
                        else -> Color(0xFF94A3B8)
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectMachine(index) },
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "م ${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                color = textColor
                            )
                            if (isRecorded) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = if (hasStoppage) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                // Summary Chip
                item {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectSummary() },
                        color = if (isSummary) FactoryGreen else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSummary) Color.White else Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Assessment,
                                contentDescription = null,
                                tint = if (isSummary) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الملخص",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSummary) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * الشاشة 1: شاشة البدء والترحيب بالوردية (Shift Welcome)
 */
@Composable
private fun ShiftWelcomeView(
    uiState: ProductionHomeUiState,
    onStartWizard: () -> Unit,
    onSetShift: (String) -> Unit,
    onOpenHandover: () -> Unit,
    onAddFridayPrayer: () -> Unit,
    onGoToSummary: () -> Unit
) {
    val scrollState = rememberScrollState()
    val recordedCount = uiState.entries.size
    val totalMachines = uiState.assets.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Handover Banner Alert if pending from previous shift
        uiState.pendingHandover?.let { pending ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenHandover() },
                colors = CardDefaults.cardColors(containerColor = FactoryLightOrange),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, FactoryOrange)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(FactoryOrange.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Handshake, contentDescription = null, tint = FactoryOrange, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "وردية بانتظار استلامك!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = FactoryDark
                        )
                        Text(
                            text = "الزميل المسلّم: ${pending.supervisorName} (${pending.shiftDisplay})",
                            fontSize = 12.sp,
                            color = FactoryTextSecondary
                        )
                    }
                    Button(
                        onClick = onOpenHandover,
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("استلام", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }

        // Welcome Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FactorySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Supervisor Avatar Circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(FactoryNavy, FactoryNavyLight)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val userName = uiState.currentUser?.name?.ifBlank { null } ?: "المستخدم"
                Text(
                    text = "مرحباً بك: $userName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = FactorySurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "مصنع: ${uiState.factoryName.ifBlank { "المصنع" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FactoryTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Single Shift Badge - إظهار وردية العمل المخصصة فقط دون إرباك المستخدم بباقي الورادي
                val shiftName = when (uiState.selectedShift) {
                    "FIRST" -> "الوردية الأولى (صباحية)"
                    "SECOND" -> "الوردية الثانية (مسائية)"
                    "THIRD" -> "الوردية الثالثة (ليلية)"
                    else -> uiState.selectedShift
                }
                Surface(
                    color = FactoryLightGreen,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactoryGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وردية العمل: $shiftName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryGreenDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Factory Machines Counter Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FactorySurfaceVariant)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("إجمالي الماكينات", fontSize = 12.sp, color = FactoryTextMuted)
                        Text("$totalMachines ماكينة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FactoryDark)
                    }

                    VerticalDivider(modifier = Modifier.height(30.dp), color = FactoryCardBorder)

                    Column {
                        Text("تم تسجيلها", fontSize = 12.sp, color = FactoryTextMuted)
                        Text(
                            "$recordedCount ماكينة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (recordedCount == totalMachines) FactoryGreen else FactoryOrange
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(30.dp), color = FactoryCardBorder)

                    Column {
                        val totalKg = uiState.entries.values.sumOf { it.finalProductionWeightKg }
                        Text("إجمالي الإنتاج", fontSize = 12.sp, color = FactoryTextMuted)
                        Text("${totalKg.toInt()} كجم", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FactoryTeal)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Giant Green Start Button
                Button(
                    onClick = onStartWizard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (recordedCount == 0) "🚀 ابدأ تسجيل ماكينات الوردية الآن" else "متابعة تسجيل الماكينات ($recordedCount / $totalMachines)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                if (recordedCount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onGoToSummary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = FactoryNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عرض ملخص الوردية والتسليم النهائي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FactoryNavy)
                    }
                }
            }
        }

        // Quick Friday Prayer Card (يظهر يوم الجمعة فقط)
        val isFriday = try {
            java.time.LocalDate.now().dayOfWeek == java.time.DayOfWeek.FRIDAY
        } catch (_: Exception) {
            false
        }
        if (isFriday) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddFridayPrayer() },
                shape = RoundedCornerShape(14.dp),
                color = FactoryLightTeal,
                border = androidx.compose.foundation.BorderStroke(1.dp, FactoryTeal.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🕌", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تسجيل إيقاف صلاة الجمعة لجميع الماكينات",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryTeal
                        )
                        Text(
                            text = "إيقاف جماعي لمدة 60 دقيقة بلمسة واحدة",
                            fontSize = 11.sp,
                            color = FactoryTextSecondary
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = FactoryTeal)
                }
            }
        }
    }
}

/**
 * الشاشة 2: السؤال الرئيسي للماكينة (Machine Status Question)
 * سؤال واحد وواضح: "ما هي حالة الماكينة في ورديتك؟"
 * زر أخضر عملاق: الماكينة شغالة ومطابقة تمام
 * زر أحمر عريض: الماكينة كانت عطلانة / متوقفة
 */
@Composable
private fun MachineStatusQuestionView(
    asset: AssetEntity,
    machineIndex: Int,
    totalMachines: Int,
    existingEntry: MachineEntryEntity?,
    stoppages: List<StoppageEntity>,
    onWorkingClick: () -> Unit,
    onStoppedClick: () -> Unit,
    onOpenException: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Machine Big Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FactorySurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, FactoryCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Machine Number Tag
                    Surface(
                        color = FactoryNavy,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ماكينة: ${asset.assetCode}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = asset.productionTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FactoryDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Machine Default Info Tags (المنتج المعتاد والعامل المعتاد)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FactorySurfaceVariant)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📦", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("المنتج المعتاد: ", fontSize = 13.sp, color = FactoryTextMuted)
                            Text(
                                text = asset.defaultProductName ?: "غير محدد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("العامل المعتاد: ", fontSize = 13.sp, color = FactoryTextMuted)
                            Text(
                                text = asset.defaultOperatorName ?: "غير محدد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚙️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اللقم المشغلة: ", fontSize = 13.sp, color = FactoryTextMuted)
                            Text(
                                text = "${asset.originalCavities} لقمة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryDark
                            )
                        }
                    }

                    // Existing Recorded State Banner (if already recorded)
                    if (existingEntry != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isStoppage = stoppages.isNotEmpty()
                        Surface(
                            color = if (isStoppage) FactoryLightOrange else FactoryLightGreen,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isStoppage) FactoryOrange else FactoryGreen
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isStoppage) "⚠️ مسجلة كعطل: ${stoppages.first().stoppageTypeDisplay}" else "✅ مسجلة بوزن: ${existingEntry.finalProductionWeightKg} كجم",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStoppage) FactoryOrangeDark else FactoryGreenDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The Main Big Question
            Text(
                text = "ما هي حالة الماكينة في ورديتك؟",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = FactoryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Giant Green Button: شغالة ومطابقة تمام
            Button(
                onClick = onWorkingClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("✅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "الماكينة شغالة ومطابقة تمام",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "اضغط لكتابة وزن الإنتاج بالكيلو",
                            fontSize = 11.sp,
                            color = Color(0xFFD1FAE5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Giant Red Button: الماكينة كانت عطلانة / متوقفة
            Button(
                onClick = onStoppedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FactoryRed),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("❌", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "الماكينة كانت عطلانة / متوقفة",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "اضغط لاختيار سبب التوقف بلمسة واحدة",
                            fontSize = 11.sp,
                            color = Color(0xFFFEE2E2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Small Optional Exception Button: تم تغيير الاسطمبة أو العامل
            OutlinedButton(
                onClick = onOpenException,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FactoryNavyLight)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = FactoryNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚙️ تم تغيير الاسطمبة أو العامل المشغل",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryNavy
                )
            }
        }

        // Bottom Navigation Row (السابق / التالي)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrevious,
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = FactoryTextMuted)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (machineIndex == 0) "رجوع للبدء" else "السابق",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryTextMuted
                )
            }

            if (machineIndex < totalMachines - 1) {
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("تخطي للماكينة التالية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FactoryNavy)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = FactoryNavy)
                }
            }
        }
    }
}

/**
 * الشاشة 3-أ: إدخال الوزن (لوحة أرقام فوري / ATM)
 * تظهر عند اختيار "الماكينة شغالة ومطابقة تمام"
 */
@Composable
private fun MachineWeightEntryView(
    asset: AssetEntity,
    machineIndex: Int,
    totalMachines: Int,
    initialWeight: Double,
    isLastMachine: Boolean,
    onSaveWeight: (Double) -> Unit,
    onBack: () -> Unit
) {
    var weightString by remember(asset.id) {
        val initial = if (initialWeight > 0.0) {
            if (initialWeight % 1.0 == 0.0) initialWeight.toInt().toString() else initialWeight.toString()
        } else ""
        mutableStateOf(initial)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = FactoryDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ماكينة: ${asset.assetCode}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FactoryDark
                    )
                    Text(
                        text = "اكتب إجمالي الوزن المنتج للوردية بالكيلو",
                        fontSize = 12.sp,
                        color = FactoryTextSecondary
                    )
                }
                Box(modifier = Modifier.size(48.dp)) // Placeholder for balance
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The ATM Numpad Composable
            AtmNumpad(
                value = weightString,
                onValueChange = { weightString = it },
                unit = "كجم"
            )
        }

        // Giant Action Button: حفظ والانتقال للماكينة التالية
        val enteredWeight = weightString.toDoubleOrNull() ?: 0.0
        val isValid = enteredWeight > 0.0

        Button(
            onClick = {
                if (isValid) {
                    onSaveWeight(enteredWeight)
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = if (isValid) 6.dp else 0.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FactoryGreen,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLastMachine) "✅ حفظ ومراجعة الوردية ➔" else "التالي: ماكينة ${machineIndex + 2} ➔",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * الشاشة 3-ب: تسجيل العطل بضغطة واحدة (في حالة اختيار عطلانة)
 * اختيار سريع ومصور لسبب التوقف والمدة بنقرة واحدة
 */
@Composable
private fun MachineStoppageEntryView(
    asset: AssetEntity,
    machineIndex: Int,
    totalMachines: Int,
    isLastMachine: Boolean,
    onSaveStoppage: (reasonCode: String, reasonDisplay: String, durationMinutes: Int, notes: String, partialWeight: Double) -> Unit,
    onBack: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("MACHINE_BREAKDOWN") }
    var selectedReasonDisplay by remember { mutableStateOf("عطل ميكانيكي / كهرباء") }
    var selectedDurationMinutes by remember { mutableStateOf(60) }
    var partialWeightString by remember { mutableStateOf("") }
    var showWeightInput by remember { mutableStateOf(false) }

    val isFriday = try {
        java.time.LocalDate.now().dayOfWeek == java.time.DayOfWeek.FRIDAY
    } catch (_: Exception) {
        false
    }

    val stoppageReasons = remember(isFriday) {
        buildList {
            add(Triple("MACHINE_BREAKDOWN", "عطل ميكانيكي / كهرباء", "🔧"))
            add(Triple("COOLING_HEATING", "عطل تبريد / حرارة", "❄️"))
            add(Triple("MOLD_CHANGE", "تغيير اسطمبة / صيانة", "🔄"))
            if (isFriday) {
                add(Triple("FRIDAY_PRAYER", "صلاة الجمعة / راحة", "🕌"))
            }
            add(Triple("RAW_MATERIAL", "نقص خامات / مواد", "📦"))
            add(Triple("OTHER", "عطل فني آخر", "⚠️"))
        }
    }

    val durationPresets = listOf(
        15 to "ربع ساعة (15 د)",
        30 to "نصف ساعة (30 د)",
        60 to "ساعة كاملة (60 د)",
        120 to "ساعتين (120 د)",
        480 to "الوردية كاملة (8 س)"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = FactoryDark)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ماكينة: ${asset.assetCode}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = FactoryDark
                    )
                    Text(
                        text = "ما هو سبب توقف الماكينة؟",
                        fontSize = 13.sp,
                        color = FactoryRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Stoppage Reason Cards Grid (2 columns)
            Text(
                text = "1. اختر سبب العطل:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = FactoryDark,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chunked = stoppageReasons.chunked(2)
                chunked.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (code, label, icon) ->
                            val isSelected = selectedReason == code
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .clickable {
                                        selectedReason = code
                                        selectedReasonDisplay = label
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) FactoryLightRed else FactorySurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) FactoryRed else FactoryCardBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) FactoryRed else FactoryDark,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Duration Selector
            Text(
                text = "2. مدة التوقف التقريبية:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = FactoryDark,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationPresets.forEach { (mins, label) ->
                    val isSelected = selectedDurationMinutes == mins
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable { selectedDurationMinutes = mins },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) FactoryNavy else FactorySurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) FactoryNavy else FactoryCardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else FactoryDark
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Optional partial weight produced before breakdown
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWeightInput = !showWeightInput },
                shape = RoundedCornerShape(10.dp),
                color = FactorySurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚖️ هل أنتجت الماكينة أي وزن قبل التوقف؟",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FactoryTextSecondary
                    )
                    Text(
                        text = if (showWeightInput) "إخفاء" else "نعم، سجل وزن",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FactoryPrimaryBlue
                    )
                }
            }

            if (showWeightInput) {
                Spacer(modifier = Modifier.height(8.dp))
                AtmNumpad(
                    value = partialWeightString,
                    onValueChange = { partialWeightString = it },
                    unit = "كجم منتج قبل العطل"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Giant Action Button: حفظ العطل والتالي
        Button(
            onClick = {
                val partialWeight = partialWeightString.toDoubleOrNull() ?: 0.0
                onSaveStoppage(
                    selectedReason,
                    selectedReasonDisplay,
                    selectedDurationMinutes,
                    "",
                    partialWeight
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FactoryOrange)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isLastMachine) "✅ حفظ العطل ومراجعة الوردية ➔" else "حفظ العطل والتالي: ماكينة ${machineIndex + 2} ➔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * الشاشة 4: شاشة المراجعة والتسليم النهائي (Shift Summary & Handover)
 */
@Composable
private fun ShiftSummaryView(
    uiState: ProductionHomeUiState,
    onEditMachine: (Int) -> Unit,
    onFinishShift: (String) -> Unit,
    onBackToMachines: () -> Unit
) {
    val scrollState = rememberScrollState()
    val totalMachines = uiState.assets.size
    val recordedCount = uiState.entries.size
    val totalWeight = uiState.entries.values.sumOf { it.finalProductionWeightKg }
    val stoppageCount = uiState.stoppages.mapNotNull { it.assetId }.distinct().size
    val workingCount = (recordedCount - stoppageCount).coerceAtLeast(0)

    var handoverNotes by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Celebration Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FactoryLightGreen),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, FactoryGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الله ينور عليك! تم التسجيل بنجاح 👏",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = FactoryGreenDark
                )
                Text(
                    text = "تم تسجيل ماكينات الوردية بالكامل بنجاح",
                    fontSize = 13.sp,
                    color = FactoryTextSecondary
                )
            }
        }

        // Summary Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FactorySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "📊 ملخص الوردية السريع:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryMetricPill(
                        label = "المسجل",
                        value = "$recordedCount من $totalMachines",
                        color = FactoryNavy
                    )
                    SummaryMetricPill(
                        label = "إجمالي الوزن",
                        value = "${totalWeight.toInt()} كجم",
                        color = FactoryGreen
                    )
                    SummaryMetricPill(
                        label = "عطلانة",
                        value = "$stoppageCount ماكينة",
                        color = if (stoppageCount > 0) FactoryRed else FactoryGreen
                    )
                }
            }
        }

        // Machine Review List (Expandable or list with 1-tap edit)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FactorySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "مراجعة الماكينات (اضغط على أي ماكينة لتعديلها):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                uiState.assets.forEachIndexed { index, asset ->
                    val entry = uiState.entries[asset.id]
                    val stoppage = uiState.stoppages.find { it.assetId == asset.id }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onEditMachine(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = FactorySurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (stoppage != null) "❌" else if (entry != null) "✅" else "⚪",
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ماكينة: ${asset.assetCode}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FactoryDark
                                    )
                                    Text(
                                        text = if (stoppage != null) "عطل: ${stoppage.stoppageTypeDisplay} (${stoppage.durationMinutes} د)" else if (entry != null) "إنتاج: ${entry.finalProductionWeightKg} كجم" else "لم تسجل بعد",
                                        fontSize = 11.sp,
                                        color = if (stoppage != null) FactoryOrangeDark else if (entry != null) FactoryGreenDark else FactoryTextMuted
                                    )
                                }
                            }

                            TextButton(onClick = { onEditMachine(index) }) {
                                Text("تعديل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FactoryPrimaryBlue)
                            }
                        }
                    }
                }
            }
        }

        // Handover Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = FactorySurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val nextShiftName = when (uiState.selectedShift) {
                    "FIRST" -> "الوردية الثانية (مسائية)"
                    "SECOND" -> "الوردية الثالثة (ليلية)"
                    "THIRD" -> "الوردية الأولى (صباحية)"
                    else -> "الوردية التالية"
                }

                Text(
                    text = "🤝 تسليم الوردية للزميل المستلم:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FactoryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الوردية المستلمة: $nextShiftName",
                    fontSize = 12.sp,
                    color = FactoryTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Notes Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("الماكينات مستقرة ونظيفة", "تم فحص الاسطمبات", "جاهزة للتشغيل").forEach { tag ->
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    handoverNotes = if (handoverNotes.isBlank()) tag else "$handoverNotes • $tag"
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = FactorySurfaceVariant
                        ) {
                            Text(
                                text = "+ $tag",
                                fontSize = 11.sp,
                                color = FactoryTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = handoverNotes,
                    onValueChange = { handoverNotes = it },
                    placeholder = { Text("اكتب ملاحظات تسليم الوردية (اختياري)...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }
        }

        // Giant Green Handover Button
        Button(
            onClick = {
                onFinishShift(handoverNotes)
                isSubmitted = true
            },
            enabled = !isSubmitted,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FactoryNavy),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSubmitted) "تم التسليم بنجاح ✅" else "🤝 تسليم الوردية للزميل المستلم",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        TextButton(onClick = onBackToMachines) {
            Text("العودة لمراجعة الماكينات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FactoryTextMuted)
        }
    }
}

@Composable
private fun SummaryMetricPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = FactoryTextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
