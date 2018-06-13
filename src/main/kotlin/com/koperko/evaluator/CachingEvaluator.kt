package com.koperko.evaluator

import com.koperko.Trader
import com.koperko.TradingParameters
import io.reactivex.Single

abstract class CachingEvaluator : Evaluator {

    private val cache = HashMap<TradingParameters, Double>()

    override fun evaluate(trader: Trader): Single<Double> {
        return if (cache.contains(trader.parameters)) {
            Single.just(cache[trader.parameters])
        } else {
            computeValuation(trader)
                    .doOnSuccess { cache[trader.parameters] = it } // cache computed valuation
        }
    }

    abstract fun computeValuation(trader: Trader): Single<Double>
}