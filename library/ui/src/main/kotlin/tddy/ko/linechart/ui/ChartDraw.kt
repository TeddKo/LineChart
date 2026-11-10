package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

internal val ChartTextStyle = TextStyle(fontSize = 10.sp, fontFeatureSettings = "tnum")

/**
 * Geometry of the last main-layer draw, shared with the overlay layer and the gestures.
 */
internal class Frame(val layout: PaneLayout, val range: YRange, val scroll: Float, val slot: Float)

/**
 * Display configuration and reusable draw resources of one panel.
 */
internal class ChartSpec(
    val type: ChartType,
    val colors: ChartColors,
    val maPeriods: List<Int>,
    val timeframe: Timeframe?,
    val priceFormatter: (Double) -> String,
    val timeFormatter: ((Long) -> String)?,
    val textStyle: TextStyle,
    val measurer: TextMeasurer,
) {
    val path: Path = Path()
    private val formats = HashMap<String, SimpleDateFormat>()

    fun time(pattern: String, millis: Long): String =
        formats.getOrPut(pattern) { SimpleDateFormat(pattern, Locale.ROOT) }.format(Date(millis))
}

private class View(val scroll: Float, val slot: Float, val visible: IntRange)

private fun LineChartState.view(plotW: Float): View {
    val slot = slotFor(plotW)
    val scroll = clampScroll(scroll, itemCount, plotW, slot)
    return View(scroll, slot, visibleRange(itemCount, scroll, plotW, slot))
}

private fun ChartSpec.axisLabel(series: Series, i: Int): String {
    if (!series.timeAxis) return (i + 1).toString()
    val date = series.items[i].date
    timeFormatter?.let { return it(date) }
    val pattern = timePattern(timeframe, series.stepMs)
    val newDay = pattern.startsWith("HH") && i > 0 && time("yyyyMMdd", date) != time("yyyyMMdd", series.items[i - 1].date)
    return time(if (newDay) "MM/dd" else pattern, date)
}

internal fun DrawScope.drawMain(state: LineChartState, spec: ChartSpec) {
    val d = density
    state.density = d
    val colors = spec.colors
    drawRect(colors.background)
    val series = state.series
    val items = series.items
    val n = items.size
    val style = spec.textStyle.copy(color = colors.text)
    val lineH = spec.measurer.measure("0", spec.textStyle).size.height.toFloat()
    val headerH = (if (spec.maPeriods.isEmpty()) 1 else 2) * lineH + 6f * d
    val timeH = lineH + 4f * d
    if (n < 2) {
        state.plot = Rect.Zero
        state.frame = null
        return
    }
    val guessW = size.width - if (state.axisW > 0f) state.axisW else 48f * d
    val guessRange = visibleYRange(items, state.view(guessW).visible, spec.type)
    val labelW = niceTicks(guessRange.min, guessRange.max, state.yAxisTicksCount)
        .maxOf { spec.measurer.measure(spec.priceFormatter(it), style).size.width }
    state.axisW = axisWidth(labelW.toFloat(), d)
    val layout = paneLayout(size.width, size.height, headerH, timeH, state.axisW, 4f * d, series.hasVolume)
    val main = layout.main
    state.plot = main
    val view = state.view(main.width)
    val range = visibleYRange(items, view.visible, spec.type)
    state.frame = Frame(layout, range, view.scroll, view.slot)

    for (tick in niceTicks(range.min, range.max, state.yAxisTicksCount)) {
        val y = yOf(tick, range, main.top, main.height)
        drawLine(colors.grid, Offset(0f, y), Offset(main.right, y), d)
        drawText(spec.measurer, spec.priceFormatter(tick), Offset(main.right + 4f * d, y - lineH / 2f), style)
    }

    val widest = spec.axisLabel(series, n - 1)
    val labelWidth = spec.measurer.measure(widest, style).size.width
    val stride = labelStride(view.slot, labelWidth + 8f * d)
    for (i in view.visible) {
        if (i % stride != 0) continue
        val x = xOf(i, n, view.scroll, main.width, view.slot)
        if (x < 0f || x > main.right) continue
        val text = spec.axisLabel(series, i)
        val w = spec.measurer.measure(text, style).size.width
        drawText(spec.measurer, text, Offset((x - w / 2f).coerceIn(0f, max(0f, main.right - w)), layout.timeAxis.top + 2f * d), style)
    }

    clipRect(0f, 0f, main.width * state.entry.value, size.height) {
        clipRect(main.left, main.top, main.right, main.bottom) {
            drawSeries(state, spec, view, range, main)
        }
    }
}

private fun DrawScope.drawSeries(
    state: LineChartState,
    spec: ChartSpec,
    view: View,
    range: YRange,
    main: Rect,
) {
    val items = state.items
    val n = items.size
    val from = (view.visible.first - 1).coerceAtLeast(0)
    val to = (view.visible.last + 1).coerceAtMost(n - 1)
    val path = spec.path
    path.reset()
    var px = 0f
    var py = 0f
    var firstX = 0f
    for (i in from..to) {
        val x = xOf(i, n, view.scroll, main.width, view.slot)
        val y = yOf(items[i].cumulativeProfit, range, main.top, main.height)
        if (i == from) {
            path.moveTo(x, y)
            firstX = x
        } else {
            val mid = (px + x) / 2f
            path.cubicTo(mid, py, mid, y, x, y)
        }
        px = x
        py = y
    }
    val color = spec.colors.forDirection(items.last().cumulativeProfit.compareTo(items.first().cumulativeProfit))
    drawPath(path, color, style = Stroke(state.strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (spec.type == ChartType.Area) {
        path.lineTo(px, main.bottom)
        path.lineTo(firstX, main.bottom)
        path.close()
        drawPath(
            path,
            Brush.verticalGradient(listOf(color.copy(alpha = AREA_ALPHA), Color.Transparent), main.top, main.bottom),
            style = Fill,
        )
    }
}

private const val AREA_ALPHA = 0.25f
