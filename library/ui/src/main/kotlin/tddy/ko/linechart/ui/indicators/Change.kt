package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

internal val MomentumColors: List<Color> = listOf(Color(0xFFE53935), Color(0xFFFB8C00))
internal val RocColors: List<Color> = listOf(Color(0xFF1E88E5))
internal val DisparityColors: List<Color> = listOf(Color(0xFF2EB82E), Color(0xFF9C27B0))

/**
 * Close divided by the close [lag] candles earlier, NaN for the first [lag] candles.
 */
internal fun closeRatio(close: DoubleArray, lag: Int): DoubleArray = DoubleArray(close.size) { if (it < lag) Double.NaN else close[it] / close[it - lag] }

/**
 * Close as a percentage of its simple moving average of [period] candles.
 */
internal fun disparity(close: DoubleArray, period: Int): DoubleArray {
    val average = sma(close, period)
    return DoubleArray(close.size) { 100.0 * close[it] / average[it] }
}

/**
 * Close as a percentage of the close [period] candles earlier, with a simple moving average of it as signal.
 *
 * @property colors color of the momentum line and of the signal line
 */
@Immutable
public class Momentum(
    public val period: Int = 10,
    public val signal: Int = 9,
    public val colors: List<Color> = MomentumColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, signal, colors, weight)
    override val scale: PaneScale get() = PaneScale.Auto
    override val levels: DoubleArray get() = doubleArrayOf(100.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val ratio = closeRatio(series.close, period)
        val values = DoubleArray(ratio.size) { 100.0 * ratio[it] }
        return listOf(
            LineMark("", values, colors.at(0), 1.dp),
            LineMark("signal", sma(values, signal), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "mom", "($period,$signal)", marks, i, ::format)
}

/**
 * Rate of change, the percent change of the close against the close [period] candles earlier.
 *
 * @property colors color of the line
 */
@Immutable
public class Roc(
    public val period: Int = 12,
    public val colors: List<Color> = RocColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val scale: PaneScale get() = PaneScale.Around(0.0)
    override val levels: DoubleArray get() = doubleArrayOf(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val ratio = closeRatio(series.close, period)
        return listOf(LineMark("", DoubleArray(ratio.size) { 100.0 * (ratio[it] - 1.0) }, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "roc", "($period)", marks, i, ::format)
}

/**
 * Disparity, the close as a percentage of its simple moving average, one line per period.
 *
 * @property periods window length of each line
 * @property colors color of each line, the text color beyond the list
 */
@Immutable
public class Disparity(
    public val periods: List<Int> = listOf(20, 60),
    public val colors: List<Color> = DisparityColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        require(periods.all { it >= 1 }) { "periods must be positive" }
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(periods, colors, weight)
    override val scale: PaneScale get() = PaneScale.Auto
    override val levels: DoubleArray get() = doubleArrayOf(100.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        periods.mapIndexed { k, period -> LineMark("$period", disparity(series.close, period), colors.at(k), 1.dp) }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "disp", "", marks, i, ::format)
}
