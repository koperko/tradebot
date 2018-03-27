package com.koperko

import hu.akarnokd.rxjava2.operators.FlowableTransformers
import hu.akarnokd.rxjava2.operators.ObservableTransformers
import io.reactivex.BackpressureStrategy
import io.reactivex.ObservableTransformer
import io.reactivex.Single

class WeeklyBalanceEvaluator(private val market: Market, private val weeklyThreshold: Double) : Evaluator {

    override fun evaluate(trader: Trader): Single<Double> {
        return trader.startTrading(market)
                .toFlowable(BackpressureStrategy.BUFFER)
                .filter { it is MarketEvent.BalanceChange || it is MarketEvent.NewWeek }
                .compose(FlowableTransformers.bufferSplit<MarketEvent> { event -> event is MarketEvent.NewWeek })
//                .map {
//                    when {
//                        computeWeeksProfit(it) >= weeklyThreshold -> 1
//                        computeWeeksProfit(it) > 0 -> 0
//                        else -> -1
//                    }
//                }
                .map { computeWeeksProfit(it) }
                .toList()
                .map { if (it.size > 0) it.average() else -1000.0}
    }

    private fun computeWeeksProfit(balanceChanges: List<MarketEvent>) : Double {
        val transformedChanges = balanceChanges
                .filter { it is MarketEvent.BalanceChange }
                .map { it as MarketEvent.BalanceChange }
        if (transformedChanges.size == 0) return 0.0
        if (transformedChanges.size == 1) return (transformedChanges.first().newBalance / transformedChanges.first().oldBalance) - 1

        val start = transformedChanges.first().oldBalance
        val end = transformedChanges.last().newBalance
        return (end / start) - 1
    }

}