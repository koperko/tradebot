package com.koperko.evolution

import com.koperko.BollingerBandsIndicator
import com.koperko.SimulatedMarket
import com.koperko.TraderImpl
import com.koperko.TradingParameters
import com.koperko.environment.InMemoryEnvironment
import com.koperko.environment.MarketSymbol
import com.koperko.evaluator.FinalBalanceEvaluator
import io.jenetics.DoubleGene
import io.jenetics.Genotype
import io.jenetics.engine.Codec
import io.jenetics.engine.Problem
import io.jenetics.util.ISeq
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.function.Function

/**
 * Created by Matus on 22.03.2018.
 */
class TradingProblem : Problem<ISeq<Double>, DoubleGene, Double> {


    private val market = SimulatedMarket(File("src/main/resources/eurusd-mini.csv"))

    override fun fitness(): Function<ISeq<Double>, Double> {

        return Function { genotype ->
            //            val evaluator = WeeklyBalanceEvaluator(market, 0.04)
            val evaluator = FinalBalanceEvaluator(market)
            val parameters = TradingParameters(genotype)
            val indicator = BollingerBandsIndicator(parameters.BBLowerFactor, parameters.BBUpperFactor, parameters.BBLookBackPeriod.toInt(), parameters.stopLoss)
//            val cachedIndicator = CachedIndicator( //fixme: cache results only to specific parameters, maybe a hashmap?
//                    indicator,
//                    CachedIndicator.CachedData(ArrayList(bollingerBandsCachedData.shouldOpen), ArrayList(bollingerBandsCachedData.shouldClose))
//            )
            val trader = TraderImpl(
                    Arrays.asList(indicator),
                    parameters,
                    InMemoryEnvironment(MarketSymbol.EURUSD, 10000.0))
            return@Function evaluator.evaluate(trader)
                    .subscribeOn(Schedulers.computation())
//                    .doOnSuccess { bollingerBandsCachedData = cachedIndicator.cachedData }
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.just(0.0)
                        } else {
                            Single.error(it)
                        }
                    }
                    .blockingGet()
        }
    }

    override fun codec(): Codec<ISeq<Double>, DoubleGene>? {
        val chromosomeFactory = ChromosomeFactory()
        return Codec.of(
                Genotype.of(chromosomeFactory.getOrderedChromosomes()),
                Function { gt ->
//                    gt.chromosome.toSeq().map { it.allele }
                    gt.toSeq().map { it.gene.allele }
                }
        )
    }

}