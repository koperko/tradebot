package com.koperko.market

import com.koperko.PriceChangeListener
import io.reactivex.disposables.Disposable

/**
 * Created by Matus on 20.03.2018.
 */
interface Market {

    fun subscribe(listener: PriceChangeListener): Disposable

}