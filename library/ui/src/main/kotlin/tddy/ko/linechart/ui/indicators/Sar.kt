package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlin.math.max
import kotlin.math.min

/**
 * Stop values of [values] with [above] set where the stop lies above the candle, NaN for the first candle.
 */
internal class SarResult(val values: DoubleArray, val above: BooleanArray)

/**
 * Wilder parabolic stop and reverse starting in the direction of the first close change, the acceleration growing by [step] up to [maxStep].
 */
internal fun parabolicSar(high: DoubleArray, low: DoubleArray, close: DoubleArray, step: Double, maxStep: Double): SarResult {
    val n = close.size
    val values = DoubleArray(n) { Double.NaN }
    val above = BooleanArray(n)
    if (n < 2) return SarResult(values, above)
    var up = close[1] >= close[0]
    var sar = if (up) low[0] else high[0]
    var extreme = if (up) high[0] else low[0]
    var factor = step
    for (i in 1 until n) {
        var next = sar + factor * (extreme - sar)
        if (up) {
            next = min(next, if (i >= 2) min(low[i - 1], low[i - 2]) else low[i - 1])
            if (low[i] < next) {
                up = false
                next = extreme
                extreme = low[i]
                factor = step
            } else if (high[i] > extreme) {
                extreme = high[i]
                factor = min(factor + step, maxStep)
            }
        } else {
            next = max(next, if (i >= 2) max(high[i - 1], high[i - 2]) else high[i - 1])
            if (high[i] > next) {
                up = true
                next = extreme
                extreme = high[i]
                factor = step
            } else if (low[i] < extreme) {
                extreme = low[i]
                factor = min(factor + step, maxStep)
            }
        }
        sar = next
        values[i] = next
        above[i] = !up
    }
    return SarResult(values, above)
}

/**
 * Parabolic SAR dots below the candles in an uptrend and above them in a downtrend.
 *
 * @property step acceleration factor increment
 * @property max largest acceleration factor
 */
@Immutable
public class ParabolicSar(
    public val step: Double = 0.02,
    public val max: Double = 0.2,
) : Overlay() {
    init {
        require(step > 0.0) { "step must be positive" }
        require(max >= step) { "max must not be below step" }
    }

    override val params: List<Any> get() = listOf(step, max)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val sar = parabolicSar(series.high, series.low, series.close, step, max)
        return listOf(DotMark(sar.values, sar.above))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString = buildAnnotatedString {
        val dots = marks[0] as DotMark
        val value = dots.values.getOrElse(i) { Double.NaN }
        withStyle(SpanStyle(color = spec.colors.text)) { append("${spec.labels.name("sar")}($step,$max)  ") }
        val color = if (dots.above.getOrElse(i) { false }) spec.colors.down else spec.colors.up
        withStyle(SpanStyle(color = color)) { append("${if (value.isNaN()) "-" else spec.priceFormatter(value)}  ") }
    }
}
