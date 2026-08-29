package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val isBaseSalary: Boolean = false,
    val month: Int,
    val year: Int,
    val timestamp: Long = System.currentTimeMillis()
)
