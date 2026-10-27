package com.tradingalert.receiver.data.notification

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var instance: NotificationDatabase? = null

        fun getInstance(context: Context): NotificationDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }

        const val DATABASE_NAME = "trading_alert_receiver.db"
    }
}
