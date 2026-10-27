package com.tradingalert.receiver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8FF),
    onPrimaryContainer = Color(0xFF0B2E70),
    secondary = Color(0xFF007F95),
    secondaryContainer = Color(0xFFC9F3FA),
    tertiary = Color(0xFF9A5B00),
    background = Color(0xFFF4F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7ECF4),
    outline = Color(0xFF747C8A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF173B79),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFF22D3EE),
    secondaryContainer = Color(0xFF073F49),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF0B111E),
    surface = Color(0xFF121A29),
    surfaceVariant = Color(0xFF1A2435),
    outline = Color(0xFF8E98AA),
)

@Composable
fun TradingAlertReceiverTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = TradingAlertTypography,
        content = content,
    )
}
