package com.mobrauntech.shoplist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Monochrome palette pulled from the reference screens.
val Bg = Color(0xFF0B0B0C)
val TopBar = Color(0xFF161618)
val Card = Color(0xFF1B1B1D)
val CardAlt = Color(0xFF161618)
val TextPrimary = Color(0xFFF4F4F5)
val TextSecondary = Color(0xFF9B9B9F)
val SectionLabel = Color(0xFF6E6E73)
val Accent = Color(0xFFFFFFFF)
val Divider = Color(0xFF2A2A2D)
val DeleteRed = Color(0xFFB23A3A)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    background = Bg,
    onBackground = TextPrimary,
    surface = Card,
    onSurface = TextPrimary,
    surfaceVariant = TopBar,
    onSurfaceVariant = TextSecondary,
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
)

@Composable
fun ShopListTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = AppTypography, content = content)
}
