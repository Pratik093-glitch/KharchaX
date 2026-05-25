package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // State Collection
    val language by viewModel.language.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val monthlyCapital by viewModel.monthlyCapital.collectAsState()
    val limitType by viewModel.limitType.collectAsState()
    val limitAmount by viewModel.limitAmount.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()

    // Navigation state (0: Home/Dashboard, 1: Profile)
    var currentTab by remember { mutableIntStateOf(0) }

    // Dialog state
    var showCapitalConfigDialog by remember { mutableStateOf(false) }

    // Calculate live budgets
    val currentMonthExpenses = expenses.filter { viewModel.isCurrentMonth(it.timestamp) }
    val todayExpenses = expenses.filter { viewModel.isToday(it.timestamp) }

    val totalSpentThisMonth = currentMonthExpenses.sumOf { it.amount }
    val totalSpentToday = todayExpenses.sumOf { it.amount }

    val balanceLeft = (monthlyCapital - totalSpentThisMonth).coerceAtLeast(0.0)
    val daysRemaining = viewModel.getDaysRemainingInMonth()
    val suggestedDailyLimit = if (daysRemaining > 0) balanceLeft / daysRemaining else 0.0

    // Is Limit Exceeded?
    val isLimitExceeded = if (limitType == "DAILY") {
        totalSpentToday > limitAmount
    } else {
        totalSpentThisMonth > limitAmount
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Text("📊", fontSize = 22.sp) },
                    label = { 
                        Text(
                            text = if (language == AppLanguage.HINDI) "डैशबोर्ड" else if (language == AppLanguage.HINGLISH) "Dashboard" else "Dashboard",
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    },
                    modifier = Modifier.testTag("dashboard_nav_item")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Text("👤", fontSize = 22.sp) },
                    label = { 
                        Text(
                            text = if (language == AppLanguage.HINDI) "प्रोफ़ाइल" else if (language == AppLanguage.HINGLISH) "Profile Setup" else "Profile Setup",
                            style = MaterialTheme.typography.labelMedium
                        ) 
                    },
                    modifier = Modifier.testTag("profile_nav_item")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Style-savvy Top Header with branding and fast toggles
            HeaderSection(
                userName = userName,
                language = language,
                isDarkMode = isDarkMode,
                onLanguageToggle = { viewModel.updateLanguage(it) },
                onThemeToggle = { viewModel.updateTheme(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            AnimatedVisibility(
                visible = currentTab == 0,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 24 }),
                exit = fadeOut()
            ) {
                DashboardScreen(
                    language = language,
                    userName = userName,
                    userRole = userRole,
                    monthlyCapital = monthlyCapital,
                    limitType = limitType,
                    limitAmount = limitAmount,
                    balanceLeft = balanceLeft,
                    totalSpentThisMonth = totalSpentThisMonth,
                    totalSpentToday = totalSpentToday,
                    daysRemaining = daysRemaining,
                    suggestedDailyLimit = suggestedDailyLimit,
                    isLimitExceeded = isLimitExceeded,
                    expenses = expenses,
                    onOpenCapitalSetup = { showCapitalConfigDialog = true },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onAddExpense = { amt, cat, note, dateMs ->
                        viewModel.addExpense(amt, cat, note, dateMs)
                    },
                    viewModel = viewModel
                )
            }

            AnimatedVisibility(
                visible = currentTab == 1,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 24 }),
                exit = fadeOut()
            ) {
                ProfileSetupScreen(
                    language = language,
                    name = userName,
                    role = userRole,
                    onSaveProfile = { nameInput, roleInput ->
                        viewModel.saveProfile(nameInput, roleInput)
                        currentTab = 0 // Auto navigate back to dashboard
                    }
                )
            }
        }
    }

    // Modal Sheet / Dialog to Setup/Change Capital Configuration
    if (showCapitalConfigDialog) {
        CapitalSetupDialog(
            language = language,
            initialCapital = monthlyCapital,
            initialIsDaily = limitType == "DAILY",
            initialLimitVal = limitAmount,
            onDismiss = { showCapitalConfigDialog = false },
            onSave = { capital, isDaily, limit ->
                viewModel.saveCapitalAndLimit(capital, isDaily, limit)
                showCapitalConfigDialog = false
            }
        )
    }
}

