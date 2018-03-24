package com.koperko

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.File
import java.io.FileReader
import java.util.*

/**
 * Created by Matus on 20.03.2018.
 */
class SimulatedMarket(val csvFile: File) : Market {

    var data: Iterable<CSVRecord>? = null

    override fun subscribe(listener: PriceChangeListener) : Disposable {
        return createDataObservable().subscribe ({ listener.onPriceChange(it) },
                { it.printStackTrace() },
                { listener.onMarketClose() })
    }

    private fun createDataObservable() : Observable<PriceChangeEvent> {
        if (data == null) data = CSVParser(FileReader(csvFile), CSVFormat.DEFAULT).records
        return Observable.fromIterable(data)
                .map { PriceChangeEvent(Date(it[0].toLong() * 1000), it[4].toDouble()) }
                .subscribeOn(Schedulers.computation())
    }

}
