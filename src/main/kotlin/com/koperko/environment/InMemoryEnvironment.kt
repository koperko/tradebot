package com.koperko.environment

/**
 * Created by Matus on 31.03.2018.
 */

class InMemoryEnvironment(override val symbol: MarketSymbol, private var balance: Double) : TradingEnvironment {

    private var position = Position.NONE
    private var openPrice = 0.0

    override fun getBalance(): Double {
        return balance
    }

    override fun openPosition(position: Position, price: Double, stopLossPercentage: Double, takeProfitPercentage: Double): Long {
        openPrice = price
        this.position = position
        return 1 // dummy ticker order number
    }

    override fun closePosition(price: Double): Double {
        val oldBalance = balance
        val coefficient = when (position) {
            Position.BUY -> price / openPrice
            Position.SELL -> openPrice / price
            else -> throw RuntimeException("Trying to update balance when no position is open at the moment")
        }
        balance *= coefficient /* (if (coefficient > 0) 0.999 else 1.001)*/
        position = Position.NONE
        return balance
    }

    override fun getOpenPosition(): Position = position

}