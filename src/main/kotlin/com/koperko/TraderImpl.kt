package com.koperko

import com.jfx.MT4
import com.jfx.TickInfo
import com.jfx.strategy.Strategy
import com.koperko.environment.Position
import com.koperko.environment.TradingEnvironment
import com.koperko.extensions.averageWith
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.chart.plot.ValueMarker
import org.jfree.data.xy.DefaultHighLowDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.Color
import java.io.File
import java.util.ArrayList
import java.util.Arrays
import java.util.Date


/**
 * Created by Matus on 20.03.2018.
 */

class TraderImpl(override var indicators: List<Indicator>, override var parameters: TradingParameters,
                 private val tradingEnvironment: TradingEnvironment) : Trader, Strategy.TickListener  {

    companion object {
        const val CANDLE_PERIOD_MS = 24 * 60 * 60 * 1000L
        const val WEEK_IN_MS = 7 * 24 * 3600 * 1000L
        const val MONTH_IN_MS = 30 * 24 * 3600 * 1000L
    }

    private var marketEventsSubject = PublishSubject.create<MarketEvent>()

    private val marketSubscriptions = CompositeDisposable()

//    private val indicators = ArrayList<Indicator>(Arrays.asList(
//            BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss)
//    ))

//    private var position = Position.NONE
//    private var openPrice = 0.0
//    private var balance = 10000.0

    private val dataset = XYSeriesCollection()
    private val testSeries = XYSeries("test")
    private val chart = ChartFactory.createXYLineChart("Trade visualization", "Minutes", "Price", dataset)
    private val plot = chart.xyPlot

    private val dates = ArrayList<Date>()
    private val open = ArrayList<Double>()
    private val high = ArrayList<Double>()
    private val low = ArrayList<Double>()
    private val close = ArrayList<Double>()

    var currentOpen = 0.0
    var currentHigh = 0.0
    var currentLow = Double.MAX_VALUE
    var firstOpenPrice = 0.0

    var lastCloseTimestamp = 0L

    var lastWeekTimestamp = 0L
    var lastMonthTimestamp = 0L


    override fun onTick(tick: TickInfo, metatrader: MT4?) {
        onPriceChange(PriceChangeEvent(tick.time, tick.bid, tick.ask))
    }

    override fun onPriceChange(priceChange: PriceChangeEvent) {
        val (created, bidPrice, askPrice) = priceChange
        val createdDouble = created.time.toDouble()
//        updateOHLCData(priceChange)

        indicators.forEach { it.updatePrice(bidPrice) }

        when (tradingEnvironment.getOpenPosition()) {
            Position.NONE -> {
                val shouldOpen = indicators.map { it.shouldOpen().getTrust(1.0) }.average()
                val openMarker = ValueMarker(createdDouble)
                openMarker.paint = Color.BLUE
                when {
                    shouldOpen > 0.5 -> {
//                        position = Position.BUY
//                        openPrice = price
                        tradingEnvironment.openPosition(Position.BUY, bidPrice, parameters.stopLoss, parameters.takeProfit)
                        indicators.forEach { it.notifyOpenTrade(tradingEnvironment.getOpenPosition()) }
                        plot.addDomainMarker(openMarker)
                    }
                    shouldOpen < -0.5 -> {
//                        position = Position.SELL
//                        openPrice = price
                        tradingEnvironment.openPosition(Position.SELL, askPrice, parameters.stopLoss, parameters.takeProfit)
                        indicators.forEach { it.notifyOpenTrade(tradingEnvironment.getOpenPosition()) }
                        plot.addDomainMarker(openMarker)
                    }
                }
            }
            Position.BUY, Position.SELL -> {
                val shouldClose = indicators.map { if (it.shouldClose()) 1 else 0 }.average()
                if (shouldClose > 0.5) {
                    val oldBalance = tradingEnvironment.getBalance()
                    val newBalance = tradingEnvironment.closePosition(
                            when (tradingEnvironment.getOpenPosition()) {
                                Position.BUY -> askPrice
                                Position.SELL -> bidPrice
                                else -> throw IllegalStateException("Trading environment does not have any open position or there is an unknown state")
                            }
                    )
                    marketEventsSubject.onNext(MarketEvent.BalanceChange(oldBalance, newBalance))
//                    val coefficient = updateBalance(price)
//                    position = Position.NONE
//                    val marker = ValueMarker(createdDouble)
//                    marker.paint = if (coefficient >= 1) Color.GREEN else Color.RED
//                    plot.addDomainMarker(marker)
                    indicators.forEach { it.reset() }
                }
            }
        }
        testSeries.add(createdDouble, askPrice.averageWith(bidPrice))

        if (priceChange.timestamp.time - lastWeekTimestamp > WEEK_IN_MS) {
            marketEventsSubject.onNext(MarketEvent.NewWeek)
            lastWeekTimestamp = priceChange.timestamp.time
        }
        if (priceChange.timestamp.time - lastMonthTimestamp > MONTH_IN_MS) {
            marketEventsSubject.onNext(MarketEvent.NewMonth)
            lastMonthTimestamp = priceChange.timestamp.time
        }
    }


    override fun startTrading(market: Market) : Observable<MarketEvent> {
//        System.out.println("Testing started with balance $balance")
//        System.out.println("${Date()}")

        marketSubscriptions.add(market.subscribe(this))
        return marketEventsSubject
    }

    override fun stopTrading() {
        marketSubscriptions.clear()
        resetBalanceChangeSubject()
//        System.out.println("${Date()}")
//        val candleDataset = DefaultHighLowDataset("trades",
//                dates.toArray(Array(dates.size, { Date() })),
//                high.toDoubleArray(),
//                low.toDoubleArray(),
//                open.toDoubleArray(),
//                close.toDoubleArray(),
//                DoubleArray(dates.size))
//        val candleChart = ChartFactory.createCandlestickChart("Candlestick", "Time", "Price", candleDataset, true)
//        candleChart.xyPlot.rangeAxis.setRange(0.0, 6000.0)
//        plot.rangeAxis.setRange(2600.0, 6000.0)
//        val outChartFile = File("trades.jpg")
//        val width = 800
//        val height = 400
//        ChartUtilities.saveChartAsJPEG(outChartFile, candleChart, width, height)

//        System.out.println("Testing finished with balance $balance")
    }

    override fun getCurrentBalance() : Double {
        return tradingEnvironment.getBalance()
    }

    override fun onMarketClose() {
        stopTrading()
    }

    private fun resetBalanceChangeSubject() {
        marketEventsSubject.onComplete()
        marketEventsSubject = PublishSubject.create()
    }

//    private fun updateBalance(closingPrice: Double) : Double {
//        val oldBalance = balance
//        val coefficient = when (position) {
//            Position.BUY -> closingPrice / openPrice
//            Position.SELL -> openPrice / closingPrice
//            else -> throw RuntimeException("Trying to update balance when no position is open at the moment")
//        } * 0.97
//
//        balance *= coefficient
//        marketEventsSubject.onNext(MarketEvent.BalanceChange(oldBalance, balance))
//        val tradeProfit = (coefficient * 100) - 100
////        System.out.println("New balance: ${balance.toInt()}\t, closed a trade with  \t \t ${if(tradeProfit>0) "   +" else ""}${"%.3f".format(tradeProfit)}")
//        return coefficient
//    }


    private fun updateOHLCData(priceChange: PriceChangeEvent) {
        if (firstOpenPrice == 0.0) firstOpenPrice = priceChange.bid
        if (lastCloseTimestamp == 0L) {
            lastCloseTimestamp = priceChange.timestamp.time
            currentOpen = priceChange.bid
        }


        if (priceChange.timestamp.time - lastCloseTimestamp >= CANDLE_PERIOD_MS) {
            lastCloseTimestamp = priceChange.timestamp.time
            dates.add(priceChange.timestamp)
            open.add(currentOpen)
            high.add(currentHigh)
            low.add(currentLow)
            close.add(priceChange.bid)

            currentOpen = priceChange.bid
            currentHigh = priceChange.bid
            currentLow = priceChange.bid
        } else {
            if (currentHigh < priceChange.bid) currentHigh = priceChange.bid
            if (currentLow > priceChange.bid) currentLow = priceChange.bid
        }
    }

}