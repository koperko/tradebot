package com.koperko

/**
 * Created by Matus on 20.03.2018.
 */
interface Trader : PriceChangeListener {

    fun startTrading(market: Market)
    fun stopTrading()
    fun getCurrentBalance() : Double

}