package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import java.util.TimeZone

internal val VwapColors: List<Color> = listOf(Color(0xFFEC407A))

private val PivotNames = listOf("P", "R1", "S1", "R2", "S2")

/**
 * Index ranges of the local days of an intraday [series], the whole series as one range otherwise.
 */
private fun sessions(series: Series, zone: TimeZone): List<IntRange> {
    val n = series.items.size
    if (n == 0) return emptyList()
    if (!series.intraday) return listOf(0 until n)
    val out = ArrayList<IntRange>()
    var start = 0
    for (i in 1..n) {
        if (i == n || localDay(series.items[i].date, zone) != localDay(series.items[start].date, zone)) {
            out += start until i
            start = i
        }
    }
    return out
}

/**
 * Pivot, first resistance, first support, second resistance and second support of every candle, from the previous local day of an intraday [series] and the previous candle otherwise, NaN where there is none.
 */
internal fun pivotLevels(series: Series, zone: TimeZone): List<DoubleArray> {
    val n = series.items.size
    val out = List(PivotNames.size) { DoubleArray(n) { Double.NaN } }
    val days = if (series.intraday) sessions(series, zone) else List(n) { it..it }
    for (k in 1 until days.size) {
        val previous = days[k - 1]
        val high = previous.maxOf { series.high[it] }
        val low = previous.minOf { series.low[it] }
        val pivot = (high + low + series.close[previous.last]) / 3.0
        val levels = doubleArrayOf(pivot, 2.0 * pivot - low, 2.0 * pivot - high, pivot + high - low, pivot - (high - low))
        for (i in days[k]) for (j in levels.indices) out[j][i] = levels[j]
    }
    return out
}

/**
 * Cumulative volume weighted average of the typical price, restarting each local day of an intraday [series], NaN while the volume is zero.
 */
internal fun vwap(series: Series, zone: TimeZone): DoubleArray {
    val out = DoubleArray(series.items.size) { Double.NaN }
    for (day in sessions(series, zone)) {
        var priced = 0.0
        var traded = 0.0
        for (i in day) {
            priced += (series.high[i] + series.low[i] + series.close[i]) / 3.0 * series.volume[i]
            traded += series.volume[i]
            if (traded > 0.0) out[i] = priced / traded
        }
    }
    return out
}

/**
 * Pivot point with two resistance and two support levels of the previous session as step lines, outside the price range.
 */
@Immutable
public class Pivot : Overlay() {
    override val params: List<Any> get() = emptyList()
    override val inScale: Boolean get() = false

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        pivotLevels(series, TimeZone.getDefault()).mapIndexed { k, values ->
            LineMark(PivotNames[k], values, Color.Unspecified, 1.dp, step = true, direction = if (k == 0) 0 else if (k % 2 == 1) 1 else -1)
        }

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "pivot", "", marks, i, spec.priceFormatter)
}

/**
 * Volume weighted average price, restarting each local day on intraday data.
 *
 * @property colors color of the line
 */
@Immutable
public class Vwap(public val colors: List<Color> = VwapColors) : Overlay() {
    override val params: List<Any> get() = listOf(colors)
    override val needsVolume: Boolean get() = true

    override fun marks(series: Series, settings: ChartSettings): List<Mark> =
        listOf(LineMark("", vwap(series, TimeZone.getDefault()), colors.at(0), 1.dp))

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "vwap", "", marks, i, spec.priceFormatter)
}
