package com.koperko.market

import com.koperko.PriceChangeEvent
import com.koperko.PriceChangeListener
import com.koperko.environment.OandaEnvironment
import com.oanda.v20.ContextBuilder
import com.oanda.v20.pricing.PricingGetRequest
import com.oanda.v20.primitives.AcceptDatetimeFormat
import com.oanda.v20.primitives.DateTime
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.util.*


class OandaMarket(private val config: OandaEnvironment.Config) : Market {

    private val context = ContextBuilder(config.url)
            .setToken(config.token)
            .setDatetimeFormat(AcceptDatetimeFormat.UNIX)
            .setApplication("TradeApp")
            .build()

    override fun subscribe(listener: PriceChangeListener): Disposable {
        return Flowable.create<Any>({ emitter ->
            try {
                val request = PricingGetRequest(config.accountID, listOf(config.instrument.toString()))

                var since: DateTime? = null

                while (!emitter.isCancelled) {
                    if (since != null) {
//                        println("Polling since $since")
                        request.setSince(since)
                    }
                    val resp = context.pricing.get(request)
                    since = resp.time

                    if (resp.prices.isNotEmpty()) {
                        val ask = resp.prices.first().asks.first()
                        val bid = resp.prices.first().bids.first()
                        val timestamp = (since?.toString()?.toDouble()?.toLong() ?: 0) * 1000

//                        println("New price... bid: ${bid.price.doubleValue()}, ask: ${ask.price.doubleValue()}")

                        listener.onPriceChange(PriceChangeEvent(Date(timestamp), bid.price.doubleValue(), ask.price.doubleValue()))
                    }

                    Thread.sleep(8500)
                }
                listener.onMarketClose()
            } catch (e: Exception) {
                listener.onMarketClose()
                throw RuntimeException(e)
            }

        }, BackpressureStrategy.BUFFER)
                .retry()
                .subscribe({}, {
                    it.printStackTrace()
                })
    }

}