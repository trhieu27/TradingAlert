package com.tradingalert.receiver.ui.alertreceiver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradingalert.receiver.R

@Composable
internal fun ConnectionStrip(state: AlertReceiverUiState) {
    val connected = isUsableToken(state.token)
    val tokenLoading = !connected && state.token == stringResource(R.string.token_loading)
    val statusLabel = when {
        !state.notificationsAllowed -> stringResource(R.string.connection_waiting_permission)
        state.tokenRefreshing || tokenLoading -> stringResource(R.string.connection_connecting)
        connected -> stringResource(R.string.connection_live)
        else -> stringResource(R.string.connection_waiting)
    }
    val statusColor = when {
        connected && state.notificationsAllowed -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.tertiary
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.CloudDone,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = statusColor,
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.connection_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.connection_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.14f),
            ) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
            }
        }
    }
}

@Composable
internal fun StatusHero(allowed: Boolean, onRequestPermission: () -> Unit) {
    val accent = if (allowed) Color(0xFF10B981) else Color(0xFFF59E0B)
    val start = if (allowed) Color(0xFF073B3A) else Color(0xFF172036)
    val end = if (allowed) Color(0xFF123D7A) else Color(0xFF162E59)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(start, end)))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (allowed) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(
                            if (allowed) R.string.status_active else R.string.status_setup_required,
                        ),
                        color = accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (allowed) {
                HeroMeta(Icons.Rounded.NotificationsActive, stringResource(R.string.high_priority_channel))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (allowed) R.string.status_ready_title else R.string.status_setup_title,
            ),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(
                if (allowed) R.string.status_ready_body else R.string.status_setup_body,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.78f),
        )
        Spacer(Modifier.height(11.dp))
        if (allowed) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroMeta(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.custom_sound))
                HeroMeta(Icons.Rounded.Security, stringResource(R.string.background_ready))
            }
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp),
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.allow_notifications), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
internal fun HeroMeta(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF67E8F9))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f))
    }
}

