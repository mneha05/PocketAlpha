package com.nehamahesh.pocketalpha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AlphaColors {
    val Background = Color(0xFF0B0D0C)
    val Surface = Color(0xFF121512)
    val SurfaceRaised = Color(0xFF191D19)
    val Line = Color(0xFF292E29)
    val Green = Color(0xFFB7F64A)
    val GreenDeep = Color(0xFF75B900)
    val Coral = Color(0xFFFF6B6B)
    val Text = Color(0xFFF3F6EF)
    val Muted = Color(0xFF9EA69B)
}

private val alphaScheme = darkColorScheme(
    primary = AlphaColors.Green,
    onPrimary = Color(0xFF101700),
    secondary = Color(0xFFCCFF82),
    background = AlphaColors.Background,
    onBackground = AlphaColors.Text,
    surface = AlphaColors.Surface,
    onSurface = AlphaColors.Text,
    surfaceVariant = AlphaColors.SurfaceRaised,
    onSurfaceVariant = AlphaColors.Muted,
    error = AlphaColors.Coral,
    outline = AlphaColors.Line
)

@Composable
fun PocketAlphaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = alphaScheme,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-1.2).sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.8).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 23.sp, letterSpacing = (-0.4).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        ),
        content = content
    )
}
