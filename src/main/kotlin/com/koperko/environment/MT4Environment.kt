package com.koperko.environment

import com.jfx.*
import com.jfx.strategy.OrderInfo
import com.koperko.extensions.roundTo

/**
 * Created by Matus on 31.03.2018.
 */

class MT4Environment(override val symbol: MarketSymbol, private val connection: MT4) : TradingEnvironment {

    override fun getBalance(): Double {
        return connection.accountBalance()
    }

    override fun openPosition(position: Position, price: Double, stopLossPercentage: Double, takeProfitPercentage: Double): Long {
        return try {
            val realPrice = getOpenPrice(position)
            connection.orderSend(symbol.toString(), position.getTradeOperation(), calculateVolume(), realPrice, 0,
                    calculateStopLoss(position, realPrice, stopLossPercentage),
                    calculateTakeProfit(position, realPrice, takeProfitPercentage),
                    "", 0, null)
        } catch (e: IllegalArgumentException) {
            println(e.message)
            -1
        }
    }

    override fun closePosition(price: Double): Double {
        connection.orderCloseAll() //todo: close only specific order and handle error states
        return getBalance()
    }

    override fun getOpenPosition(): Position {
        val orderInfo: OrderInfo? = connection.orderGet(0, SelectionType.SELECT_BY_POS, SelectionPool.MODE_TRADES)
        return when (orderInfo?.type) {
            TradeOperation.OP_BUY -> Position.BUY
            TradeOperation.OP_SELL -> Position.SELL
            else -> Position.NONE
        }
    }

    private fun calculateVolume(): Double {
        return (connection.accountBalance() * connection.accountLeverage()) / 100000
    }


    private fun calculateTakeProfit(position: Position, price: Double, takeProfitPercentage: Double): Double {
        return when (position) {
            Position.BUY -> price + (price * takeProfitPercentage).roundTo(4)
            Position.SELL -> price - (price * takeProfitPercentage).roundTo(4)
            else -> throw IllegalArgumentException("Could not calculate take profit price to desired position \"$this.name\"")
        }
    }

    private fun calculateStopLoss(position: Position, price: Double, stopLossPercentage: Double): Double {
        return when (position) {
            Position.BUY -> price - (price * stopLossPercentage).roundTo(4)
            Position.SELL -> price + (price * stopLossPercentage).roundTo(4)
            else -> throw IllegalArgumentException("Could not calculate stop loss price to desired position \"$this.name\"")
        }
    }

    private fun getOpenPrice(position: Position): Double {
        return when (position) {
            Position.BUY -> connection.marketInfo(symbol.toString(), MarketInfo.MODE_BID)
            Position.SELL -> connection.marketInfo(symbol.toString(), MarketInfo.MODE_ASK)
            else -> throw IllegalArgumentException("Could not get price to desired position \"$this.name\"")
        }
    }

    private fun Position.getTradeOperation(): TradeOperation {
        return when (this) {
            Position.BUY -> TradeOperation.OP_BUY
            Position.SELL -> TradeOperation.OP_SELL
            else -> throw IllegalArgumentException("Could not map any trade operation to desired position \"$this.name\"")
        }
    }

}