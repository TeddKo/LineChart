package tddy.ko.linechart.ui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

/**
 * Inclusive value range mapped onto a pane, linearly or through the natural logarithm when [log].
 */
internal class YRange(val min: Double, val max: Double, val log: Boolean = false)

/**
 * Padded value range of the items in [range], using low and high for candles and close for line and area, widened by the [marks] values in [range] and by [include] unless it is NaN.
 * With [log] the padding is applied in log space, falling back to a linear range when a value is not positive.
 */
internal fun visibleYRange(
    items: List<ChartData>,
    range: IntRange,
    type: ChartType,
    marks: List<Mark> = emptyList(),
    log: Boolean = false,
    include: Double = Double.NaN,
): YRange {
    if (range.isEmpty()) return YRange(0.0, 1.0)
    var min = if (include.isNaN()) Double.POSITIVE_INFINITY else include
    var max = if (include.isNaN()) Double.NEGATIVE_INFINITY else include
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
    if (log && min > 0.0) {
        val low = ln(min)
        val span = ln(max) - low
        if (span <= 0.0) return YRange(min * 0.99, max * 1.01, true)
        return YRange(exp(low - span * 0.05), exp(low + span * 1.05), true)
    }
    val span = max - min
    if (span <= 0.0) {
        val delta = if (min == 0.0) 1.0 else abs(min) * 0.01
        return YRange(min - delta, max + delta)
    }
    return YRange(min - span * 0.05, max + span * 0.05)
}

private fun YRange.fraction(value: Double): Double =
    if (log) (ln(value.coerceAtLeast(min * LOG_FLOOR)) - ln(min)) / (ln(max) - ln(min)) else (value - min) / (max - min)

internal fun yOf(value: Double, range: YRange, top: Float, height: Float): Float =
    (top + height * (1.0 - range.fraction(value))).toFloat()

internal fun valueAt(y: Float, range: YRange, top: Float, height: Float): Double {
    val fraction = 1.0 - (y - top) / height
    return if (range.log) exp(ln(range.min) + fraction * (ln(range.max) - ln(range.min))) else range.min + fraction * (range.max - range.min)
}

/**
 * Ticks at 1, 2 and 5 times a power of ten inside [min]..[max], falling back to [niceTicks] when fewer than two fit.
 */
internal fun logTicks(min: Double, max: Double, target: Int): List<Double> {
    if (min <= 0.0 || max <= min) return niceTicks(min, max, target)
    val ticks = ArrayList<Double>()
    for (decade in floor(log10(min)).toInt()..ceil(log10(max)).toInt()) {
        for (factor in intArrayOf(1, 2, 5)) {
            val tick = factor * 10.0.pow(decade)
            if (tick >= min * (1 - 1e-9) && tick <= max * (1 + 1e-9)) ticks += tick
        }
    }
    return if (ticks.size >= 2) ticks else niceTicks(min, max, target)
}

/**
 * Prices at round percentage changes against [base] inside [min]..[max], falling back to [niceTicks] for a non-positive base.
 */
internal fun percentTicks(min: Double, max: Double, base: Double, target: Int): List<Double> {
    if (!(base > 0.0)) return niceTicks(min, max, target)
    return niceTicks((min / base - 1.0) * 100.0, (max / base - 1.0) * 100.0, target).map { base * (1.0 + it / 100.0) }
}

/**
 * Signed change percent of [value] against [base] as text.
 */
internal fun percentText(value: Double, base: Double): String = formatChange((value / base - 1.0) * 100.0)

private const val LOG_FLOOR = 1e-3

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
