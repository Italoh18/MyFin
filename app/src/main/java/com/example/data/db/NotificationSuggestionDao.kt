package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NotificationSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSuggestionDao {
    @Query("SELECT * FROM notification_suggestions WHERE status = 'PENDING' ORDER BY detectedTimestamp DESC")
    fun getPendingSuggestionsFlow(): Flow<List<NotificationSuggestionEntity>>

    @Query("SELECT * FROM notification_suggestions ORDER BY detectedTimestamp DESC")
    fun getAllSuggestionsFlow(): Flow<List<NotificationSuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: NotificationSuggestionEntity): Long

    @Update
    suspend fun updateSuggestion(suggestion: NotificationSuggestionEntity)

    @Query("UPDATE notification_suggestions SET status = 'DISMISSED' WHERE id = :id")
    suspend fun dismissSuggestion(id: Long)

    @Query("UPDATE notification_suggestions SET status = 'ACCEPTED' WHERE id = :id")
    suspend fun markAccepted(id: Long)

    @Delete
    suspend fun deleteSuggestion(suggestion: NotificationSuggestionEntity)

    @Query("DELETE FROM notification_suggestions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
