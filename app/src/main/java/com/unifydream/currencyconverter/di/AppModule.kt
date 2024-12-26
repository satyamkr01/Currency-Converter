package com.unifydream.currencyconverter.di

import android.content.Context
import androidx.room.Room
import com.unifydream.currencyconverter.data.db.AppDatabase
import com.unifydream.currencyconverter.data.api.OpenExchangeRatesApi
import com.unifydream.currencyconverter.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): OpenExchangeRatesApi = retrofit
        .create(OpenExchangeRatesApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(context,
        AppDatabase::class.java, "currency_db")
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideDao(db: AppDatabase) = db.currencyDao()
}