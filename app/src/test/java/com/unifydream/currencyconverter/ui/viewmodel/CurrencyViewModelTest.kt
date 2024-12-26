package com.unifydream.currencyconverter.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.unifydream.currencyconverter.data.db.CurrencyEntity
import com.unifydream.currencyconverter.data.repository.CurrencyRepository
import com.unifydream.currencyconverter.utils.Constants.APP_ID
import com.unifydream.currencyconverter.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CurrencyViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: CurrencyRepository

    private lateinit var viewModel: CurrencyViewModel
    private val currentTimestamp = System.currentTimeMillis()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CurrencyViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_refreshDataIfNeeded_updateExchangeRates_onSuccess() = runTest {
        val currencyEntities = listOf(
            CurrencyEntity("USD", 1.0, currentTimestamp),
            CurrencyEntity("EUR", 0.85, currentTimestamp)
        )
        whenever(mockRepository.getExchangeData(APP_ID)).thenReturn(Resource.Success(currencyEntities))

        viewModel.refreshDataIfNeeded()
        advanceUntilIdle()

        verify(mockRepository, atLeastOnce()).getExchangeData(APP_ID)
        assertEquals(currencyEntities, viewModel.exchangeRates.value)
    }

    @Test
    fun test_refreshDataIfNeeded_updateErrorMessage_onError() = runTest {
        val errorMessage = "Error occurred"
        whenever(mockRepository.getExchangeData(APP_ID)).thenReturn(Resource.Error(errorMessage))

        viewModel.refreshDataIfNeeded()
        mockRepository.getExchangeData(APP_ID)
        advanceUntilIdle()

        verify(mockRepository, atLeastOnce()).getExchangeData(APP_ID)
        assertEquals(errorMessage, viewModel.errorMessage.value)
    }

    @Test
    fun test_calculateConversion_withBaseCurrencyRateAvailable() = runTest {
        val currencyEntities = listOf(
            CurrencyEntity("USD", 1.0, currentTimestamp),
            CurrencyEntity("EUR", 0.85, currentTimestamp)
        )
        whenever(mockRepository.getExchangeData(APP_ID)).thenReturn(Resource.Success(currencyEntities))

        viewModel.refreshDataIfNeeded()
        mockRepository.getExchangeData(APP_ID)
        advanceUntilIdle()
        viewModel.calculateConversion(100.0, "USD")

        val expectedResults = listOf("USD: 100.00", "EUR: 85.00")

        verify(mockRepository, atLeastOnce()).getExchangeData(APP_ID)
        assertEquals(expectedResults, viewModel.conversionResults.value)
    }

    @Test
    fun test_calculateConversion_updateErrorMessage_whenBaseCurrencyRateUnavailable() = runTest {
        val currencyEntities = listOf(
            CurrencyEntity("USD", 1.0, currentTimestamp),
            CurrencyEntity("EUR", 0.85, currentTimestamp)
        )
        whenever(mockRepository.getExchangeData(APP_ID)).thenReturn(Resource.Success(currencyEntities))

        viewModel.refreshDataIfNeeded()
        advanceUntilIdle()
        viewModel.calculateConversion(100.0, "GBP")

        assertEquals("Base currency rate not available.", viewModel.errorMessage.value)
    }
}