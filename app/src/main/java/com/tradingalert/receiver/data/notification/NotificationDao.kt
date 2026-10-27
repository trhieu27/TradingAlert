package com.tradingalert.receiver.data.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY received_at DESC, id DESC LIMIT :limit")
    abstract fun observeLatest(limit: Int): Flow<List<NotificationEntity>>

    @Insert
    protected abstract suspend fun insert(entity: NotificationEntity)

    @Query(
        """
        DELETE FROM notifications
        WHERE id NOT IN (
            SELECT id FROM notifications
            ORDER BY received_at DESC, id DESC
            LIMIT :limit
        )
        """,
    )
    protected abstract suspend fun deleteOlderThanLimit(limit: Int)

    @Transaction
    open suspend fun insertAndTrim(entity: NotificationEntity, limit: Int) {
        insert(entity)
        deleteOlderThanLimit(limit)
    }
}
