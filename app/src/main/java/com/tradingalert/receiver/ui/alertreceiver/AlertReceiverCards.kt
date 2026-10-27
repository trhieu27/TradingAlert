package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tradingalert.receiver.R
import java.text.DateFormat
import java.util.Date

@Composable
internal fun QuickActionsCard(
    notificationsAllowed: Boolean,
    onTestNotification: () -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(
                Icons.Rounded.Bolt,
                stringResource(R.string.quick_check_title),
                stringResource(R.string.quick_check_subtitle),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onTestNotification,
                enabled = notificationsAllowed,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.send_test_alert))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenSoundSettings,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_sound_settings))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.channel_security_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun TokenCard(
    state: AlertReceiverUiState,
    onCopy: () -> Unit,
    onRefresh: () -> Unit,
) {
    val tokenReady = isUsableToken(state.token)
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
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.firebase_device_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.firebase_device_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.tokenRefreshing,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.refresh_token), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Key,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.token,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onCopy, enabled = tokenReady) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.copy_token),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    state.tokenRefreshing -> stringResource(R.string.token_syncing)
                    tokenReady && state.tokenUpdatedAt > 0L -> stringResource(
                        R.string.token_synced,
                        relativeTimeText(state.tokenUpdatedAt),
                    )
                    tokenReady -> stringResource(R.string.token_ready)
                    else -> stringResource(R.string.token_unavailable)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun LastMessageCard(state: AlertReceiverUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            SectionTitle(
                icon = if (state.lastTitle == null) Icons.Rounded.CloudOff else Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.latest_signal_title),
                subtitle = stringResource(
                    if (state.lastTitle == null) {
                        R.string.latest_signal_empty_subtitle
                    } else {
                        R.string.latest_signal_received_subtitle
                    },
                ),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            Spacer(Modifier.height(12.dp))
            if (state.lastTitle == null) {
                Text(
                    stringResource(R.string.latest_signal_empty_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(state.lastTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                state.lastBody?.let {
                    Spacer(Modifier.height(5.dp))
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.lastTime > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        DateFormat.getDateTimeInstance().format(Date(state.lastTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeliveryTipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.delivery_tip_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.delivery_tip_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun relativeTimeText(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0L) / 60_000L)
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
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
    }
}

