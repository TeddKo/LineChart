package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

internal val VolumeMaColors: List<Color> = listOf(Color(0xFFFF9800), Color(0xFF9C27B0))

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

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val bars = BarMark(series.volume, Tone.Direction)
        if (!settings.volumeMa) return listOf(bars)
        return listOf(bars) + maPeriods.mapIndexed { k, period ->
            LineMark("MA$period", sma(series.volume, period), maColors.getOrElse(k) { Color.Unspecified }, DefaultMaWidth)
        }
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString = buildAnnotatedString {
        val tone = spec.colors.forDirection(direction(series.items, i, spec.type))
        withStyle(SpanStyle(color = spec.colors.text)) { append("${spec.labels.volume} ") }
        withStyle(SpanStyle(color = tone)) { append("${formatCompact(series.volume[i])}  ") }
        marks.drop(1).forEachIndexed { k, mark ->
            mark as LineMark
            val value = mark.values.getOrElse(i) { Double.NaN }
            withStyle(SpanStyle(color = spec.lineColor(mark.color))) {
                append("${maLegend(maPeriods[k], spec.labels.unit)} ${if (value.isNaN()) "-" else formatCompact(value)}  ")
            }
        }
    }
}
