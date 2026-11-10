package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Simple moving average of [period] values, NaN until the first full window.
 *
 * ponytail: window sum per output is O(n·period) and tolerates NaN inputs, a running sum if profiling shows it matters.
 */
internal fun sma(values: DoubleArray, period: Int): DoubleArray {
    val out = DoubleArray(values.size) { Double.NaN }
    for (i in period - 1 until values.size) {
        var sum = 0.0
        for (k in i - period + 1..i) sum += values[k]
        out[i] = sum / period
    }
    return out
}

/**
 * Exponential moving average with factor 2 / (period + 1), seeded by the simple average of the first [period] values after any leading NaN.
 */
internal fun ema(values: DoubleArray, period: Int): DoubleArray {
    val out = DoubleArray(values.size) { Double.NaN }
    val start = values.indexOfFirst { !it.isNaN() }
    if (start < 0 || start + period > values.size) return out
    val alpha = 2.0 / (period + 1)
    var previous = 0.0
    for (k in start until start + period) previous += values[k]
    previous /= period
    out[start + period - 1] = previous
    for (i in start + period until values.size) {
        previous = values[i] * alpha + previous * (1.0 - alpha)
        out[i] = previous
    }
    return out
}

/**
 * Linearly weighted moving average where the newest value of a window weighs [period] and the oldest 1.
 */
internal fun wma(values: DoubleArray, period: Int): DoubleArray {
    val out = DoubleArray(values.size) { Double.NaN }
    val weights = period * (period + 1) / 2.0
    for (i in period - 1 until values.size) {
        var sum = 0.0
        for (k in 0 until period) sum += values[i - period + 1 + k] * (k + 1)
        out[i] = sum / weights
    }
    return out
}

internal fun average(type: MaType, values: DoubleArray, period: Int): DoubleArray = when (type) {
    MaType.Simple -> sma(values, period)
    MaType.Exponential -> ema(values, period)
    MaType.Weighted -> wma(values, period)
}

internal val MaColors: List<Color> = listOf(
    Color(0xFF2EB82E),
    Color(0xFFFF9800),
    Color(0xFF9C27B0),
    Color(0xFF8D6E63),
)

internal val DefaultMaWidth: Dp = 1.dp

/**
 * Moving average lines over the close on the main pane, one line per period counted in candles of the current timeframe.
 *
 * @property periods window length of each line
 * @property type average formula
 * @property colors color of each line, the text color beyond the list
 * @property widths stroke width of each line, 1dp beyond the list
 * @property visible whether each line is drawn, true beyond the list
 */
@Immutable
public class MovingAverage(
    public val periods: List<Int> = listOf(5, 20, 60, 120),
    public val type: MaType = MaType.Simple,
    public val colors: List<Color> = MaColors,
    public val widths: List<Dp> = emptyList(),
    public val visible: List<Boolean> = emptyList(),
) : Overlay() {
    init {
        require(periods.all { it >= 1 }) { "periods must be positive" }
    }

    override val params: List<Any> get() = listOf(periods, type, colors, widths, visible)

    private val shown: List<Int> get() = periods.indices.filter { visible.getOrElse(it) { true } }

    override fun marks(series: Series, settings: ChartSettings): List<Mark> = shown.map {
        LineMark(
            label = "MA${periods[it]}",
            values = average(type, series.close, periods[it]),
            color = colors.getOrElse(it) { Color.Unspecified },
            width = widths.getOrElse(it) { DefaultMaWidth },
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString = buildAnnotatedString {
        shown.forEachIndexed { k, index ->
            val mark = marks[k] as LineMark
            val value = mark.values.getOrElse(i) { Double.NaN }
            withStyle(SpanStyle(color = spec.lineColor(mark.color))) {
                append("${maLegend(periods[index], spec.labels.unit)} ${if (value.isNaN()) "-" else spec.priceFormatter(value)}  ")
            }
        }
    }
}

/**
 * Population standard deviation over windows of [period] values, NaN until the first full window.
 */
internal fun stdev(values: DoubleArray, period: Int): DoubleArray {
    val mean = sma(values, period)
    val out = DoubleArray(values.size) { Double.NaN }
    for (i in period - 1 until values.size) {
        var sum = 0.0
        for (k in i - period + 1..i) sum += (values[k] - mean[i]) * (values[k] - mean[i])
        out[i] = sqrt(sum / period)
    }
    return out
}

private fun windowExtreme(values: DoubleArray, period: Int, better: (Double, Double) -> Boolean): DoubleArray {
    val out = DoubleArray(values.size) { Double.NaN }
    for (i in period - 1 until values.size) {
        var best = values[i]
        for (k in i - period + 1 until i) if (better(values[k], best)) best = values[k]
        out[i] = best
    }
    return out
}

/**
 * Highest value of each window of [period] values, NaN until the first full window.
 */
internal fun highest(values: DoubleArray, period: Int): DoubleArray = windowExtreme(values, period) { a, b -> a > b }

/**
 * Lowest value of each window of [period] values, NaN until the first full window.
 */
internal fun lowest(values: DoubleArray, period: Int): DoubleArray = windowExtreme(values, period) { a, b -> a < b }

/**
 * Wilder smoothing of [period] values, seeded by their simple average from [start] on, NaN before the seed.
 */
internal fun wilder(values: DoubleArray, period: Int, start: Int = 0): DoubleArray {
    val out = DoubleArray(values.size) { Double.NaN }
    if (start + period > values.size) return out
    var previous = 0.0
    for (k in start until start + period) previous += values[k]
    previous /= period
    out[start + period - 1] = previous
    for (i in start + period until values.size) {
        previous = (previous * (period - 1) + values[i]) / period
        out[i] = previous
    }
    return out
}
