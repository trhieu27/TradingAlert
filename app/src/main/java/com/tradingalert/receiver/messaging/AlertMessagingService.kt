package com.tradingalert.receiver.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AlertMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val payload = remoteMessage.data.toMutableMap().apply {
            remoteMessage.notification?.title?.let { putIfAbsent("title", it) }
            remoteMessage.notification?.body?.let { putIfAbsent("body", it) }
        }
        val message = PushMessage.from(payload)

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_TITLE, message.title)
            .putString(KEY_LAST_BODY, message.body)
            .putLong(KEY_LAST_TIME, System.currentTimeMillis())
            .apply()

        NotificationHelper.show(this, message)
        Log.d(TAG, "FCM message handled: id=${remoteMessage.messageId}")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()

        // TODO: Send the refreshed token to your authenticated application server.
        Log.d(TAG, "FCM registration token refreshed")
    }

    companion object {
        const val PREFS_NAME = "firebase_alert_preferences"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_LAST_TITLE = "last_title"
        const val KEY_LAST_BODY = "last_body"
        const val KEY_LAST_TIME = "last_time"
        private const val TAG = "AlertMessaging"
    }
}
