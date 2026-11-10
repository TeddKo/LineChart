package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max

internal val CciColors: List<Color> = listOf(Color(0xFF00897B))
internal val AtrColors: List<Color> = listOf(Color(0xFF5E35B1))

private const val CCI_FACTOR = 0.015

/**
 * True range of each candle, the high minus the low for the first one.
 */
internal fun trueRange(high: DoubleArray, low: DoubleArray, close: DoubleArray): DoubleArray = DoubleArray(close.size) {
    if (it == 0) high[0] - low[0] else max(high[it] - low[it], max(abs(high[it] - close[it - 1]), abs(low[it] - close[it - 1])))
}

/**
 * Mean absolute deviation of each window of [period] values from the window mean, NaN until the first full window.
 */
internal fun meanDeviation(values: DoubleArray, period: Int): DoubleArray {
    val mean = sma(values, period)
    val out = DoubleArray(values.size) { Double.NaN }
    for (i in period - 1 until values.size) {
        var sum = 0.0
        for (k in i - period + 1..i) sum += abs(values[k] - mean[i])
        out[i] = sum / period
    }
    return out
}

/**
 * Commodity channel index of the typical price, scaled so that about 70 to 80 percent of values fall within plus and minus 100.
 *
 * @property colors color of the line
 */
@Immutable
public class Cci(
    public val period: Int = 20,
    public val colors: List<Color> = CciColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val scale: PaneScale get() = PaneScale.Auto
    override val levels: DoubleArray get() = doubleArrayOf(-100.0, 100.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val typical = DoubleArray(series.close.size) { (series.high[it] + series.low[it] + series.close[it]) / 3.0 }
        val mean = sma(typical, period)
        val deviation = meanDeviation(typical, period)
        val values = DoubleArray(typical.size) {
            if (mean[it].isNaN()) Double.NaN else if (deviation[it] == 0.0) 0.0 else (typical[it] - mean[it]) / (CCI_FACTOR * deviation[it])
        }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "cci", "($period)", marks, i, ::format)
}

/**
 * Directional movement index with Wilder smoothing, the +DI and -DI lines and the ADX of their spread.
 */
@Immutable
public class Dmi(
    public val period: Int = 14,
    public val adx: Int = 14,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period, adx)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, adx, weight)
    override val scale: PaneScale get() = PaneScale.FromZero

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val n = series.close.size
        val high = series.high
        val low = series.low
        val range = trueRange(high, low, series.close)
        val up = DoubleArray(n) { if (it == 0) Double.NaN else high[it] - high[it - 1] }
        val down = DoubleArray(n) { if (it == 0) Double.NaN else low[it - 1] - low[it] }
        val plus = DoubleArray(n) { if (it == 0) Double.NaN else if (up[it] > down[it] && up[it] > 0.0) up[it] else 0.0 }
        val minus = DoubleArray(n) { if (it == 0) Double.NaN else if (down[it] > up[it] && down[it] > 0.0) down[it] else 0.0 }
        val smoothRange = wilder(range, period, 1)
        val smoothPlus = wilder(plus, period, 1)
        val smoothMinus = wilder(minus, period, 1)
        fun di(smooth: DoubleArray) = DoubleArray(n) { if (smoothRange[it].isNaN()) Double.NaN else if (smoothRange[it] == 0.0) 0.0 else 100.0 * smooth[it] / smoothRange[it] }
        val plusDi = di(smoothPlus)
        val minusDi = di(smoothMinus)
        val dx = DoubleArray(n) {
            if (plusDi[it].isNaN()) Double.NaN else if (plusDi[it] + minusDi[it] == 0.0) 0.0 else 100.0 * abs(plusDi[it] - minusDi[it]) / (plusDi[it] + minusDi[it])
        }
        return listOf(
            LineMark("+DI", plusDi, Color.Unspecified, 1.dp, direction = 1),
            LineMark("-DI", minusDi, Color.Unspecified, 1.dp, direction = -1),
            LineMark("ADX", wilder(dx, adx, period), Color.Unspecified, 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "dmi", "($period,$adx)", marks, i, ::format)
}

/**
 * Average true range with Wilder smoothing.
 *
 * @property colors color of the line
 */
@Immutable
public class Atr(
    public val period: Int = 14,
    public val colors: List<Color> = AtrColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val scale: PaneScale get() = PaneScale.Auto

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        listOf(LineMark("", wilder(trueRange(series.high, series.low, series.close), period), colors.at(0), 1.dp))

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "atr", "($period)", marks, i, ::format)
}
