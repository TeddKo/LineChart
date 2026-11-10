package tddy.ko.linechart.ui

import java.util.TimeZone

/**
 * Daily candles of the trades for the price history chart, in the order of time.
 *
 * Volume is the traded quantity and value is price times quantity, both summed per day. The day starts at midnight of [timeZone].
 * Use [aggregate] on the result for longer timeframes.
 */
public fun List<MarketTrade>.toDailyChartData(timeZone: TimeZone = TimeZone.getDefault()): List<ChartData> =
    sortedBy { it.time }
        .map { ChartData(it.price.toDouble(), it.time, volume = it.quantity.toDouble(), value = it.price.toDouble() * it.quantity) }
        .aggregate(Timeframe.Day, timeZone)
