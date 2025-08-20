package com.bn.bassemexpensetrackerlite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val amountOriginal: Double,
    val currencyCode: String,
    val amountUsd: Double,
    val isIncome: Boolean,
    val dateEpochMillis: Long,
    val receiptUri: String?
)


