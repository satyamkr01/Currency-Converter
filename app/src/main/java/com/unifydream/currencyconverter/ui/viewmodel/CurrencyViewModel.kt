package com.unifydream.currencyconverter.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifydream.currencyconverter.data.db.CurrencyEntity
import com.unifydream.currencyconverter.data.repository.CurrencyRepository
import com.unifydream.currencyconverter.utils.Constants.APP_ID
import com.unifydream.currencyconverter.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val repository: CurrencyRepository) : ViewModel() {

    private val _exchangeRates = MutableLiveData<List<CurrencyEntity>>()
    val exchangeRates: LiveData<List<CurrencyEntity>> get() = _exchangeRates

    private val _conversionResults = MutableLiveData<List<String>>()
    val conversionResults: LiveData<List<String>> get() = _conversionResults

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    init {
        refreshDataIfNeeded()
    }

    /**
     * Fetches exchange rates from the repository and updates the LiveData.
     * If the fetch is successful, exchange rates are saved; otherwise, an error is set.
     */
    fun refreshDataIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.getExchangeData(APP_ID)) {
                is Resource.Loading -> _loading.postValue(true)
                is Resource.Success -> {
                    _loading.postValue(false)
                    result.data?.let { rates ->
                        _exchangeRates.postValue(rates)
                    }
                }
                is Resource.Error -> {
                    _loading.postValue(false)
                    _errorMessage.postValue(result.message ?: "Something went wrong!")
                    _exchangeRates.postValue(result.data ?: emptyList())
                }
            }
        }
    }

    /**
     * Calculates conversion results based on the given amount and selected base currency.
     * The results are displayed as a list of strings representing each converted value.
     *
     * @param amount The amount to convert.
     * @param baseCurrency The base currency code selected by the user.
     */
    fun calculateConversion(amount: Double, baseCurrency: String) {
        val baseRate = _exchangeRates.value?.find { it.code == baseCurrency }?.rate
        if (baseRate != null) {
            val conversions = _exchangeRates.value?.map { rate ->
                "${rate.code}: ${String.format(Locale.US, "%.2f", 
                    (amount / baseRate * rate.rate))}"
            } ?: emptyList()
            _conversionResults.postValue(conversions)
        } else {
            _errorMessage.postValue("Base currency rate not available.")
        }
    }
}
