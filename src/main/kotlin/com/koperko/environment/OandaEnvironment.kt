package com.koperko.environment

import com.oanda.v20.ContextBuilder
import com.oanda.v20.account.AccountID
import com.oanda.v20.order.MarketOrderRequest
import com.oanda.v20.order.OrderCreateRequest
import com.oanda.v20.primitives.AcceptDatetimeFormat
import com.oanda.v20.primitives.DecimalNumber
import com.oanda.v20.primitives.InstrumentName
import com.oanda.v20.trade.TradeCloseRequest
import com.oanda.v20.trade.TradeSpecifier
import com.oanda.v20.transaction.TransactionID

class OandaEnvironment(override val symbol: MarketSymbol, private val config: Config) : TradingEnvironment {

    private var position = Position.NONE


    private var tradeId: TransactionID? = null
    private val context = ContextBuilder(config.url)
            .setToken(config.token)
            .setDatetimeFormat(AcceptDatetimeFormat.UNIX)
            .setApplication("TradeApp")
            .build()

    override fun getBalance(): Double {
        return context.account.get(config.accountID).account.balance.doubleValue()
    }

    override fun openPosition(position: Position, price: Double, stopLossPercentage: Double, takeProfitPercentage: Double): Long {
        val orderCreateRequest = OrderCreateRequest(config.accountID)
        val orderRequest = MarketOrderRequest()

        orderRequest.instrument = config.instrument
        orderRequest.units = when (position) {
            Position.BUY -> DecimalNumber(10000.0)
            Position.SELL -> DecimalNumber(-10000.0)
            else -> throw IllegalArgumentException("Unknown position was requested to open: ${position.name}")
        }

        this.position = position
        //todo: set stop loss and take profit values

        orderCreateRequest.setOrder(orderRequest)
        tradeId = context.order.create(orderCreateRequest).orderFillTransaction.id
        return -1
    }

    override fun closePosition(price: Double): Double {
        context.trade.close(TradeCloseRequest(config.accountID, TradeSpecifier(tradeId)))

        position = Position.NONE
        return getBalance()
    }

    override fun getOpenPosition(): Position = position

    class Config(val url: String, val token: String, val accountID: AccountID, val instrument: InstrumentName)

    companion object {
        val DEMO_ACCOUNT = Config("https://api-fxpractice.oanda.com",
                "2feee581fc6c50ef8ed0bceb26d91f43-068636d4379d0724be03345b3be7f427",
                AccountID("101-004-8614846-001"),
                InstrumentName("EUR_USD"))
    }
}