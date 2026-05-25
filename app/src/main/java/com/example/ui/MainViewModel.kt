package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "kharcha-tracker-db"
    ).fallbackToDestructiveMigration().build()

    private val repository = ExpenseRepository(db.expenseDao())
    private val prefs = application.getSharedPreferences("kharcha_prefs", Context.MODE_PRIVATE)

    // UI state flows initialized from SharedPreferences
    private val _language = MutableStateFlow(
        try {
            AppLanguage.valueOf(prefs.getString("key_language", AppLanguage.HINGLISH.name) ?: AppLanguage.HINGLISH.name)
        } catch (e: Exception) {
            AppLanguage.HINGLISH
        }
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _isDarkMode = MutableStateFlow(
        prefs.getBoolean("key_dark_mode", false)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userName = MutableStateFlow(
        prefs.getString("key_user_name", "") ?: ""
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userRole = MutableStateFlow(
        prefs.getString("key_user_role", "personal") ?: "personal"
    )
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _monthlyCapital = MutableStateFlow(
        prefs.getFloat("key_monthly_capital", 5000f).toDouble()
    )
    val monthlyCapital: StateFlow<Double> = _monthlyCapital.asStateFlow()

    private val _limitType = MutableStateFlow(
        prefs.getString("key_limit_type", "DAILY") ?: "DAILY"
    )
    val limitType: StateFlow<String> = _limitType.asStateFlow()

    private val _limitAmount = MutableStateFlow(
        prefs.getFloat("key_limit_amount", 150f).toDouble()
    )
    val limitAmount: StateFlow<Double> = _limitAmount.asStateFlow()

    // Flow of all expenses from database
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateLanguage(newLang: AppLanguage) {
        prefs.edit().putString("key_language", newLang.name).apply()
        _language.value = newLang
    }

    fun updateTheme(isDark: Boolean) {
        prefs.edit().putBoolean("key_dark_mode", isDark).apply()
        _isDarkMode.value = isDark
    }

    fun saveProfile(name: String, role: String) {
        prefs.edit().apply {
            putString("key_user_name", name)
            putString("key_user_role", role)
        }.apply()
        _userName.value = name
        _userRole.value = role
    }

    fun saveCapitalAndLimit(capital: Double, isDaily: Boolean, limitAmount: Double) {
        val typeStr = if (isDaily) "DAILY" else "MONTHLY"
        prefs.edit().apply {
            putFloat("key_monthly_capital", capital.toFloat())
            putString("key_limit_type", typeStr)
            putFloat("key_limit_amount", limitAmount.toFloat())
        }.apply()
        _monthlyCapital.value = capital
        _limitType.value = typeStr
        _limitAmount.value = limitAmount
    }

    fun addExpense(amount: Double, category: String, notes: String, timestamp: Long) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                category = category,
                notes = notes,
                timestamp = timestamp
            )
            repository.insert(expense)
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    // Helper functions for Date tracking
    fun isToday(timestamp: Long): Boolean {
        val calInstance = Calendar.getInstance()
        val todayDay = calInstance.get(Calendar.DAY_OF_YEAR)
        val todayYear = calInstance.get(Calendar.YEAR)

        val expCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return expCal.get(Calendar.DAY_OF_YEAR) == todayDay && expCal.get(Calendar.YEAR) == todayYear
    }

    fun isCurrentMonth(timestamp: Long): Boolean {
        val calInstance = Calendar.getInstance()
        val month = calInstance.get(Calendar.MONTH)
        val year = calInstance.get(Calendar.YEAR)

        val expCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return expCal.get(Calendar.MONTH) == month && expCal.get(Calendar.YEAR) == year
    }

    fun getDaysRemainingInMonth(): Int {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (maxDays - today + 1).coerceAtLeast(1)
    }
}
