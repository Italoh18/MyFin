package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val category: String = ExpenseCategory.CONTAS.id,
    val startDate: Long = System.currentTimeMillis(), // Initial payment date / base day
    val recurrenceType: String = RecurrenceType.MENSAL.id, // SEMANAL, QUINZENAL, MENSAL
    val projectedUntilDate: Long = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000), // Until when it's projected (e.g. 6 months default)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
