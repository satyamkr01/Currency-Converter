package com.unifydream.currencyconverter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    /**
     * Inserts a list of CurrencyEntity into the database.
     * If there's a conflict, the new data replaces the old data.
     *
     * @param data The list of CurrencyEntity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<CurrencyEntity>)

    /**
     * Fetches all currency rates from the database.
     *
     * @return List of CurrencyEntity.
     */
    @Query("SELECT * FROM currencyentity")
    suspend fun getAllCurrencyData(): List<CurrencyEntity>

    /**
     * Fetches the most recent timestamp of currency rates from the database.
     *
     * @return latest timestamp of CurrencyEntity.
     */
    @Query("SELECT MAX(timestamp) FROM currencyentity")
    suspend fun getLastFetchTimestamp(): Long?
}
