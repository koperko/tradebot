package com.koperko.evolution

import com.koperko.Indicator
import com.koperko.environment.Position

class CachedIndicator(val indicator: Indicator, val cachedData: CachedData) : Indicator {

    private val isTraining = cachedData.shouldOpen.isEmpty() && cachedData.shouldClose.isEmpty()

    private var seekPointer = 0

    override fun shouldOpen(): Position {
        return if (isTraining) {
            val shouldOpen = indicator.shouldOpen()
            cachedData.shouldOpen.add(shouldOpen)
            shouldOpen
        } else {
            cachedData.shouldOpen[seekPointer]
        }
    }

    override fun shouldClose(): Boolean {
        return if (isTraining) {
            val shouldClose = indicator.shouldClose()
            cachedData.shouldClose.add(shouldClose)
            shouldClose
        } else {
            cachedData.shouldClose[seekPointer]
        }
    }

    override fun updatePrice(price: Double) {
        if (isTraining) {
            indicator.updatePrice(price)
        } else {
            seekPointer++
        }
    }

    override fun notifyOpenTrade(position: Position) {
        if (isTraining) {
            indicator.notifyOpenTrade(position)
        }
    }

    data class CachedData(val shouldOpen: MutableList<Position>, val shouldClose: MutableList<Boolean>)
}