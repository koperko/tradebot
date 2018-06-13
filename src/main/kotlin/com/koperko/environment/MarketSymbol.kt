package com.koperko.environment

/**
 * Created by Matus on 31.03.2018.
 */
enum class MarketSymbol(val symbol: String) {
    EURUSD("EURUSD");

    override fun toString(): String {
        return symbol
    }
}