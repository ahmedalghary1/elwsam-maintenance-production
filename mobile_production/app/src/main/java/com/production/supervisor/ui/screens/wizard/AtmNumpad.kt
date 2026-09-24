package com.production.supervisor.ui.screens.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.ui.theme.*

/**
 * لوحة أرقام ماكينة فوري / الصراف الآلي (ATM Keypad) المبسطة تماماً
 * مصممة خصيصاً للمشرفين والعمال البسطاء في بيئة المصانع:
 * 1. تمنع فتح كيبورد الهاتف المعقد تماماً
 * 2. أزرار أرقام ضخمة جداً (ارتفاع 66dp) مع استجابة لمسية واضحة
 * 3. شاشة رقمية عريضة تحاكي ميزان المصنع أو ماكينة فوري (LCD Display)
 * 4. حماية برمجية 100% ضد الأخطاء:
 *    - منع تكرار النقطة العشرية
 *    - عدم قبول أكثر من رقمين بعد العلامة
 *    - تصحيح الصفر المزدوج والبدايات غير المنطقية
 *    - زر مسح رقم عريض مكتوب ومصور (⌫ مسح) وزر تفريغ كامل (مسح الكل)
 */
@Composable
fun AtmNumpad(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String = "كجم",
    modifier: Modifier = Modifier,
    maxIntegerDigits: Int = 5 // حتى 99,999 كجم كحد أقصى للوردية
) {
    val haptic = LocalHapticFeedback.current

    fun performClickFeedback() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    fun handleDigit(digit: String) {
        performClickFeedback()

        // الحالة 1: إدخال نقطة عشرية
        if (digit == ".") {
            if (value.contains(".")) return
            if (value.isEmpty() || value == "0") {
                onValueChange("0.")
            } else {
                onValueChange("$value.")
            }
            return
        }

        // الحالة 2: إدخال صفر
        if (digit == "0") {
            if (value.isEmpty() || value == "0") {
                onValueChange("0")
                return
            }
        }

        // الحالة 3: إذا كانت القيمة الحالية "0" فقط، يتم استبدالها بالرقم الجديد
        if (value == "0") {
            onValueChange(digit)
            return
        }

        // الحالة 4: التدقيق في عدد الأرقام بعد وقبل النقطة
        val dotIndex = value.indexOf(".")
        if (dotIndex != -1) {
            // يوجد نقطة: منع أكثر من رقمين عشريين
            val decimals = value.substring(dotIndex + 1)
            if (decimals.length >= 2) return
            onValueChange(value + digit)
        } else {
            // لا يوجد نقطة: التحقق من الحد الأقصى للأرقام الصحيحة
            if (value.length >= maxIntegerDigits) return
            onValueChange(value + digit)
        }
    }

    fun handleBackspace() {
        performClickFeedback()
        if (value.isNotEmpty()) {
            val dropped = value.dropLast(1)
            // إذا تبقى بعد الحذف "0." نحذف النقطة أيضاً لتصبح فارغة
            val finalVal = if (dropped == "0." || dropped == "0") "" else dropped
            onValueChange(finalVal)
        }
    }

    fun handleClear() {
        performClickFeedback()
        onValueChange("")
    }

    fun handleQuickAdd(amount: Double) {
        performClickFeedback()
        val current = value.toDoubleOrNull() ?: 0.0
        val sum = (current + amount).coerceAtMost(99999.0)
        val formatted = if (sum % 1.0 == 0.0) sum.toInt().toString() else String.format(java.util.Locale.US, "%.1f", sum)
        onValueChange(formatted)
    }

    val currentNumericValue = value.toDoubleOrNull() ?: 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F172A)) // لوح أسود داكن صناعي مضاد للانعكاس
            .border(2.dp, Color(0xFF334155), RoundedCornerShape(22.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ==========================================
        // 1. شاشة العرض الرقمية (Digital LCD Display)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF020617)) // شاشة سوداء تماماً
                .border(2.dp, if (value.isNotEmpty()) Color(0xFF10B981) else Color(0xFF1E293B), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // شارة الوحدة (كجم)
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = unit,
                            color = Color(0xFF34D399),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // الرقم المكتوب بخط عريض وواضح جداً
                    val displayText = if (value.isEmpty()) "0.0" else value
                    Text(
                        text = displayText,
                        color = if (value.isEmpty()) Color(0xFF475569) else Color(0xFFF8FAFC),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }

                // إشارات بصرية مساعدة للمشرف
                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentNumericValue > 10000.0) {
                            Text(
                                text = "⚠️ تنبيه: الرقم أكثر من 10 طن (تأكد من صحته)",
                                fontSize = 11.sp,
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "✅ الوزن المسجل جاهز للحفظ",
                                fontSize = 11.sp,
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ==========================================
        // 2. أزرار الإضافة السريعة (+10, +50, +100, +500)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10.0, 50.0, 100.0, 500.0).forEach { inc ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { handleQuickAdd(inc) },
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        text = "+${inc.toInt()}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // 3. شبكة أزرار فوري / ATM الضخمة (4 صفوف)
        // ==========================================
        val keypadRows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "BACKSPACE")
        )

        keypadRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "BACKSPACE" -> {
                            // زر مسح رقم (عريض ومميز بلون أحمر غامق ومكتوب بالعربي)
                            AtmButton(
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color(0xFF7F1D1D),
                                borderColor = Color(0xFFB91C1C),
                                onClick = { handleBackspace() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "مسح رقم",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مسح",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        }
                        "." -> {
                            AtmButton(
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color(0xFF1E293B),
                                borderColor = Color(0xFF334155),
                                onClick = { handleDigit(".") }
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                        else -> {
                            AtmButton(
                                modifier = Modifier.weight(1f),
                                backgroundColor = Color(0xFF1E293B),
                                borderColor = Color(0xFF334155),
                                onClick = { handleDigit(key) }
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // 4. زر تفريغ الخانة بالكامل (مسح الكل)
        // ==========================================
        if (value.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { handleClear() },
                color = Color(0xFF450A0A),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "تفريغ الخانة",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تفريغ الخانة والبدء من جديد",
                        color = Color(0xFFF87171),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * زر لوحة الأرقام بهندسة خاصة للمصانع:
 * - ارتفاع 66dp يسهل لمسه دون أخطاء
 * - انحناءات ناعمة (16dp)
 * - حواف بارزة وظل خفيف
 */
@Composable
private fun AtmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1E293B),
    borderColor: Color = Color(0xFF334155),
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(66.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
