package com.koperko.evaluator

import com.koperko.MarketEvent
import com.koperko.Trader
import com.koperko.market.Market
import io.reactivex.Single

class FinalBalanceEvaluator(private val market: Market) : CachingEvaluator() {

    override fun computeValuation(trader: Trader): Single<Double> {
        return trader.startTrading(market)
                .filter { it is MarketEvent.BalanceChange }
                .lastOrError()
                .map { (it as MarketEvent.BalanceChange).newBalance }
    }

}