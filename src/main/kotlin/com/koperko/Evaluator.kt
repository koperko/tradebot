package com.koperko

import io.reactivex.Single

/**
 * Created by Matus on 22.03.2018.
 */
interface Evaluator {

    /**
     * Evaluates the performance of given trader
     */
    fun evaluate(trader: Trader) : Single<EvaluationResult>

}


/**
 * Result of trader evaluation by @{link Evaluator}
 */
data class EvaluationResult(val balance: Double)