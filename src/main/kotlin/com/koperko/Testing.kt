package com.koperko

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtilities
import org.jfree.chart.plot.ValueMarker
import org.jfree.data.time.ohlc.OHLCSeries
import org.jfree.data.xy.DefaultHighLowDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.Color
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.ArrayList

fun List<Double>.takeLastAsArray(n: Int) : DoubleArray {
    val array = DoubleArray(n)
    var index = 0
    for (i in size-n until size) {
        array[index++] = this[i]
    }
    return array
}


enum class Position(var i: Int) {
    NONE(0),
    BUY(1),
    SELL(-1);

    fun getTrust(factor: Double) : Double {
        return i * factor
    }
}


fun Double.isInRange(min: Double, max: Double): Boolean {
    return (min <= this) && (max >= this)
}


class TradingBot {

    private val indicators = ArrayList<Indicator>(Arrays.asList(
            BollingerBandsIndicator(2.0, 2.0, 800)
    ))

    private var position = Position.NONE
    private var openPrice = 0.0
    private var balance = 10000.0


    fun test(csv: File) {
        System.out.println("Testing stated with balance $balance")

        val dataset = XYSeriesCollection()
//        val candleDataset = DefaultHighLowDataset()
        val testSeries = XYSeries("test")
        val chart = ChartFactory.createXYLineChart("Trade visualization", "Minutes", "Price", dataset)
        val plot = chart.xyPlot
        plot.rangeAxis.setRange(2600.0, 6000.0)
        CSVParser(FileReader(csv), CSVFormat.DEFAULT)
                .records
                .map { it[4].toDouble() }
                .forEachIndexed { index, price ->
                    indicators.forEach { it.updatePrice(price) }

                    when (position) {
                        Position.NONE -> {
                            val shouldOpen = indicators.map { it.shouldOpen().getTrust(1.0) }.average()
                            val openMarker = ValueMarker(index.toDouble())
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
                                val marker = ValueMarker(index.toDouble())
                                marker.paint = if (coefficient >= 1) Color.GREEN else Color.RED
                                plot.addDomainMarker(marker)
                                indicators.forEach { it.reset() }
                            }
                        }
                    }
                    testSeries.add(index.toDouble(), price)
                }
        dataset.addSeries(testSeries)

        val indicator = indicators.first() as BollingerBandsIndicator

//        dataset.addSeries(indicator.meanSeries)
//        dataset.addSeries(indicator.upperBandSeries)
//        dataset.addSeries(indicator.lowerBandSeries)

        val outChartFile = File("trades.jpg")
        val width = 20500
        val height = 2480
        ChartUtilities.saveChartAsJPEG(outChartFile, chart, width, height)

        System.out.println("Testing finished with balance $balance")
    }


    private fun updateBalance(closingPrice: Double) : Double {
        val coefficient = when (position) {
            Position.BUY -> closingPrice / openPrice
            Position.SELL -> openPrice / closingPrice
            else -> throw RuntimeException("Trying to update balance when no position is open at the moment")
        } * 0.999

        balance *= coefficient
        System.out.println("New balance: $balance, closed a trade with \t \t ${coefficient * 100}")
        return coefficient
    }

}

fun main(args: Array<String>) {

    val tradingBot = TradingBot()

    tradingBot.test(File("src/main/resources/bitcoin-lite.csv"))

}


