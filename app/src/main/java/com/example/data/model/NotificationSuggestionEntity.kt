package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SuggestionStatus {
    PENDING,
    ACCEPTED,
    DISMISSED
}

@Entity(tableName = "notification_suggestions")
data class NotificationSuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val messageText: String,
    val packageName: String,
    val extractedAmount: Double,
    val extractedDescription: String,
    val suggestedCategory: String = ExpenseCategory.OUTROS.id,
    val detectedTimestamp: Long = System.currentTimeMillis(),
    val status: String = SuggestionStatus.PENDING.name
)
