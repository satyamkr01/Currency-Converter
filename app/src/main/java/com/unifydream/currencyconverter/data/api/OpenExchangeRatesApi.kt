package com.unifydream.currencyconverter.data.api

import com.unifydream.currencyconverter.data.model.ExchangeRatesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenExchangeRatesApi {
    /**
     * Fetches the latest exchange rates data from Open Exchange Rates API.
     * Requires an app ID for authentication.
     *
     * @param appId Your Open Exchange Rates app ID.
     * @return Response containing the exchange rates data.
     */
    @GET("latest.json")
    suspend fun getLatestExchangeData(@Query("app_id") appId: String):
            Response<ExchangeRatesResponse>
}