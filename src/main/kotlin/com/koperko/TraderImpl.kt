package com.koperko

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

class TraderImpl(override var parameters: TradingParameters) : Trader  {

    companion object {
        const val CANDLE_PERIOD_MS = 24 * 60 * 60 * 1000
    }

    private var balanceChangeSubject = PublishSubject.create<BalanceChange>()

    private val marketSubscriptions = CompositeDisposable()

    private val indicators = ArrayList<Indicator>(Arrays.asList(
            BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.BBStopLoss)
    ))

    private var position = Position.NONE
    private var openPrice = 0.0
    private var balance = 10000.0

    val dataset = XYSeriesCollection()
    val testSeries = XYSeries("test")
    val chart = ChartFactory.createXYLineChart("Trade visualization", "Minutes", "Price", dataset)
    val plot = chart.xyPlot

    val dates = ArrayList<Date>()
    val open = ArrayList<Double>()
    val high = ArrayList<Double>()
    val low = ArrayList<Double>()
    val close = ArrayList<Double>()

    var currentOpen = 0.0
    var currentHigh = 0.0
    var currentLow = Double.MAX_VALUE
    var firstOpenPrice = 0.0

    var lastCloseTimestamp = 0L

    override fun onPriceChange(priceChange: PriceChangeEvent) {
        val (created, price) = priceChange
        val createdDouble = created.time.toDouble()

//        updateOHLCData(priceChange)

        indicators.forEach { it.updatePrice(price) }

        when (position) {
            Position.NONE -> {
                val shouldOpen = indicators.map { it.shouldOpen().getTrust(1.0) }.average()
                val openMarker = ValueMarker(createdDouble)
                openMarker.paint = Color.BLUE
                when {
                    shouldOpen > 0.5 -> {
                        position = Position.BUY
                        openPrice = price
                        indicators.forEach { it.notifyOpenTrade(position) }
                        plot.addDomainMarker(openMarker)
                    }
                    shouldOpen < -0.5 -> {
                        position = Position.SELL
                        openPrice = price
                        indicators.forEach { it.notifyOpenTrade(position) }
                        plot.addDomainMarker(openMarker)
                    }
                }
            }
            Position.BUY, Position.SELL -> {
                val shouldClose = indicators.map { if (it.shouldClose()) 1 else 0 }.average()
                if (shouldClose > 0.5) {
                    val coefficient = updateBalance(price)
                    position = Position.NONE
                    val marker = ValueMarker(createdDouble)
                    marker.paint = if (coefficient >= 1) Color.GREEN else Color.RED
                    plot.addDomainMarker(marker)
                    indicators.forEach { it.reset() }
                }
            }
        }
        testSeries.add(createdDouble, price)
    }


    override fun startTrading(market: Market) : Observable<BalanceChange> {
//        System.out.println("Testing started with balance $balance")
//        System.out.println("${Date()}")

        marketSubscriptions.add(market.subscribe(this))
        return balanceChangeSubject
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
        return balance
    }

    override fun onMarketClose() {
        stopTrading()
    }

    private fun resetBalanceChangeSubject() {
        balanceChangeSubject.onComplete()
        balanceChangeSubject = PublishSubject.create()
    }

    private fun updateBalance(closingPrice: Double) : Double {
        val oldBalance = balance
        val coefficient = when (position) {
            Position.BUY -> closingPrice / openPrice
            Position.SELL -> openPrice / closingPrice
            else -> throw RuntimeException("Trying to update balance when no position is open at the moment")
        } * 0.999

        balance *= coefficient
        balanceChangeSubject.onNext(BalanceChange(oldBalance, balance))
        val tradeProfit = (coefficient * 100) - 100
//        System.out.println("New balance: ${balance.toInt()}\t, closed a trade with  \t \t ${if(tradeProfit>0) "   +" else ""}${"%.3f".format(tradeProfit)}")
        return coefficient
    }


    private fun updateOHLCData(priceChange: PriceChangeEvent) {
        if (firstOpenPrice == 0.0) firstOpenPrice = priceChange.price
        if (lastCloseTimestamp == 0L) {
            lastCloseTimestamp = priceChange.timestamp.time
            currentOpen = priceChange.price
        }


        if (priceChange.timestamp.time - lastCloseTimestamp >= CANDLE_PERIOD_MS) {
            lastCloseTimestamp = priceChange.timestamp.time
            dates.add(priceChange.timestamp)
            open.add(currentOpen)
            high.add(currentHigh)
            low.add(currentLow)
            close.add(priceChange.price)

            currentOpen = priceChange.price
            currentHigh = priceChange.price
            currentLow = priceChange.price
        } else {
            if (currentHigh < priceChange.price) currentHigh = priceChange.price
            if (currentLow > priceChange.price) currentLow = priceChange.price
        }

    }

}