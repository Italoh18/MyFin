package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.data.model.IncomeEntity
import com.example.data.model.NotificationSuggestionEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.RecurringExpenseEntity
import com.example.data.model.SuggestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        ExpenseEntity::class,
        RecurringExpenseEntity::class,
        NotificationSuggestionEntity::class,
        IncomeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun notificationSuggestionDao(): NotificationSuggestionDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financas_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val appDb = getDatabase(context)
                    populateInitialData(appDb)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val now = Calendar.getInstance()
            val currentYear = now.get(Calendar.YEAR)
            val currentMonth = now.get(Calendar.MONTH)

            fun dateForDay(day: Int): Long {
                val cal = Calendar.getInstance()
                cal.set(currentYear, currentMonth, day.coerceIn(1, 28), 12, 0, 0)
                return cal.timeInMillis
            }

            val sampleExpenses = listOf(
                ExpenseEntity(
                    description = "Supermercado",
                    amount = 320.50,
                    category = "Supermercado",
                    purpose = "Compras do mês: carnes, hortifruti e itens de higiene",
                    timestamp = dateForDay(2)
                ),
                ExpenseEntity(
                    description = "Combustível",
                    amount = 180.00,
                    category = "Combustível",
                    purpose = "Abastecimento do carro no posto para semana de trabalho",
                    timestamp = dateForDay(5)
                ),
                ExpenseEntity(
                    description = "Internet",
                    amount = 119.90,
                    category = "Internet",
                    purpose = "Mensalidade do plano de 500 Mega residencial",
                    timestamp = dateForDay(10),
                    isRecurring = true
                ),
                ExpenseEntity(
                    description = "Cinema",
                    amount = 65.00,
                    category = "Cinema",
                    purpose = "Ingressos e pipoca para sessão de sábado à noite",
                    timestamp = dateForDay(14)
                ),
                ExpenseEntity(
                    description = "Farmácia",
                    amount = 89.40,
                    category = "Farmácia",
                    purpose = "Vitaminas C, D e analgésicos",
                    timestamp = dateForDay(18)
                ),
                ExpenseEntity(
                    description = "Almoço",
                    amount = 45.00,
                    category = "Almoço",
                    purpose = "Almoço executivo com os colegas de trabalho",
                    timestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 4) // 4 hours ago
                )
            )
            db.expenseDao().insertAll(sampleExpenses)

            // Sample Recurring Expenses
            val calRecurringEnd = Calendar.getInstance()
            calRecurringEnd.add(Calendar.MONTH, 12)

            val sampleRecurring = listOf(
                RecurringExpenseEntity(
                    name = "Aluguel & Condomínio",
                    amount = 1450.00,
                    category = "Aluguel",
                    startDate = dateForDay(10),
                    recurrenceType = RecurrenceType.MENSAL.id,
                    projectedUntilDate = calRecurringEnd.timeInMillis
                ),
                RecurringExpenseEntity(
                    name = "Academia",
                    amount = 119.90,
                    category = "Academia",
                    startDate = dateForDay(15),
                    recurrenceType = RecurrenceType.MENSAL.id,
                    projectedUntilDate = calRecurringEnd.timeInMillis
                ),
                RecurringExpenseEntity(
                    name = "Feira",
                    amount = 90.00,
                    category = "Feira",
                    startDate = dateForDay(1),
                    recurrenceType = RecurrenceType.SEMANAL.id,
                    projectedUntilDate = calRecurringEnd.timeInMillis
                ),
                RecurringExpenseEntity(
                    name = "Diarista",
                    amount = 180.00,
                    category = "Diarista",
                    startDate = dateForDay(5),
                    recurrenceType = RecurrenceType.QUINZENAL.id,
                    projectedUntilDate = calRecurringEnd.timeInMillis
                )
            )
            db.recurringExpenseDao().insertAll(sampleRecurring)

            // Sample Notification Suggestion detected from bank
            val sampleNotification = NotificationSuggestionEntity(
                title = "Nubank",
                messageText = "Compra de R$ 42,90 aprovada no iFood",
                packageName = "com.nu.production",
                extractedAmount = 42.90,
                extractedDescription = "iFood",
                suggestedCategory = "iFood",
                detectedTimestamp = System.currentTimeMillis() - (1000 * 60 * 30),
                status = SuggestionStatus.PENDING.name
            )
            db.notificationSuggestionDao().insertSuggestion(sampleNotification)

            // Initial Monthly Salary & Extra for demonstration
            db.incomeDao().insertIncome(
                IncomeEntity(
                    title = "Salário Mensal",
                    amount = 3800.00,
                    isBaseSalary = true,
                    month = currentMonth,
                    year = currentYear,
                    timestamp = dateForDay(1)
                )
            )
            db.incomeDao().insertIncome(
                IncomeEntity(
                    title = "Freelance Design",
                    amount = 450.00,
                    isBaseSalary = false,
                    month = currentMonth,
                    year = currentYear,
                    timestamp = dateForDay(12)
                )
            )
        }
    }
}
