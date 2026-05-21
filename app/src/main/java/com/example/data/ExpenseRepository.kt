package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun insertAll(expenses: List<Expense>) {
        expenseDao.insertExpenses(expenses)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun deleteById(id: Int) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun clearAll() {
        expenseDao.clearAllExpenses()
    }
}
