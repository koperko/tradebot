package com.koperko

import com.koperko.evolution.TradingProblem
import io.jenetics.DoubleGene
import io.jenetics.Optimize
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.engine.EvolutionStatistics
import io.jenetics.engine.Limits
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

fun List<Double>.takeLastAsArray(n: Int): DoubleArray {
    val array = DoubleArray(n)
    var index = 0
    for (i in size - n until size) {
        array[index++] = this[i]
    }
    return array
}


enum class Position(var i: Int) {
    NONE(0),
    BUY(1),
    SELL(-1);

    fun getTrust(factor: Double): Double {
        return i * factor
    }
}


fun Double.isInRange(min: Double, max: Double): Boolean {
    return (min <= this) && (max >= this)
}

fun main(args: Array<String>) {

//    val market: Market = SimulatedMarket(File("src/main/resources/bitcoin-lite.csv"))
//    val trader: Trader = TraderImpl(TradingParameters(0.04299748953891125, 0.17878435004074766, 27.725265442748604, 0.0054511954340837115))
//
//    val balance = trader.startTrading(market)
//            .filter { it is MarketEvent.BalanceChange }
//            .map { it as MarketEvent.BalanceChange }
//            .last(MarketEvent.BalanceChange(-1.0, -1.0))
//            .blockingGet()
//
//    System.out.println("Testing finished with balance ${balance.newBalance}")

    val engine = Engine.builder(TradingProblem())
            .maximizing()
            .populationSize(100)
            .build()

    val statistics = EvolutionStatistics.ofNumber<Double>()

    val best = engine
//            .limit(Limits.byStea<Double>(25))
            .limit(50)
            .stream()
            .peek(statistics)
            .peek { System.out.println("Best trader in generation ${it.generation} balance: ${"%.4f".format(it.bestFitness)} \t (${it.bestPhenotype.genotype}") }
            .collect(EvolutionResult.toBestPhenotype<DoubleGene, Double>())

    System.out.print(statistics)

}


