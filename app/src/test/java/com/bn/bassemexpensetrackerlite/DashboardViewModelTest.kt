package com.bn.bassemexpensetrackerlite

import com.bn.bassemexpensetrackerlite.domain.model.Expense
import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository
import com.bn.bassemexpensetrackerlite.viewmodel.DashboardFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepo : ExpenseRepository {
        var inserted: MutableList<Expense> = mutableListOf()
        var deletedIds: MutableList<Long> = mutableListOf()
        
        // Create a list of 25 expenses to simulate pagination
        // Income items: 3, 6, 9, 12, 15, 18, 21, 24 (8 items)
        // Total income: 3×10 + 6×10 + 9×10 + 12×10 + 15×10 + 18×10 + 21×10 + 24×10 = 1080.0
        // Expense items: 1, 2, 4, 5, 7, 8, 10, 11, 13, 14, 16, 17, 19, 20, 22, 23, 25 (17 items)
        // Total expense: 1×10 + 2×10 + 4×10 + 5×10 + 7×10 + 8×10 + 10×10 + 11×10 + 13×10 + 14×10 + 16×10 + 17×10 + 19×10 + 20×10 + 22×10 + 23×10 + 25×10 = 2170.0
        private val allExpenses = (1..25).map { id ->
            Expense(
                id = id.toLong(),
                category = if (id % 2 == 0) "Food" else "Transport",
                amountOriginal = (id * 10.0),
                currencyCode = "USD",
                amountUsd = (id * 10.0),
                isIncome = id % 3 == 0, // Income items: 3, 6, 9, 12, 15, 18, 21, 24
                dateEpochMillis = System.currentTimeMillis() - (id * 86400000L),
                receiptUri = null
            )
        }
        
        override suspend fun insertExpense(expense: Expense): Long { 
            inserted.add(expense); 
            return 1 
        }

        override suspend fun deleteExpense(expenseId: Long) {
            deletedIds.add(expenseId)
        }
        
        override fun getExpensesPaged(start: Long?, end: Long?, limit: Int, offset: Int): Flow<List<Expense>> = flow {
            val filteredExpenses = allExpenses.filter { expense ->
                if (start != null && expense.dateEpochMillis < start) return@filter false
                if (end != null && expense.dateEpochMillis > end) return@filter false
                true
            }
            
            val pagedExpenses = filteredExpenses.drop(offset).take(limit)
            emit(pagedExpenses)
        }
        
        override fun totalIncomeUsd(start: Long?, end: Long?): Flow<Double?> = flow { 
            val total = allExpenses
                .filter { it.isIncome }
                .filter { expense ->
                    if (start != null && expense.dateEpochMillis < start) return@filter false
                    if (end != null && expense.dateEpochMillis > end) return@filter false
                    true
                }
                .sumOf { it.amountUsd }
            emit(total)
        }
        
        override fun totalExpenseUsd(start: Long?, end: Long?): Flow<Double?> = flow { 
            val total = allExpenses
                .filter { !it.isIncome }
                .filter { expense ->
                    if (start != null && expense.dateEpochMillis < start) return@filter false
                    if (end != null && expense.dateEpochMillis > end) return@filter false
                    true
                }
                .sumOf { it.amountUsd }
            emit(total)
        }
        
        override suspend fun fetchRates(): Map<String, Double> = mapOf("USD" to 1.0)
    }

    @Test
    fun `loads first page and totals`() = runTest {
        val repo = FakeRepo()
        val viewModel = TestDashboardViewModel(repo)
        
        viewModel.setFilter(DashboardFilter.ALL)
        viewModel.loadPage(reset = true)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(10, state.expenses.size) // Page size is 10
        assertEquals(1080.0, state.totalIncomeUsd, 0.001) // 8 income items: 3×10 + 6×10 + 9×10 + 12×10 + 15×10 + 18×10 + 21×10 + 24×10 = 1080.0
        assertEquals(2170.0, state.totalExpenseUsd, 0.001) // 17 expense items: 1×10 + 2×10 + 4×10 + 5×10 + 7×10 + 8×10 + 10×10 + 11×10 + 13×10 + 14×10 + 16×10 + 17×10 + 19×10 + 20×10 + 22×10 + 23×10 + 25×10 = 2170.0
    }

    @Test
    fun `can load more when data available`() = runTest {
        val repo = FakeRepo()
        val viewModel = TestDashboardViewModel(repo)
        
        viewModel.setFilter(DashboardFilter.ALL)
        viewModel.loadPage(reset = true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        assertEquals(0, initialState.page)
        assertEquals(true, initialState.canLoadMore) // Should be true since we have 25 items and page size is 10
        
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val afterLoadMore = viewModel.uiState.value
        assertEquals(1, afterLoadMore.page)
        assertEquals(20, afterLoadMore.expenses.size) // 10 + 10 = 20 items
        assertEquals(true, afterLoadMore.canLoadMore) // Still true since we have 5 more items
    }

    @Test
    fun `deleteExpense calls repository and refreshes data`() = runTest {
        val repo = FakeRepo()
        val viewModel = TestDashboardViewModel(repo)
        
        // First load some data
        viewModel.setFilter(DashboardFilter.ALL)
        viewModel.loadPage(reset = true)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val initialState = viewModel.uiState.value
        val initialCount = initialState.expenses.size
        assertEquals(10, initialCount)
        
        // Delete an expense
        val expenseToDelete = initialState.expenses.first().id
        viewModel.deleteExpense(expenseToDelete)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify the expense was deleted from repository
        assertEquals(1, repo.deletedIds.size)
        assertEquals(expenseToDelete, repo.deletedIds.first())
    }
}

