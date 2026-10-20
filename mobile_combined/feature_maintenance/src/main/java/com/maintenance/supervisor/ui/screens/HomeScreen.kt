package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.maintenance.supervisor.domain.model.DailyMaintenance
import com.maintenance.supervisor.domain.model.SyncStatus
import com.maintenance.supervisor.ui.HomeViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeScreen(vm: HomeViewModel, onInspect: () -> Unit, onLogout: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.onResume()
    }
    val state by vm.state.collectAsState()
    val daily = state.snapshot.daily
    var menu by remember { mutableStateOf(false) }
    var showAssetPicker by remember { mutableStateOf(false) }

    if (showAssetPicker) {
        AlertDialog(
            onDismissRequest = { showAssetPicker = false },
            title = { Text("اختيار ماكينة الصيانة") },
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(state.snapshot.availableAssets, key = { it.id }) { asset ->
                        ListItem(
                            headlineContent = { Text(asset.code, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(asset.normalizedTypeName) },
                            leadingContent = { RadioButton(selected = asset.id == daily?.asset?.id, onClick = null) },
                            modifier = Modifier.clickable {
                                showAssetPicker = false
                                vm.selectAsset(asset.id)
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAssetPicker = false }) { Text("إلغاء") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("إدارة الصيانة", fontWeight = FontWeight.Bold)
                        val subTitle = state.snapshot.user?.displayName?.takeIf { it.isNotBlank() } ?: "لوحة مشرف الصيانة"
                        Text(subTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "القائمة") }
                    DropdownMenu(menu, { menu = false }) {
                        state.snapshot.user?.let { u ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(u.displayName, fontWeight = FontWeight.Bold)
                                        Text(u.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = { },
                                leadingIcon = { Icon(Icons.Outlined.Person, null) },
                                enabled = false
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem({ Text("مزامنة البيانات") }, { menu = false; vm.refresh() }, leadingIcon = { Icon(Icons.Outlined.Sync, null) })
                        DropdownMenuItem({ Text("تسجيل الخروج") }, { menu = false; vm.logout(onLogout) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    val displayName = state.snapshot.user?.displayName?.takeIf { it.isNotBlank() }
                    if (displayName != null) {
                        Text("مرحبًا، $displayName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("مرحبًا بك", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(state.snapshot.factory?.name ?: "بيانات المصنع غير متاحة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
            item { SyncBanner(state.refreshing, state.connected, daily?.report?.status, daily?.report?.lastError, vm::refresh) }
            state.message?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp)); Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (daily?.report?.status == SyncStatus.SYNC_ERROR) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "تعارض في مزامنة التقرير",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                daily.report.lastError ?: "البيانات المسجلة تتعارض مع الخادم (ربما تم تغيير ماكينة اليوم من لوحة التحكم). يمكنك إعادة التعيين لبدء الفحص للماكينة المحدثة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { vm.resetTodayReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Icon(Icons.Outlined.RestartAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("إعادة التعيين وبدء فحص الماكينة المحدثة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                if (daily == null) EmptyMaintenanceCard(state.snapshot.factory == null, state.snapshot.isMaintenanceDay, vm::refresh)
                else DailyMaintenanceCard(
                    daily = daily,
                    selectionMode = state.snapshot.selectionMode,
                    canChange = state.snapshot.availableAssets.size > 1,
                    onOpen = { vm.start(onInspect) },
                    onChange = { showAssetPicker = true },
                    onReset = { vm.resetTodayReport() }
                )
            }
            state.snapshot.lastSync?.let { value ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp)); Text("آخر تحديث: ${value.take(16).replace('T', ' ')}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun SyncBanner(refreshing: Boolean, connected: Boolean, status: SyncStatus?, error: String?, onRefresh: () -> Unit) {
    val isError = status == SyncStatus.SYNC_ERROR
    val container = when { isError -> MaterialTheme.colorScheme.errorContainer; !connected -> MaterialTheme.colorScheme.surfaceVariant; else -> MaterialTheme.colorScheme.secondaryContainer }
    val content = when { isError -> MaterialTheme.colorScheme.onErrorContainer; !connected -> MaterialTheme.colorScheme.onSurfaceVariant; else -> MaterialTheme.colorScheme.onSecondaryContainer }
    val text = when {
        refreshing -> "جاري تحديث البيانات..."
        !connected -> "أنت تعمل دون إنترنت — بياناتك محفوظة"
        else -> syncLabel(status, error)
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = content)
            else Icon(if (!connected) Icons.Outlined.CloudOff else if (isError) Icons.Outlined.CloudSync else Icons.Outlined.CloudDone, null, Modifier.size(23.dp))
            Spacer(Modifier.width(10.dp)); Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh, enabled = !refreshing) { Icon(Icons.Outlined.Refresh, "تحديث") }
        }
    }
}

@Composable private fun DailyMaintenanceCard(
    daily: DailyMaintenance,
    selectionMode: String,
    canChange: Boolean,
    onOpen: () -> Unit,
    onChange: () -> Unit,
    onReset: () -> Unit
) {
    val report = daily.report
    val isError = report?.status == SyncStatus.SYNC_ERROR
    val locked = report?.isLocked == true
    val completed = report?.completedAt != null
    val totalItems = daily.sections.sumOf { it.items.size }
    val answerMap = report?.answers?.associateBy { it.checklistItemId }.orEmpty()
    val checkedItems = answerMap.values.count { it.checked }
    val progressFraction = if (totalItems > 0) (checkedItems.toFloat() / totalItems).coerceIn(0f, 1f) else 0f
    val percent = (progressFraction * 100).toInt()
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when {
                        isError -> MaterialTheme.colorScheme.errorContainer
                        completed -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        when {
                            isError -> Icons.Outlined.WarningAmber
                            completed -> Icons.Outlined.TaskAlt
                            else -> Icons.Outlined.BuildCircle
                        },
                        null,
                        Modifier.padding(11.dp).size(28.dp),
                        tint = when {
                            isError -> MaterialTheme.colorScheme.error
                            completed -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            isError -> "تعارض أو خطأ في المزامنة"
                            locked -> "تقرير اليوم"
                            completed -> "تم إنجاز فحص اليوم"
                            report != null -> "فحص قيد التنفيذ"
                            else -> "مهمة اليوم"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    Text(daily.reportDate.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale.forLanguageTag("ar-EG"))), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (locked) Icon(Icons.Outlined.Lock, "مقفل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                daily.asset.maintenanceTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        daily.asset.normalizedTypeName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        "كود: ${daily.asset.code}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    if (selectionMode == "manual") "تم اختيارها يدويًا" else "محددة تلقائيًا حسب الدور",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        completed -> "تم إنجاز فحص اليوم"
                        report != null -> "تقدم الاختيارات في التقرير"
                        else -> "خيارات الفحص المقررة"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (totalItems > 0) "$checkedItems من $totalItems بند ($percent%)" else "لا توجد بنود",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ترتيب الماكينة في دورة المصنع: ${daily.position} من ${daily.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report != null && !completed && totalItems > checkedItems) {
                    Text(
                        "متبقي ${totalItems - checkedItems} بند",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f))
            Spacer(Modifier.height(16.dp))

            if (isError) {
                Button(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("إعادة التعيين وبدء فحص الماكينة المحدثة", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpen, Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Outlined.Visibility, null)
                    Spacer(Modifier.width(8.dp))
                    Text("مراجعة الفحص السابق المسجل")
                }
            } else {
                Button(onClick = onOpen, Modifier.fillMaxWidth().height(58.dp)) {
                    Icon(when { locked -> Icons.Outlined.Visibility; report == null -> Icons.Outlined.PlayArrow; else -> Icons.Outlined.Edit }, null)
                    Spacer(Modifier.width(8.dp)); Text(when { locked -> "عرض التقرير"; completed -> "مراجعة تقرير اليوم"; report != null -> "استكمال الفحص"; else -> "بدء الفحص الآن" })
                }
            }

            if (canChange && !completed && !locked && !isError) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onChange, Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.SwapHoriz, null); Spacer(Modifier.width(8.dp)); Text("تغيير الماكينة")
                }
                Text("يمكن تغيير الماكينة قبل اكتمال أو اعتماد الفحص، ويتطلب اتصالًا بالإنترنت.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun EmptyMaintenanceCard(needsInternet: Boolean, isMaintenanceDay: Boolean, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val friday = !isMaintenanceDay
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) { Icon(if (needsInternet) Icons.Outlined.CloudOff else if (friday) Icons.Outlined.Weekend else Icons.Outlined.EventAvailable, null, Modifier.padding(18.dp).size(34.dp), tint = MaterialTheme.colorScheme.secondary) }
        Spacer(Modifier.height(16.dp)); Text(when { needsInternet -> "نحتاج اتصالًا أول مرة"; friday -> "الجمعة عطلة الصيانة"; else -> "لا توجد مهمة صيانة حاليًا" }, style = MaterialTheme.typography.titleLarge)
        Text(when { needsInternet -> "اتصل بالإنترنت لتحميل بيانات المصنع وقائمة الفحص."; friday -> "ستبقى ماكينة الدور نفسها وتظهر تلقائيًا في يوم العمل التالي."; else -> "ستظهر هنا الماكينة التالية فور تحديدها." }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!friday) { Spacer(Modifier.height(20.dp)); OutlinedButton(onRefresh) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(7.dp)); Text("إعادة المحاولة") } }
    } }
}

private fun syncLabel(status: SyncStatus?, lastError: String?) = when (status) {
    SyncStatus.SYNCED -> "تمت مزامنة التقرير مع الخادم"
    SyncStatus.PENDING_SYNC -> "التقرير جاهز للإرسال"
    SyncStatus.LOCAL_DRAFT -> "مسودة محفوظة على الهاتف"
    SyncStatus.SYNCING -> "جاري إرسال التقرير..."
    SyncStatus.SYNC_ERROR -> lastError ?: "تعذر إرسال التقرير إلى الخادم"
    null -> "البيانات محدّثة وجاهزة للعمل"
}
