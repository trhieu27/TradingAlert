package com.tradingalert.receiver.data.notification

data class NotificationHistoryItem(
    val id: Long,
    val title: String,
    val body: String,
    val receivedAt: Long,
    val imageUrl: String? = null,
)
