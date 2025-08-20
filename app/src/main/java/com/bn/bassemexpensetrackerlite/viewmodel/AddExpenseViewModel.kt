package com.bn.bassemexpensetrackerlite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository
import com.bn.bassemexpensetrackerlite.domain.usecase.ConvertToUsdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExpenseUiState(
    val category: String = "General",
    val amount: String = "",
    val currencyCode: String = "USD",
    val isIncome: Boolean = false,
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val receiptUri: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val convertToUsd: ConvertToUsdUseCase
) : ViewModel() {

    private val _uiState: MutableStateFlow<AddExpenseUiState> = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _currencies: MutableStateFlow<List<String>> = MutableStateFlow(listOf("USD", "EUR", "GBP", "AED", "SAR", "EGP"))
    val currencies: StateFlow<List<String>> = _currencies.asStateFlow()

    init {
        // load currencies from API (keys from rates)
        viewModelScope.launch {
            try {
                val keys = repository.fetchRates().keys
                if (keys.isNotEmpty()) {
                    val sorted = keys.toMutableList().apply { remove("USD") }.sorted()
                    _currencies.value = listOf("USD") + sorted
                }
            } catch (_: Throwable) {
                // keep defaults
            }
        }
    }

    fun updateCategory(category: String) { _uiState.value = _uiState.value.copy(category = category) }
    fun updateAmount(amount: String) { _uiState.value = _uiState.value.copy(amount = amount) }
    fun updateCurrency(currency: String) { _uiState.value = _uiState.value.copy(currencyCode = currency) }
    fun updateIsIncome(isIncome: Boolean) { _uiState.value = _uiState.value.copy(isIncome = isIncome) }
    fun updateDate(epochMillis: Long) { _uiState.value = _uiState.value.copy(dateEpochMillis = epochMillis) }
    fun updateReceipt(uri: String?) { _uiState.value = _uiState.value.copy(receiptUri = uri) }

    fun save() {
        val amountDouble = _uiState.value.amount.toDoubleOrNull()
        if (amountDouble == null || amountDouble <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid amount")
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
                val usd = convertToUsd(amountDouble, _uiState.value.currencyCode)
                val model = Expense(
                    id = 0L,
                    category = _uiState.value.category,
                    amountOriginal = amountDouble,
                    currencyCode = _uiState.value.currencyCode,
                    amountUsd = usd,
                    isIncome = _uiState.value.isIncome,
                    dateEpochMillis = _uiState.value.dateEpochMillis,
                    receiptUri = _uiState.value.receiptUri
                )
                repository.insertExpense(model)
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = t.message ?: "Failed to save expense")
            }
        }
    }
}


