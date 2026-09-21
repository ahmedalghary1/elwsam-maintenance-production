package com.production.supervisor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand & Structural Colors
val FactoryDark = Color(0xFF0F172A)          // Slate 900
val FactoryNavy = Color(0xFF1E293B)          // Slate 800 (Top bar, primary buttons)
val FactoryNavyLight = Color(0xFF334155)     // Slate 700
val FactoryPrimaryBlue = Color(0xFF2563EB)   // Cobalt 600

// Status & Action Accents (Warm, Ergonomic, Eye-Friendly)
val FactoryOrange = Color(0xFFD97706)        // Amber 600 (Stoppages & alerts - warm, avoids eye strain)
val FactoryLightOrange = Color(0xFFFEF3C7)   // Amber 100
val FactoryOrangeDark = Color(0xFFB45309)    // Amber 700

val FactoryGreen = Color(0xFF059669)         // Emerald 600 (Saved, recorded, confirm - soothing natural green)
val FactoryLightGreen = Color(0xFFECFDF5)    // Emerald 50
val FactoryGreenDark = Color(0xFF047857)     // Emerald 700

val FactoryRed = Color(0xFFDC2626)           // Red 600
val FactoryLightRed = Color(0xFFFEF2F2)      // Red 50

val FactoryTeal = Color(0xFF0D9488)          // Teal 600 (Friday prayer action)
val FactoryLightTeal = Color(0xFFF0FDFA)     // Teal 50

// Backgrounds & Surface Textures (Low glare for long shifts)
val FactoryBg = Color(0xFFF8FAFC)            // Slate 50
val FactorySurface = Color(0xFFFFFFFF)       // White
val FactorySurfaceVariant = Color(0xFFF1F5F9) // Slate 100
val FactoryCardBorder = Color(0xFFE2E8F0)    // Slate 200
val FactoryCardBorderHover = Color(0xFFCBD5E1)// Slate 300

// High Readability Typography Tones
val FactoryTextPrimary = Color(0xFF0F172A)   // Slate 900
val FactoryTextSecondary = Color(0xFF475569) // Slate 600
val FactoryTextMuted = Color(0xFF64748B)     // Slate 500

private val LightColorScheme = lightColorScheme(
    primary = FactoryNavy,
    onPrimary = Color.White,
    primaryContainer = FactorySurfaceVariant,
    onPrimaryContainer = FactoryDark,
    secondary = FactoryOrange,
    onSecondary = Color.White,
    secondaryContainer = FactoryLightOrange,
    onSecondaryContainer = FactoryOrangeDark,
    tertiary = FactoryGreen,
    onTertiary = Color.White,
    tertiaryContainer = FactoryLightGreen,
    onTertiaryContainer = FactoryGreenDark,
    background = FactoryBg,
    onBackground = FactoryTextPrimary,
    surface = FactorySurface,
    onSurface = FactoryTextPrimary,
    surfaceVariant = FactorySurfaceVariant,
    onSurfaceVariant = FactoryTextSecondary,
    outline = FactoryCardBorder,
    error = FactoryRed,
    onError = Color.White,
    errorContainer = FactoryLightRed,
    onErrorContainer = FactoryRed
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = FactoryTextPrimary
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = FactoryTextPrimary
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        color = FactoryTextPrimary
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = FactoryTextPrimary
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        color = FactoryTextPrimary
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = FactoryTextMuted
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
)

@Composable
fun ProductionSupervisorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
