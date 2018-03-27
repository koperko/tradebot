package com.koperko

import io.reactivex.Single

class FinalBalanceEvaluator(private val market: Market) : Evaluator {

    override fun evaluate(trader: Trader): Single<Double> {
        return trader.startTrading(market)
                .filter { it is MarketEvent.BalanceChange }
                .lastOrError()
                .map { (it as MarketEvent.BalanceChange).newBalance }
    }

}