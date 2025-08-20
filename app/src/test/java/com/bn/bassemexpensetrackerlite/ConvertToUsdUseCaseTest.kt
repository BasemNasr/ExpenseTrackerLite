package com.bn.bassemexpensetrackerlite

import com.bn.bassemexpensetrackerlite.data.local.ExpenseDao
import com.bn.bassemexpensetrackerlite.data.remote.ExchangeRateApi
import com.bn.bassemexpensetrackerlite.data.repository.ExpenseRepositoryImpl
import com.bn.bassemexpensetrackerlite.domain.usecase.ConvertToUsdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConvertToUsdUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `converts using rates map`() = runTest {
        val fakeDao = object : ExpenseDao {
            override suspend fun insertExpense(expense: com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity): Long = 1
            override suspend fun deleteExpense(expenseId: Long) {}
            override fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int) = flow { emit(emptyList<com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity>()) }
            override fun totalIncomeUsd(start: Long?, end: Long?) = flow { emit(0.0) }
            override fun totalExpenseUsd(start: Long?, end: Long?) = flow { emit(0.0) }
        }
        val fakeApi = object : ExchangeRateApi {
            override suspend fun getLatestRates(): com.bn.bassemexpensetrackerlite.data.remote.RatesResponse = com.bn.bassemexpensetrackerlite.data.remote.RatesResponse("success", "USD", mapOf("EUR" to 0.5))
        }
        val repo = ExpenseRepositoryImpl(fakeDao, fakeApi)
        val useCase = ConvertToUsdUseCase(repo)
        
        val usd = useCase(amount = 10.0, currencyCode = "EUR", rates = mapOf("EUR" to 0.5))
        
        assertEquals(20.0, usd, 0.001)
    }

    @Test
    fun `returns original amount when currency is USD`() = runTest {
        val fakeDao = object : ExpenseDao {
            override suspend fun insertExpense(expense: com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity): Long = 1
            override suspend fun deleteExpense(expenseId: Long) {}
            override fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int) = flow { emit(emptyList<com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity>()) }
            override fun totalIncomeUsd(start: Long?, end: Long?) = flow { emit(0.0) }
            override fun totalExpenseUsd(start: Long?, end: Long?) = flow { emit(0.0) }
        }
        val fakeApi = object : ExchangeRateApi {
            override suspend fun getLatestRates(): com.bn.bassemexpensetrackerlite.data.remote.RatesResponse = com.bn.bassemexpensetrackerlite.data.remote.RatesResponse("success", "USD", mapOf("USD" to 1.0))
        }
        val repo = ExpenseRepositoryImpl(fakeDao, fakeApi)
        val useCase = ConvertToUsdUseCase(repo)
        
        val usd = useCase(amount = 15.0, currencyCode = "USD", rates = mapOf("USD" to 1.0))
        
        assertEquals(15.0, usd, 0.001)
    }
}


