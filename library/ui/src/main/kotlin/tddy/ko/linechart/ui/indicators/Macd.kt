package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

internal val MacdColors: List<Color> = listOf(Color(0xFF1E88E5), Color(0xFFFB8C00))
internal val TrixColors: List<Color> = listOf(Color(0xFFE53935), Color(0xFF1E88E5))

/**
 * Moving average convergence divergence line, its signal line and their difference, NaN until the signal line starts.
 */
private class MacdParts(close: DoubleArray, fast: Int, slow: Int, signal: Int) {
    val line: DoubleArray
    val signal: DoubleArray
    val osc: DoubleArray

    init {
        val quick = ema(close, fast)
        val steady = ema(close, slow)
        line = DoubleArray(close.size) { quick[it] - steady[it] }
        this.signal = ema(line, signal)
        osc = DoubleArray(close.size) { line[it] - this.signal[it] }
    }
}

/**
 * Difference of two exponential moving averages of the close with its signal line and the histogram of their difference.
 *
 * @property colors color of the MACD line and of the signal line
 */
@Immutable
public class Macd(
    public val fast: Int = 12,
    public val slow: Int = 26,
    public val signal: Int = 9,
    public val colors: List<Color> = MacdColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(fast, slow, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(fast, slow, signal, colors, weight)
    override val scale: PaneScale get() = PaneScale.Around(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val parts = MacdParts(series.close, fast, slow, signal)
        return listOf(
            BarMark(parts.osc, Tone.Sign),
            LineMark("macd", parts.line, colors.at(0), 1.dp),
            LineMark("signal", parts.signal, colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "macd", "($fast,$slow,$signal)", marks, i, ::format)
}

/**
 * Histogram of the MACD line minus its signal line.
 */
@Immutable
public class MacdOscillator(
    public val fast: Int = 12,
    public val slow: Int = 26,
    public val signal: Int = 9,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(fast, slow, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(fast, slow, signal, weight)
    override val scale: PaneScale get() = PaneScale.Around(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        listOf(BarMark(MacdParts(series.close, fast, slow, signal).osc, Tone.Sign))

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "macdo", "($fast,$slow,$signal)", marks, i, ::format)
}

/**
 * Percent change of a triple exponential moving average of the close with a simple moving average of it as signal.
 *
 * @property colors color of the TRIX line and of the signal line
 */
@Immutable
public class Trix(
    public val period: Int = 12,
    public val signal: Int = 9,
    public val colors: List<Color> = TrixColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, signal, colors, weight)
    override val scale: PaneScale get() = PaneScale.Around(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val triple = ema(ema(ema(series.close, period), period), period)
        val trix = DoubleArray(triple.size) { if (it == 0) Double.NaN else 100.0 * (triple[it] - triple[it - 1]) / triple[it - 1] }
        return listOf(
            LineMark("", trix, colors.at(0), 1.dp),
            LineMark("signal", sma(trix, signal), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "trix", "($period,$signal)", marks, i, ::format)
}

/**
 * Percent change of the exponential moving average of the close against its value [lag] candles earlier, with a simple moving average of it as signal.
 * Brokers define Sonar differently, this is the percent change form.
 *
 * @property colors color of the Sonar line and of the signal line
 */
@Immutable
public class Sonar(
    public val period: Int = 20,
    public val lag: Int = 9,
    public val signal: Int = 9,
    public val colors: List<Color> = TrixColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period, lag, signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, lag, signal, colors, weight)
    override val scale: PaneScale get() = PaneScale.Around(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val average = ema(series.close, period)
        val sonar = DoubleArray(average.size) { if (it < lag) Double.NaN else 100.0 * (average[it] - average[it - lag]) / average[it - lag] }
        return listOf(
            LineMark("", sonar, colors.at(0), 1.dp),
            LineMark("signal", sma(sonar, signal), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "sonar", "($period,$lag,$signal)", marks, i, ::format)
}
