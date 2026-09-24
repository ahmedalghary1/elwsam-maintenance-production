package com.production.supervisor.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.production.supervisor.data.local.entity.AssetEntity
import com.production.supervisor.data.local.entity.OperatorEntity
import com.production.supervisor.data.local.entity.ProductEntity
import com.production.supervisor.ui.theme.*

/**
 * نافذة سريعة ومبسطة لتعديل الاستثناءات:
 * تم تصميمها لفتحها فقط عند الرغبة في تغيير العامل أو المنتج أو عدد اللقم لماكينة معينة،
 * دون تعقيد الشاشة الرئيسية لباقي الماكينات.
 */
@Composable
fun MachineExceptionDialog(
    asset: AssetEntity,
    operators: List<OperatorEntity>,
    products: List<ProductEntity>,
    currentOperatorId: Int?,
    currentProductId: Int?,
    currentCavities: Int,
    onDismiss: () -> Unit,
    onConfirm: (selectedOperatorId: Int?, selectedProductId: Int?, selectedCavities: Int) -> Unit
) {
    var selectedOpId by remember { mutableStateOf(currentOperatorId ?: asset.defaultOperatorId) }
    var selectedProdId by remember { mutableStateOf(currentProductId ?: asset.defaultProductId) }
    var cavities by remember { mutableStateOf(currentCavities.coerceAtLeast(1)) }
    var activeTab by remember { mutableStateOf(0) } // 0: Operator, 1: Product, 2: Cavities

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = FactorySurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "تعديل الاستثناءات: ${asset.assetCode}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FactoryDark
                        )
                        Text(
                            text = "تغيير العامل أو المنتج المشغل لهذه الوردية فقط",
                            fontSize = 12.sp,
                            color = FactoryTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = FactoryTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector Tabs (العامل / المنتج / اللقم)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabPill(
                        title = "العامل المشغل",
                        selected = activeTab == 0,
                        modifier = Modifier.weight(1f)
                    ) { activeTab = 0 }

                    TabPill(
                        title = "المنتج المشغل",
                        selected = activeTab == 1,
                        modifier = Modifier.weight(1f)
                    ) { activeTab = 1 }

                    TabPill(
                        title = "عدد اللقم",
                        selected = activeTab == 2,
                        modifier = Modifier.weight(1f)
                    ) { activeTab = 2 }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Area
                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> {
                            // Operators List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(operators) { op ->
                                    val isSelected = op.id == selectedOpId
                                    val isDefault = op.id == asset.defaultOperatorId
                                    ItemSelectionCard(
                                        title = op.name,
                                        subtitle = if (isDefault) "⭐ العامل المعتاد للماكينة" else op.phone,
                                        isSelected = isSelected,
                                        badge = if (isDefault) "المعتاد" else null,
                                        onClick = { selectedOpId = op.id }
                                    )
                                }
                            }
                        }
                        1 -> {
                            // Products List
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(products) { prod ->
                                    val isSelected = prod.id == selectedProdId
                                    val isDefault = prod.id == asset.defaultProductId
                                    ItemSelectionCard(
                                        title = prod.name,
                                        subtitle = if (isDefault) "⭐ الاسطمبة والمنتج المعتاد" else "كود: ${prod.code}",
                                        isSelected = isSelected,
                                        badge = if (isDefault) "المعتاد" else null,
                                        onClick = { selectedProdId = prod.id }
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Cavities Stepper
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "عدد اللقم الشغالة في الاسطمبة:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FactoryDark
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { if (cavities > 1) cavities-- },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = FactoryNavy),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Text("-", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Surface(
                                        color = FactorySurfaceVariant,
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(2.dp, FactoryNavy),
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = "$cavities لقمة",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = FactoryDark,
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { cavities++ },
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = FactoryNavy),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Text("+", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "العدد الافتراضي المسجل للماكينة: ${asset.originalCavities} لقمة",
                                    fontSize = 12.sp,
                                    color = FactoryTextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FactoryTextSecondary)
                    }

                    Button(
                        onClick = {
                            onConfirm(selectedOpId, selectedProdId, cavities)
                            onDismiss()
                        },
                        modifier = Modifier.weight(2f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FactoryGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تطبيق التعديلات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (selected) FactoryNavy else FactorySurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) FactoryNavy else FactoryCardBorder
        )
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else FactoryTextPrimary,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ItemSelectionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) FactoryLightGreen else FactorySurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) FactoryGreen else FactoryCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) FactoryGreenDark else FactoryDark
                    )
                    badge?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = FactoryOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = it,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = FactoryOrangeDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = FactoryTextMuted
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "تم الاختيار",
                    tint = FactoryGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
