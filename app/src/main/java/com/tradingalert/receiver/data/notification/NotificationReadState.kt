package com.tradingalert.receiver.data.notification

import android.content.Context

object NotificationReadState {
    private const val PREFS_NAME = "notification_read_state"
    private const val KEY_LAST_READ_ID = "last_read_notification_id"

    fun lastReadId(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_READ_ID, 0L)

    fun markReadThrough(context: Context, notificationId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_READ_ID, notificationId.coerceAtLeast(0L))
            .apply()
    }
}
