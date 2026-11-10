package tddy.ko.linechart.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Inclusive value range mapped onto the main pane.
 */
internal class YRange(val min: Double, val max: Double)

/**
 * Padded value range of the items in [range], using low and high for candles and close for line and area, widened by the [marks] values in [range].
 */
internal fun visibleYRange(items: List<ChartData>, range: IntRange, type: ChartType, marks: List<Mark> = emptyList()): YRange {
    if (range.isEmpty()) return YRange(0.0, 1.0)
    var min = Double.POSITIVE_INFINITY
    var max = Double.NEGATIVE_INFINITY
    for (i in range) {
        val item = items[i]
        val low = if (type == ChartType.Candle) item.low else item.cumulativeProfit
        val high = if (type == ChartType.Candle) item.high else item.cumulativeProfit
        if (low < min) min = low
        if (high > max) max = high
        for (mark in marks) {
            mark.eachValue(i) {
                if (it < min) min = it
                if (it > max) max = it
            }
        }
    }
    val span = max - min
    if (span <= 0.0) {
        val delta = if (min == 0.0) 1.0 else abs(min) * 0.01
        return YRange(min - delta, max + delta)
    }
    return YRange(min - span * 0.05, max + span * 0.05)
}

internal fun yOf(value: Double, range: YRange, top: Float, height: Float): Float =
    (top + height * (1.0 - (value - range.min) / (range.max - range.min))).toFloat()

internal fun valueAt(y: Float, range: YRange, top: Float, height: Float): Double =
    range.min + (1.0 - (y - top) / height) * (range.max - range.min)

/**
 * Ascending round tick values inside [min]..[max], about [target] of them with a minimum target of 2.
 */
internal fun niceTicks(min: Double, max: Double, target: Int): List<Double> {
    if (max <= min) return listOf(min)
    val raw = (max - min) / (target.coerceAtLeast(2) - 1)
    val magnitude = 10.0.pow(floor(log10(raw)))
    val fraction = raw / magnitude
    val step = magnitude * when {
        fraction < 1.5 -> 1.0
        fraction < 3.0 -> 2.0
        fraction < 7.0 -> 5.0
        else -> 10.0
    }
    val first = ceil(min / step - 1e-9).toLong()
    val last = floor(max / step + 1e-9).toLong()
    return (first..last).map { it * step }
}

/**
 * Sign of the move at [i]: close versus open for candles, close versus the previous close for line and area.
 */
internal fun direction(items: List<ChartData>, i: Int, type: ChartType): Int {
    val close = items[i].cumulativeProfit
    val reference = when {
        type == ChartType.Candle -> items[i].open
        i == 0 -> close
        else -> items[i - 1].cumulativeProfit
    }
    return close.compareTo(reference)
}
