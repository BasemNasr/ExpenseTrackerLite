package com.bn.bassemexpensetrackerlite.domain.repository

import com.bn.bassemexpensetrackerlite.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun insertExpense(expense: Expense): Long
    suspend fun deleteExpense(expenseId: Long)
    fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int): Flow<List<Expense>>
    fun totalIncomeUsd(start: Long?, end: Long?): Flow<Double?>
    fun totalExpenseUsd(start: Long?, end: Long?): Flow<Double?>
    suspend fun fetchRates(): Map<String, Double>
}


