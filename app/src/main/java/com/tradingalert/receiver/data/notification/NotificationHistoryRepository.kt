package com.tradingalert.receiver.data.notification

import android.content.Context
import com.tradingalert.receiver.messaging.PushMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object NotificationHistoryRepository {
    private const val MAX_HISTORY_ITEMS = 50

    fun observe(context: Context): Flow<List<NotificationHistoryItem>> {
        val dao = NotificationDatabase.getInstance(context).notificationDao()
        return dao.observeLatest(MAX_HISTORY_ITEMS)
            .map { entities -> entities.map(NotificationEntity::toHistoryItem) }
    }

    suspend fun append(
        context: Context,
        message: PushMessage,
        receivedAt: Long = System.currentTimeMillis(),
    ) {
        NotificationDatabase.getInstance(context).notificationDao().insertAndTrim(
            entity = NotificationEntity(
                title = message.title,
                body = message.body,
                receivedAt = receivedAt,
                imageUrl = message.imageUrl,
            ),
            limit = MAX_HISTORY_ITEMS,
        )
    }

}
