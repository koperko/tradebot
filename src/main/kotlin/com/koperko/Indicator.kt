package com.koperko


interface Indicator {
    fun shouldOpen() : Position
    fun shouldClose() : Boolean
    fun updatePrice(price: Double)
    fun notifyOpenTrade(position: Position)
    fun reset() {

    }
}