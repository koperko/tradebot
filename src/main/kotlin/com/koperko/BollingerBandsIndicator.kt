package com.koperko

import com.koperko.environment.Position
import com.koperko.extensions.isInRange
import com.koperko.extensions.takeLastAsArray
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation
import org.jfree.data.xy.XYSeries
import java.util.*

class BollingerBandsIndicator(private val lowerFactor: Double, private val upperFactor: Double, private val lookBackPeriod: Int, private val stopLossPercentage: Double) : Indicator {


    object Math {
        val STANDARD_DEVIATION = StandardDeviation()
    }

    companion object {
        const val STOP_LIMIT = 2000
    }

    data class State(val mean: Double, val std: Double, val lowerBand: Double, val upperBand: Double,
                     val isLowerBandBreached: Boolean, val isUpperBandBreached: Boolean)

    private val historyPoints = ArrayList<Double>(lookBackPeriod)
    private var state = State(0.0, 0.0, 0.0, 0.0, false, false)
    private var openTradeIndex: Int = 0
    private var openTradePosition = Position.NONE

    private val meanSeries = XYSeries("mean")
    private val upperBandSeries = XYSeries("upper")
    private val lowerBandSeries = XYSeries("lower")

    override fun shouldOpen(): Position {
        if (historyPoints.size < lookBackPeriod) return Position.NONE

        val price = historyPoints.last()

        return if (state.isUpperBandBreached && price < state.upperBand)
            Position.SELL
        else if (state.isLowerBandBreached && price > state.lowerBand)
            Position.BUY
        else
            Position.NONE
    }

    override fun shouldClose(): Boolean {
        if (historyPoints.size < lookBackPeriod) return false

//        if (openTradeIndex != null && (historyPoints.lastIndex - openTradeIndex!!) > STOP_LIMIT) return true

        if (isStopLossHit()) return true

        val price = historyPoints.last()
        val prevPrice = historyPoints[historyPoints.lastIndex - 1]

        return state.mean.isInRange(prevPrice, price) || state.mean.isInRange(price, prevPrice)
    }

    override fun updatePrice(price: Double) {
        historyPoints.add(price)
        if (historyPoints.size < lookBackPeriod) return

        val mean = computeMean()
        val std = computeStandardDeviation(mean)
        val lowerBand = mean - (lowerFactor * std)
        val upperBand = mean + (upperFactor * std)
        val isLowerBandBreached = price <= lowerBand || state.isLowerBandBreached
        val isUpperBandBreached = price >= upperBand || state.isUpperBandBreached
        state = State(mean, std, lowerBand, upperBand, isLowerBandBreached, isUpperBandBreached)

        meanSeries.add(historyPoints.lastIndex, mean)
        upperBandSeries.add(historyPoints.lastIndex, upperBand)
        lowerBandSeries.add(historyPoints.lastIndex, lowerBand)
    }

    override fun notifyOpenTrade(position: Position) {
        openTradeIndex = historyPoints.lastIndex
        openTradePosition = position
    }

    override fun reset() {
        state = BollingerBandsIndicator.State(state.mean, state.std, state.lowerBand, state.upperBand, false, false)
    }

    private fun computeMean() : Double {
        return historyPoints.takeLastAsArray(lookBackPeriod).average()
    }

    private fun computeStandardDeviation(mean: Double) : Double {
        return Math.STANDARD_DEVIATION.evaluate(historyPoints.takeLastAsArray(lookBackPeriod), mean)
    }

    private fun isStopLossHit() : Boolean {

        if (openTradePosition != Position.NONE) {
            val coefficient = when (openTradePosition) {
                Position.BUY -> historyPoints.last() / historyPoints[openTradeIndex]
                Position.SELL -> historyPoints[openTradeIndex] / historyPoints.last()
                else -> throw RuntimeException("No position is open at the moment")
            }
            return coefficient < 1 - stopLossPercentage
        }
        return false
    }

}
