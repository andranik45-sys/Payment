package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,
    val amount: Double,
    val currency: String, // e.g. "USD", "EUR", "AMD", "GBP"
    val category: String, // e.g. "Food", "Transport", "Shopping", "Entertainment", "Utilities", "Other"
    val timestamp: Long,
    val walletSource: String, // e.g. "Visa **** 4321", "Mastercard **** 8822"
    val notes: String = ""
)
