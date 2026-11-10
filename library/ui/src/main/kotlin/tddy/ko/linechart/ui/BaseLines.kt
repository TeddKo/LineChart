package tddy.ko.linechart.ui

import java.util.TimeZone

private const val DAY_MILLIS = 86_400_000L

private fun localDay(date: Long, zone: TimeZone): Long = Math.floorDiv(date + zone.getOffset(date), DAY_MILLIS)

/**
 * Indices of the session before the one of the last item: the previous local day for intraday series, the previous candle otherwise, empty when none exists.
 */
internal fun previousSession(series: Series, zone: TimeZone): IntRange {
    val items = series.items
    val n = items.size
    if (n < 2) return IntRange.EMPTY
    if (!series.intraday) return (n - 2)..(n - 2)
    val today = localDay(items[n - 1].date, zone)
    var end = n - 1
    while (end >= 0 && localDay(items[end].date, zone) == today) end--
    if (end < 0) return IntRange.EMPTY
    val day = localDay(items[end].date, zone)
    var start = end
    while (start > 0 && localDay(items[start - 1].date, zone) == day) start--
    return start..end
}

internal fun previousClose(series: Series, zone: TimeZone): Double {
    val session = previousSession(series, zone)
    return if (session.isEmpty()) Double.NaN else series.items[session.last].cumulativeProfit
}

/**
 * Open of the first candle of the last local day for intraday series, the open of the last candle otherwise.
 */
internal fun todayOpen(series: Series, zone: TimeZone): Double {
    val items = series.items
    if (items.isEmpty()) return Double.NaN
    if (!series.intraday) return items.last().open
    val today = localDay(items.last().date, zone)
    var first = items.lastIndex
    while (first > 0 && localDay(items[first - 1].date, zone) == today) first--
    return items[first].open
}

/**
 * Typical price (high + low + close) / 3 of the previous session.
 */
internal fun pivotPoint(series: Series, zone: TimeZone): Double {
    val session = previousSession(series, zone)
    if (session.isEmpty()) return Double.NaN
    var high = Double.NEGATIVE_INFINITY
    var low = Double.POSITIVE_INFINITY
    for (i in session) {
        if (series.items[i].high > high) high = series.items[i].high
        if (series.items[i].low < low) low = series.items[i].low
    }
    return (high + low + series.items[session.last].cumulativeProfit) / 3.0
}

/**
 * Value of [baseLine] for [series], NaN for [BaseLine.None] or when the data lacks the needed session.
 */
internal fun baseValue(series: Series, baseLine: BaseLine, zone: TimeZone): Double = when (baseLine) {
    BaseLine.None -> Double.NaN
    BaseLine.PreviousClose -> previousClose(series, zone)
    BaseLine.TodayOpen -> todayOpen(series, zone)
    BaseLine.Pivot -> pivotPoint(series, zone)
    is BaseLine.Custom -> baseLine.value
}

/**
 * Upper and lower price limits [percent] away from [previousClose].
 */
internal fun limitLines(previousClose: Double, percent: Double): Pair<Double, Double> =
    previousClose * (1.0 + percent / 100.0) to previousClose * (1.0 - percent / 100.0)
