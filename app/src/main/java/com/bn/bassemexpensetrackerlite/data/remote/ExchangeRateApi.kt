package com.bn.bassemexpensetrackerlite.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET

interface ExchangeRateApi {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): RatesResponse
}

data class RatesResponse(
    @Json(name = "result") val result: String?,
    @Json(name = "base_code") val baseCode: String?,
    @Json(name = "rates") val rates: Map<String, Double>?
)


