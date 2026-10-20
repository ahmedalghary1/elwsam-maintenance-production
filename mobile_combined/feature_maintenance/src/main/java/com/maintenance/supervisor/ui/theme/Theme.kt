package com.maintenance.supervisor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Primary = Color(0xFF12304A)
val Secondary = Color(0xFF0F8B7B)
val Success = Color(0xFF2E7D32)
val Warning = Color(0xFFE6A700)
val Error = Color(0xFFC62828)
val Background = Color(0xFFF5F7F9)
val TextPrimary = Color(0xFF17212B)
val TextSecondary = Color(0xFF66717D)
val SurfaceSoft = Color(0xFFF0F5F6)
val Outline = Color(0xFFD4E0E4)

private val colors = lightColorScheme(primary = Primary, secondary = Secondary, error = Error, background = Background,
    surface = Color.White, surfaceVariant = SurfaceSoft, onPrimary = Color.White, onSecondary = Color.White,
    onBackground = TextPrimary, onSurface = TextPrimary, onSurfaceVariant = TextSecondary, outline = Outline,
    primaryContainer = Color(0xFFE5EEF3), onPrimaryContainer = Primary,
    secondaryContainer = Color(0xFFE2F4F1), onSecondaryContainer = Color(0xFF075F56),
    errorContainer = Color(0xFFFDEBEC), onErrorContainer = Error)

private val typography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 25.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp)
)

@Composable fun MaintenanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)), content = content)
}
