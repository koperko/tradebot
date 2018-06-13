package com.koperko.environment

/**
 * Created by Matus on 31.03.2018.
 */
enum class Position(var i: Int) {
    NONE(0),
    BUY(1),
    SELL(-1);

    fun getTrust(factor: Double): Double {
        return i * factor
    }
}
