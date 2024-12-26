import com.unifydream.currencyconverter.data.api.OpenExchangeRatesApi
import com.unifydream.currencyconverter.data.db.CurrencyDao
import com.unifydream.currencyconverter.data.db.CurrencyEntity
import com.unifydream.currencyconverter.data.model.ExchangeRatesResponse
import com.unifydream.currencyconverter.data.repository.CurrencyRepository
import com.unifydream.currencyconverter.utils.Constants.APP_ID
import com.unifydream.currencyconverter.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyList
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import retrofit2.Response

@ExperimentalCoroutinesApi
class CurrencyRepositoryTest {
    @Mock
    private lateinit var mockApi: OpenExchangeRatesApi

    @Mock
    private lateinit var mockCurrencyDao: CurrencyDao

    private lateinit var repository: CurrencyRepository
    private val currentTimestamp = System.currentTimeMillis()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        repository = CurrencyRepository(mockApi, mockCurrencyDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_fetchExchangeApiData_saveToDB_whenApiCallSucceeds() = runTest {
        val mockResponse = ExchangeRatesResponse(mapOf("USD" to 1.0, "JPY" to 152.95))
        val expectedCurrencyEntities = listOf(
            CurrencyEntity("USD", 1.0, currentTimestamp),
            CurrencyEntity("JPY", 152.95, currentTimestamp)
        )

        whenever(mockApi.getLatestExchangeData(APP_ID))
            .thenReturn(Response.success(mockResponse))
        whenever(mockCurrencyDao.getLastFetchTimestamp()).thenReturn(1699678945678L)
        whenever(mockCurrencyDao.getAllCurrencyData()).thenReturn(expectedCurrencyEntities)

        val result = repository.getExchangeData(APP_ID)

        assert(result is Resource.Success && result.data!!.isNotEmpty())
        assertEquals(expectedCurrencyEntities[0].code, result.data!![0].code)
        assertEquals(expectedCurrencyEntities[0].rate, result.data!![0].rate, 0.0)
        verify(mockCurrencyDao).insertAll(anyList())
    }

    @Test
    fun test_fetchExchangeData_returnErrorAndFetchLocalData_whenApiCallFails() = runTest {
        val localData = listOf(
            CurrencyEntity("INR", 1.0, currentTimestamp),
            CurrencyEntity("JPY", 1.82, currentTimestamp)
        )

        whenever(mockApi.getLatestExchangeData(APP_ID)).thenReturn(
            Response.error(500, ResponseBody.create(null, "")))
        whenever(mockCurrencyDao.getAllCurrencyData()).thenReturn(localData)

        val result = repository.getExchangeData(APP_ID)

        assertTrue(result is Resource.Error)
        assert(result is Resource.Error && result.message == "API call failed: Response.error()")
        assertEquals(localData, result.data)
    }

    @Test
    fun test_fetchExchangeData_returnLocalData_whenFreshDataAvailable() = runTest {
        val localData = listOf(
            CurrencyEntity("IND", 1.0, currentTimestamp),
            CurrencyEntity("AUD", 0.018, currentTimestamp)
        )

        whenever(mockCurrencyDao.getLastFetchTimestamp()).thenReturn(currentTimestamp)
        whenever(mockCurrencyDao.getAllCurrencyData()).thenReturn(localData)

        val result = repository.getExchangeData(APP_ID)

        assert(result is Resource.Success && result.data == localData)
        verify(mockApi, never()).getLatestExchangeData(any())
    }

    @Test
    fun test_fetchExchangeData_returnSuccessWithEmptyData() = runTest {
        whenever(mockCurrencyDao.getLastFetchTimestamp()).thenReturn(currentTimestamp)
        whenever(mockCurrencyDao.getAllCurrencyData()).thenReturn(emptyList())

        val result = repository.getExchangeData(APP_ID)

        assertTrue(result is Resource.Success)
        assertTrue(result.data!!.isEmpty())
    }
}