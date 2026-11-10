package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

internal val BollingerColors: List<Color> = listOf(Color(0xFFFF9800), Color(0xFF7E57C2))
internal val EnvelopeColors: List<Color> = listOf(Color(0xFF8D6E63), Color(0xFF26A69A))
internal val ChannelColors: List<Color> = listOf(Color(0xFF00897B))

private const val BAND_FILL_ALPHA = 0.08f

private fun requirePeriod(period: Int) = require(period >= 1) { "period must be positive" }

/**
 * Filled band of [mid] plus and minus [spread] with the middle line, [colors] holding the middle and the band color.
 */
private fun bandMarks(mid: DoubleArray, spread: DoubleArray, colors: List<Color>): List<Mark> {
    val band = colors.getOrElse(1) { Color.Unspecified }
    val upper = DoubleArray(mid.size) { mid[it] + spread[it] }
    val lower = DoubleArray(mid.size) { mid[it] - spread[it] }
    return listOf(
        FillMark(upper, lower, band.copy(alpha = BAND_FILL_ALPHA)),
        LineMark("mid", mid, colors.getOrElse(0) { Color.Unspecified }, 1.dp),
        LineMark("upper", upper, band, 1.dp),
        LineMark("lower", lower, band, 1.dp),
    )
}

/**
 * Simple moving average of the close with bands [k] population standard deviations away.
 *
 * @property colors color of the middle line and of the bands
 */
@Immutable
public class BollingerBands(
    public val period: Int = 20,
    public val k: Double = 2.0,
    public val colors: List<Color> = BollingerColors,
) : Overlay() {
    init {
        requirePeriod(period)
        require(k > 0.0) { "k must be positive" }
    }

    override val params: List<Any> get() = listOf(period, k, colors)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        bandMarks(sma(series.close, period), stdev(series.close, period).map { it * k }.toDoubleArray(), colors)

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "bb", "($period,$k)", marks, i, spec.priceFormatter)
}

/**
 * Simple moving average of the close with bands [percent] percent above and below it.
 *
 * @property colors color of the middle line and of the bands
 */
@Immutable
public class Envelope(
    public val period: Int = 20,
    public val percent: Double = 6.0,
    public val colors: List<Color> = EnvelopeColors,
) : Overlay() {
    init {
        requirePeriod(period)
        require(percent > 0.0) { "percent must be positive" }
    }

    override val params: List<Any> get() = listOf(period, percent, colors)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val mid = sma(series.close, period)
        return bandMarks(mid, DoubleArray(mid.size) { mid[it] * percent / 100.0 }, colors)
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "env", "($period,$percent)", marks, i, spec.priceFormatter)
}

/**
 * Highest high and lowest low of the [period] candles before each candle, the current one excluded.
 *
 * @property colors color of both lines
 */
@Immutable
public class PriceChannel(
    public val period: Int = 20,
    public val colors: List<Color> = ChannelColors,
) : Overlay() {
    init {
        requirePeriod(period)
    }

    override val params: List<Any> get() = listOf(period, colors)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val color = colors.getOrElse(0) { Color.Unspecified }
        fun previous(values: DoubleArray) = DoubleArray(values.size) { if (it == 0) Double.NaN else values[it - 1] }
        return listOf(
            LineMark("upper", previous(highest(series.high, period)), color, 1.dp),
            LineMark("lower", previous(lowest(series.low, period)), color, 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "pc", "($period)", marks, i, spec.priceFormatter)
}