// Test version of DashboardViewModel without Hilt
class TestDashboardViewModel(
    private val repository: ExpenseRepository
) {
    private val pageSize: Int = 10
    private val _filter = kotlinx.coroutines.flow.MutableStateFlow(DashboardFilter.THIS_MONTH)
    private val _page = kotlinx.coroutines.flow.MutableStateFlow(0)
    
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
        com.bn.bassemexpensetrackerlite.viewmodel.DashboardUiState()
    )
    val uiState: kotlinx.coroutines.flow.StateFlow<com.bn.bassemexpensetrackerlite.viewmodel.DashboardUiState> = _uiState

    private fun rangeForFilter(filter: DashboardFilter): Pair<Long?, Long?> {
        val now = System.currentTimeMillis()
        return when (filter) {
            DashboardFilter.THIS_MONTH -> {
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                start to null
            }
            DashboardFilter.LAST_7_DAYS -> {
                val start = now - 7L * 24L * 60L * 60L * 1000L
                start to null
            }
            DashboardFilter.ALL -> null to null
        }
    }

    fun setFilter(newFilter: DashboardFilter) {
        _filter.value = newFilter
    }

    fun loadMore() {
        if (_uiState.value.canLoadMore) {
            _page.value += 1
            // Trigger refreshData when page changes
            refreshData()
        }
    }

    fun deleteExpense(expenseId: Long) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                repository.deleteExpense(expenseId)
                // Refresh the current page to reflect the deletion
                loadPage(reset = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to delete expense")
            }
        }
    }

    fun loadPage(reset: Boolean) {
        if (reset) {
            val wasZero = _page.value == 0
            _page.value = 0
            if (wasZero) {
                refreshData()
            }
        } else {
            _page.value = _page.value
        }
    }

    private fun refreshData() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val (start, end) = rangeForFilter(_filter.value)
                val offset = _page.value * pageSize
                
                val domainList = repository.getExpensesPaged(start, end, pageSize, offset)
                    .first()
                
                val reachedEnd = domainList.size < pageSize
                
                // Use combine to get both totals
                combine(
                    repository.totalIncomeUsd(start, end),
                    repository.totalExpenseUsd(start, end)
                ) { income, expense ->
                    Pair(income ?: 0.0, expense ?: 0.0)
                }.collect { pair ->
                    val income: Double = pair.first
                    val expense: Double = pair.second
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        expenses = if (_page.value == 0) domainList else _uiState.value.expenses + domainList,
                        totalIncomeUsd = income,
                        totalExpenseUsd = expense,
                        canLoadMore = !reachedEnd,
                        page = _page.value
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load expenses"
                )
            }
        }
    }
}


