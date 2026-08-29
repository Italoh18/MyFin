package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ExpenseHistoryList
import com.example.ui.components.FastExpenseInputBar
import com.example.ui.components.MonthlySalaryHeader
import com.example.ui.components.NotificationSuggestionsCard
import com.example.ui.components.PieChartMonthly
import com.example.ui.components.RecurringExpensesSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FinanceApp()
            }
        }
    }
}

enum class FinanceTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
) {
    OVERVIEW(
        "Início",
        Icons.Filled.PieChart,
        Icons.Outlined.PieChart,
        "tab_overview"
    ),
    RECURRING(
        "Recorrentes",
        Icons.Filled.EventRepeat,
        Icons.Outlined.EventRepeat,
        "tab_recurring"
    ),
    NOTIFICATIONS(
        "Notificações",
        Icons.Filled.NotificationsActive,
        Icons.Outlined.NotificationsActive,
        "tab_notifications"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(
    viewModel: FinanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MyFin",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                FinanceTab.entries.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            if (tab == FinanceTab.NOTIFICATIONS && uiState.pendingSuggestions.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text(text = uiState.pendingSuggestions.size.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content_animation"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> {
                            // OVERVIEW TAB: Salary Header + Pie Chart + Fast Input Bar + History
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Pending notification banner if any
                                if (uiState.pendingSuggestions.isNotEmpty()) {
                                    NotificationSuggestionsCard(
                                        pendingSuggestions = uiState.pendingSuggestions,
                                        onAcceptSuggestion = { viewModel.acceptNotificationSuggestion(it) },
                                        onDismissSuggestion = { viewModel.dismissNotificationSuggestion(it) },
                                        onSimulateNotification = { title, text -> viewModel.simulateNotification(title, text) }
                                    )
                                }

                                // Monthly Salary & Extra Incomes Header
                                MonthlySalaryHeader(
                                    baseSalary = uiState.baseSalary,
                                    extraIncomes = uiState.incomes.filter { !it.isBaseSalary },
                                    totalExpense = uiState.totalExpense,
                                    onSaveSalary = { viewModel.setMonthlySalary(it) },
                                    onAddExtra = { title, amount -> viewModel.addExtraIncome(title, amount) },
                                    onDeleteIncome = { viewModel.deleteIncome(it) }
                                )

                                // 1. Top Circular Pie Chart
                                PieChartMonthly(
                                    monthName = uiState.monthName,
                                    totalExpense = uiState.totalExpense,
                                    slices = uiState.categorySlices,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onResetMonth = { viewModel.resetToCurrentMonth() }
                                )

                                // 2. Fast Expense Input Bar (With Auto-complete on tap & + for purpose)
                                FastExpenseInputBar(
                                    lastExpense = uiState.lastExpense,
                                    recentExpenses = uiState.recentExpenses,
                                    onAddExpense = { desc, amount, purpose ->
                                        viewModel.addExpense(desc, amount, purpose)
                                    }
                                )

                                // 3. Expense History List (with circular exclamation button for purpose)
                                ExpenseHistoryList(
                                    expenses = uiState.expenses,
                                    onDeleteExpense = { viewModel.deleteExpense(it) }
                                )
                            }
                        }

                        1 -> {
                            // RECURRING EXPENSES TAB: Registration Form & Projection
                            RecurringExpensesSection(
                                recurringList = uiState.recurringExpenses,
                                projectedPayments = uiState.projectedPayments,
                                totalProjectedThisMonth = uiState.totalProjectedThisMonth,
                                onAddRecurring = { name, amount, cat, startDate, recurrence, projUntil ->
                                    viewModel.addRecurringExpense(
                                        name, amount, cat, startDate, recurrence, projUntil
                                    )
                                },
                                onToggleActive = { viewModel.toggleRecurringActive(it) },
                                onDeleteRecurring = { viewModel.deleteRecurring(it) },
                                onLaunchAsExpense = { name, amount, cat ->
                                    viewModel.addExpense(name, amount, source = "recurring")
                                }
                            )
                        }

                        2 -> {
                            // NOTIFICATIONS TAB: Live listener status, detected pending suggestions, testing simulator
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                NotificationSuggestionsCard(
                                    pendingSuggestions = uiState.pendingSuggestions,
                                    onAcceptSuggestion = { viewModel.acceptNotificationSuggestion(it) },
                                    onDismissSuggestion = { viewModel.dismissNotificationSuggestion(it) },
                                    onSimulateNotification = { title, text -> viewModel.simulateNotification(title, text) }
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Como funciona a detecção?",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "O aplicativo monitora notificações bancárias (Nubank, Itaú, Bradesco, Inter, PicPay, Cartões, etc.) diretamente no seu aparelho. Quando detecta uma compra ou Pix com valor em R$, ele sugere automaticamente a adição para você aprovar com um toque.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
