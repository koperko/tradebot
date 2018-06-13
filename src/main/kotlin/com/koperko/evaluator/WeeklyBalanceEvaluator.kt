package com.koperko.evaluator

import com.koperko.Market
import com.koperko.MarketEvent
import com.koperko.Trader
import hu.akarnokd.rxjava2.operators.FlowableTransformers
import io.reactivex.BackpressureStrategy
import io.reactivex.Single

class WeeklyBalanceEvaluator(private val market: Market, private val weeklyThreshold: Double) : Evaluator {

    override fun evaluate(trader: Trader): Single<Double> {

        return trader.startTrading(market)
                .toFlowable(BackpressureStrategy.BUFFER)
                .filter { it is MarketEvent.BalanceChange || it === MarketEvent.NewWeek }
                .compose(FlowableTransformers.bufferSplit<MarketEvent> { event -> event === MarketEvent.NewWeek })
                .map {
                    val weeksProfit = computeWeeksProfit(it)
                    when {
                        weeksProfit >= weeklyThreshold -> 1.0
                        else -> weeksProfit / weeklyThreshold
                    }
                }
//                .map { computeWeeksProfit(it) }
                .toList()
                .map { if (it.size > 0) it.average() else -1000.0}
    }

    private fun computeWeeksProfit(balanceChanges: List<MarketEvent>) : Double {
        val transformedChanges = balanceChanges
                .filter { it is MarketEvent.BalanceChange }
                .map { it as MarketEvent.BalanceChange }
        if (transformedChanges.isEmpty()) return 0.0
        if (transformedChanges.size == 1) return (transformedChanges.first().newBalance / transformedChanges.first().oldBalance) - 1

        val start = transformedChanges.first().oldBalance
        val end = transformedChanges.last().newBalance
        return (end / start) - 1
    }

}