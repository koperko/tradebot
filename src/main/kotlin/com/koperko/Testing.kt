package com.koperko

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

fun runEvolution(datasetFilePath: String) {
    val engine = Engine.builder(TradingProblem(datasetFilePath))
            .maximizing()
            .populationSize(3)
            .build()

    val statistics = EvolutionStatistics.ofNumber<Double>()

    val best = engine
//            .limit(Limits.byStea<Double>(25))
            .limit(100)
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
    val parameters = TradingParameters(0.06036718720554109, 0.003131477609432154, 1.7553265172528825, 0.045952899326520136, 84.24036703564275) // 2015 year training
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
    val parameters = TradingParameters(0.06036718720554109, 0.003131477609432154, 1.7553265172528825, 0.045952899326520136, 84.24036703564275) // 2015 year training
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


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        runEvolution("src/main/resources/eurusd-2012-mini.csv")
    } else {
        runEvolution(args.first())
    }
//    runSingleTrader()
//    runOnOanda()

}

