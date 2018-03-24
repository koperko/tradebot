package com.koperko.evolution

import io.jenetics.DoubleChromosome
import io.jenetics.DoubleGene
import io.jenetics.util.ISeq

/**
 * Created by Matus on 24.03.2018.
 */

class ChromosomeFactory {

    val BBUpperFactor = DoubleChromosome.of(0.0, 5.0, 1)
    val BBLowerFactor = DoubleChromosome.of(0.0, 5.0, 1)
    val BBLookBackPeriod = DoubleChromosome.of(10.0, 10000.0, 1)
    val BBStopLoss = DoubleChromosome.of(0.0, 0.1, 1)

    fun getOrderedChromosomes() : ISeq<DoubleChromosome> {
        return ISeq.of(BBUpperFactor, BBLowerFactor, BBLookBackPeriod, BBStopLoss)
//        return chromosomes.toArray(Array(chromosomes.length(), { DoubleChromosome.of(0.0, 1.0)}))
    }
}