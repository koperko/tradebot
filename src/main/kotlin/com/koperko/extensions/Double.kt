package com.koperko.extensions

import java.math.BigDecimal
import java.util.*

/**
 * Created by Matus on 01.04.2018.
 */

fun Double.averageWith(vararg another: Double) : Double {
    return another.plus(this)
        .average()
}

fun Double.roundTo(decimalPoints: Int) =
        BigDecimal(this).setScale(decimalPoints, BigDecimal.ROUND_HALF_UP).toDouble()


fun List<Double>.takeLastAsArray(n: Int): DoubleArray {
    val array = DoubleArray(n)
    var index = 0
    for (i in size - n until size) {
        array[index++] = this[i]
    }
    return array
}

fun Double.isInRange(min: Double, max: Double): Boolean {
    return (min <= this) && (max >= this)
}
