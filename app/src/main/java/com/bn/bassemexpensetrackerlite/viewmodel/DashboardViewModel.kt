package com.bn.bassemexpensetrackerlite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bn.bassemexpensetrackerlite.data.export.ExportService
import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DashboardFilter { THIS_MONTH, LAST_7_DAYS, ALL }

data class DashboardUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val totalIncomeUsd: Double = 0.0,
    val totalExpenseUsd: Double = 0.0,
    val page: Int = 0,
    val canLoadMore: Boolean = true,
    val filter: DashboardFilter = DashboardFilter.ALL,
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportData: ExportData? = null
)

data class ExportData(
    val type: String, // "CSV" or "PDF"
    val csvContent: String? = null,
    val pdfContent: ByteArray? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val exportService: ExportService
) : ViewModel() {

    private val pageSize: Int = 10

    private val _filter: MutableStateFlow<DashboardFilter> = MutableStateFlow(DashboardFilter.ALL)
    val filter: StateFlow<DashboardFilter> = _filter.asStateFlow()

    private val _page: MutableStateFlow<Int> = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

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

    private val _uiState: MutableStateFlow<DashboardUiState> = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            filter.collect { newFilter ->
                _uiState.value = _uiState.value.copy(filter = newFilter)
                loadPage(reset = true)
            }
        }
        viewModelScope.launch {
            page.collect { _ ->
                refreshData()
            }
        }
    }

    fun setFilter(newFilter: DashboardFilter) {
        _filter.value = newFilter
    }

    fun loadMore() {
        if (_uiState.value.canLoadMore) {
            _page.value += 1
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                // Refresh the current page to reflect the deletion
                loadPage(reset = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to delete expense")
            }
        }
    }

    fun exportToCsv() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null, exportData = null)
                
                val allExpenses = mutableListOf<Expense>()
                var currentPage = 0
                var hasMore = true
                
                while (hasMore) {
                    val (start, end) = rangeForFilter(_filter.value)
                    val expenses = repository.getExpensesPaged(start, end, pageSize, currentPage * pageSize).first()
                    allExpenses.addAll(expenses)
                    hasMore = expenses.size == pageSize
                    currentPage++
                }
                
                val csvContent = exportService.exportToCsv(allExpenses)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportData = ExportData(type = "CSV", csvContent = csvContent)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Failed to export CSV: ${e.message}"
                )
            }
        }
    }

    fun exportToPdf() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null, exportData = null)
                
                val allExpenses = mutableListOf<Expense>()
                var currentPage = 0
                var hasMore = true
                
                while (hasMore) {
                    val (start, end) = rangeForFilter(_filter.value)
                    val expenses = repository.getExpensesPaged(start, end, pageSize, currentPage * pageSize).first()
                    allExpenses.addAll(expenses)
                    hasMore = expenses.size == pageSize
                    currentPage++
                }
                
                val pdfContent = exportService.exportToPdf(allExpenses)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportData = ExportData(type = "PDF", pdfContent = pdfContent)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Failed to export PDF: ${e.message}"
                )
            }
        }
    }

    fun clearExportData() {
        _uiState.value = _uiState.value.copy(exportData = null)
    }

    fun loadPage(reset: Boolean) {
        if (reset) {
            val wasZero = _page.value == 0
            _page.value = 0
            if (wasZero) {
                // Force a refresh when already on first page
                refreshData()
            }
        } else {
            _page.value = _page.value
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val (start, end) = rangeForFilter(_filter.value)
                repository.getExpensesPaged(start, end, pageSize, _page.value * pageSize).collect { domainList ->
                    val reachedEnd = domainList.size < pageSize
                    repository.totalIncomeUsd(start, end)
                        .combine(repository.totalExpenseUsd(start, end)) { income, expense ->
                            Pair(income ?: 0.0, expense ?: 0.0)
                        }
                        .stateIn(viewModelScope, SharingStarted.Eagerly, Pair(0.0, 0.0))
                        .collect { pair ->
                            val income: Double = pair.first
                            val expense: Double = pair.second
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                expenses = if (_page.value == 0) domainList else _uiState.value.expenses + domainList,
                                totalIncomeUsd = income,
                                totalExpenseUsd = expense,
                                canLoadMore = !reachedEnd,
                                filter = _filter.value
                            )
                        }
                }
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = t.message)
            }
        }
    }
}


