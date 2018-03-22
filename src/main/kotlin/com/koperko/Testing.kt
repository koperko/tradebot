package com.koperko

import io.jenetics.engine.Engine
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

fun main(args: Array<String>) {

    val market: Market = SimulatedMarket(File("src/main/resources/bitcoin-lite.csv"))
    val trader: Trader = TraderImpl()

    trader.startTrading(market)

}


