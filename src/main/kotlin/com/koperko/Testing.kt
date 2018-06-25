package com.koperko

import com.jfx.Broker
import com.jfx.net.JFXServer
import com.jfx.strategy.PositionChangeInfo
import com.jfx.strategy.PositionInfo
import com.jfx.strategy.PositionListener
import com.jfx.strategy.Strategy
import com.koperko.environment.InMemoryEnvironment
import com.koperko.environment.MarketSymbol
import com.koperko.environment.OandaEnvironment
import com.koperko.evolution.TradingProblem
import com.koperko.market.Market
import com.koperko.market.OandaMarket
import com.koperko.market.SimulatedMarket
import io.jenetics.DoubleGene
import io.jenetics.engine.Engine
import io.jenetics.engine.EvolutionResult
import io.jenetics.engine.EvolutionStatistics
import java.io.File
import java.util.*

fun runEvolution() {
    val engine = Engine.builder(TradingProblem())
            .maximizing()
            .populationSize(10)
            .build()

    val statistics = EvolutionStatistics.ofNumber<Double>()

    val best = engine
//            .limit(Limits.byStea<Double>(25))
            .limit(50)
            .stream()
            .peek(statistics)
            .peek { System.out.println("Best trader in generation ${it.generation} balance: ${"%.4f".format(it.bestFitness)} \t at ${Date()} \t (${it.bestPhenotype.genotype}") }
            .collect(EvolutionResult.toBestPhenotype<DoubleGene, Double>())

    System.out.print(statistics)
}

fun runSingleTrader() {
    val market: Market = SimulatedMarket(File("src/main/resources/eurusd-2015-mini.csv"))
//    val parameters = TradingParameters(0.04299748953891125, 0.04299748953891125, 0.17878435004074766, 27.725265442748604, 0.0054511954340837115)
//    val parameters = TradingParameters(0.033892554726637096, 0.05591689815067605, 1.685870747233853, 2.5763840292468037, 370.33698098456443)
    val parameters = TradingParameters(0.0137756595607479, 0.042841892379461656, 2.5526036710316236, 2.082400728400062, 25.374658459606756) // 2015 year training
    val indicators = Arrays.asList(BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss))
    val trader = TraderImpl(indicators, parameters, InMemoryEnvironment(MarketSymbol.EURUSD, 10000.0))
//
    var wins = 0
    var defeats = 0

    val balance = trader.startTrading(market)
            .filter { it is MarketEvent.BalanceChange }
            .map { it as MarketEvent.BalanceChange }
            .doOnNext { System.out.println("New balance: ${it.newBalance}   profit: ${it.newBalance / it.oldBalance}") }
            .doOnNext { if (it.newBalance > it.oldBalance) wins++ else defeats++ }
            .last(MarketEvent.BalanceChange(-1.0, -1.0))
            .blockingGet()

    System.out.println("Testing finished with balance ${balance.newBalance}   wins: $wins  defeats: $defeats")


}

fun runOnOanda() {
//    val market: Market = SimulatedMarket(File("src/main/resources/eurusd-2015-mini.csv"))
    val market: Market = OandaMarket(OandaEnvironment.DEMO_ACCOUNT)
//    val parameters = TradingParameters(0.04299748953891125, 0.04299748953891125, 0.17878435004074766, 27.725265442748604, 0.0054511954340837115)
//    val parameters = TradingParameters(0.033892554726637096, 0.05591689815067605, 1.685870747233853, 2.5763840292468037, 370.33698098456443)
    val parameters = TradingParameters(0.0137756595607479, 0.042841892379461656, 2.5526036710316236, 2.082400728400062, 25.374658459606756) // 2015 year training
    val indicators = Arrays.asList(BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss))
    val environment = OandaEnvironment(MarketSymbol.EURUSD, OandaEnvironment.DEMO_ACCOUNT)
    val trader = TraderImpl(indicators, parameters, environment)
//
    var wins = 0
    var defeats = 0

    System.out.println("oanda balance: ${environment.getBalance()}")

    val balance = trader.startTrading(market)
            .filter { it is MarketEvent.BalanceChange }
            .map { it as MarketEvent.BalanceChange }
            .doOnNext { System.out.println("New balance: ${it.newBalance}   profit: ${it.newBalance / it.oldBalance}") }
            .doOnNext { if (it.newBalance > it.oldBalance) wins++ else defeats++ }
            .last(MarketEvent.BalanceChange(-1.0, -1.0))
            .blockingGet()

    System.out.println("Testing finished with balance ${balance.newBalance}   wins: $wins  defeats: $defeats")

}

fun runWithMT4() {
    val parameters = TradingParameters(0.04299748953891125, 0.04299748953891125, 0.17878435004074766, 27.725265442748604, 0.0054511954340837115)
    val indicators = Arrays.asList(BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss))
    val trader = TraderImpl(indicators, parameters, InMemoryEnvironment(MarketSymbol.EURUSD, 10000.0))

    val broker = Broker("FxPro.com-Demo05")
    val user = "8227627"
    val password = "p3Fa6Vn3"

    val strategy = Strategy()
    strategy.isReconnect = true

    strategy.addTickListener("EURUSD", trader)
            .withDedicatedInstrumentOrdersWorker("EURUSD")
//            .setPositionListener(MyPositionListener(), 1000, 1000)
            .connect("127.0.0.1", 7788, broker, user, password)

    JFXServer.getInstance()
    println(JFXServer.getInstance().bindHost + ":" + JFXServer.getInstance().bindPort)

    Thread.sleep(Long.MAX_VALUE)
}

fun main(args: Array<String>) {

    runEvolution()
//    runSingleTrader()
//    runOnOanda()

}


class MyPositionListener : PositionListener {

    override fun onInit(p0: PositionInfo?) {
        System.out.println(p0.toString())
    }

    override fun onChange(p0: PositionInfo?, p1: PositionChangeInfo?) {
        System.out.println(p0.toString())
    }

}