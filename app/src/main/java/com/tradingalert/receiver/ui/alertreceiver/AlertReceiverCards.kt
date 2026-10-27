package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.Slider as ClassicSlider
import androidx.compose.material.SliderDefaults as ClassicSliderDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tradingalert.receiver.R
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
internal fun QuickActionsCard(
    volumePercent: Int,
    soundDurationSeconds: Int,
    selectedSoundName: String?,
    onVolumeChange: (Int) -> Unit,
    onSoundDurationChange: (Int) -> Unit,
    onChooseAlertSound: () -> Unit,
    onResetAlertSound: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(
                Icons.Rounded.Settings,
                stringResource(R.string.alert_settings_title),
                stringResource(R.string.alert_settings_subtitle),
            )
            Spacer(Modifier.height(14.dp))
            AlertSettingSlider(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = stringResource(R.string.alert_volume),
                valueLabel = stringResource(R.string.alert_volume_value, volumePercent),
                value = volumePercent.toFloat(),
                valueRange = 0f..100f,
                onValueChange = { onVolumeChange(((it / 10f).roundToInt() * 10).coerceIn(0, 100)) },
            )
            Spacer(Modifier.height(8.dp))
            AlertSettingSlider(
                icon = Icons.Rounded.Timer,
                title = stringResource(R.string.sound_duration),
                valueLabel = stringResource(R.string.sound_duration_value, soundDurationSeconds),
                value = soundDurationSeconds.toFloat(),
                valueRange = 1f..30f,
                onValueChange = { onSoundDurationChange(it.roundToInt().coerceIn(1, 30)) },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onChooseAlertSound,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.alert_sound),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = selectedSoundName
                            ?: stringResource(R.string.default_alert_sound),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
            if (selectedSoundName != null) {
                TextButton(
                    onClick = onResetAlertSound,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.reset_alert_sound))
                }
            } else {
                Spacer(Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = onOpenSoundSettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_sound_settings))
            }
        }
    }
}

@Composable
private fun AlertSettingSlider(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
    ClassicSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.fillMaxWidth(),
        colors = ClassicSliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
internal fun LastMessageLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.latest_signal_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun LastMessageCard(state: AlertReceiverUiState) {
    val context = LocalContext.current
    val latestMessage = state.notificationHistory.firstOrNull()
    val latestTime = latestMessage?.receivedAt ?: 0L
    val formattedTime = remember(context, latestTime) {
        if (latestTime <= 0L) {
            null
        } else {
            val receivedAt = Date(latestTime)
            val date = android.text.format.DateFormat.getMediumDateFormat(context).format(receivedAt)
            val time = android.text.format.DateFormat.getTimeFormat(context).format(receivedAt)
            "$date · $time"
        }
    }
    val currentTimeMillis by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = latestTime,
    ) {
        while (true) {
            val now = System.currentTimeMillis()
            value = now
            val elapsed = (now - latestTime).coerceAtLeast(0L)
            delay(60_000L - elapsed.mod(60_000L))
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (latestMessage == null) {
                                Icons.Rounded.CloudOff
                            } else {
                                Icons.Rounded.NotificationsActive
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.latest_signal_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            if (latestMessage == null) {
                                R.string.latest_signal_empty_subtitle
                            } else {
                                R.string.latest_signal_received_subtitle
                            },
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (latestTime > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    ) {
                        Text(
                            text = relativeTimeText(latestTime, currentTimeMillis),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
                if (latestMessage == null) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            stringResource(R.string.latest_signal_empty_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                latestMessage.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                latestMessage.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            formattedTime?.let { receivedTime ->
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = receivedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        latestMessage.imageUrl?.let { imageUrl ->
                            Spacer(Modifier.width(12.dp))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth(0.25f)
                                    .widthIn(max = 84.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeviceReliabilityCards(
    state: AlertReceiverUiState,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenBackgroundActivitySettings: () -> Unit,
    onOpenBackgroundDataSettings: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
) {
    val settings = buildList {
        if (state.batteryOptimizationRequired) add(
            DeviceSettingItem(
                icon = Icons.Rounded.BatteryAlert,
                title = stringResource(R.string.battery_optimization_title),
                body = stringResource(R.string.battery_optimization_body),
                onClick = onOpenBatteryOptimizationSettings,
            ),
        )
        if (state.backgroundActivityRestricted) add(
            DeviceSettingItem(
                icon = Icons.Rounded.PauseCircle,
                title = stringResource(R.string.background_activity_title),
                body = stringResource(R.string.background_activity_body),
                onClick = onOpenBackgroundActivitySettings,
            ),
        )
        if (state.backgroundDataRestricted) add(
            DeviceSettingItem(
                icon = Icons.Rounded.DataUsage,
                title = stringResource(R.string.background_data_title),
                body = stringResource(R.string.background_data_body),
                onClick = onOpenBackgroundDataSettings,
            ),
        )
        if (state.autoStartSettingsAvailable) add(
            DeviceSettingItem(
                icon = Icons.Rounded.PlayCircle,
                title = stringResource(R.string.auto_start_title),
                body = stringResource(R.string.auto_start_body),
                onClick = onOpenAutoStartSettings,
            ),
        )
    }
    if (settings.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            settings.forEachIndexed { index, setting ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                }
                DeviceSettingSection(setting)
            }
        }
    }
}

private data class DeviceSettingItem(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val onClick: () -> Unit,
)

@Composable
private fun DeviceSettingSection(setting: DeviceSettingItem) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = setting.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = setting.title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = setting.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = setting.onClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(stringResource(R.string.open_device_setting))
        }
    }
}

@Composable
internal fun SectionTitle(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp).size(19.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun relativeTimeText(timestamp: Long, currentTimeMillis: Long): String {
    val minutes = ((currentTimeMillis - timestamp).coerceAtLeast(0L) / 60_000L)
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> pluralStringResource(
            R.plurals.time_minutes_ago,
            minutes.toInt(),
            minutes,
        )
        minutes < 1_440 -> {
            val hours = minutes / 60
            pluralStringResource(R.plurals.time_hours_ago, hours.toInt(), hours)
        }
        minutes < 43_200 -> {
            val days = minutes / 1_440
            pluralStringResource(R.plurals.time_days_ago, days.toInt(), days)
        }
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
    }
}

