package com.tradingalert.receiver.messaging

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.tradingalert.receiver.R
import com.tradingalert.receiver.data.notification.NotificationHistoryRepository
import com.tradingalert.receiver.localization.AppLanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AlertMessagingService : FirebaseMessagingService() {
    override fun handleIntent(intent: Intent) {
        if (intent.action == ACTION_REMOTE_MESSAGE) {
            intent.extras?.let { extras ->
                persistIncomingMessage(RemoteMessage(extras))
            }
        }
        super.handleIntent(intent)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val message = remoteMessage.toPushMessage()
        persistIncomingMessage(remoteMessage, message)

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

    private fun persistIncomingMessage(
        remoteMessage: RemoteMessage,
        message: PushMessage = remoteMessage.toPushMessage(),
    ) {
        val messageId = remoteMessage.messageId
        if (messageId != null && !markForPersistence(messageId)) return

        val receivedAt = System.currentTimeMillis()
        val persisted = runCatching {
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
        }.onFailure { error ->
            Log.e(TAG, "Unable to persist incoming FCM message", error)
        }.isSuccess

        if (!persisted && messageId != null) forgetPersistenceMark(messageId)
    }

    private fun RemoteMessage.toPushMessage(): PushMessage {
        val payload = data.toMutableMap().apply {
            notification?.title?.let { putIfAbsent("title", it) }
            notification?.body?.let { putIfAbsent("body", it) }
            notification?.imageUrl?.toString()?.let { putIfAbsent("image", it) }
        }
        val localizedContext = AppLanguageManager.localizedContext(this@AlertMessagingService)
        return PushMessage.from(
            data = payload,
            defaultTitle = localizedContext.getString(R.string.default_notification_title),
            defaultBody = localizedContext.getString(R.string.default_notification_body),
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_TOKEN_TIME, System.currentTimeMillis())
            .apply()

        FirebaseMessaging.getInstance()
            .subscribeToTopic(BROADCAST_TOPIC)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "FCM token refreshed and broadcast topic subscribed")
                } else {
                    Log.w(TAG, "Unable to subscribe to FCM broadcast topic", task.exception)
                }
            }
    }

    companion object {
        const val PREFS_NAME = "firebase_alert_preferences"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_TOKEN_TIME = "fcm_token_time"
        const val KEY_LAST_TITLE = "last_title"
        const val KEY_LAST_BODY = "last_body"
        const val KEY_LAST_TIME = "last_time"
        const val BROADCAST_TOPIC = "trading_alerts"
        private const val ACTION_REMOTE_MESSAGE = "com.google.android.c2dm.intent.RECEIVE"
        private const val MAX_RECENT_MESSAGE_IDS = 20
        private const val TAG = "AlertMessaging"
        private val recentMessageIds = ArrayDeque<String>()

        private fun markForPersistence(messageId: String): Boolean = synchronized(recentMessageIds) {
            if (messageId in recentMessageIds) return@synchronized false
            if (recentMessageIds.size >= MAX_RECENT_MESSAGE_IDS) recentMessageIds.removeFirst()
            recentMessageIds.addLast(messageId)
            true
        }

        private fun forgetPersistenceMark(messageId: String) = synchronized(recentMessageIds) {
            recentMessageIds.remove(messageId)
            Unit
        }
    }
}
