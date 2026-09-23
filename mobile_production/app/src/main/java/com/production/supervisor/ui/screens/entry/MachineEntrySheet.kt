package com.production.supervisor.ui.screens.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.production.supervisor.data.local.entity.AssetEntity
import com.production.supervisor.data.local.entity.MachineEntryEntity
import com.production.supervisor.data.local.entity.OperatorEntity
import com.production.supervisor.data.local.entity.ProductEntity
import com.production.supervisor.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MachineEntrySheet(
    asset: AssetEntity,
    currentReportId: String,
    existingEntry: MachineEntryEntity?,
    operators: List<OperatorEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (MachineEntryEntity) -> Unit
) {
    // 1. Initial values for Operator
    val defaultOp = operators.find { it.id == asset.defaultOperatorId }
    val initialOperatorName = existingEntry?.operatorName?.ifBlank { null }
        ?: asset.defaultOperatorName?.takeIf { it.isNotBlank() }
        ?: defaultOp?.name
        ?: ""

    var operatorNameText by remember(asset.id, existingEntry?.id) { mutableStateOf(initialOperatorName) }
    var selectedOperator by remember(asset.id, existingEntry?.id) {
        mutableStateOf(operators.find { it.name == initialOperatorName } ?: defaultOp)
    }

    val originalOperatorDisplayName = asset.defaultOperatorName?.takeIf { it.isNotBlank() }
        ?: defaultOp?.name
        ?: existingEntry?.originalOperatorName?.ifBlank { null }
        ?: ""

    val isOperatorChanged = originalOperatorDisplayName.isNotBlank() &&
            operatorNameText.trim().isNotBlank() &&
            operatorNameText.trim() != originalOperatorDisplayName.trim()

    // 2. Initial values for Product
    val defaultProd = products.find { it.id == asset.defaultProductId }
    val initialProductName = existingEntry?.productName?.ifBlank { null }
        ?: asset.defaultProductName?.takeIf { it.isNotBlank() }
        ?: defaultProd?.name
        ?: ""

    var productNameText by remember(asset.id, existingEntry?.id) { mutableStateOf(initialProductName) }
    var selectedProduct by remember(asset.id, existingEntry?.id) {
        mutableStateOf(products.find { it.name == initialProductName } ?: defaultProd)
    }

    val originalProductDisplayName = asset.defaultProductName?.takeIf { it.isNotBlank() }
        ?: defaultProd?.name
        ?: existingEntry?.originalProductName?.ifBlank { null }
        ?: ""

    val isProductChanged = originalProductDisplayName.isNotBlank() &&
            productNameText.trim().isNotBlank() &&
            productNameText.trim() != originalProductDisplayName.trim()

    // 3. Technical parameters (Actual vs Default)
    var currentCavitiesText by remember(asset.id, existingEntry?.id) {
        mutableStateOf(
            existingEntry?.currentCavities?.toString()
                ?: asset.originalCavities.toString()
        )
    }

    var coolingTimeText by remember(asset.id, existingEntry?.id) {
        mutableStateOf(
            existingEntry?.coolingTimeSeconds?.takeIf { it > 0 }?.toString()
                ?: asset.coolingTimeSeconds.takeIf { it > 0 }?.toString()
                ?: ""
        )
    }

    var cycleTimeText by remember(asset.id, existingEntry?.id) {
        mutableStateOf(
            existingEntry?.cycleTimeSeconds?.takeIf { it > 0 }?.toString()
                ?: asset.cycleTimeSeconds.takeIf { it > 0 }?.toString()
                ?: ""
        )
    }

    var targetProductionText by remember(asset.id, existingEntry?.id) {
        mutableStateOf(
            existingEntry?.targetCycleProduction?.takeIf { it > 0 }?.toString()
                ?: asset.targetCycleProduction.takeIf { it > 0 }?.toString()
                ?: ""
        )
    }

    // 4. Operation Mode & Outputs
    var operationMode by remember(asset.id, existingEntry?.id) {
        mutableStateOf(existingEntry?.operationMode ?: "AUTO")
    }
    var rawMaterial by remember(asset.id, existingEntry?.id) {
        mutableStateOf(existingEntry?.rawMaterial ?: "")
    }
    var weightText by remember(asset.id, existingEntry?.id) {
        mutableStateOf(existingEntry?.finalProductionWeightKg?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var packagingType by remember(asset.id, existingEntry?.id) {
        mutableStateOf(existingEntry?.packagingType ?: "كراتين")
    }
    var notes by remember(asset.id, existingEntry?.id) {
        mutableStateOf(existingEntry?.notes ?: "")
    }

    // Picker Dialog States
    var showOperatorDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = FactorySurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = FactoryNavy,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = asset.assetCode,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل إنتاج الماكينة",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = FactoryNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = asset.productionTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = FactoryTextMuted
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ==========================================
            // Dashboard Defaults Reference Card (المفروض كذا)
            // ==========================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FactorySurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = FactoryNavy,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بيانات الماكينة المعتمدة بالداشبورد (المرجع)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = FactoryNavy
                            )
                        }
                        Text(
                            text = "المعتاد",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryOrangeDark
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("اللقم الأصلي", fontSize = 10.sp, color = FactoryTextMuted)
                            Text(
                                "${asset.originalCavities}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FactoryDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("التبريد", fontSize = 10.sp, color = FactoryTextMuted)
                            Text(
                                if (asset.coolingTimeSeconds > 0) "${asset.coolingTimeSeconds} ث" else "غير محدد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FactoryDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الدورة", fontSize = 10.sp, color = FactoryTextMuted)
                            Text(
                                if (asset.cycleTimeSeconds > 0) "${asset.cycleTimeSeconds} ث" else "غير محدد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FactoryDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المستهدف", fontSize = 10.sp, color = FactoryTextMuted)
                            Text(
                                if (asset.targetCycleProduction > 0) "${asset.targetCycleProduction.toInt()} كجم" else "غير محدد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FactoryGreenDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 1. القائم على الماكينة (العامل)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "القائم على الماكينة (العامل):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FactoryDark
                )
                if (originalOperatorDisplayName.isNotBlank()) {
                    Text(
                        text = "المعتاد: $originalOperatorDisplayName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FactoryNavy
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Professional Operator Selector Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOperatorDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = FactorySurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    if (isOperatorChanged) FactoryOrange else FactoryCardBorder
                ),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isOperatorChanged) FactoryLightOrange else FactoryNavy.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (isOperatorChanged) FactoryOrangeDark else FactoryNavy,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = operatorNameText.ifBlank { "اضغط لاختيار القائم على الماكينة" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (operatorNameText.isNotBlank()) FactoryDark else FactoryTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (operatorNameText.trim() == originalOperatorDisplayName.trim() && originalOperatorDisplayName.isNotBlank()) {
                                Text(
                                    text = "الافتراضي بالداشبورد ⭐",
                                    fontSize = 11.sp,
                                    color = FactoryGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (selectedOperator?.phone?.isNotBlank() == true) {
                                Text(
                                    text = "هاتف: ${selectedOperator?.phone}",
                                    fontSize = 11.sp,
                                    color = FactoryTextMuted
                                )
                            } else {
                                Text(
                                    text = "اضغط لتغيير أو اختيار عامل آخر",
                                    fontSize = 11.sp,
                                    color = FactoryTextMuted
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "اختيار",
                        tint = FactoryNavy,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isOperatorChanged) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FactoryLightOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactoryOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = FactoryOrangeDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚠️ تم تغيير القائم على الماكينة!\nكانت القيمة: $originalOperatorDisplayName ➔ وتم تغييرها إلى: ${operatorNameText.trim()}",
                            color = FactoryOrangeDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. المنتج المشغل
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المنتج المشغل:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FactoryDark
                )
                if (originalProductDisplayName.isNotBlank()) {
                    Text(
                        text = "المعتاد: $originalProductDisplayName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = FactoryNavy
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Professional Product Selector Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProductDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = FactorySurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    if (isProductChanged) FactoryOrange else FactoryCardBorder
                ),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isProductChanged) FactoryLightOrange else FactoryNavy.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = if (isProductChanged) FactoryOrangeDark else FactoryNavy,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = productNameText.ifBlank { "اضغط لاختيار المنتج المشغل" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (productNameText.isNotBlank()) FactoryDark else FactoryTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (productNameText.trim() == originalProductDisplayName.trim() && originalProductDisplayName.isNotBlank()) {
                                Text(
                                    text = "الافتراضي بالداشبورد ⭐",
                                    fontSize = 11.sp,
                                    color = FactoryGreenDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else if (selectedProduct != null) {
                                val details = buildList {
                                    if (selectedProduct!!.code.isNotBlank()) add("كود: ${selectedProduct!!.code}")
                                    if (selectedProduct!!.weightPerPieceGrams > 0) add("${selectedProduct!!.weightPerPieceGrams} جم/قطعة")
                                }.joinToString(" | ")
                                if (details.isNotBlank()) {
                                    Text(
                                        text = details,
                                        fontSize = 11.sp,
                                        color = FactoryTextMuted
                                    )
                                }
                            } else {
                                Text(
                                    text = "اضغط لتغيير أو اختيار منتج آخر",
                                    fontSize = 11.sp,
                                    color = FactoryTextMuted
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "اختيار",
                        tint = FactoryNavy,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isProductChanged) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FactoryLightOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactoryOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = FactoryOrangeDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚠️ تم تغيير المنتج عن المعتاد!\nكانت القيمة: $originalProductDisplayName ➔ وتم تغييرها إلى: ${productNameText.trim()}",
                            color = FactoryOrangeDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 3. المواصفات الفنية الفعلية المنفذة في الوردية
            // (اللقم الحالي، التبريد الفعلي، الدورة الفعلية، المستهدف الفعلي)
            // ==========================================
            Text(
                text = "المواصفات الفنية المنفذة فعلياً بالوردية:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = FactoryDark
            )
            Text(
                text = "يمكنك تعديل أي قيمة وفقاً لما هو قائم بالماكينة الآن (القيم الافتراضية معروضة كمرجع):",
                fontSize = 11.sp,
                color = FactoryTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            // الصف الأول: اللقم الفعلي والتبريد الفعلي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = currentCavitiesText,
                    onValueChange = { currentCavitiesText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("اللقم الفعلي") },
                    placeholder = { Text("${asset.originalCavities}") },
                    leadingIcon = { Icon(Icons.Outlined.GridOn, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("الافتراضي: ${asset.originalCavities}", fontSize = 10.sp) }
                )

                OutlinedTextField(
                    value = coolingTimeText,
                    onValueChange = { coolingTimeText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("التبريد الفعلي (ث)") },
                    placeholder = { Text("${asset.coolingTimeSeconds}") },
                    leadingIcon = { Icon(Icons.Outlined.AcUnit, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("الافتراضي: ${asset.coolingTimeSeconds}ث", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // الصف الثاني: الدورة الفعلية والمستهدف الفعلي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = cycleTimeText,
                    onValueChange = { cycleTimeText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("الدورة الفعلية (ث)") },
                    placeholder = { Text("${asset.cycleTimeSeconds}") },
                    leadingIcon = { Icon(Icons.Outlined.Speed, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("الافتراضي: ${asset.cycleTimeSeconds}ث", fontSize = 10.sp) }
                )

                OutlinedTextField(
                    value = targetProductionText,
                    onValueChange = { targetProductionText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("المستهدف (كجم)") },
                    placeholder = { Text("${asset.targetCycleProduction}") },
                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = FactoryGreenDark, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = { Text("الافتراضي: ${asset.targetCycleProduction.toInt()}كجم", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 4. حالة التشغيل (أوتو / يدوي)
            // ==========================================
            Text(
                text = "حالة التشغيل:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { operationMode = "AUTO" },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (operationMode == "AUTO") FactoryGreen else FactorySurfaceVariant,
                        contentColor = if (operationMode == "AUTO") Color.White else FactoryTextSecondary
                    ),
                    border = if (operationMode == "AUTO") null else androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("أوتو", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("AUTO", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Button(
                    onClick = { operationMode = "MANUAL" },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (operationMode == "MANUAL") FactoryOrange else FactorySurfaceVariant,
                        contentColor = if (operationMode == "MANUAL") Color.White else FactoryTextSecondary
                    ),
                    border = if (operationMode == "MANUAL") null else androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.PanTool, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("يدوي", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("MANUAL", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 5. الخامة والوزن والعبوة والملاحظات
            // ==========================================
            OutlinedTextField(
                value = rawMaterial,
                onValueChange = { rawMaterial = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("الخامة المستخدمة (مثل: PP سابك)") },
                leadingIcon = { Icon(Icons.Outlined.Science, contentDescription = null, tint = FactoryNavy) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("وزن الإنتاج النهائي (بالكيلو) *") },
                placeholder = { Text("مثال: 350.5") },
                leadingIcon = { Icon(Icons.Outlined.Scale, contentDescription = null, tint = FactoryGreenDark) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = packagingType,
                onValueChange = { packagingType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نوع العبوة (كراتين، شكاير، براميل...)") },
                leadingIcon = { Icon(Icons.Outlined.AllInbox, contentDescription = null, tint = FactoryNavy) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ملاحظات الماكينة (اختياري)") },
                leadingIcon = { Icon(Icons.Outlined.EditNote, contentDescription = null, tint = FactoryNavy) },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val currentCavities = currentCavitiesText.toIntOrNull() ?: asset.originalCavities
                    val actualCooling = coolingTimeText.toDoubleOrNull() ?: asset.coolingTimeSeconds
                    val actualCycle = cycleTimeText.toDoubleOrNull() ?: asset.cycleTimeSeconds
                    val actualTarget = targetProductionText.toDoubleOrNull() ?: asset.targetCycleProduction
                    val weight = weightText.toDoubleOrNull() ?: 0.0

                    val entry = MachineEntryEntity(
                        id = existingEntry?.id ?: 0,
                        clientReportId = currentReportId,
                        assetId = asset.id,
                        assetCode = asset.assetCode,
                        operatorId = selectedOperator?.takeIf { it.name == operatorNameText.trim() }?.id,
                        operatorName = operatorNameText.trim(),
                        originalOperatorId = asset.defaultOperatorId ?: existingEntry?.originalOperatorId,
                        originalOperatorName = originalOperatorDisplayName,
                        operatorChanged = isOperatorChanged,
                        productId = selectedProduct?.takeIf { it.name == productNameText.trim() }?.id,
                        productName = productNameText.trim(),
                        originalProductId = asset.defaultProductId ?: existingEntry?.originalProductId,
                        originalProductName = originalProductDisplayName,
                        productChanged = isProductChanged,
                        originalCavities = asset.originalCavities,
                        currentCavities = currentCavities,
                        operationMode = operationMode,
                        coolingTimeSeconds = actualCooling,
                        cycleTimeSeconds = actualCycle,
                        rawMaterial = rawMaterial,
                        finalProductionWeightKg = weight,
                        targetCycleProduction = actualTarget,
                        packagingType = packagingType,
                        notes = notes
                    )
                    onSave(entry)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حفظ بيانات الماكينة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // ==========================================
    // Professional Operator Picker Dialog
    // ==========================================
    if (showOperatorDialog) {
        OperatorPickerDialog(
            operators = operators,
            currentSelectedName = operatorNameText,
            defaultOperatorName = originalOperatorDisplayName,
            onDismiss = { showOperatorDialog = false },
            onSelect = { name, op ->
                operatorNameText = name
                selectedOperator = op
                showOperatorDialog = false
            }
        )
    }

    // ==========================================
    // Professional Product Picker Dialog
    // ==========================================
    if (showProductDialog) {
        ProductPickerDialog(
            products = products,
            currentSelectedName = productNameText,
            defaultProductName = originalProductDisplayName,
            onDismiss = { showProductDialog = false },
            onSelect = { name, prod ->
                productNameText = name
                selectedProduct = prod
                showProductDialog = false
            }
        )
    }
}

// ==========================================
// Dialog: اختيار القائم على الماكينة
// ==========================================
@Composable
private fun OperatorPickerDialog(
    operators: List<OperatorEntity>,
    currentSelectedName: String,
    defaultOperatorName: String,
    onDismiss: () -> Unit,
    onSelect: (String, OperatorEntity?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, operators) {
        if (searchQuery.isBlank()) operators
        else operators.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = FactoryNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "اختيار القائم على الماكينة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = FactoryNavy
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث عن عامل بالاسم أو الهاتف...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FactoryTextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option: Default Dashboard Operator
                    if (defaultOperatorName.isNotBlank() && searchQuery.isBlank()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val matched = operators.find { it.name == defaultOperatorName }
                                        onSelect(defaultOperatorName, matched)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = FactoryLightOrange,
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, FactoryOrange)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = FactoryOrangeDark, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(defaultOperatorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FactoryNavy)
                                            Text("الافتراضي المعتمد بالداشبورد ⭐", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FactoryOrangeDark)
                                        }
                                    }
                                    if (currentSelectedName.trim() == defaultOperatorName.trim()) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = FactoryOrangeDark)
                                    }
                                }
                            }
                        }
                    }

                    // Operators list
                    items(filtered) { op ->
                        val isSelected = currentSelectedName.trim() == op.name.trim()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(op.name, op) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) FactoryNavy.copy(alpha = 0.08f) else FactorySurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) FactoryNavy else FactoryCardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) FactoryNavy else FactoryTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            op.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) FactoryNavy else FactoryDark
                                        )
                                        if (op.phone.isNotBlank()) {
                                            Text(op.phone, fontSize = 11.sp, color = FactoryTextMuted)
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FactoryNavy)
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا يوجد عمال مطابقين للبحث", fontSize = 12.sp, color = FactoryTextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Manual Name Entry Option
                Text("أو إدخال اسم عامل يدوي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FactoryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اكتب اسم العامل هنا...") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (customName.trim().isNotBlank()) {
                                onSelect(customName.trim(), null)
                            }
                        },
                        enabled = customName.trim().isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryNavy)
                    ) {
                        Text("تأكيد")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ==========================================
// Dialog: اختيار المنتج المشغل
// ==========================================
@Composable
private fun ProductPickerDialog(
    products: List<ProductEntity>,
    currentSelectedName: String,
    defaultProductName: String,
    onDismiss: () -> Unit,
    onSelect: (String, ProductEntity?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products
        else products.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = FactoryNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "اختيار المنتج المشغل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = FactoryNavy
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث باسم المنتج أو الكود...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FactoryTextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option: Default Dashboard Product
                    if (defaultProductName.isNotBlank() && searchQuery.isBlank()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val matched = products.find { it.name == defaultProductName }
                                        onSelect(defaultProductName, matched)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = FactoryLightOrange,
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, FactoryOrange)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = FactoryOrangeDark, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(defaultProductName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FactoryNavy)
                                            Text("المنتج الافتراضي المعتمد بالداشبورد ⭐", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = FactoryOrangeDark)
                                        }
                                    }
                                    if (currentSelectedName.trim() == defaultProductName.trim()) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = FactoryOrangeDark)
                                    }
                                }
                            }
                        }
                    }

                    // Products list
                    items(filtered) { prod ->
                        val isSelected = currentSelectedName.trim() == prod.name.trim()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(prod.name, prod) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) FactoryNavy.copy(alpha = 0.08f) else FactorySurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) FactoryNavy else FactoryCardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = if (isSelected) FactoryNavy else FactoryTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            prod.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) FactoryNavy else FactoryDark
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (prod.code.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "كود: ${prod.code}",
                                                        fontSize = 10.sp,
                                                        color = FactoryNavy,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            if (prod.weightPerPieceGrams > 0) {
                                                Text(
                                                    "الوزن: ${prod.weightPerPieceGrams} جم/قطعة",
                                                    fontSize = 11.sp,
                                                    color = FactoryTextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = FactoryNavy)
                                }
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("لا توجد منتجات مسجلة مطابقة للبحث", fontSize = 12.sp, color = FactoryTextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Manual Product Entry Option
                Text("أو إدخال اسم منتج يدوي:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FactoryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("اكتب اسم المنتج هنا...") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Button(
                        onClick = {
                            if (customName.trim().isNotBlank()) {
                                onSelect(customName.trim(), null)
                            }
                        },
                        enabled = customName.trim().isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryNavy)
                    ) {
                        Text("تأكيد")
                    }
                }
            }
        },
        confirmButton = {}
    )
}
