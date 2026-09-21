package com.production.supervisor.ui.screens.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
    // Initial values
    val defaultOp = operators.find { it.id == asset.defaultOperatorId }
    val initialOperatorName = existingEntry?.operatorName?.ifBlank { null }
        ?: asset.defaultOperatorName
        ?: defaultOp?.name
        ?: ""

    var operatorNameText by remember { mutableStateOf(initialOperatorName) }
    var selectedOperator by remember {
        mutableStateOf(operators.find { it.name == initialOperatorName } ?: defaultOp)
    }

    val defaultProd = products.find { it.id == asset.defaultProductId }
    val initialProductName = existingEntry?.productName?.ifBlank { null }
        ?: asset.defaultProductName
        ?: defaultProd?.name
        ?: ""

    var productNameText by remember { mutableStateOf(initialProductName) }
    var selectedProduct by remember {
        mutableStateOf(products.find { it.name == initialProductName } ?: defaultProd)
    }

    val originalOperatorDisplayName = asset.defaultOperatorName
        ?: existingEntry?.originalOperatorName?.ifBlank { null }
        ?: defaultOp?.name
        ?: ""

    val isOperatorChanged = originalOperatorDisplayName.isNotBlank() &&
            operatorNameText.trim().isNotBlank() &&
            operatorNameText.trim() != originalOperatorDisplayName.trim()

    val originalProductDisplayName = asset.defaultProductName
        ?: existingEntry?.originalProductName?.ifBlank { null }
        ?: defaultProd?.name
        ?: ""

    val isProductChanged = originalProductDisplayName.isNotBlank() &&
            productNameText.trim().isNotBlank() &&
            productNameText.trim() != originalProductDisplayName.trim()

    var currentCavitiesText by remember {
        mutableStateOf(
            existingEntry?.currentCavities?.toString() ?: asset.originalCavities.toString()
        )
    }
    var operationMode by remember {
        mutableStateOf(existingEntry?.operationMode ?: "AUTO")
    }
    var rawMaterial by remember {
        mutableStateOf(existingEntry?.rawMaterial ?: "")
    }
    var weightText by remember {
        mutableStateOf(existingEntry?.finalProductionWeightKg?.let { if (it > 0) it.toString() else "" } ?: "")
    }
    var packagingType by remember {
        mutableStateOf(existingEntry?.packagingType ?: "كراتين")
    }
    var notes by remember {
        mutableStateOf(existingEntry?.notes ?: "")
    }

    var showOperatorDropdown by remember { mutableStateOf(false) }
    var showProductDropdown by remember { mutableStateOf(false) }

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
                    Text(
                        text = "تسجيل بيانات: ${asset.assetCode}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = FactoryNavy
                    )
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

            // 1. القائم على الماكينة (العامل)
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
                        color = FactoryTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = operatorNameText,
                    onValueChange = {
                        operatorNameText = it
                        selectedOperator = operators.find { op -> op.name == it.trim() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("اختر من القائمة أو اكتب اسم العامل يدوي") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = FactoryNavy) },
                    trailingIcon = {
                        IconButton(onClick = { showOperatorDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة العمال", tint = FactoryNavy)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                DropdownMenu(
                    expanded = showOperatorDropdown,
                    onDismissRequest = { showOperatorDropdown = false }
                ) {
                    operators.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op.name, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedOperator = op
                                operatorNameText = op.name
                                showOperatorDropdown = false
                            }
                        )
                    }
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
                            text = "تم تغيير القائم على الماكينة!\nالأصلي: $originalOperatorDisplayName ➡ الجديد: ${operatorNameText.trim()}",
                            color = FactoryOrangeDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. المنتج
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
                        color = FactoryTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = productNameText,
                    onValueChange = {
                        productNameText = it
                        selectedProduct = products.find { prod -> prod.name == it.trim() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("اختر من القائمة أو اكتب اسم المنتج يدوي") },
                    leadingIcon = { Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = FactoryNavy) },
                    trailingIcon = {
                        IconButton(onClick = { showProductDropdown = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "قائمة المنتجات", tint = FactoryNavy)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                DropdownMenu(
                    expanded = showProductDropdown,
                    onDismissRequest = { showProductDropdown = false }
                ) {
                    products.forEach { prod ->
                        DropdownMenuItem(
                            text = { Text(prod.name, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedProduct = prod
                                productNameText = prod.name
                                showProductDropdown = false
                            }
                        )
                    }
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
                            text = "تم تغيير المنتج عن المعتاد!\nالأصلي: $originalProductDisplayName ➡ الجديد: ${productNameText.trim()}",
                            color = FactoryOrangeDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. عدد اللقم: الأصلي (مقفول) و الحالي (مدخل يدوي)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // عدد اللقم الأصلي (مقفول)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "مقفول", tint = FactoryNavy, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اللقم الأصلي", fontSize = 11.sp, color = FactoryTextMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${asset.originalCavities} لقم",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryNavy
                        )
                    }
                }

                // عدد اللقم الحالي
                OutlinedTextField(
                    value = currentCavitiesText,
                    onValueChange = { currentCavitiesText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("اللقم الحالي") },
                    leadingIcon = { Icon(Icons.Outlined.GridOn, contentDescription = null, tint = FactoryNavy, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. حالة التشغيل (أوتو / يدوي)
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
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (operationMode == "AUTO") FactoryGreen else FactorySurfaceVariant,
                        contentColor = if (operationMode == "AUTO") Color.White else FactoryTextSecondary
                    ),
                    border = if (operationMode == "AUTO") null else androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
                ) {
                    Icon(Icons.Outlined.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("أوتو (AUTO)", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { operationMode = "MANUAL" },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (operationMode == "MANUAL") FactoryOrange else FactorySurfaceVariant,
                        contentColor = if (operationMode == "MANUAL") Color.White else FactoryTextSecondary
                    ),
                    border = if (operationMode == "MANUAL") null else androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
                ) {
                    Icon(Icons.Outlined.PanTool, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("يدوي (MANUAL)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. التبريد والدورة (مقفولين بجانب بعض)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = FactorySurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "مقفول", tint = FactoryNavy, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("التبريد", fontSize = 11.sp, color = FactoryTextMuted)
                        }
                        Text("${asset.coolingTimeSeconds} ثانية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FactoryDark)
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(FactoryCardBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "مقفول", tint = FactoryNavy, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الدورة", fontSize = 11.sp, color = FactoryTextMuted)
                        }
                        Text("${asset.cycleTimeSeconds} ثانية", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FactoryDark)
                    }

                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(FactoryCardBorder))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "مقفول", tint = FactoryNavy, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("الإنتاج المستهدف", fontSize = 11.sp, color = FactoryTextMuted)
                        }
                        Text("${asset.targetCycleProduction}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FactoryDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. الخامة المستخدمة
            OutlinedTextField(
                value = rawMaterial,
                onValueChange = { rawMaterial = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("الخامة المستخدمة (مثل: PP سابك)") },
                leadingIcon = { Icon(Icons.Outlined.Science, contentDescription = null, tint = FactoryNavy) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. وزن الإنتاج النهائي بالكيلو
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("وزن الإنتاج النهائي (بالكيلو) *") },
                leadingIcon = { Icon(Icons.Outlined.Scale, contentDescription = null, tint = FactoryNavy) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 8. العبوة
            OutlinedTextField(
                value = packagingType,
                onValueChange = { packagingType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نوع العبوة (كراتين، شكاير، براميل...)") },
                leadingIcon = { Icon(Icons.Outlined.AllInbox, contentDescription = null, tint = FactoryNavy) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 9. ملاحظات
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
                        coolingTimeSeconds = asset.coolingTimeSeconds,
                        cycleTimeSeconds = asset.cycleTimeSeconds,
                        rawMaterial = rawMaterial,
                        finalProductionWeightKg = weight,
                        targetCycleProduction = asset.targetCycleProduction,
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
}
