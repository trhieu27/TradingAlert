package com.tradingalert.receiver.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tradingalert.receiver.R
import com.tradingalert.receiver.data.notification.NotificationHistoryRepository
import com.tradingalert.receiver.localization.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AlertMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val payload = remoteMessage.data.toMutableMap().apply {
            remoteMessage.notification?.title?.let { putIfAbsent("title", it) }
            remoteMessage.notification?.body?.let { putIfAbsent("body", it) }
            remoteMessage.notification?.imageUrl?.toString()?.let { putIfAbsent("image", it) }
        }
        val localizedContext = AppLanguageManager.localizedContext(this)
        val message = PushMessage.from(
            data = payload,
            defaultTitle = localizedContext.getString(R.string.default_notification_title),
            defaultBody = localizedContext.getString(R.string.default_notification_body),
        )
        val receivedAt = System.currentTimeMillis()

        runBlocking(Dispatchers.IO) {
            NotificationHistoryRepository.append(
                context = this@AlertMessagingService,
                message = message,
                receivedAt = receivedAt,
            )
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_TITLE, message.title)
            .putString(KEY_LAST_BODY, message.body)
            .putLong(KEY_LAST_TIME, receivedAt)
            .apply()

        val notificationId = NotificationHelper.nextId()
        val shown = NotificationHelper.show(
            context = this,
            message = message,
            notificationId = notificationId,
        )
        if (shown && message.imageUrl != null) {
            NotificationImageWorker.enqueue(
                context = this,
                message = message,
                notificationId = notificationId,
            )
        }
        val payloadType = when {
            remoteMessage.notification != null && remoteMessage.data.isNotEmpty() -> "notification+data"
            remoteMessage.notification != null -> "notification"
            else -> "data"
        }
        Log.d(
            TAG,
            "FCM message handled: id=${remoteMessage.messageId}, type=$payloadType, hasImage=${message.imageUrl != null}",
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_TIME, System.currentTimeMillis())
            .apply()

        // TODO: Send the refreshed token to your authenticated application server.
        Log.d(TAG, "FCM registration token refreshed")
    }

    companion object {
        const val PREFS_NAME = "firebase_alert_preferences"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_TOKEN_TIME = "fcm_token_time"
        const val KEY_LAST_TITLE = "last_title"
        const val KEY_LAST_BODY = "last_body"
        const val KEY_LAST_TIME = "last_time"
        private const val TAG = "AlertMessaging"
    }
}
