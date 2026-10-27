package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradingalert.receiver.R
import com.tradingalert.receiver.localization.AppLanguage
import com.tradingalert.receiver.ui.theme.TradingAlertReceiverTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertReceiverContent(
    state: AlertReceiverUiState,
    selectedLanguage: AppLanguage,
    snackbarHostState: SnackbarHostState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onRequestPermission: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSoundDurationChange: (Int) -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenBackgroundActivitySettings: () -> Unit,
    onOpenBackgroundDataSettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onOpenNotificationHistory: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF082F66),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.trading_alert_foreground),
                                contentDescription = stringResource(R.string.app_icon_description),
                                modifier = Modifier.padding(4.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_short_name),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    LanguageToggleButton(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = onLanguageSelected,
                    )
                    NotificationHistoryButton(
                        notificationCount = state.unreadNotificationCount,
                        onClick = onOpenNotificationHistory,
                    )
                },
            )
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            if (maxWidth < 720.dp) {
                PhoneLayout(
                    state = state,
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 14.dp,
                        end = 20.dp,
                        bottom = 28.dp,
                    ),
                    onRequestPermission = onRequestPermission,
                    onVolumeChange = onVolumeChange,
                    onSoundDurationChange = onSoundDurationChange,
                    onOpenSoundSettings = onOpenSoundSettings,
                    onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                    onOpenBackgroundActivitySettings = onOpenBackgroundActivitySettings,
                    onOpenBackgroundDataSettings = onOpenBackgroundDataSettings,
                    onOpenAutoStartSettings = onOpenAutoStartSettings,
                )
            } else {
                WideLayout(
                    state = state,
                    contentPadding = PaddingValues(0.dp),
                    onRequestPermission = onRequestPermission,
                    onVolumeChange = onVolumeChange,
                    onSoundDurationChange = onSoundDurationChange,
                    onOpenSoundSettings = onOpenSoundSettings,
                    onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                    onOpenBackgroundActivitySettings = onOpenBackgroundActivitySettings,
                    onOpenBackgroundDataSettings = onOpenBackgroundDataSettings,
                    onOpenAutoStartSettings = onOpenAutoStartSettings,
                )
            }
        }
    }
}

@Composable
private fun NotificationHistoryButton(
    notificationCount: Int,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Notifications,
                contentDescription = stringResource(R.string.notification_history_open),
                modifier = Modifier.size(26.dp),
            )
            if (notificationCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageToggleButton(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    val nextLanguage = when (selectedLanguage) {
        AppLanguage.VIETNAMESE -> AppLanguage.ENGLISH
        AppLanguage.ENGLISH -> AppLanguage.VIETNAMESE
    }
    val switchDescription = when (nextLanguage) {
        AppLanguage.VIETNAMESE -> stringResource(R.string.switch_to_vietnamese)
        AppLanguage.ENGLISH -> stringResource(R.string.switch_to_english)
    }

    IconButton(
        onClick = { onLanguageSelected(nextLanguage) },
        modifier = Modifier
            .size(40.dp)
            .semantics {
                contentDescription = switchDescription
            },
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            LanguageFlag(selectedLanguage)
        }
    }
}

@Composable
private fun LanguageFlag(language: AppLanguage) {
    Canvas(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape),
    ) {
        when (language) {
            AppLanguage.VIETNAMESE -> {
                drawRect(Color(0xFFDA251D))
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension * 0.27f
                val innerRadius = outerRadius * 0.38f
                val star = Path()
                repeat(10) { index ->
                    val radius = if (index % 2 == 0) outerRadius else innerRadius
                    val angle = -PI / 2 + index * PI / 5
                    val x = center.x + (cos(angle) * radius).toFloat()
                    val y = center.y + (sin(angle) * radius).toFloat()
                    if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(star, Color(0xFFFFD700))
            }

            AppLanguage.ENGLISH -> {
                drawRect(Color(0xFF012169))
                val diagonalWidth = size.minDimension * 0.22f
                val diagonalRedWidth = size.minDimension * 0.09f
                drawLine(Color.White, Offset.Zero, Offset(size.width, size.height), diagonalWidth)
                drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), diagonalWidth)
                drawLine(Color(0xFFC8102E), Offset.Zero, Offset(size.width, size.height), diagonalRedWidth)
                drawLine(Color(0xFFC8102E), Offset(size.width, 0f), Offset(0f, size.height), diagonalRedWidth)

                val crossWidth = size.minDimension * 0.3f
                val crossRedWidth = size.minDimension * 0.15f
                drawLine(Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), crossWidth)
                drawLine(Color.White, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), crossWidth)
                drawLine(Color(0xFFC8102E), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), crossRedWidth)
                drawLine(Color(0xFFC8102E), Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), crossRedWidth)
            }
        }
    }
}

