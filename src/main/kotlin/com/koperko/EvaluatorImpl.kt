package com.koperko

import io.reactivex.Single

class EvaluatorImpl(private val market: Market) : Evaluator {

    override fun evaluate(trader: Trader): Single<EvaluationResult> {
        return trader.startTrading(market)
                .lastOrError()
                .map { EvaluationResult(it.newBalance) }
    }

}