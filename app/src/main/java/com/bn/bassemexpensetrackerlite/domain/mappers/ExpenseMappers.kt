package com.bn.bassemexpensetrackerlite.domain.mappers

import com.bn.bassemexpensetrackerlite.data.local.ExpenseEntity
import com.bn.bassemexpensetrackerlite.domain.model.Expense

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    category = category,
    amountOriginal = amountOriginal,
    currencyCode = currencyCode,
    amountUsd = amountUsd,
    isIncome = isIncome,
    dateEpochMillis = dateEpochMillis,
    receiptUri = receiptUri
)