@Composable
internal fun PhoneLayout(
    state: AlertReceiverUiState,
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSoundDurationChange: (Int) -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenBackgroundActivitySettings: () -> Unit,
    onOpenBackgroundDataSettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ConnectionStrip(state) }
        if (state.notificationsAllowed) {
            item {
                if (state.historyLoading) LastMessageLoadingCard() else LastMessageCard(state)
            }
            item {
                QuickActionsCard(
                    volumePercent = state.alertVolumePercent,
                    soundDurationSeconds = state.soundDurationSeconds,
                    selectedSoundName = state.alertSoundName,
                    onVolumeChange = onVolumeChange,
                    onSoundDurationChange = onSoundDurationChange,
                    onOpenSoundSettings = onOpenSoundSettings,
                )
            }
        } else {
            item { NotificationPermissionCard(onRequestPermission) }
        }
        item {
            DeviceReliabilityCards(
                state = state,
                onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                onOpenBackgroundActivitySettings = onOpenBackgroundActivitySettings,
                onOpenBackgroundDataSettings = onOpenBackgroundDataSettings,
                onOpenAutoStartSettings = onOpenAutoStartSettings,
            )
        }
    }
}

@Composable
internal fun WideLayout(
    state: AlertReceiverUiState,
    contentPadding: PaddingValues,
    onRequestPermission: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onSoundDurationChange: (Int) -> Unit,
    onOpenSoundSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenBackgroundActivitySettings: () -> Unit,
    onOpenBackgroundDataSettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(0.9f),
            contentPadding = PaddingValues(
                start = 32.dp,
                top = contentPadding.calculateTopPadding() + 20.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ConnectionStrip(state) }
            if (!state.notificationsAllowed) item { NotificationPermissionCard(onRequestPermission) }
            item {
                DeviceReliabilityCards(
                    state = state,
                    onOpenBatteryOptimizationSettings = onOpenBatteryOptimizationSettings,
                    onOpenBackgroundActivitySettings = onOpenBackgroundActivitySettings,
                    onOpenBackgroundDataSettings = onOpenBackgroundDataSettings,
                    onOpenAutoStartSettings = onOpenAutoStartSettings,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1.1f),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 20.dp,
                end = 32.dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.notificationsAllowed) {
                item {
                    QuickActionsCard(
                        volumePercent = state.alertVolumePercent,
                        soundDurationSeconds = state.soundDurationSeconds,
                        selectedSoundName = state.alertSoundName,
                        onVolumeChange = onVolumeChange,
                        onSoundDurationChange = onSoundDurationChange,
                        onOpenSoundSettings = onOpenSoundSettings,
                    )
                }
                item {
                    if (state.historyLoading) LastMessageLoadingCard() else LastMessageCard(state)
                }
            }
        }
    }
}

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Phone · Large font", device = Devices.PHONE, fontScale = 1.5f, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
@Composable
internal fun AlertReceiverContentPreview() {
    TradingAlertReceiverTheme {
        AlertReceiverContent(
            state = AlertReceiverUiState(
                notificationsAllowed = true,
                token = "fcm_preview_token_1234567890_abcdefghijklmnopqrstuvwxyz",
                tokenUpdatedAt = System.currentTimeMillis(),
                lastTitle = stringResource(R.string.preview_signal_title),
                lastBody = stringResource(R.string.preview_signal_body),
                lastTime = System.currentTimeMillis(),
            ),
            selectedLanguage = AppLanguage.VIETNAMESE,
            snackbarHostState = remember { SnackbarHostState() },
            onLanguageSelected = {},
            onRequestPermission = {},
            onVolumeChange = {},
            onSoundDurationChange = {},
            onOpenSoundSettings = {},
            onOpenBatteryOptimizationSettings = {},
            onOpenBackgroundActivitySettings = {},
            onOpenBackgroundDataSettings = {},
            onOpenAutoStartSettings = {},
            onOpenNotificationHistory = {},
        )
    }
}

