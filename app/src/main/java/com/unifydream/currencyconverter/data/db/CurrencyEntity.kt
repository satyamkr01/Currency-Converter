package com.unifydream.currencyconverter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val rate: Double,
    val timestamp: Long
)