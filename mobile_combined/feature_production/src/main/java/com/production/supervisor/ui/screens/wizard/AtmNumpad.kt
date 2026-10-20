package com.production.supervisor.ui.screens.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.production.supervisor.ui.theme.*

/** Large numeric controls keep quantity entry independent of the phone keyboard. */
@Composable
fun AtmNumpad(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String = "كجم",
    modifier: Modifier = Modifier,
    maxIntegerDigits: Int = 7,
    enabled: Boolean = true
) {
    fun append(digit: String) {
        if (digit == ".") {
            if (!value.contains('.')) onValueChange(if (value.isEmpty()) "0." else "$value.")
            return
        }
        val fraction = value.substringAfter('.', "")
        if (value.contains('.') && fraction.length >= 2) return
        if (!value.contains('.') && value.length >= maxIntegerDigits) return
        onValueChange(if (value == "0") digit else value + digit)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FactorySurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(unit, fontSize = 17.sp, color = FactoryTextSecondary)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        value.ifBlank { "0" }, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        color = if (value.isBlank()) FactoryTextMuted else FactoryNavy,
                        textAlign = TextAlign.End, maxLines = 1
                    )
                }
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "delete")).forEach { keys ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { key ->
                        OutlinedButton(
                            onClick = { if (key == "delete") onValueChange(value.dropLast(1)) else append(key) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = FactorySurface, contentColor = FactoryNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, FactoryCardBorder),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            if (key == "delete") Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "حذف آخر رقم", modifier = Modifier.size(24.dp))
                            else Text(key, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
