package com.bn.bassemexpensetrackerlite.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bn.bassemexpensetrackerlite.R
import com.bn.bassemexpensetrackerlite.domain.model.Expense
import com.bn.bassemexpensetrackerlite.utils.FileUtils
import com.bn.bassemexpensetrackerlite.viewmodel.DashboardFilter
import com.bn.bassemexpensetrackerlite.viewmodel.DashboardUiState
import com.bn.bassemexpensetrackerlite.viewmodel.DashboardViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    onAddClicked: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state: DashboardUiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val savedStateHandle = navBackStackEntry?.savedStateHandle
    val expenseAddedFlow = savedStateHandle?.getStateFlow("expense_added", 0L)
    val expenseAddedTs by expenseAddedFlow?.collectAsState(initial = 0L)
        ?: run { val v = remember { mutableLongStateOf(0L) }; v }

    LaunchedEffect(expenseAddedTs) {
        if (expenseAddedTs != 0L) {
            viewModel.loadPage(reset = true)
            savedStateHandle?.remove<Long>("expense_added")
        }
    }

    // Handle export success
    LaunchedEffect(state.exportData) {
        state.exportData?.let { exportData ->
            when (exportData.type) {
                "CSV" -> {
                    exportData.csvContent?.let { csvContent ->
                        FileUtils.createCsvFile(context, csvContent)?.let { uri ->
                            FileUtils.shareFile(context, uri, "text/csv", "Share Expenses CSV")
                        }
                    }
                }

                "PDF" -> {
                    exportData.pdfContent?.let { pdfContent ->
                        FileUtils.createPdfFile(context, pdfContent)?.let { uri ->
                            FileUtils.shareFile(
                                context,
                                uri,
                                "application/pdf",
                                "Share Expense Report PDF"
                            )
                        }
                    }
                }
            }
            viewModel.clearExportData()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClicked) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_button_description)
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Header()
            Spacer(modifier = Modifier.height(16.dp))
            SummaryCard(totalIncome = state.totalIncomeUsd, totalExpenses = state.totalExpenseUsd)
            Spacer(modifier = Modifier.height(16.dp))
            FilterRow(selected = state.filter, onSelected = viewModel::setFilter)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.isLoading && state.expenses.isEmpty()) {
                AnimatedLoadingIndicator()
            }
            if (state.expenses.isEmpty() && !state.isLoading) {
                Text(stringResource(R.string.no_expenses_yet), color = Color.Gray)
            } else {
                ExpenseList(
                    expenses = state.expenses,
                    onDeleteExpense = { expenseId -> viewModel.deleteExpense(expenseId) },
                    modifier = Modifier.weight(1f)
                )
                if (state.canLoadMore) {
                    TextButton(onClick = { viewModel.loadMore() }) { Text(stringResource(R.string.load_more)) }
                }

                // Export buttons
                if (state.expenses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportToCsv() },
                            enabled = !state.isExporting
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.export_csv))
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportToPdf() },
                            enabled = !state.isExporting
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.export_pdf))
                        }
                    }
                }
            }
            state.errorMessage?.let { Text(it, color = Color.Red) }
        }
    }
}

@Composable
private fun Header() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.track_spending_smartly),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.height(48.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun SummaryCard(totalIncome: Double, totalExpenses: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.total_balance),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            val balance = totalIncome - totalExpenses
            Text(
                "$" + String.format(Locale.US, "%.2f", balance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        R.string.income_label,
                        String.format(Locale.US, "%.2f", totalIncome)
                    )
                )
                Text(
                    stringResource(
                        R.string.expenses_label,
                        String.format(Locale.US, "%.2f", totalExpenses)
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterRow(selected: DashboardFilter, onSelected: (DashboardFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            label = stringResource(R.string.filter_all),
            selected = selected == DashboardFilter.ALL
        ) { onSelected(DashboardFilter.ALL) }
        FilterChip(
            label = stringResource(R.string.filter_this_month),
            selected = selected == DashboardFilter.THIS_MONTH
        ) { onSelected(DashboardFilter.THIS_MONTH) }
        FilterChip(
            label = stringResource(R.string.filter_last_7_days),
            selected = selected == DashboardFilter.LAST_7_DAYS
        ) { onSelected(DashboardFilter.LAST_7_DAYS) }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun ExpenseList(
    expenses: List<Expense>,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(
            items = expenses,
            key = { it.id }
        ) { item ->
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.category, fontWeight = FontWeight.SemiBold)
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (item.isIncome) "+ ${item.currencyCode} ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            item.amountOriginal
                                        )
                                    }" else "- ${item.currencyCode} ${
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            item.amountOriginal
                                        )
                                    }"
                                )
                                Text(
                                    stringResource(
                                        R.string.usd_amount,
                                        String.format(Locale.US, "%.2f", item.amountUsd)
                                    )
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteExpense(item.id) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete expense",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer(
                    alpha = alpha,
                    scaleX = scale,
                    scaleY = scale
                )
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading expenses...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun DashboardPreview() {
    SummaryCard(1200.0, 800.0)
}


