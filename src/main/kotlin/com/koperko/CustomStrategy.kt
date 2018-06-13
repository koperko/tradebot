package com.koperko

import com.jfx.Broker
import com.jfx.MarketInfo
import com.jfx.strategy.Strategy
import com.koperko.environment.MT4Environment
import com.koperko.environment.MarketSymbol
import java.util.*


/**
 * Created by Matus on 27.03.2018.
 */

class CustomStrategy() : Strategy() {


    private val parameters = TradingParameters(0.0054511954340837115, 0.05, 0.04299748953891125, 0.17878435004074766, 27.725265442748604)

    private val trader = TraderImpl(
            Arrays.asList(BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss)),
            parameters,
            MT4Environment(MarketSymbol.EURUSD, this))

    override fun init(symbol: String?, period: Int) {
        super.init(symbol, period)
        isReconnect = true
//        withDedicatedInstrumentOrdersWorker("EURUSD")

//        addTickListener("EURUSD", trader)

    }

    override fun coordinate() {
        super.coordinate()
        val ask = marketInfo("EURUSD", MarketInfo.MODE_ASK)
        val bid = marketInfo("EURUSD", MarketInfo.MODE_BID)
        val time = marketInfo("EURUSD", MarketInfo.MODE_TIME)

        println("Ask: $ask, Bid: $bid, Date: ${Date(time.toLong())}, Balance: ${trader.getCurrentBalance()}")

        trader.onPriceChange(PriceChangeEvent(Date(time.toLong()), ask, bid))

    }
}