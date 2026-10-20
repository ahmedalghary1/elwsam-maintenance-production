package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maintenance.supervisor.domain.model.Asset
import com.maintenance.supervisor.domain.model.ChecklistItem
import com.maintenance.supervisor.domain.model.EmergencyMaintenance
import com.maintenance.supervisor.domain.model.SyncStatus
import com.maintenance.supervisor.ui.InspectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InspectionScreen(vm: InspectionViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by vm.state.collectAsState(); val daily = state.home?.daily; val report = daily?.report
    val answerMap = report?.answers?.associateBy { it.checklistItemId }.orEmpty()
    val isError = report?.status == SyncStatus.SYNC_ERROR
    val isLocked = report?.isLocked == true
    val totalItems = daily?.sections?.sumOf { it.items.size } ?: 0
    val checkedItems = answerMap.values.count { it.checked }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column {
            Text(daily?.asset?.maintenanceTitle ?: "الصيانة الدورية", fontWeight = FontWeight.Bold)
            Text(
                when {
                    isError -> "تعارض مع الخادم - يجب إعادة التعيين"
                    isLocked -> "تقرير مقفل - للعرض فقط"
                    else -> "يُحفظ كل تغيير تلقائيًا"
                },
                fontSize = 13.sp,
                color = if (isError || isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
    }, bottomBar = {
        if (isError) {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(56.dp)) {
                        Text("رجوع")
                    }
                    Button(
                        onClick = { vm.resetTodayReport(onBack) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(2f).height(56.dp)
                    ) {
                        Icon(Icons.Outlined.RestartAlt, null)
                        Spacer(Modifier.width(6.dp))
                        Text("إعادة التعيين للماكينة المحدثة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (!isLocked) {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) { Button(onClick = { vm.complete(onSaved) }, enabled = report != null && !state.saving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(58.dp)) {
                if (state.saving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.CheckCircle, null)
                Spacer(Modifier.width(8.dp)); Text(if (state.saving) "جاري الحفظ" else "حفظ وإنهاء الفحص", fontSize = 20.sp)
            } }
        } else {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(16.dp).height(62.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("هذا التقرير مقفل ولا يمكن تعديله", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }) { padding ->
        if (daily == null || report == null) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isError) {
                item(key = "conflict-warning-card") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "تعارض في التقرير مع بيانات الخادم",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                report.lastError ?: "الماكينة المسجلة في هذا التقرير لا تطابق الماكينة المطلوبة اليوم. يرجى إعادة التعيين للبدء في فحص الماكينة المحددة.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { vm.resetTodayReport(onBack) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Outlined.RestartAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("إعادة التعيين وبدء فحص الماكينة المحدثة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item(key = "progress") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("تقدم الاختيارات في التقرير", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("يمكنك إضافة ملاحظة لأي بند", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f)) }
                            val percent = if (totalItems == 0) 0 else ((checkedItems.toFloat() / totalItems) * 100).toInt()
                            Text("$checkedItems / $totalItems ($percent%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(11.dp)); LinearProgressIndicator(progress = { if (totalItems == 0) 0f else checkedItems.toFloat() / totalItems }, Modifier.fillMaxWidth().height(8.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
            state.error?.let { message ->
                item(key = "save-error") { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            daily.sections.forEach { section ->
                item(key = "s${section.id}") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section.title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) { Text("${section.items.count { answerMap[it.id]?.checked == true }} / ${section.items.size}", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                val uiItems = section.items.map { it to answerMap[it.id] }
                items(uiItems, key = { it.first.id }) { (item, answer) ->
                    ChecklistRow(item, answer?.checked == true, answer?.note.orEmpty(), isLocked) { checked, note -> vm.update(item.id, checked, note) }
                }
            }
            item(key = "emergency_section") {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))
                EmergencyMaintenanceSection(
                    emergencyItems = state.emergencyItems,
                    availableAssets = state.home?.availableAssets.orEmpty(),
                    defaultAssetId = daily.asset.id,
                    isLocked = isLocked,
                    onAdd = { vm.addEmergency(it) },
                    onUpdate = { index, item -> vm.updateEmergency(index, item) },
                    onRemove = { index -> vm.removeEmergency(index) }
                )
            }
            item(key = "personnel_section") {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(Modifier.height(14.dp))
                MaintenancePersonnelSection(
                    supervisorName = state.home?.user?.displayName.orEmpty(),
                    cleanerName = state.cleanerName,
                    mechanicalTechnician = state.mechanicalTechnician,
                    electricalTechnician = state.electricalTechnician,
                    maintenanceManager = state.maintenanceManager,
                    isLocked = isLocked,
                    onCleanerChange = { vm.updateCleanerName(it) },
                    onMechanicalChange = { vm.updateMechanicalTechnician(it) },
                    onElectricalChange = { vm.updateElectricalTechnician(it) },
                    onManagerChange = { vm.updateMaintenanceManager(it) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun EmergencyMaintenanceSection(
    emergencyItems: List<EmergencyMaintenance>,
    availableAssets: List<Asset>,
    defaultAssetId: Int,
    isLocked: Boolean,
    onAdd: (Int) -> Unit,
    onUpdate: (Int, EmergencyMaintenance) -> Unit,
    onRemove: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("الصيانة الطارئة (اختياري)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("سجل أي أعطال طارئة حدثت خلال الوردية مع تحديد الماكينة أو المكبس، أو اتركها فارغة.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (emergencyItems.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text("${emergencyItems.size} مسجل", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (emergencyItems.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Build, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("لا توجد صيانة طارئة مسجلة لهذا اليوم.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            emergencyItems.forEachIndexed { index, item ->
                EmergencyItemCard(
                    index = index,
                    item = item,
                    availableAssets = availableAssets,
                    isLocked = isLocked,
                    onUpdate = { updated -> onUpdate(index, updated) },
                    onRemove = { onRemove(index) }
                )
            }
        }

        if (!isLocked) {
            OutlinedButton(
                onClick = { onAdd(defaultAssetId) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Outlined.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("إضافة صيانة طارئة")
            }
        }
    }
}

@Composable
private fun EmergencyItemCard(
    index: Int,
    item: EmergencyMaintenance,
    availableAssets: List<Asset>,
    isLocked: Boolean,
    onUpdate: (EmergencyMaintenance) -> Unit,
    onRemove: () -> Unit
) {
    var showAssetPicker by remember { mutableStateOf(false) }
    val currentAsset = availableAssets.find { it.id == item.assetId }

    if (showAssetPicker) {
        AlertDialog(
            onDismissRequest = { showAssetPicker = false },
            title = { Text("اختيار الماكينة أو المكبس") },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(availableAssets, key = { it.id }) { asset ->
                        ListItem(
                            headlineContent = { Text(asset.code, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(asset.normalizedTypeName) },
                            leadingContent = {
                                RadioButton(selected = asset.id == item.assetId, onClick = null)
                            },
                            modifier = Modifier.clickable {
                                showAssetPicker = false
                                onUpdate(item.copy(assetId = asset.id))
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssetPicker = false }) { Text("إلغاء") }
            }
        )
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("بند صيانة طارئة #${index + 1}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (!isLocked) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) { showAssetPicker = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الماكينة / المكبس", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            currentAsset?.let { "${it.code} (${it.normalizedTypeName})" } ?: "اختر الماكينة أو المكبس",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isLocked) {
                        Icon(Icons.Outlined.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(
                value = item.issueDescription,
                onValueChange = { newText ->
                    if (!isLocked) onUpdate(item.copy(issueDescription = newText))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("العطل *") },
                placeholder = { Text("وصف العطل الطارئ...") },
                leadingIcon = { Icon(Icons.Outlined.WarningAmber, null) },
                singleLine = true,
                readOnly = isLocked
            )

            OutlinedTextField(
                value = item.responsiblePerson,
                onValueChange = { newText ->
                    if (!isLocked) onUpdate(item.copy(responsiblePerson = newText))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("المسؤول *") },
                placeholder = { Text("اسم الفني أو المشرف المسؤول...") },
                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                singleLine = true,
                readOnly = isLocked
            )

            OutlinedTextField(
                value = item.notes,
                onValueChange = { newText ->
                    if (!isLocked) onUpdate(item.copy(notes = newText))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ملاحظات") },
                placeholder = { Text("ملاحظات إضافية أو إجراء متخذ...") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) },
                minLines = 1,
                maxLines = 3,
                readOnly = isLocked
            )
        }
    }
}

@Composable private fun ChecklistRow(item: ChecklistItem, initialChecked: Boolean, initialNote: String, isLocked: Boolean, onChange: (Boolean, String) -> Unit) {
    var checked by remember(initialChecked) { mutableStateOf(initialChecked) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.secondary.copy(alpha = .28f) else MaterialTheme.colorScheme.outline.copy(alpha = .65f))
    ) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).then(
            if (isLocked) Modifier else Modifier.clickable {
                val newVal = !checked; checked = newVal; onChange(newVal, note)
            }
        ), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange = if (isLocked) null else { newVal -> checked = newVal; onChange(newVal, note) }, Modifier.size(48.dp), enabled = !isLocked)
            Spacer(Modifier.width(8.dp)); Text(item.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
        }
        OutlinedTextField(note, onValueChange = if (isLocked) { _ -> } else { newNote -> note = newNote; onChange(checked, newNote) },
            Modifier.fillMaxWidth(), label = { Text("ملاحظة اختيارية") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) }, minLines = 1, maxLines = 3, readOnly = isLocked)
    } }
}

@Composable
private fun MaintenancePersonnelSection(
    supervisorName: String,
    cleanerName: String,
    mechanicalTechnician: String,
    electricalTechnician: String,
    maintenanceManager: String,
    isLocked: Boolean,
    onCleanerChange: (String) -> Unit,
    onMechanicalChange: (String) -> Unit,
    onElectricalChange: (String) -> Unit,
    onManagerChange: (String) -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.SupervisorAccount,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "فريق الصيانة والاعتماد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "تسجيل القائمين بالصيانة والاعتماد بنهاية التقرير",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (supervisorName.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "مشرف الفحص المسؤول:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                supervisorName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = cleanerName,
                onValueChange = onCleanerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("القائم بالنظافة") },
                placeholder = { Text("اسم القائم بالنظافة...") },
                leadingIcon = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                singleLine = true,
                readOnly = isLocked
            )

            OutlinedTextField(
                value = mechanicalTechnician,
                onValueChange = onMechanicalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("القائم بالصيانة الميكانيكية") },
                placeholder = { Text("اسم فني الصيانة الميكانيكية...") },
                leadingIcon = { Icon(Icons.Outlined.Build, contentDescription = null) },
                singleLine = true,
                readOnly = isLocked
            )

            OutlinedTextField(
                value = electricalTechnician,
                onValueChange = onElectricalChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("القائم بالصيانة الكهربية") },
                placeholder = { Text("اسم فني الصيانة الكهربية...") },
                leadingIcon = { Icon(Icons.Outlined.Bolt, contentDescription = null) },
                singleLine = true,
                readOnly = isLocked
            )

            OutlinedTextField(
                value = maintenanceManager,
                onValueChange = onManagerChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مدير الصيانة") },
                placeholder = { Text("اسم مدير الصيانة للاعتماد...") },
                leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                singleLine = true,
                readOnly = isLocked
            )
        }
    }
}


