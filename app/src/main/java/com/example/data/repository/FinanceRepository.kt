package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.ExpenseDao
import com.example.data.db.IncomeDao
import com.example.data.db.NotificationSuggestionDao
import com.example.data.db.RecurringExpenseDao
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.NotificationSuggestionEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SuggestionStatus
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao,
    private val notificationSuggestionDao: NotificationSuggestionDao,
    private val incomeDao: IncomeDao
) {
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpensesFlow()
    val lastExpenseFlow: Flow<ExpenseEntity?> = expenseDao.getLastExpenseFlow()
    val recentExpensesFlow: Flow<List<ExpenseEntity>> = expenseDao.getRecentExpensesFlow(10)
    val allRecurringExpenses: Flow<List<RecurringExpenseEntity>> = recurringExpenseDao.getAllRecurringFlow()
    val pendingNotificationSuggestions: Flow<List<NotificationSuggestionEntity>> = notificationSuggestionDao.getPendingSuggestionsFlow()
    val latestBaseSalaryFlow: Flow<IncomeEntity?> = incomeDao.getLatestBaseSalaryFlow()

    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesBetweenFlow(startTime, endTime)
    }

    fun getIncomesForMonth(year: Int, month: Int): Flow<List<IncomeEntity>> {
        return incomeDao.getIncomesForMonthFlow(year, month)
    }

    suspend fun setMonthlySalary(year: Int, month: Int, amount: Double): Long {
        incomeDao.deleteBaseSalaryForMonth(year, month)
        val salary = IncomeEntity(
            title = "Salário Mensal",
            amount = amount,
            isBaseSalary = true,
            month = month,
            year = year,
            timestamp = System.currentTimeMillis()
        )
        return incomeDao.insertIncome(salary)
    }

    suspend fun addExtraIncome(year: Int, month: Int, title: String, amount: Double): Long {
        val extra = IncomeEntity(
            title = title.trim(),
            amount = amount,
            isBaseSalary = false,
            month = month,
            year = year,
            timestamp = System.currentTimeMillis()
        )
        return incomeDao.insertIncome(extra)
    }

    suspend fun deleteIncome(income: IncomeEntity) {
        incomeDao.deleteIncome(income)
    }

    suspend fun deleteIncomeById(id: Long) {
        incomeDao.deleteById(id)
    }

    suspend fun getLastExpense(): ExpenseEntity? {
        return expenseDao.getLastExpense()
    }

    suspend fun addExpense(expense: ExpenseEntity): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteById(id)
    }

    // Recurring
    suspend fun addRecurringExpense(recurring: RecurringExpenseEntity): Long {
        return recurringExpenseDao.insertRecurring(recurring)
    }

    suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity) {
        recurringExpenseDao.updateRecurring(recurring)
    }

    suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity) {
        recurringExpenseDao.deleteRecurring(recurring)
    }

    suspend fun deleteRecurringById(id: Long) {
        recurringExpenseDao.deleteById(id)
    }

    // Notifications
    suspend fun addNotificationSuggestion(suggestion: NotificationSuggestionEntity): Long {
        return notificationSuggestionDao.insertSuggestion(suggestion)
    }

    suspend fun dismissSuggestion(id: Long) {
        notificationSuggestionDao.dismissSuggestion(id)
    }

    suspend fun acceptSuggestion(suggestion: NotificationSuggestionEntity): Long {
        notificationSuggestionDao.markAccepted(suggestion.id)
        val expense = ExpenseEntity(
            description = suggestion.extractedDescription,
            amount = suggestion.extractedAmount,
            category = suggestion.suggestedCategory,
            timestamp = suggestion.detectedTimestamp,
            source = "notification"
        )
        return expenseDao.insertExpense(expense)
    }

    companion object {
        fun create(database: AppDatabase): FinanceRepository {
            return FinanceRepository(
                database.expenseDao(),
                database.recurringExpenseDao(),
                database.notificationSuggestionDao(),
                database.incomeDao()
            )
        }
    }
}
