package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CategoryColorProvider
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.NotificationSuggestionEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.RecurringExpenseEntity
import com.example.data.repository.FinanceRepository
import com.example.service.NotificationParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySlice(
    val categoryName: String,
    val color: Color,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

data class ProjectedPayment(
    val recurringId: Long,
    val name: String,
    val amount: Double,
    val color: Color,
    val dueDate: Long,
    val isCurrentMonth: Boolean
)

data class MonthlyFinanceUiState(
    val year: Int,
    val month: Int, // 0-based (Calendar.JANUARY = 0)
    val monthName: String,
    val totalExpense: Double,
    val expenses: List<ExpenseEntity>,
    val categorySlices: List<CategorySlice>,
    val lastExpense: ExpenseEntity?,
    val recentExpenses: List<ExpenseEntity>,
    val recurringExpenses: List<RecurringExpenseEntity>,
    val projectedPayments: List<ProjectedPayment>,
    val totalProjectedThisMonth: Double,
    val pendingSuggestions: List<NotificationSuggestionEntity>,
    val incomes: List<IncomeEntity> = emptyList(),
    val baseSalary: Double = 0.0,
    val totalExtraIncome: Double = 0.0,
    val totalIncome: Double = 0.0,
    val remainingBalance: Double = 0.0
)

data class MonthlyDataBundle(
    val expenses: List<ExpenseEntity>,
    val lastExpense: ExpenseEntity?,
    val incomes: List<IncomeEntity>,
    val year: Int,
    val month: Int,
    val monthName: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar: StateFlow<Calendar> = _currentCalendar.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository.create(db)
    }

    private val monthRangeFlow = _currentCalendar.flatMapLatest { cal ->
        val startCal = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        val monthName = monthFormatter.format(cal.time).replaceFirstChar { it.uppercase() }

        combine(
            repository.getExpensesBetween(startCal.timeInMillis, endCal.timeInMillis),
            repository.lastExpenseFlow,
            repository.getIncomesForMonth(year, month)
        ) { expenses, lastExp, incomes ->
            MonthlyDataBundle(expenses, lastExp, incomes, year, month, monthName)
        }
    }

    val uiState: StateFlow<MonthlyFinanceUiState> = combine(
        monthRangeFlow,
        repository.recentExpensesFlow,
        repository.allRecurringExpenses,
        repository.pendingNotificationSuggestions
    ) { bundle, recentList, recurringList, suggestions ->
        val (expenses, lastExpense, incomes, year, month, monthName) = bundle
        val total = expenses.sumOf { it.amount }

        val baseSalary = incomes.firstOrNull { it.isBaseSalary }?.amount ?: 0.0
        val extras = incomes.filter { !it.isBaseSalary }
        val totalExtraIncome = extras.sumOf { it.amount }
        val totalIncome = baseSalary + totalExtraIncome
        val remainingBalance = totalIncome - total

        // Compute Category slices dynamically based on the expense name as category
        val grouped = expenses.groupBy {
            val key = if (it.category.isNotBlank()) it.category else it.description
            key.trim().replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
        }
        val slices = grouped.map { (catName, list) ->
            val catTotal = list.sumOf { it.amount }
            val percentage = if (total > 0) (catTotal / total).toFloat() else 0f
            CategorySlice(
                categoryName = catName,
                color = CategoryColorProvider.getColorForName(catName),
                totalAmount = catTotal,
                percentage = percentage,
                count = list.size
            )
        }.sortedByDescending { it.totalAmount }

        // Calculate recurring projections
        val projections = calculateProjections(recurringList, year, month)
        val projectedThisMonth = projections.filter { it.isCurrentMonth }.sumOf { it.amount }

        MonthlyFinanceUiState(
            year = year,
            month = month,
            monthName = monthName,
            totalExpense = total,
            expenses = expenses,
            categorySlices = slices,
            lastExpense = lastExpense,
            recentExpenses = recentList,
            recurringExpenses = recurringList,
            projectedPayments = projections,
            totalProjectedThisMonth = projectedThisMonth,
            pendingSuggestions = suggestions,
            incomes = incomes,
            baseSalary = baseSalary,
            totalExtraIncome = totalExtraIncome,
            totalIncome = totalIncome,
            remainingBalance = remainingBalance
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyFinanceUiState(
            year = Calendar.getInstance().get(Calendar.YEAR),
            month = Calendar.getInstance().get(Calendar.MONTH),
            monthName = "",
            totalExpense = 0.0,
            expenses = emptyList(),
            categorySlices = emptyList(),
            lastExpense = null,
            recentExpenses = emptyList(),
            recurringExpenses = emptyList(),
            projectedPayments = emptyList(),
            totalProjectedThisMonth = 0.0,
            pendingSuggestions = emptyList()
        )
    )

    fun setMonthlySalary(amount: Double) {
        val cal = _currentCalendar.value
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        viewModelScope.launch {
            repository.setMonthlySalary(year, month, amount)
        }
    }

    fun addExtraIncome(title: String, amount: Double) {
        if (title.isBlank() || amount <= 0.0) return
        val cal = _currentCalendar.value
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        viewModelScope.launch {
            repository.addExtraIncome(year, month, title.trim(), amount)
        }
    }

    fun deleteIncome(income: IncomeEntity) {
        viewModelScope.launch {
            repository.deleteIncome(income)
        }
    }

    fun previousMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _currentCalendar.value.timeInMillis
            add(Calendar.MONTH, -1)
        }
        _currentCalendar.value = newCal
    }

    fun nextMonth() {
        val newCal = Calendar.getInstance().apply {
            timeInMillis = _currentCalendar.value.timeInMillis
            add(Calendar.MONTH, 1)
        }
        _currentCalendar.value = newCal
    }

    fun resetToCurrentMonth() {
        _currentCalendar.value = Calendar.getInstance()
    }

    fun addExpense(
        description: String,
        amount: Double,
        purpose: String = "",
        timestamp: Long = System.currentTimeMillis(),
        source: String = "manual"
    ) {
        if (description.isBlank() || amount <= 0.0) return
        val cleanName = description.trim()
        viewModelScope.launch {
            repository.addExpense(
                ExpenseEntity(
                    description = cleanName,
                    amount = amount,
                    category = cleanName,
                    purpose = purpose.trim(),
                    timestamp = timestamp,
                    source = source
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addRecurringExpense(
        name: String,
        amount: Double,
        category: String = "",
        startDate: Long,
        recurrenceType: RecurrenceType,
        projectedUntilDate: Long
    ) {
        if (name.isBlank() || amount <= 0.0) return
        val cleanName = name.trim()
        viewModelScope.launch {
            repository.addRecurringExpense(
                RecurringExpenseEntity(
                    name = cleanName,
                    amount = amount,
                    category = if (category.isNotBlank()) category.trim() else cleanName,
                    startDate = startDate,
                    recurrenceType = recurrenceType.id,
                    projectedUntilDate = projectedUntilDate,
                    isActive = true
                )
            )
        }
    }

    fun toggleRecurringActive(recurring: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.updateRecurringExpense(recurring.copy(isActive = !recurring.isActive))
        }
    }

    fun deleteRecurring(recurring: RecurringExpenseEntity) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurring)
        }
    }

    fun acceptNotificationSuggestion(suggestion: NotificationSuggestionEntity) {
        viewModelScope.launch {
            repository.acceptSuggestion(suggestion)
        }
    }

    fun dismissNotificationSuggestion(id: Long) {
        viewModelScope.launch {
            repository.dismissSuggestion(id)
        }
    }

    // Helper for user testing & demonstration in UI
    fun simulateNotification(title: String, messageText: String) {
        val parsed = NotificationParser.parse(title, messageText)
        if (parsed != null) {
            val suggestion = NotificationSuggestionEntity(
                title = title,
                messageText = messageText,
                packageName = "com.banco.simulado",
                extractedAmount = parsed.amount,
                extractedDescription = parsed.description,
                suggestedCategory = parsed.description,
                detectedTimestamp = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.addNotificationSuggestion(suggestion)
            }
        }
    }

    private fun calculateProjections(
        recurringList: List<RecurringExpenseEntity>,
        targetYear: Int,
        targetMonth: Int
    ): List<ProjectedPayment> {
        val results = mutableListOf<ProjectedPayment>()
        val startOfTargetMonth = Calendar.getInstance().apply {
            set(targetYear, targetMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfTargetMonth = Calendar.getInstance().apply {
            set(targetYear, targetMonth, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        for (item in recurringList) {
            if (!item.isActive) continue

            val recurType = RecurrenceType.fromId(item.recurrenceType)
            val currentCal = Calendar.getInstance().apply { timeInMillis = item.startDate }
            val endLimit = item.projectedUntilDate

            // Iterate through occurrences
            var count = 0
            while (currentCal.timeInMillis <= endLimit && count < 100) {
                count++
                val payTime = currentCal.timeInMillis
                val isThisMonth = payTime in startOfTargetMonth..endOfTargetMonth

                // Add occurrence if it's within target window
                if (payTime >= startOfTargetMonth - (30L * 24 * 60 * 60 * 1000) && payTime <= endLimit) {
                    results.add(
                        ProjectedPayment(
                            recurringId = item.id,
                            name = item.name,
                            amount = item.amount,
                            color = CategoryColorProvider.getColorForName(item.category.ifBlank { item.name }),
                            dueDate = payTime,
                            isCurrentMonth = isThisMonth
                        )
                    )
                }

                // Advance by recurrence
                when (recurType) {
                    RecurrenceType.SEMANAL -> currentCal.add(Calendar.DAY_OF_YEAR, 7)
                    RecurrenceType.QUINZENAL -> currentCal.add(Calendar.DAY_OF_YEAR, 14)
                    RecurrenceType.MENSAL -> currentCal.add(Calendar.MONTH, 1)
                }
            }
        }

        return results.sortedBy { it.dueDate }
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
            return format.format(amount)
        }

        fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            return sdf.format(Date(timestamp))
        }

        fun formatDateTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
            return sdf.format(Date(timestamp))
        }
    }
}
