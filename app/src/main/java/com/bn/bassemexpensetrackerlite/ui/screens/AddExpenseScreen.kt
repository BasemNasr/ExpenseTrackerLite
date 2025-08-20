package com.bn.bassemexpensetrackerlite.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bn.bassemexpensetrackerlite.R
import com.bn.bassemexpensetrackerlite.viewmodel.AddExpenseUiState
import com.bn.bassemexpensetrackerlite.viewmodel.AddExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val state: AddExpenseUiState by viewModel.uiState.collectAsState()
    if (state.saved) {
        onBack()
        return
    }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.add_expense_income)) }) }) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            val expanded = remember { mutableStateOf(false) }
            val typeLabel = if (state.isIncome) "Income" else "Expense"
            ExposedDropdownMenuBox(expanded = expanded.value, onExpandedChange = { expanded.value = !expanded.value }) {
                OutlinedTextField(
                    value = typeLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.type_label)) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) }
                )
                ExposedDropdownMenu(
                    expanded = expanded.value,
                    onDismissRequest = { expanded.value = false }
                ) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.expense_type)) }, onClick = {
                        viewModel.updateIsIncome(false)
                        expanded.value = false
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.income_type)) }, onClick = {
                        viewModel.updateIsIncome(true)
                        expanded.value = false
                    })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = state.category, onValueChange = viewModel::updateCategory, label = { Text(stringResource(R.string.category_label)) }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = state.amount, onValueChange = viewModel::updateAmount, label = { Text(stringResource(R.string.amount_label)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(modifier = Modifier.height(8.dp))
            val currencyExpanded = remember { mutableStateOf(false) }
            val currencies by viewModel.currencies.collectAsState()
            val query = state.currencyCode
            val filtered = currencies.filter { it.contains(query, ignoreCase = true) }.take(20)
            ExposedDropdownMenuBox(expanded = currencyExpanded.value, onExpandedChange = { currencyExpanded.value = !currencyExpanded.value }) {
                OutlinedTextField(
                    value = state.currencyCode,
                    onValueChange = { viewModel.updateCurrency(it.uppercase()) },
                    label = { Text(stringResource(R.string.currency_label)) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded.value) }
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded.value,
                    onDismissRequest = { currencyExpanded.value = false }
                ) {
                    filtered.forEach { code ->
                        DropdownMenuItem(text = { Text(code) }, onClick = {
                            viewModel.updateCurrency(code)
                            currencyExpanded.value = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.save() }, enabled = !state.isSaving) { Text(stringResource(R.string.save_button)) }
            state.errorMessage?.let { Text(it) }
        }
    }
}


