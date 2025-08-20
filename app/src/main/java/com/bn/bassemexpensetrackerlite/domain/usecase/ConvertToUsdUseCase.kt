package com.bn.bassemexpensetrackerlite.domain.usecase

import com.bn.bassemexpensetrackerlite.domain.repository.ExpenseRepository

class ConvertToUsdUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(amount: Double, currencyCode: String, rates: Map<String, Double>? = null): Double {
        if (currencyCode.equals("USD", ignoreCase = true)) return amount
        val localRates = rates ?: repository.fetchRates()
        val rate = localRates[currencyCode.uppercase()] ?: throw IllegalStateException("Rate not found for ${'$'}currencyCode")
        return amount / rate
    }
}


