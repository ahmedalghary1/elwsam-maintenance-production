package com.elwsam.operations

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElwsamColors = lightColorScheme(
    primary = Color(0xFF123A52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EAF5),
    onPrimaryContainer = Color(0xFF092535),
    secondary = Color(0xFFF28C28),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5C8),
    onSecondaryContainer = Color(0xFF542700),
    tertiary = Color(0xFF16846F),
    background = Color(0xFFF4F7F9),
    surface = Color.White,
    surfaceVariant = Color(0xFFE7EEF2),
    outline = Color(0xFF71808A),
    error = Color(0xFFBA1A1A)
)

@Composable
fun ElwsamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ElwsamColors, typography = Typography(), content = content)
}
