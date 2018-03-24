package com.koperko.evolution

import com.koperko.*
import io.jenetics.DoubleChromosome
import io.jenetics.DoubleGene
import io.jenetics.Genotype
import io.jenetics.engine.Codec
import io.jenetics.engine.Problem
import io.jenetics.internal.collection.ArrayMSeq
import io.jenetics.util.DoubleRange
import io.jenetics.util.ISeq
import io.jenetics.util.MSeq
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.*
import java.util.function.Function

/**
 * Created by Matus on 22.03.2018.
 */
class TradingProblem : Problem<ISeq<Double>, DoubleGene, Double> {

    override fun fitness(): Function<ISeq<Double>, Double> {
        return Function { genotype ->
            val evaluator = EvaluatorImpl(SimulatedMarket(File("src/main/resources/bitcoin-lite.csv")))
            return@Function evaluator.evaluate(TraderImpl(TradingParameters(genotype)))
                    .subscribeOn(Schedulers.computation())
                    .onErrorResumeNext {
                        if (it is NoSuchElementException) {
                            Single.just(EvaluationResult(0.0))
                        } else {
                            Single.error(it)
                        }
                    }
                    .blockingGet().balance
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