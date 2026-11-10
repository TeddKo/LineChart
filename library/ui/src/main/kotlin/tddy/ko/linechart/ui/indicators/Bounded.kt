package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

internal val RsiColors: List<Color> = listOf(Color(0xFF8E24AA), Color(0xFFFB8C00))
internal val StochasticColors: List<Color> = listOf(Color(0xFFE53935), Color(0xFF1E88E5))
internal val WilliamsColors: List<Color> = listOf(Color(0xFF8E24AA))
internal val FlowColors: List<Color> = listOf(Color(0xFF00897B))
internal val PsychologicalColors: List<Color> = listOf(Color(0xFF8D6E63))

private val PercentScale = PaneScale.Fixed(0.0, 100.0)

/**
 * Relative strength index with Wilder smoothing, NaN for the first [period] values, 100 when nothing fell and 50 when nothing moved.
 */
internal fun rsi(close: DoubleArray, period: Int): DoubleArray {
    val gain = DoubleArray(close.size) { if (it == 0) Double.NaN else maxOf(close[it] - close[it - 1], 0.0) }
    val loss = DoubleArray(close.size) { if (it == 0) Double.NaN else maxOf(close[it - 1] - close[it], 0.0) }
    val up = wilder(gain, period, 1)
    val down = wilder(loss, period, 1)
    return DoubleArray(close.size) {
        when {
            up[it].isNaN() -> Double.NaN
            down[it] == 0.0 -> if (up[it] == 0.0) 50.0 else 100.0
            else -> 100.0 - 100.0 / (1.0 + up[it] / down[it])
        }
    }
}

/**
 * Close within the [period] range of highs and lows as 0 to 100, 50 for a flat range.
 */
internal fun stochasticK(high: DoubleArray, low: DoubleArray, close: DoubleArray, period: Int): DoubleArray {
    val top = highest(high, period)
    val bottom = lowest(low, period)
    return DoubleArray(close.size) {
        when {
            top[it].isNaN() -> Double.NaN
            top[it] == bottom[it] -> 50.0
            else -> 100.0 * (close[it] - bottom[it]) / (top[it] - bottom[it])
        }
    }
}

/**
 * Relative strength index with Wilder smoothing and a simple moving average of it as signal.
 *
 * @property colors color of the RSI line and of the signal line
 */
@Immutable
public class Rsi(
    public val period: Int = 14,
    public val signal: Int = 6,
    public val colors: List<Color> = RsiColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, signal, colors, weight)
    override val scale: PaneScale get() = PercentScale
    override val levels: DoubleArray get() = doubleArrayOf(30.0, 70.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val rsi = rsi(series.close, period)
        return listOf(
            LineMark("", rsi, colors.at(0), 1.dp),
            LineMark("signal", sma(rsi, signal), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "rsi", "($period,$signal)", marks, i, ::format)
}

/**
 * Fast stochastic oscillator, %K of the last [k] candles and %D as its [d] candle simple average.
 *
 * @property colors color of %K and of %D
 */
@Immutable
public class StochasticFast(
    public val k: Int = 5,
    public val d: Int = 3,
    public val colors: List<Color> = StochasticColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(k, d)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(k, d, colors, weight)
    override val scale: PaneScale get() = PercentScale
    override val levels: DoubleArray get() = doubleArrayOf(20.0, 80.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val fast = stochasticK(series.high, series.low, series.close, k)
        return listOf(
            LineMark("%K", fast, colors.at(0), 1.dp),
            LineMark("%D", sma(fast, d), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "stochf", "($k,$d)", marks, i, ::format)
}

/**
 * Slow stochastic oscillator, %K smoothed over [smooth] candles and %D as its [d] candle simple average.
 *
 * @property colors color of slow %K and of slow %D
 */
@Immutable
public class StochasticSlow(
    public val k: Int = 5,
    public val smooth: Int = 3,
    public val d: Int = 3,
    public val colors: List<Color> = StochasticColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(k, smooth, d)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(k, smooth, d, colors, weight)
    override val scale: PaneScale get() = PercentScale
    override val levels: DoubleArray get() = doubleArrayOf(20.0, 80.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val slow = sma(stochasticK(series.high, series.low, series.close, k), smooth)
        return listOf(
            LineMark("%K", slow, colors.at(0), 1.dp),
            LineMark("%D", sma(slow, d), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "stochs", "($k,$smooth,$d)", marks, i, ::format)
}

/**
 * Williams %R, the close within the [period] range of highs and lows as -100 to 0, -50 for a flat range.
 *
 * @property colors color of the line
 */
@Immutable
public class WilliamsR(
    public val period: Int = 14,
    public val colors: List<Color> = WilliamsColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val scale: PaneScale get() = PaneScale.Fixed(-100.0, 0.0)
    override val levels: DoubleArray get() = doubleArrayOf(-20.0, -80.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val top = highest(series.high, period)
        val bottom = lowest(series.low, period)
        val values = DoubleArray(series.close.size) {
            when {
                top[it].isNaN() -> Double.NaN
                top[it] == bottom[it] -> -50.0
                else -> -100.0 * (top[it] - series.close[it]) / (top[it] - bottom[it])
            }
        }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "willr", "($period)", marks, i, ::format)
}

/**
 * Money flow index, the share of positive money flow of the typical price times volume over [period] candles as 0 to 100.
 *
 * @property colors color of the line
 */
@Immutable
public class Mfi(
    public val period: Int = 14,
    public val colors: List<Color> = FlowColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PercentScale
    override val levels: DoubleArray get() = doubleArrayOf(20.0, 80.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val n = series.close.size
        val typical = DoubleArray(n) { (series.high[it] + series.low[it] + series.close[it]) / 3.0 }
        val positive = DoubleArray(n) { if (it > 0 && typical[it] > typical[it - 1]) typical[it] * series.volume[it] else 0.0 }
        val negative = DoubleArray(n) { if (it > 0 && typical[it] < typical[it - 1]) typical[it] * series.volume[it] else 0.0 }
        val values = DoubleArray(n) { i ->
            if (i < period) {
                Double.NaN
            } else {
                val up = (i - period + 1..i).sumOf { positive[it] }
                val down = (i - period + 1..i).sumOf { negative[it] }
                if (down == 0.0) (if (up == 0.0) 50.0 else 100.0) else 100.0 - 100.0 / (1.0 + up / down)
            }
        }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "mfi", "($period)", marks, i, ::format)
}

/**
 * Psychological line, the share of rising closes among the last [period] candles as 0 to 100.
 *
 * @property colors color of the line
 */
@Immutable
public class Psychological(
    public val period: Int = 10,
    public val colors: List<Color> = PsychologicalColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val scale: PaneScale get() = PercentScale
    override val levels: DoubleArray get() = doubleArrayOf(25.0, 75.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val close = series.close
        val values = DoubleArray(close.size) { i ->
            if (i < period) Double.NaN else 100.0 * (i - period + 1..i).count { close[it] > close[it - 1] } / period
        }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "psy", "($period)", marks, i, ::format)
}
