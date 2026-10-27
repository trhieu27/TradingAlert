package com.tradingalert.receiver.ui.alertreceiver

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.tradingalert.receiver.R
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.AlertPreferenceStore
import com.tradingalert.receiver.data.notification.NotificationHistoryItem

internal data class AlertReceiverUiState(
    val notificationsAllowed: Boolean = false,
    val token: String = "",
    val tokenUpdatedAt: Long = 0L,
    val tokenRefreshing: Boolean = false,
    val alertVolumePercent: Int = AlertPreferenceStore.DEFAULT_VOLUME_PERCENT,
    val soundDurationSeconds: Int = AlertPreferenceStore.DEFAULT_SOUND_DURATION_SECONDS,
    val notificationHistory: List<NotificationHistoryItem> = emptyList(),
    val lastTitle: String? = null,
    val lastBody: String? = null,
    val lastTime: Long = 0L,
)

internal fun loadAlertReceiverState(context: Context): AlertReceiverUiState {
    val prefs = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
    val alertPreferences = AlertPreferenceStore.load(context)
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    return AlertReceiverUiState(
        notificationsAllowed = appNotificationsEnabled,
        token = prefs.getString(AlertMessagingService.KEY_TOKEN, null)
            ?: context.getString(R.string.token_loading),
        tokenUpdatedAt = prefs.getLong(AlertMessagingService.KEY_TOKEN_TIME, 0L),
        alertVolumePercent = alertPreferences.volumePercent,
        soundDurationSeconds = alertPreferences.soundDurationSeconds,
        lastTitle = prefs.getString(AlertMessagingService.KEY_LAST_TITLE, null),
        lastBody = prefs.getString(AlertMessagingService.KEY_LAST_BODY, null),
        lastTime = prefs.getLong(AlertMessagingService.KEY_LAST_TIME, 0L),
    )
}

internal fun isUsableToken(token: String): Boolean = token.length > 40

