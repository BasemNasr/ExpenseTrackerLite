package com.bn.bassemexpensetrackerlite.data.repository

import com.bn.bassemexpensetrackerlite.data.local.ExpenseDao
import com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity
import com.bn.bassemexpensetrackerlite.data.remote.ExchangeRateApi
import com.bn.bassemexpensetrackerlite.domain.mappers.toDomain
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val exchangeRateApi: ExchangeRateApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRepository {

    override suspend fun insertExpense(expense: Expense): Long = withContext(ioDispatcher) {
        val entity = ExpenseEntity(
            id = expense.id,
            category = expense.category,
            amountOriginal = expense.amountOriginal,
            currencyCode = expense.currencyCode,
            amountUsd = expense.amountUsd,
            isIncome = expense.isIncome,
            dateEpochMillis = expense.dateEpochMillis,
            receiptUri = expense.receiptUri
        )
        expenseDao.insertExpense(entity)
    }

    override suspend fun deleteExpense(expenseId: Long) = withContext(ioDispatcher) {
        expenseDao.deleteExpense(expenseId)
    }

    override fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int): Flow<List<Expense>> {
        return expenseDao.getExpensesPaged(start, end, limit, offset).map { list -> list.map { it.toDomain() } }
    }

    override fun totalIncomeUsd(start: Long?, end: Long?): Flow<Double?> = expenseDao.totalIncomeUsd(start, end)

    override fun totalExpenseUsd(start: Long?, end: Long?): Flow<Double?> = expenseDao.totalExpenseUsd(start, end)

    override suspend fun fetchRates(): Map<String, Double> = withContext(ioDispatcher) {
        val response = exchangeRateApi.getLatestRates()
        response.rates ?: emptyMap()
    }
}


