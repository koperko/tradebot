package com.koperko

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.io.FileReader
import java.util.*

/**
 * Created by Matus on 20.03.2018.
 */
class SimulatedMarket(val csvFile: File) : Market {


    override fun subscribe(listener: PriceChangeListener) : Disposable {
        return createDataObservable().subscribe ({ listener.onPriceChange(it) },
                {/* todo: error handling */},
                { listener.onMarketClose() })
    }

    private fun createDataObservable() : Observable<PriceChangeEvent> {
        val csvParser = CSVParser(FileReader(csvFile), CSVFormat.DEFAULT)
        return Observable.fromIterable(csvParser)
                .map { PriceChangeEvent(Date(it[0].toLong() * 1000), it[4].toDouble()) }
    }

}
