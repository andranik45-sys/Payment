package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao())
    }

    // Reactively observe All Expenses from Room
    val expensesState: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI Configuration States
    var selectedLanguage by mutableStateOf(AppLanguage.ENGLISH)
    var selectedCurrency by mutableStateOf(AppCurrency.USD)
    var currentScreen by mutableStateOf("dashboard") // dashboard, wallet, insights, settings

    // AI Analysis States
    var aiInsight by mutableStateOf("")
    var isAiLoading by mutableStateOf(false)
    var lastAiAnalysisTime by mutableStateOf("")

    // Interactive Flags
    var showAddManualDialog by mutableStateOf(false)
    var showPassGeneratedDialog by mutableStateOf(false)
    var lastGeneratedPassDetails by mutableStateOf<Pair<String, Double>?>(null)
    var toastMessage by mutableStateOf<String?>(null)

    // Currency Conversion helper
    fun convertCurrency(amount: Double, fromCurrencyCode: String, toCurrency: AppCurrency): Double {
        val fromCurrency = AppCurrency.values().find { it.code == fromCurrencyCode } ?: AppCurrency.USD
        val amountInUsd = amount / fromCurrency.usdExchangeRate
        return amountInUsd * toCurrency.usdExchangeRate
    }

    // Get localized translation string
    fun translate(key: String): String {
        return Localization.get(key, selectedLanguage)
    }

    // Auto-generate mock Google Wallet payments (simulating Visa, Mastercard, AMEX transactions)
    fun syncGoogleWalletCards() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            val mockWalletExpenses = listOf(
                Expense(
                    merchant = "Starbucks",
                    amount = 7.80,
                    currency = "USD",
                    category = "cat_food",
                    timestamp = now - (0.1 * dayMs).toLong(),
                    walletSource = "card_visa",
                    notes = "Match Caffè Latte & Caramel Macchiato"
                ),
                Expense(
                    merchant = "Shell Stations",
                    amount = 54.00,
                    currency = "EUR",
                    category = "cat_transport",
                    timestamp = now - (1.2 * dayMs).toLong(),
                    walletSource = "card_master",
                    notes = "Gasoline fill-up in Germany"
                ),
                Expense(
                    merchant = "Amazon.com Retail",
                    amount = 135.50,
                    currency = "USD",
                    category = "cat_shopping",
                    timestamp = now - (2.5 * dayMs).toLong(),
                    walletSource = "card_amex",
                    notes = "Noise cancelling headphones & micro cables"
                ),
                Expense(
                    merchant = "Armenia Yerevan Tavern",
                    amount = 14500.00,
                    currency = "AMD",
                    category = "cat_food",
                    timestamp = now - (3.8 * dayMs).toLong(),
                    walletSource = "card_visa",
                    notes = "Traditional dinner"
                ),
                Expense(
                    merchant = "London Underground TFL",
                    amount = 8.40,
                    currency = "GBP",
                    category = "cat_transport",
                    timestamp = now - (4.9 * dayMs).toLong(),
                    walletSource = "card_visa",
                    notes = "Contactless tube ticket"
                ),
                Expense(
                    merchant = "Netflix Streaming",
                    amount = 12.99,
                    currency = "EUR",
                    category = "cat_entertainment",
                    timestamp = now - (6.1 * dayMs).toLong(),
                    walletSource = "card_master",
                    notes = "Monthly premium ultra subscription"
                ),
                Expense(
                    merchant = "Yerevan Mall Shopping",
                    amount = 28000.00,
                    currency = "AMD",
                    category = "cat_shopping",
                    timestamp = now - (8.5 * dayMs).toLong(),
                    walletSource = "card_visa",
                    notes = "Leather winter boots"
                ),
                Expense(
                    merchant = "Electric Gas Utility",
                    amount = 112.30,
                    currency = "USD",
                    category = "cat_utilities",
                    timestamp = now - (12.0 * dayMs).toLong(),
                    walletSource = "card_master",
                    notes = "Heating and power bills"
                ),
                Expense(
                    merchant = "Tokyo Akihabara Games",
                    amount = 6800.00,
                    currency = "JPY",
                    category = "cat_entertainment",
                    timestamp = now - (15.2 * dayMs).toLong(),
                    walletSource = "card_visa",
                    notes = "Retro handheld indie simulator console"
                ),
                Expense(
                    merchant = "Local Bakery",
                    amount = 12.20,
                    currency = "USD",
                    category = "cat_food",
                    timestamp = now - (18.6 * dayMs).toLong(),
                    walletSource = "card_amex",
                    notes = "Artisanal bakery and croissants"
                )
            )

            withContext(Dispatchers.IO) {
                repository.insertAll(mockWalletExpenses)
            }
            toastMessage = String.format(translate("feedback_synced"), mockWalletExpenses.size)
        }
    }

    // Add manual entry
    fun addExpense(
        merchant: String,
        amount: Double,
        currencyCode: String,
        category: String,
        source: String,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = Expense(
                merchant = merchant,
                amount = amount,
                currency = currencyCode,
                category = category,
                timestamp = System.currentTimeMillis(),
                walletSource = source,
                notes = notes
            )
            repository.insert(expense)
        }
        toastMessage = "Added transaction successfully!"
    }

    // Simulate pushing a new Google Wallet digital pass receipt (transit ticket or store card)
    fun simulateWalletPassAdd(merchant: String, amount: Double, currencyCode: String, category: String) {
        viewModelScope.launch {
            val expense = Expense(
                merchant = merchant,
                amount = amount,
                currency = currencyCode,
                category = category,
                timestamp = System.currentTimeMillis(),
                walletSource = "card_loyalty",
                notes = "Pushed simulated digital receipt pass metadata to Google Wallet."
            )
            withContext(Dispatchers.IO) {
                repository.insert(expense)
            }
            lastGeneratedPassDetails = Pair(merchant, amount)
            showPassGeneratedDialog = true
        }
    }

    // Delete single expense
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(expense)
        }
    }

    // Erase all data
    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
        aiInsight = ""
        toastMessage = "All transaction histories successfully erased."
    }

    // Generate generative AI Insights via Gemini 3.5 Flash
    fun generateAiInsights() {
        val currentExpenses = expensesState.value
        if (currentExpenses.isEmpty()) {
            aiInsight = translate("empty_state_text")
            return
        }

        viewModelScope.launch {
            isAiLoading = true
            aiInsight = ""

            // Build a structured, lightweight summary catalog to send to Gemini
            val summaryList = currentExpenses.take(25).map {
                mapOf(
                    "merchant" to it.merchant,
                    "originalAmount" to "${it.amount} ${it.currency}",
                    "category" to translate(it.category),
                    "notes" to it.notes,
                    "date" to SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it.timestamp))
                )
            }

            val expensesJson = summaryList.toString()
            val languageName = selectedLanguage.englishName
            val currencySymbol = selectedCurrency.symbol

            val result = withContext(Dispatchers.IO) {
                GeminiClient.getSpendingInsights(expensesJson, languageName, currencySymbol)
            }

            aiInsight = result
            isAiLoading = false
            lastAiAnalysisTime = SimpleDateFormat("HH:mm:ss (yyyy-MM-dd)", Locale.getDefault()).format(Date())
        }
    }
}
