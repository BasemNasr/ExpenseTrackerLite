package com.bn.bassemexpensetrackerlite

import com.bn.bassemexpensetrackerlite.data.local.ExpenseDao
import com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity
import com.bn.bassemexpensetrackerlite.data.remote.ExchangeRateApi
import com.bn.bassemexpensetrackerlite.data.remote.RatesResponse
import com.bn.bassemexpensetrackerlite.data.repository.ExpenseRepositoryImpl
import com.bn.bassemexpensetrackerlite.domain.mappers.toDomain
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseRepositoryTest {

    private lateinit var repository: ExpenseRepositoryImpl
    private lateinit var mockExpenseDao: ExpenseDao
    private lateinit var mockExchangeRateApi: ExchangeRateApi
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockExpenseDao = mock()
        mockExchangeRateApi = mock()
        repository = ExpenseRepositoryImpl(
            expenseDao = mockExpenseDao,
            exchangeRateApi = mockExchangeRateApi,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `insertExpense converts domain model to entity and calls dao`() = runTest {
        // Arrange
        val expense = createTestExpense()
        val expectedEntity = ExpenseEntity(
            id = expense.id,
            category = expense.category,
            amountOriginal = expense.amountOriginal,
            currencyCode = expense.currencyCode,
            amountUsd = expense.amountUsd,
            isIncome = expense.isIncome,
            dateEpochMillis = expense.dateEpochMillis,
            receiptUri = expense.receiptUri
        )
        whenever(mockExpenseDao.insertExpense(expectedEntity)).thenReturn(1L)

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertEquals(1L, result)
        verify(mockExpenseDao).insertExpense(expectedEntity)
    }

    @Test
    fun `getExpensesPaged returns mapped domain models`() = runTest {
        // Arrange
        val entities = listOf(
            createTestExpenseEntity(1L, "Food", 10.0, "USD", 10.0, false),
            createTestExpenseEntity(2L, "Transport", 20.0, "EUR", 22.0, true)
        )
        val expectedExpenses = entities.map { it.toDomain() }
        whenever(mockExpenseDao.getExpensesPaged(100L, 200L, 10, 0))
            .thenReturn(flowOf(entities))

        // Act
        val result = repository.getExpensesPaged(100L, 200L, 10, 0)

        // Assert
        val actualExpenses = result.first()
        assertEquals(expectedExpenses.size, actualExpenses.size)
        assertEquals(expectedExpenses[0].id, actualExpenses[0].id)
        assertEquals(expectedExpenses[0].category, actualExpenses[0].category)
        assertEquals(expectedExpenses[1].id, actualExpenses[1].id)
        assertEquals(expectedExpenses[1].category, actualExpenses[1].category)
    }

    @Test
    fun `getExpensesPaged with null parameters calls dao correctly`() = runTest {
        // Arrange
        val entities = listOf(createTestExpenseEntity(1L, "Food", 10.0, "USD", 10.0, false))
        whenever(mockExpenseDao.getExpensesPaged(null, null, 10, 0))
            .thenReturn(flowOf(entities))

        // Act
        val result = repository.getExpensesPaged(null, null, 10, 0)

        // Assert
        val actualExpenses = result.first()
        assertEquals(1, actualExpenses.size)
        verify(mockExpenseDao).getExpensesPaged(null, null, 10, 0)
    }

    @Test
    fun `totalIncomeUsd returns dao result`() = runTest {
        // Arrange
        val expectedTotal = 150.0
        whenever(mockExpenseDao.totalIncomeUsd(100L, 200L))
            .thenReturn(flowOf(expectedTotal))

        // Act
        val result = repository.totalIncomeUsd(100L, 200L)

        // Assert
        val actualTotal = result.first()
        assertEquals(expectedTotal, actualTotal)
        verify(mockExpenseDao).totalIncomeUsd(100L, 200L)
    }

    @Test
    fun `totalIncomeUsd with null parameters calls dao correctly`() = runTest {
        // Arrange
        val expectedTotal = 300.0
        whenever(mockExpenseDao.totalIncomeUsd(null, null))
            .thenReturn(flowOf(expectedTotal))

        // Act
        val result = repository.totalIncomeUsd(null, null)

        // Assert
        val actualTotal = result.first()
        assertEquals(expectedTotal, actualTotal)
        verify(mockExpenseDao).totalIncomeUsd(null, null)
    }

    @Test
    fun `totalExpenseUsd returns dao result`() = runTest {
        // Arrange
        val expectedTotal = 250.0
        whenever(mockExpenseDao.totalExpenseUsd(100L, 200L))
            .thenReturn(flowOf(expectedTotal))

        // Act
        val result = repository.totalExpenseUsd(100L, 200L)

        // Assert
        val actualTotal = result.first()
        assertEquals(expectedTotal, actualTotal)
        verify(mockExpenseDao).totalExpenseUsd(100L, 200L)
    }

    @Test
    fun `totalExpenseUsd with null parameters calls dao correctly`() = runTest {
        // Arrange
        val expectedTotal = 500.0
        whenever(mockExpenseDao.totalExpenseUsd(null, null))
            .thenReturn(flowOf(expectedTotal))

        // Act
        val result = repository.totalExpenseUsd(null, null)

        // Assert
        val actualTotal = result.first()
        assertEquals(expectedTotal, actualTotal)
        verify(mockExpenseDao).totalExpenseUsd(null, null)
    }

    @Test
    fun `fetchRates returns api response rates`() = runTest {
        // Arrange
        val expectedRates = mapOf("USD" to 1.0, "EUR" to 1.1, "GBP" to 1.3)
        val apiResponse = RatesResponse(result = "success", baseCode = "USD", rates = expectedRates)
        whenever(mockExchangeRateApi.getLatestRates()).thenReturn(apiResponse)

        // Act
        val result = repository.fetchRates()

        // Assert
        assertEquals(expectedRates, result)
        verify(mockExchangeRateApi).getLatestRates()
    }

    @Test
    fun `fetchRates returns empty map when api response rates is null`() = runTest {
        // Arrange
        val apiResponse = RatesResponse(result = "success", baseCode = "USD", rates = null)
        whenever(mockExchangeRateApi.getLatestRates()).thenReturn(apiResponse)

        // Act
        val result = repository.fetchRates()

        // Assert
        assertEquals(emptyMap<String, Double>(), result)
        verify(mockExchangeRateApi).getLatestRates()
    }

    @Test
    fun `fetchRates handles api exception gracefully`() = runTest {
        // Arrange
        val exception = RuntimeException("Network error")
        whenever(mockExchangeRateApi.getLatestRates()).thenThrow(exception)

        // Act & Assert
        try {
            repository.fetchRates()
            assert(false) { "Expected exception to be thrown" }
        } catch (e: RuntimeException) {
            assertEquals("Network error", e.message)
        }
        verify(mockExchangeRateApi).getLatestRates()
    }

    @Test
    fun `deleteExpense calls dao with correct id`() = runTest {
        // Arrange
        val expenseId = 123L

        // Act
        repository.deleteExpense(expenseId)

        // Assert
        verify(mockExpenseDao).deleteExpense(expenseId)
    }

    @Test
    fun `insertExpense with zero id creates entity with auto-generated id`() = runTest {
        // Arrange
        val expense = createTestExpense(id = 0L)
        val expectedEntity = ExpenseEntity(
            id = 0L, // Should be 0 for auto-generation
            category = expense.category,
            amountOriginal = expense.amountOriginal,
            currencyCode = expense.currencyCode,
            amountUsd = expense.amountUsd,
            isIncome = expense.isIncome,
            dateEpochMillis = expense.dateEpochMillis,
            receiptUri = expense.receiptUri
        )
        whenever(mockExpenseDao.insertExpense(expectedEntity)).thenReturn(5L)

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertEquals(5L, result)
        verify(mockExpenseDao).insertExpense(expectedEntity)
    }

    @Test
    fun `getExpensesPaged returns empty list when dao returns empty`() = runTest {
        // Arrange
        whenever(mockExpenseDao.getExpensesPaged(100L, 200L, 10, 0))
            .thenReturn(flowOf(emptyList()))

        // Act
        val result = repository.getExpensesPaged(100L, 200L, 10, 0)

        // Assert
        val actualExpenses = result.first()
        assertEquals(0, actualExpenses.size)
        verify(mockExpenseDao).getExpensesPaged(100L, 200L, 10, 0)
    }

    @Test
    fun `totalIncomeUsd returns null when dao returns null`() = runTest {
        // Arrange
        whenever(mockExpenseDao.totalIncomeUsd(100L, 200L))
            .thenReturn(flowOf(null))

        // Act
        val result = repository.totalIncomeUsd(100L, 200L)

        // Assert
        val actualTotal = result.first()
        assertEquals(null, actualTotal)
        verify(mockExpenseDao).totalIncomeUsd(100L, 200L)
    }

    @Test
    fun `totalExpenseUsd returns null when dao returns null`() = runTest {
        // Arrange
        whenever(mockExpenseDao.totalExpenseUsd(100L, 200L))
            .thenReturn(flowOf(null))

        // Act
        val result = repository.totalExpenseUsd(100L, 200L)

        // Assert
        val actualTotal = result.first()
        assertEquals(null, actualTotal)
        verify(mockExpenseDao).totalExpenseUsd(100L, 200L)
    }

    // Helper functions
    private fun createTestExpense(
        id: Long = 1L,
        category: String = "Food",
        amountOriginal: Double = 10.0,
        currencyCode: String = "USD",
        amountUsd: Double = 10.0,
        isIncome: Boolean = false,
        dateEpochMillis: Long = System.currentTimeMillis(),
        receiptUri: String? = null
    ): Expense = Expense(
        id = id,
        category = category,
        amountOriginal = amountOriginal,
        currencyCode = currencyCode,
        amountUsd = amountUsd,
        isIncome = isIncome,
        dateEpochMillis = dateEpochMillis,
        receiptUri = receiptUri
    )

    private fun createTestExpenseEntity(
        id: Long = 1L,
        category: String = "Food",
        amountOriginal: Double = 10.0,
        currencyCode: String = "USD",
        amountUsd: Double = 10.0,
        isIncome: Boolean = false,
        dateEpochMillis: Long = System.currentTimeMillis(),
        receiptUri: String? = null
    ): ExpenseEntity = ExpenseEntity(
        id = id,
        category = category,
        amountOriginal = amountOriginal,
        currencyCode = currencyCode,
        amountUsd = amountUsd,
        isIncome = isIncome,
        dateEpochMillis = dateEpochMillis,
        receiptUri = receiptUri
    )
}
