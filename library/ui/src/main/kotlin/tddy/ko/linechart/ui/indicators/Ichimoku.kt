package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

internal val IchimokuColors: List<Color> = listOf(Color(0xFF1E88E5), Color(0xFFFB8C00), Color(0xFF8E24AA))

private const val SPAN_ALPHA = 0.6f
private const val CLOUD_ALPHA = 0.12f

/**
 * Middle of the highest high and the lowest low over windows of [period] candles, NaN until the first full window.
 */
internal fun midpoint(high: DoubleArray, low: DoubleArray, period: Int): DoubleArray {
    val top = highest(high, period)
    val bottom = lowest(low, period)
    return DoubleArray(high.size) { (top[it] + bottom[it]) / 2.0 }
}

/**
 * Ichimoku cloud with the conversion, base and lagging lines and the two leading spans shifted forward by `shift - 1` candles, the cloud filled in the up color while span A is above span B and in the down color otherwise.
 *
 * @property colors color of the conversion, base and lagging lines
 */
@Immutable
public class Ichimoku(
    public val conversion: Int = 9,
    public val base: Int = 26,
    public val span: Int = 52,
    public val shift: Int = 26,
    public val colors: List<Color> = IchimokuColors,
) : Overlay() {
    init {
        requirePositive(conversion, base, span, shift)
    }

    override val params: List<Any> get() = listOf(conversion, base, span, shift, colors)
    override val rightPad: Int get() = shift - 1

    override fun marks(series: Series, settings: ChartSettings): List<Mark> {
        val conversionLine = midpoint(series.high, series.low, conversion)
        val baseLine = midpoint(series.high, series.low, base)
        val spanA = DoubleArray(conversionLine.size) { (conversionLine[it] + baseLine[it]) / 2.0 }
        val spanB = midpoint(series.high, series.low, span)
        val forward = shift - 1
        return listOf(
            FillMark(spanA, spanB, null, forward, CLOUD_ALPHA),
            LineMark("conv", conversionLine, colors.at(0), 1.dp),
            LineMark("base", baseLine, colors.at(1), 1.dp),
            LineMark("lag", series.close, colors.at(2), 1.dp, offset = -forward),
            LineMark("span1", spanA, Color.Unspecified, 1.dp, offset = forward, direction = 1, alpha = SPAN_ALPHA),
            LineMark("span2", spanB, Color.Unspecified, 1.dp, offset = forward, direction = -1, alpha = SPAN_ALPHA),
        )
    }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "ichimoku", "($conversion,$base,$span)", marks, i, spec.priceFormatter)
}
