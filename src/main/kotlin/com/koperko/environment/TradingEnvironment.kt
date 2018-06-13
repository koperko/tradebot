package com.koperko.environment

/**
 * Created by Matus on 31.03.2018.
 */
interface TradingEnvironment {

    val symbol: MarketSymbol

    fun getBalance() : Double

    /**
     * Opens a position
     * @return ticket number or -1 if opening fails
     */
    fun openPosition(position: Position, price: Double, stopLossPercentage: Double, takeProfitPercentage: Double) : Long

    /**
     * Closes the current open order.
     * @return new balance, or a number < 0 if closing fails
     */
    fun closePosition(price: Double) : Double

    fun getOpenPosition() : Position

}