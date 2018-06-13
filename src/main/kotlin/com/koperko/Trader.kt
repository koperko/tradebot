package com.koperko

import io.jenetics.util.ISeq
import io.reactivex.Observable

/**
 * Created by Matus on 20.03.2018.
 */
interface Trader : PriceChangeListener {

    var indicators: List<Indicator>
    var parameters: TradingParameters

    /**
     *  Returns observable that instructs this trader to start trading upon subscribing
     *  and emits any balance changes
     */
    fun startTrading(market: Market) : Observable<MarketEvent>
    fun stopTrading()
    fun getCurrentBalance() : Double

}

data class TradingParameters(val stopLoss: Double, val takeProfit: Double, val BBUpperFactor: Double, val BBLowerFactor: Double, val BBLookBackPeriod: Double){

    constructor(genotype: ISeq<Double>) : this(genotype[0], genotype[1], genotype[2], genotype[3], genotype[4]) {

    }

}

sealed class MarketEvent {

    class BalanceChange(val oldBalance: Double, val newBalance: Double) : MarketEvent()

    object NewWeek : MarketEvent()
    object NewMonth : MarketEvent()

}