@Composable
fun HeaderSection(
    userName: String,
    language: AppLanguage,
    isDarkMode: Boolean,
    onLanguageToggle: (AppLanguage) -> Unit,
    onThemeToggle: (Boolean) -> Unit
) {
    val initials = remember(userName) {
        if (userName.isBlank()) "KT" else {
            val parts = userName.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
            } else if (parts.isNotEmpty()) {
                parts[0].take(2).uppercase()
            } else {
                "KT"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Elegant circular avatar badge showing initials
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("user_avatar_badge"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            Column {
                Text(
                    text = Translations.get("app_title", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (language == AppLanguage.HINDI) "आसान बजट ट्रैकर" else "Aasaan Budget Tracker",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Language selector button cycles language
            IconButton(
                onClick = {
                    val nextLang = when (language) {
                        AppLanguage.HINGLISH -> AppLanguage.ENGLISH
                        AppLanguage.ENGLISH -> AppLanguage.HINDI
                        AppLanguage.HINDI -> AppLanguage.HINGLISH
                    }
                    onLanguageToggle(nextLang)
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .testTag("language_toggle_btn")
            ) {
                val flag = when (language) {
                    AppLanguage.ENGLISH -> "🇬🇧"
                    AppLanguage.HINDI -> "🇮🇳"
                    AppLanguage.HINGLISH -> "🗣️"
                }
                Text(flag, fontSize = 18.sp)
            }

            // Dark/Light Mode toggle button
            IconButton(
                onClick = { onThemeToggle(!isDarkMode) },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        CircleShape
                    )
                    .testTag("theme_toggle_btn")
            ) {
                Text(if (isDarkMode) "☀️" else "🌙", fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    language: AppLanguage,
    userName: String,
    userRole: String,
    monthlyCapital: Double,
    limitType: String,
    limitAmount: Double,
    balanceLeft: Double,
    totalSpentThisMonth: Double,
    totalSpentToday: Double,
    daysRemaining: Int,
    suggestedDailyLimit: Double,
    isLimitExceeded: Boolean,
    expenses: List<Expense>,
    onOpenCapitalSetup: () -> Unit,
    onDeleteExpense: (Int) -> Unit,
    onAddExpense: (Double, String, String, Long) -> Unit,
    viewModel: MainViewModel
) {
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
        ) {
            // Welcome Header / General Role Dynamic Tag
            item {
                val greetName = userName.ifEmpty { 
                    if (language == AppLanguage.HINDI) "दोस्त" else "Friend" 
                }
                val roleKey = userRole.lowercase()
                val roleTag = if (roleKey == "student" || roleKey == "housewife" || roleKey == "personal" || roleKey == "salaried" || roleKey == "freelancer" || roleKey == "business") {
                    Translations.get(roleKey, language)
                } else {
                    Translations.get("personal", language)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.HINDI) "नमस्ते, $greetName! 👋" else "Namaste, $greetName! 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = roleTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Summary Card showing local capital and limits
            item {
                val spentPercentage = if (monthlyCapital > 0) (totalSpentThisMonth / monthlyCapital).toFloat().coerceIn(0f, 1f) else 0f
                
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLimitExceeded) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Translations.get("balance_left", language).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isLimitExceeded) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                }
                            ) {
                                val limitLabel = if (limitType == "DAILY") {
                                    Translations.get("daily_reset", language)
                                } else {
                                    Translations.get("monthly_reset", language)
                                }
                                Text(
                                    text = limitLabel,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${"%,.2f".format(balanceLeft)}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Styled custom progress bar matching CSS .h-2 .bg-white/20 with .bg-[#D0BCFF]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(spentPercentage)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = Translations.get("monthly_capital", language),
                                    fontSize = 11.sp,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${"%,.0f".format(monthlyCapital)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Translations.get("budget_limit", language),
                                    fontSize = 11.sp,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${"%,.0f".format(limitAmount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(
                            color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f) 
                                    else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Spent stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = Translations.get("today_spent", language),
                                    fontSize = 11.sp,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${"%,.1f".format(totalSpentToday)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Translations.get("month_spent", language),
                                    fontSize = 11.sp,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${"%,.1f".format(totalSpentThisMonth)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Alert warning pill / Safe indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLimitExceeded) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isLimitExceeded) {
                                    Translations.get("limit_warning", language)
                                } else {
                                    Translations.get("limit_ok", language)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days remaining auto suggestion calculator for Housewife / Student budget
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${Translations.get("suggested_limit", language)} ₹${"%.1f".format(suggestedDailyLimit)}/day ($daysRemaining ${Translations.get("days_left", language)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLimitExceeded) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Quick side-by-side action Grid (from HTML Theme)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Set Capital Dialog Trigger Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenCapitalSetup() },
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💳", fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.HINDI) "बजट सेट करें" else if (language == AppLanguage.HINGLISH) "Set Capital" else "Set Budget",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Add Expense Bottom Sheet Trigger Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddExpenseSheet = true },
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("➕", fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Translations.get("add_expense", language),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Recent Expense Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translations.get("recent_expenses", language),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "(${expenses.size})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty state indicator
            if (expenses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌸", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Translations.get("no_expenses_yet", language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseItemRow(
                        expense = expense,
                        language = language,
                        onDelete = { onDeleteExpense(expense.id) },
                        viewModel = viewModel
                    )
                }
            }
        }

        // Add Expense Modal Bottom Sheet
        if (showAddExpenseSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddExpenseSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    AddExpenseForm(
                        language = language,
                        onSaveExpense = { amt, cat, note, dateMs ->
                            onAddExpense(amt, cat, note, dateMs)
                            showAddExpenseSheet = false
                        },
                        viewModel = viewModel
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun AddExpenseForm(
    language: AppLanguage,
    onSaveExpense: (Double, String, String, Long) -> Unit,
    viewModel: MainViewModel
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedDateMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Formatting date label
    val formattedDate = remember(selectedDateMs) {
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        formatter.format(Date(selectedDateMs))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = Translations.get("add_expense", language),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it.filter { char -> char.isDigit() || char == '.' }
                    showError = false
                },
                label = { Text(Translations.get("amount_daalo", language)) },
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_amount_input"),
                singleLine = true,
                isError = showError
            )

            if (showError) {
                Text(
                    text = Translations.get("enter_amount_error", language),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Quick cash selector buttons to log instantly
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val suggestions = listOf(10.0, 50.0, 100.0, 500.0)
                suggestions.forEach { amt ->
                    val amtLabel = "₹${amt.toInt()}"
                    Button(
                        onClick = {
                            amountText = amt.toInt().toString()
                            showError = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Text(amtLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Category choice grid-style scroll buttons with touch target 48dp
            Text(
                text = Translations.get("category_label", language),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            val categories = listOf("Food", "Travel", "Study", "Recharge", "Other")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { category ->
                    val visualEmoji = when (category) {
                        "Food" -> "🍛"
                        "Travel" -> "🚗"
                        "Study" -> "📚"
                        "Recharge" -> "📱"
                        else -> "🪙"
                    }
                    val isSelected = selectedCategory == category

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .clickable { selectedCategory = category }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(visualEmoji, fontSize = 18.sp)
                            Text(
                                text = Translations.get(category.lowercase(), language),
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Notes / Description
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(Translations.get("notes_daalo", language)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_note_input"),
                singleLine = true
            )

            // Date Pick Button that opens standard calendar dialog
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, selectYear, selectMonth, selectDay ->
                                val selectCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectYear)
                                    set(Calendar.MONTH, selectMonth)
                                    set(Calendar.DAY_OF_MONTH, selectDay)
                                }
                                selectedDateMs = selectCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📅", fontSize = 18.sp)
                    Text(
                        text = Translations.get("date", language),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Save Expense pill button
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountVal <= 0.0) {
                        showError = true
                    } else {
                        onSaveExpense(amountVal, selectedCategory, noteText, selectedDateMs)
                        // reset form UI
                        amountText = ""
                        noteText = ""
                        selectedCategory = "Food"
                        selectedDateMs = System.currentTimeMillis()
                        showError = false
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_expense_btn")
            ) {
                Text(
                    text = Translations.get("save_expense", language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ExpenseItemRow(
    expense: Expense,
    language: AppLanguage,
    onDelete: () -> Unit,
    viewModel: MainViewModel
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Category emoji icon badge
                val visualEmoji = when (expense.category) {
                    "Food" -> "🍛"
                    "Travel" -> "🚗"
                    "Study" -> "📚"
                    "Recharge" -> "📱"
                    else -> "🪙"
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(visualEmoji, fontSize = 22.sp)
                }

                Column {
                    val categoryTranslated = Translations.get(expense.category.lowercase(), language)
                    val noteDisplay = expense.notes.ifEmpty { categoryTranslated }
                    Text(
                        text = noteDisplay,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Relative date representation (Today, Yesterday, or precise date)
                    val formattedTime = remember(expense.timestamp) {
                        val calendar = Calendar.getInstance()
                        val today = calendar.get(Calendar.DAY_OF_YEAR)
                        val year = calendar.get(Calendar.YEAR)

                        val expCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
                        val expDay = expCal.get(Calendar.DAY_OF_YEAR)
                        val expYear = expCal.get(Calendar.YEAR)

                        if (expYear == year) {
                            if (expDay == today) {
                                Translations.get("today", language)
                            } else if (today - expDay == 1) {
                                Translations.get("yesterday", language)
                            } else {
                                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(expense.timestamp))
                            }
                        } else {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.timestamp))
                        }
                    }

                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "₹${"%,.1f".format(expense.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = Translations.get("delete", language),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(44.dp)
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ProfileSetupScreen(
    language: AppLanguage,
    name: String,
    role: String,
    onSaveProfile: (String, String) -> Unit
) {
    var nameInput by remember { mutableStateOf(name) }
    var roleInput by remember { mutableStateOf(if (role.isEmpty()) "personal" else role.lowercase()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = Translations.get("profile", language),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = Translations.get("user_info", language),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Name input
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(Translations.get("name", language)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        singleLine = true
                    )

                    // Role setup choices (general and professional)
                    Text(
                        text = Translations.get("role", language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleSelectionButton(
                                label = Translations.get("personal", language),
                                selected = roleInput == "personal",
                                onClick = { roleInput = "personal" },
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectionButton(
                                label = Translations.get("salaried", language),
                                selected = roleInput == "salaried",
                                onClick = { roleInput = "salaried" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleSelectionButton(
                                label = Translations.get("freelancer", language),
                                selected = roleInput == "freelancer",
                                onClick = { roleInput = "freelancer" },
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectionButton(
                                label = Translations.get("business", language),
                                selected = roleInput == "business",
                                onClick = { roleInput = "business" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RoleSelectionButton(
                                label = Translations.get("student", language),
                                selected = roleInput == "student",
                                onClick = { roleInput = "student" },
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectionButton(
                                label = Translations.get("housewife", language),
                                selected = roleInput == "housewife",
                                onClick = { roleInput = "housewife" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Save trigger pill
                    Button(
                        onClick = { onSaveProfile(nameInput, roleInput) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_btn")
                    ) {
                        Text(Translations.get("save_profile", language), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapitalSetupDialog(
    language: AppLanguage,
    initialCapital: Double,
    initialIsDaily: Boolean,
    initialLimitVal: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean, Double) -> Unit
) {
    var capitalText by remember { mutableStateOf(initialCapital.toInt().toString()) }
    var isDaily by remember { mutableStateOf(initialIsDaily) }
    var limitText by remember { mutableStateOf(initialLimitVal.toInt().toString()) }

    var errorAmt by remember { mutableStateOf(false) }
    var errorLim by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val capVal = capitalText.toDoubleOrNull()
                    val limVal = limitText.toDoubleOrNull()

                    if (capVal == null || capVal < 0.0) {
                        errorAmt = true
                    } else if (limVal == null || limVal < 0.0) {
                        errorLim = true
                    } else {
                        onSave(capVal, isDaily, limVal)
                    }
                },
                modifier = Modifier.testTag("config_dialog_confirm")
            ) {
                Text(if (language == AppLanguage.HINDI) "ठीक है" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("config_dialog_cancel")
            ) {
                Text(if (language == AppLanguage.HINDI) "रद्द करें" else "Cancel")
            }
        },
        title = {
            Text(
                text = Translations.get("monthly_capital", language),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = Translations.get("capital_helper", language),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Starting Monthly Capital
                OutlinedTextField(
                    value = capitalText,
                    onValueChange = {
                        capitalText = it.filter { char -> char.isDigit() }
                        errorAmt = false
                    },
                    label = { Text(Translations.get("set_capital", language)) },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorAmt,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_capital_input")
                )

                // Limit choice Daily vs Monthly
                Text(
                    text = Translations.get("limit_type", language),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chooseDaily = isDaily
                    Button(
                        onClick = { isDaily = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (chooseDaily) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (chooseDaily) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(Translations.get("daily_reset", language), fontSize = 10.sp)
                    }

                    Button(
                        onClick = { isDaily = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!chooseDaily) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!chooseDaily) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(Translations.get("monthly_reset", language), fontSize = 10.sp)
                    }
                }

                // Alert Threshold Limit Amount
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it.filter { char -> char.isDigit() }
                        errorLim = false
                    },
                    label = { Text(Translations.get("set_limit", language)) },
                    prefix = { Text("₹ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorLim,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("config_limit_input")
                )
            }
        }
    )
}
