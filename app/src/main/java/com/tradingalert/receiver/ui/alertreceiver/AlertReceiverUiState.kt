package com.tradingalert.receiver.ui.alertreceiver

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.tradingalert.receiver.R
import com.tradingalert.receiver.messaging.AlertMessagingService
import com.tradingalert.receiver.messaging.AlertPreferenceStore
import com.tradingalert.receiver.messaging.NotificationHelper
import com.tradingalert.receiver.messaging.MessagingServerConfig
import com.tradingalert.receiver.data.notification.NotificationHistoryItem
import com.tradingalert.receiver.data.notification.NotificationReadState

internal data class AlertReceiverUiState(
    val notificationsAllowed: Boolean = false,
    val token: String = "",
    val tokenUpdatedAt: Long = 0L,
    val tokenRefreshing: Boolean = false,
    val serverBaseUrl: String? = null,
    val historyLoading: Boolean = false,
    val alertVolumePercent: Int = AlertPreferenceStore.DEFAULT_VOLUME_PERCENT,
    val soundDurationSeconds: Int = AlertPreferenceStore.DEFAULT_SOUND_DURATION_SECONDS,
    val alertSoundName: String? = null,
    val batteryOptimizationRequired: Boolean = false,
    val backgroundActivityRestricted: Boolean = false,
    val backgroundDataRestricted: Boolean = false,
    val autoStartSettingsAvailable: Boolean = false,
    val notificationHistory: List<NotificationHistoryItem> = emptyList(),
    val lastReadNotificationId: Long = 0L,
    val lastTitle: String? = null,
    val lastBody: String? = null,
    val lastTime: Long = 0L,
) {
    val firebaseConnected: Boolean
        get() = !serverBaseUrl.isNullOrBlank() &&
            !tokenRefreshing &&
            isUsableToken(token)

    val unreadNotificationCount: Int
        get() {
            val newestId = notificationHistory.maxOfOrNull(NotificationHistoryItem::id)
                ?: return 0
            if (lastReadNotificationId > newestId) return notificationHistory.size
            return notificationHistory.count { it.id > lastReadNotificationId }
        }
}

internal fun loadAlertReceiverState(context: Context): AlertReceiverUiState {
    val prefs = context.getSharedPreferences(AlertMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
    val alertPreferences = AlertPreferenceStore.load(context)
    val appNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    return AlertReceiverUiState(
        notificationsAllowed = appNotificationsEnabled,
        token = prefs.getString(AlertMessagingService.KEY_TOKEN, null)
            ?: context.getString(R.string.token_loading),
        tokenUpdatedAt = prefs.getLong(AlertMessagingService.KEY_TOKEN_TIME, 0L),
        serverBaseUrl = MessagingServerConfig.serverBaseUrl(context),
        alertVolumePercent = alertPreferences.volumePercent,
        soundDurationSeconds = alertPreferences.soundDurationSeconds,
        alertSoundName = NotificationHelper.channelSoundName(context),
        batteryOptimizationRequired = DeviceReliabilitySettings.isBatteryOptimizationRequired(context),
        backgroundActivityRestricted = DeviceReliabilitySettings.isBackgroundActivityRestricted(context),
        backgroundDataRestricted = DeviceReliabilitySettings.isBackgroundDataRestricted(context),
        autoStartSettingsAvailable = DeviceReliabilitySettings.hasAutoStartSettings(context),
        lastReadNotificationId = NotificationReadState.lastReadId(context),
        lastTitle = prefs.getString(AlertMessagingService.KEY_LAST_TITLE, null),
        lastBody = prefs.getString(AlertMessagingService.KEY_LAST_BODY, null),
        lastTime = prefs.getLong(AlertMessagingService.KEY_LAST_TIME, 0L),
    )
}

internal fun isUsableToken(token: String): Boolean = token.length > 40

