package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal val VolumeMaColors: List<Color> = listOf(Color(0xFFFF9800), Color(0xFF9C27B0))
internal val ObvColors: List<Color> = listOf(Color(0xFF1E88E5), Color(0xFFFB8C00))
internal val RatioColors: List<Color> = listOf(Color(0xFF00897B))
internal val OscillatorColors: List<Color> = listOf(Color(0xFF5E35B1))

private fun volumeMarks(values: DoubleArray, periods: List<Int>, colors: List<Color>, settings: ChartSettings): List<Mark> {
    val bars = BarMark(values, Tone.Direction)
    if (!settings.volumeMa) return listOf(bars)
    return listOf(bars) + periods.mapIndexed { k, period ->
        LineMark("MA$period", sma(values, period), colors.at(k), DefaultMaWidth)
    }
}

private fun volumeLegend(spec: ChartSpec, series: Series, title: String, values: DoubleArray, marks: List<Mark>, periods: List<Int>, i: Int): AnnotatedString = buildAnnotatedString {
    val tone = spec.colors.forDirection(direction(series.items, i, spec.type))
    withStyle(SpanStyle(color = spec.colors.text)) { append("$title ") }
    withStyle(SpanStyle(color = tone)) { append("${formatCompact(values[i])}  ") }
    marks.drop(1).forEachIndexed { k, mark ->
        mark as LineMark
        val value = mark.values.getOrElse(i) { Double.NaN }
        withStyle(SpanStyle(color = spec.lineColor(mark.color))) {
            append("${maLegend(periods[k], spec.labels.unit)} ${if (value.isNaN()) "-" else formatCompact(value)}  ")
        }
    }
}

/**
 * Volume bars colored by candle direction in a pane of their own, with moving averages of the volume.
 *
 * @property maPeriods window length of each moving average line
 * @property maColors color of each moving average line, the text color beyond the list
 */
@Immutable
public class Volume(
    public val maPeriods: List<Int> = listOf(5, 20),
    public val maColors: List<Color> = VolumeMaColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        require(maPeriods.all { it >= 1 }) { "maPeriods must be positive" }
        require(weight > 0f) { "weight must be positive" }
    }

    override val params: List<Any> get() = listOf(maPeriods, maColors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PaneScale.FromZero
    override val compact: Boolean get() = true

    override fun marks(series: Series, settings: ChartSettings): List<Mark> = volumeMarks(series.volume, maPeriods, maColors, settings)

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        volumeLegend(spec, series, spec.labels.volume, series.volume, marks, maPeriods, i)
}

/**
 * Traded value bars colored by candle direction, the value of each candle where it has one and the close times the volume otherwise, with moving averages.
 *
 * @property maPeriods window length of each moving average line
 * @property maColors color of each moving average line, the text color beyond the list
 */
@Immutable
public class TradingValue(
    public val maPeriods: List<Int> = listOf(5, 20),
    public val maColors: List<Color> = VolumeMaColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        require(maPeriods.all { it >= 1 }) { "maPeriods must be positive" }
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(maPeriods, maColors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PaneScale.FromZero
    override val compact: Boolean get() = true

    override fun marks(series: Series, settings: ChartSettings): List<Mark> = volumeMarks(series.tradedValue, maPeriods, maColors, settings)

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        volumeLegend(spec, series, spec.labels.name("tv"), series.tradedValue, marks, maPeriods, i)
}

/**
 * On balance volume, the running sum of the volume signed by the close change, with a simple moving average as signal.
 *
 * @property colors color of the OBV line and of the signal line
 */
@Immutable
public class Obv(
    public val signal: Int = 9,
    public val colors: List<Color> = ObvColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(signal)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(signal, colors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PaneScale.Auto
    override val compact: Boolean get() = true

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val close = series.close
        val obv = DoubleArray(close.size)
        for (i in 1 until close.size) obv[i] = obv[i - 1] + series.volume[i] * close[i].compareTo(close[i - 1])
        return listOf(
            LineMark("", obv, colors.at(0), 1.dp),
            LineMark("signal", sma(obv, signal), colors.at(1), 1.dp),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "obv", "($signal)", marks, i, ::format)
}

/**
 * Volume ratio, the volume of rising candles plus half of the unchanged ones against the falling ones plus half of the unchanged ones over [period] candles, in percent.
 *
 * @property colors color of the line
 */
@Immutable
public class VolumeRatio(
    public val period: Int = 20,
    public val colors: List<Color> = RatioColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(period)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(period, colors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PaneScale.Auto
    override val levels: DoubleArray get() = doubleArrayOf(70.0, 450.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val close = series.close
        val values = DoubleArray(close.size) { i ->
            if (i < period) return@DoubleArray Double.NaN
            var rising = 0.0
            var falling = 0.0
            var flat = 0.0
            for (k in i - period + 1..i) {
                when {
                    close[k] > close[k - 1] -> rising += series.volume[k]
                    close[k] < close[k - 1] -> falling += series.volume[k]
                    else -> flat += series.volume[k]
                }
            }
            if (falling + flat / 2.0 == 0.0) Double.NaN else 100.0 * (rising + flat / 2.0) / (falling + flat / 2.0)
        }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "vr", "($period)", marks, i, ::format)
}

/**
 * Volume oscillator, the percent difference of a short and a long exponential moving average of the volume.
 *
 * @property colors color of the line
 */
@Immutable
public class VolumeOscillator(
    public val short: Int = 5,
    public val long: Int = 20,
    public val colors: List<Color> = OscillatorColors,
    weight: Float = 1f,
) : PaneIndicator(weight) {
    init {
        requirePositive(short, long)
        requireWeight(weight)
    }

    override val params: List<Any> get() = listOf(short, long, colors, weight)
    override val needsVolume: Boolean get() = true
    override val scale: PaneScale get() = PaneScale.Around(0.0)

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val quick = ema(series.volume, short)
        val steady = ema(series.volume, long)
        val values = DoubleArray(quick.size) { if (steady[it] == 0.0) Double.NaN else 100.0 * (quick[it] - steady[it]) / steady[it] }
        return listOf(LineMark("", values, colors.at(0), 1.dp))
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "vo", "($short,$long)", marks, i, ::format)
}
