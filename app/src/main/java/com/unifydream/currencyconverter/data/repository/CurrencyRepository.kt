package com.unifydream.currencyconverter.data.repository

import com.unifydream.currencyconverter.data.api.OpenExchangeRatesApi
import com.unifydream.currencyconverter.data.db.CurrencyDao
import com.unifydream.currencyconverter.data.db.CurrencyEntity
import com.unifydream.currencyconverter.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CurrencyRepository @Inject constructor(
    private val openExchangeRatesApi: OpenExchangeRatesApi,
    private val currencyDao: CurrencyDao) {
    /**
     * Fetches exchange rates from the API or the local database.
     * If API call succeeds, it stores the data in the database.
     *
     * @return Resource containing the list of CurrencyEntity or error message.
     */
    suspend fun getExchangeData(appId: String): Resource<List<CurrencyEntity>> {
        val lastFetchTimestamp = currencyDao.getLastFetchTimestamp() ?: DEFAULT_TIMESTAMP
        val currentTime = System.currentTimeMillis()

        return if (currentTime - lastFetchTimestamp > TimeUnit.MINUTES.toMillis(REFRESH_INTERVAL)) {
            try {
                val response = openExchangeRatesApi.getLatestExchangeData(appId)
                if (response.isSuccessful && response.body() != null) {
                    response.body()?.let { apiResponse ->
                        val rates = apiResponse.rates.map { (code, rate) ->
                            CurrencyEntity(code, rate, currentTime)
                        }
                        insertData(rates)
                        Resource.Success(rates)
                    } ?: Resource.Error("Empty response from API.")
                } else {
                    // If API call fails, fetch from local database
                    Resource.Error("API call failed: ${response.message()}", fetchFromLocal())
                }
            } catch (e: Exception) {
                Resource.Error("Network error occurred!", fetchFromLocal())
            }
        } else {
            // If data is still fresh, return the local database data
            Resource.Success(fetchFromLocal())
        }
    }

    /**
     * Inserts the fetched currency rates into the local database.
     *
     * @param data List of CurrencyEntity to be inserted.
     */
    private suspend fun insertData(data: List<CurrencyEntity>) {
        withContext(Dispatchers.IO) {
            currencyDao.insertAll(data)
        }
    }

    /**
     * Fetches currency rates from the local database.
     *
     * @return List of CurrencyEntity from the local database.
     */
    private suspend fun fetchFromLocal(): List<CurrencyEntity> {
        return withContext(Dispatchers.IO) {
            currencyDao.getAllCurrencyData()
        }
    }

    companion object {
        private const val DEFAULT_TIMESTAMP = 0L
        private const val REFRESH_INTERVAL = 30L
    }
}
