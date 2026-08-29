package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.NotificationSuggestionEntity
import com.example.data.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ExpenseNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        try {
            val packageName = sbn.packageName.orEmpty()
            // Ignore own package notifications
            if (packageName == applicationContext.packageName) return

            val extras = sbn.notification.extras ?: return
            val title = extras.getString(Notification.EXTRA_TITLE)
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            val text = extras.getString(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

            if (title.isNullOrBlank() && text.isNullOrBlank()) return

            if (NotificationParser.isFinancialNotification(title, text)) {
                val parsed = NotificationParser.parse(title, text)
                if (parsed != null) {
                    val suggestion = NotificationSuggestionEntity(
                        title = title ?: "Notificação Bancária",
                        messageText = text ?: parsed.originalText,
                        packageName = packageName,
                        extractedAmount = parsed.amount,
                        extractedDescription = parsed.description,
                        suggestedCategory = parsed.category.id,
                        detectedTimestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        status = SuggestionStatus.PENDING.name
                    )

                    serviceScope.launch {
                        val db = AppDatabase.getDatabase(applicationContext)
                        db.notificationSuggestionDao().insertSuggestion(suggestion)
                        Log.d("ExpenseNotificationService", "Extracted transaction: $suggestion")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExpenseNotificationService", "Error processing notification", e)
        }
    }
}
