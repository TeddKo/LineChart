package tddy.ko.linechart.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

private val PriceFormat = ThreadLocal.withInitial {
    DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.ROOT))
}

private const val MINUTE_MILLIS = 60_000L
private const val DAY_MILLIS = 86_400_000L
private const val MONTH_MIN_STEP_MILLIS = 28 * DAY_MILLIS
private const val YEAR_MIN_STEP_MILLIS = 360 * DAY_MILLIS

/**
 * Price text with thousands separators and at most two decimals, independent of the default locale.
 */
internal fun formatPrice(value: Double): String {
    val text = checkNotNull(PriceFormat.get()).format(value)
    return if (text == "-0") "0" else text
}

/**
 * Value text with a K, M or B suffix and two decimals from 1,000 up, plain [formatPrice] below.
 */
internal fun formatCompact(value: Double): String {
    val magnitude = abs(value)
    return when {
        magnitude >= 1e9 -> String.format(Locale.ROOT, "%.2fB", value / 1e9)
        magnitude >= 1e6 -> String.format(Locale.ROOT, "%.2fM", value / 1e6)
        magnitude >= 1e3 -> String.format(Locale.ROOT, "%.2fK", value / 1e3)
        else -> formatPrice(value)
    }
}

/**
 * Oscillator value text with two decimals, independent of the default locale.
 */
internal fun formatOscillator(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

private fun unitOf(timeframe: Timeframe?, stepMs: Long): TimeframeUnit = when {
    timeframe != null -> timeframe.unit
    stepMs < MINUTE_MILLIS -> TimeframeUnit.Second
    stepMs < DAY_MILLIS -> TimeframeUnit.Minute
    stepMs < MONTH_MIN_STEP_MILLIS -> TimeframeUnit.Day
    stepMs < YEAR_MIN_STEP_MILLIS -> TimeframeUnit.Month
    else -> TimeframeUnit.Year
}

/**
 * Time axis label pattern for [timeframe], inferred from the sample step [stepMs] when the timeframe is null.
 */
internal fun timePattern(timeframe: Timeframe?, stepMs: Long): String = when (unitOf(timeframe, stepMs)) {
    TimeframeUnit.Tick, TimeframeUnit.Second -> "HH:mm:ss"
    TimeframeUnit.Minute -> "HH:mm"
    TimeframeUnit.Day, TimeframeUnit.Week -> "MM/dd"
    TimeframeUnit.Month -> "yyyy/MM"
    TimeframeUnit.Year -> "yyyy"
}

/**
 * Crosshair time tag pattern, the long form of [timePattern].
 */
internal fun tagPattern(timeframe: Timeframe?, stepMs: Long): String = when (unitOf(timeframe, stepMs)) {
    TimeframeUnit.Tick, TimeframeUnit.Second -> "yyyy/MM/dd HH:mm:ss"
    TimeframeUnit.Minute -> "yyyy/MM/dd HH:mm"
    TimeframeUnit.Day, TimeframeUnit.Week -> "yyyy/MM/dd"
    TimeframeUnit.Month -> "yyyy/MM"
    TimeframeUnit.Year -> "yyyy"
}

/**
 * Smallest stride of 1, 2, 5, 10, 20, 50 and so on whose span of slots fits a label of [labelWidth].
 */
internal fun labelStride(slot: Float, labelWidth: Float): Int {
    if (!(slot > 0f)) return 1
    val needed = labelWidth / slot
    var decade = 1
    while (decade < Int.MAX_VALUE / 10) {
        for (factor in intArrayOf(1, 2, 5)) {
            if (decade * factor >= needed) return decade * factor
        }
        decade *= 10
    }
    return decade
}
