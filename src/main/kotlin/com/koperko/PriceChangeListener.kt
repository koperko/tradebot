package com.koperko

import java.util.*

/**
 * Created by Matus on 20.03.2018.
 */
interface PriceChangeListener {

    fun onPriceChange(priceChange: PriceChangeEvent)
    fun onMarketClose()

}

data class PriceChangeEvent(val timestamp: Date, val bid: Double, val ask: Double)