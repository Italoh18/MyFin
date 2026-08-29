package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Query("SELECT * FROM incomes WHERE year = :year AND month = :month ORDER BY isBaseSalary DESC, timestamp DESC")
    fun getIncomesForMonthFlow(year: Int, month: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE isBaseSalary = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBaseSalaryFlow(): Flow<IncomeEntity?>

    @Query("SELECT * FROM incomes WHERE year = :year AND month = :month AND isBaseSalary = 1 LIMIT 1")
    suspend fun getBaseSalaryForMonth(year: Int, month: Int): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM incomes WHERE year = :year AND month = :month AND isBaseSalary = 1")
    suspend fun deleteBaseSalaryForMonth(year: Int, month: Int)
}
