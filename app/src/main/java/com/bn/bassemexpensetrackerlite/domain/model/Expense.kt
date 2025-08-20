package com.bn.bassemexpensetrackerlite.domain.model

data class Expense(
    val id: Long,
    val category: String,
    val amountOriginal: Double,
    val currencyCode: String,
    val amountUsd: Double,
    val isIncome: Boolean,
    val dateEpochMillis: Long,
    val receiptUri: String?
)


