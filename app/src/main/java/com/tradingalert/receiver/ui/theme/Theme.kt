package com.tradingalert.receiver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF075FD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E7FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF006A7D),
    secondaryContainer = Color(0xFFB1ECFA),
    tertiary = Color(0xFF8B5000),
    background = Color(0xFFF6F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7ECF4),
    outline = Color(0xFF747C8A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F66),
    primaryContainer = Color(0xFF00468F),
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFF59D5ED),
    secondaryContainer = Color(0xFF004E5C),
    tertiary = Color(0xFFFFB95F),
    background = Color(0xFF080C14),
    surface = Color(0xFF101722),
    surfaceVariant = Color(0xFF1A2433),
    outline = Color(0xFF8D96A5),
)

@Composable
fun TradingAlertReceiverTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
