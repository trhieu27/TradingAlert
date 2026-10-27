package com.tradingalert.receiver.data.notification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    @ColumnInfo(name = "received_at", index = true)
    val receivedAt: Long,
    @ColumnInfo(name = "image_url")
    val imageUrl: String?,
)

internal fun NotificationEntity.toHistoryItem() = NotificationHistoryItem(
    id = id,
    title = title,
    body = body,
    receivedAt = receivedAt,
    imageUrl = imageUrl,
)
