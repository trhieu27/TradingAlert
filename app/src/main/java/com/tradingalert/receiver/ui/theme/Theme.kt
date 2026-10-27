package com.tradingalert.receiver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF315FC0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF0A2E69),
    secondary = Color(0xFF047D92),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F1F7),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Color(0xFF8A5A00),
    tertiaryContainer = Color(0xFFFFDEA3),
    onTertiaryContainer = Color(0xFF2B1A00),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF18202B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18202B),
    surfaceVariant = Color(0xFFEAF0F7),
    onSurfaceVariant = Color(0xFF4B5667),
    outline = Color(0xFF707B8D),
    outlineVariant = Color(0xFFC5CEDA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF74A2FF),
    onPrimary = Color(0xFF071A3D),
    primaryContainer = Color(0xFF183B73),
    onPrimaryContainer = Color(0xFFDCE8FF),
    secondary = Color(0xFF22B8CF),
    onSecondary = Color(0xFF002026),
    secondaryContainer = Color(0xFF103A46),
    onSecondaryContainer = Color(0xFFC8F5FF),
    tertiary = Color(0xFFF2B84B),
    onTertiary = Color(0xFF2A1800),
    tertiaryContainer = Color(0xFF4D3508),
    onTertiaryContainer = Color(0xFFFFE4A8),
    background = Color(0xFF08111F),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF111B2B),
    onSurface = Color(0xFFE8ECF4),
    surfaceVariant = Color(0xFF1A273A),
    onSurfaceVariant = Color(0xFFBBC4D3),
    outline = Color(0xFF758196),
    outlineVariant = Color(0xFF2A374A),
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
