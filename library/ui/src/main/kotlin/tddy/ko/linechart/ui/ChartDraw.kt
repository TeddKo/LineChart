package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

internal val ChartTextStyle = TextStyle(fontSize = 10.sp, fontFeatureSettings = "tnum")

/**
 * Geometry of the last main-layer draw, shared with the overlay layer and the gestures.
 */
internal class Frame(
    val layout: PaneLayout,
    val range: YRange,
    val paneRanges: List<YRange>,
    val scroll: Float,
    val slot: Float,
    val computed: Computed,
)

/**
 * Display configuration and reusable draw resources of one panel, including the header and legend text of the last drawn [cachedIndex] of [cachedSeries] and [cachedComputed].
 */
internal class ChartSpec(
    val type: ChartType,
    val colors: ChartColors,
    val timeframe: Timeframe?,
    val priceFormatter: (Double) -> String,
    val timeFormatter: ((Long) -> String)?,
    val textStyle: TextStyle,
    val measurer: TextMeasurer,
    val labels: ChartLabels,
    val paneHeight: Dp,
) {
    val path: Path = Path()
    val fillPath: Path = Path()
    var cachedSeries: Series? = null
    var cachedComputed: Computed? = null
    var cachedIndex: Int = -1
    var header: AnnotatedString = AnnotatedString("")
    var overlayLegends: List<AnnotatedString> = emptyList()
    var paneLegends: List<AnnotatedString> = emptyList()
    private var dashDensity = 0f
    private var dashEffect: PathEffect? = null
    private val formats = HashMap<String, SimpleDateFormat>()

    fun lineColor(color: Color): Color = if (color == Color.Unspecified) colors.text else color

    fun dash(density: Float): PathEffect {
        if (dashEffect == null || dashDensity != density) {
            dashDensity = density
            dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f * density, 4f * density))
        }
        return checkNotNull(dashEffect)
    }

    fun time(pattern: String, millis: Long): String =
        formats.getOrPut(pattern) { SimpleDateFormat(pattern, Locale.ROOT) }.format(Date(millis))
}

/**
 * Indices of the highest and the lowest value inside [range], candles using high and low and other types the close, or -1 to -1 for an empty range.
 */
internal fun extremeIndices(items: List<ChartData>, range: IntRange, type: ChartType): Pair<Int, Int> {
    if (range.isEmpty()) return -1 to -1
    var high = range.first
    var low = range.first
    for (i in range) {
        val top = if (type == ChartType.Candle) items[i].high else items[i].cumulativeProfit
        val bottom = if (type == ChartType.Candle) items[i].low else items[i].cumulativeProfit
        if (top > (if (type == ChartType.Candle) items[high].high else items[high].cumulativeProfit)) high = i
        if (bottom < (if (type == ChartType.Candle) items[low].low else items[low].cumulativeProfit)) low = i
    }
    return high to low
}

/**
 * Candle body width in px for a slot width, at least one pixel.
 */
internal fun candleBodyWidth(slot: Float): Float = max(1f, slot * BODY_SHARE)

/**
 * Localized header labels and the timeframe unit word of the moving average legend.
 */
internal data class ChartLabels(
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String,
    val unit: String,
)

/**
 * Viewport of a draw pass, [visible] being the item indices in the plot and [window] the slot indices including the blank slots.
 */
internal class View(val scroll: Float, val slot: Float, val visible: IntRange, val window: IntRange)

internal fun LineChartState.view(plotW: Float): View {
    val slot = slotFor(plotW)
    val scroll = clampScroll(scroll, slotCount, plotW, slot)
    return View(
        scroll,
        slot,
        visibleRange(slotCount, scroll, plotW, slot, itemCount),
        visibleRange(slotCount, scroll, plotW, slot),
    )
}

internal fun ChartSpec.tagLabel(series: Series, i: Int): String {
    if (!series.timeAxis) return (i + 1).toString()
    val date = series.items[i].date
    return timeFormatter?.invoke(date) ?: time(tagPattern(timeframe, series.stepMs), date)
}

private fun ChartSpec.axisLabel(series: Series, i: Int): String {
    if (!series.timeAxis) return (i + 1).toString()
    val date = series.items[i].date
    timeFormatter?.let { return it(date) }
    val pattern = timePattern(timeframe, series.stepMs)
    val newDay = pattern.startsWith("HH") && i > 0 && time("yyyyMMdd", date) != time("yyyyMMdd", series.items[i - 1].date)
    return time(if (newDay) "MM/dd" else pattern, date)
}

private const val PANE_TICK_TARGET = 3

private fun paneTicks(pane: PaneIndicator, range: YRange): List<Double> =
    if (pane.levels.isNotEmpty()) pane.levels.toList() else niceTicks(range.min, range.max, PANE_TICK_TARGET)

internal fun DrawScope.drawMain(state: LineChartState, spec: ChartSpec) {
    val d = density
    state.density = d
    val colors = spec.colors
    drawRect(colors.background)
    val series = state.series
    val items = series.items
    val n = items.size
    val computed = state.computed
    val style = spec.textStyle.copy(color = colors.text)
    val lineH = spec.measurer.measure("0", spec.textStyle).size.height.toFloat()
    val headerH = (1 + computed.overlays.size) * lineH + 6f * d
    val timeH = lineH + 4f * d
    if (n < 2) {
        state.plot = Rect.Zero
        state.frame = null
        return
    }
    val guessW = size.width - if (state.axisW > 0f) state.axisW else 48f * d
    val guessView = state.view(guessW)
    val guessRange = visibleYRange(items, guessView.visible, spec.type, computed.scaleMarks)
    var labelW = (niceTicks(guessRange.min, guessRange.max, state.yAxisTicksCount) + items.last().cumulativeProfit)
        .maxOf { spec.measurer.measure(spec.priceFormatter(it), style).size.width }
    computed.panes.forEachIndexed { k, pane ->
        val range = paneYRange(computed.paneMarks[k], guessView.visible, pane.scale, pane.levels)
        for (tick in paneTicks(pane, range)) labelW = max(labelW, spec.measurer.measure(pane.format(tick), style).size.width)
    }
    state.axisW = axisWidth(labelW.toFloat(), d)
    val layout = paneLayout(
        size.width, size.height, headerH, timeH, state.axisW, 4f * d,
        spec.paneHeight.toPx(), computed.panes.map { it.weight }, false,
    )
    val main = layout.main
    state.plot = main
    val view = state.view(main.width)
    if (main.width <= 0f || view.visible.isEmpty()) {
        state.frame = null
        return
    }
    val range = visibleYRange(items, view.visible, spec.type, computed.scaleMarks)
    val paneRanges = computed.panes.mapIndexed { k, pane ->
        paneYRange(computed.paneMarks[k], view.visible, pane.scale, pane.levels)
    }
    state.frame = Frame(layout, range, paneRanges, view.scroll, view.slot, computed)

    for (tick in niceTicks(range.min, range.max, state.yAxisTicksCount)) {
        val y = yOf(tick, range, main.top, main.height)
        drawLine(colors.grid, Offset(0f, y), Offset(main.right, y), d)
        drawText(spec.measurer, spec.priceFormatter(tick), Offset(main.right + 4f * d, y - lineH / 2f), style)
    }
    computed.panes.forEachIndexed { k, pane ->
        val rect = layout.panes[k]
        for (tick in paneTicks(pane, paneRanges[k])) {
            val y = yOf(tick, paneRanges[k], rect.top, rect.height)
            if (pane.levels.isEmpty()) drawLine(colors.grid, Offset(0f, y), Offset(rect.right, y), d)
            val top = (y - lineH / 2f).coerceIn(rect.top, max(rect.top, rect.bottom - lineH))
            drawText(spec.measurer, pane.format(tick), Offset(rect.right + 4f * d, top), style)
        }
    }

    val widest = spec.axisLabel(series, n - 1)
    val labelWidth = spec.measurer.measure(widest, style).size.width
    val stride = labelStride(view.slot, labelWidth + 8f * d)
    for (i in view.visible) {
        if (i % stride != 0) continue
        val x = xOf(i, state.slotCount, view.scroll, main.width, view.slot)
        if (x < 0f || x > main.right) continue
        val text = spec.axisLabel(series, i)
        val w = spec.measurer.measure(text, style).size.width
        drawText(spec.measurer, text, Offset((x - w / 2f).coerceIn(0f, max(0f, main.right - w)), layout.timeAxis.top + 2f * d), style)
    }

    clipRect(0f, 0f, main.width * state.entry.value, size.height) {
        computed.panes.forEachIndexed { k, pane ->
            val rect = layout.panes[k]
            val context = PaneContext(rect, paneRanges[k], view, state.slotCount, series)
            clipRect(rect.left, rect.top, rect.right, rect.bottom) {
                drawLevels(pane.levels, spec, context)
                for (mark in computed.paneMarks[k]) drawMark(mark, spec, context)
            }
        }
        clipRect(main.left, main.top, main.right, main.bottom) {
            if (spec.type == ChartType.Candle) drawCandles(state, spec, view, range, main) else drawSeries(state, spec, view, range, main)
            val context = PaneContext(main, range, view, state.slotCount, series)
            for (marks in computed.overlayMarks) for (mark in marks) drawMark(mark, spec, context)
        }
    }
    drawLastPrice(state, spec, range, layout, lineH, style)
    drawExtremes(state, spec, view, range, main, lineH, style)
}

private fun DrawScope.drawCandles(state: LineChartState, spec: ChartSpec, view: View, range: YRange, main: Rect) {
    val items = state.items
    val body = candleBodyWidth(view.slot)
    val wick = max(1f, density)
    for (i in view.visible) {
        val item = items[i]
        val color = spec.colors.forDirection(direction(items, i, ChartType.Candle))
        val x = xOf(i, state.slotCount, view.scroll, main.width, view.slot)
        val open = yOf(item.open, range, main.top, main.height)
        val close = yOf(item.cumulativeProfit, range, main.top, main.height)
        val top = yOf(maxOf(item.high, item.open, item.cumulativeProfit), range, main.top, main.height)
        val bottom = yOf(minOf(item.low, item.open, item.cumulativeProfit), range, main.top, main.height)
        drawLine(color, Offset(x, top), Offset(x, bottom), wick)
        if (view.slot >= WICK_ONLY_SLOT) {
            drawRect(color, Offset(x - body / 2f, minOf(open, close)), Size(body, max(1f, abs(open - close))))
        }
    }
}

private fun DrawScope.drawLastPrice(
    state: LineChartState,
    spec: ChartSpec,
    range: YRange,
    layout: PaneLayout,
    lineH: Float,
    style: TextStyle,
) {
    val items = state.items
    val main = layout.main
    val last = items.lastIndex
    val color = spec.colors.forDirection(direction(items, last, spec.type))
    val y = yOf(items[last].cumulativeProfit, range, main.top, main.height).coerceIn(main.top, main.bottom)
    drawLine(color, Offset(0f, y), Offset(main.right, y), density, pathEffect = spec.dash(density))
    val text = spec.priceFormatter(items[last].cumulativeProfit)
    val tagH = lineH + 2f * density
    val top = (y - tagH / 2f).coerceIn(main.top, max(main.top, main.bottom - tagH))
    drawRect(color, Offset(layout.axis.left, top), Size(layout.axis.width, tagH))
    drawText(spec.measurer, text, Offset(layout.axis.left + 4f * density, top + density), style.copy(color = spec.colors.background))
}

private fun DrawScope.drawExtremes(
    state: LineChartState,
    spec: ChartSpec,
    view: View,
    range: YRange,
    main: Rect,
    lineH: Float,
    style: TextStyle,
) {
    val items = state.items
    val (high, low) = extremeIndices(items, view.visible, spec.type)
    val candle = spec.type == ChartType.Candle
    for ((i, isHigh) in listOf(high to true, low to false)) {
        val value = if (candle) (if (isHigh) items[i].high else items[i].low) else items[i].cumulativeProfit
        val text = spec.priceFormatter(value)
        val width = spec.measurer.measure(text, style).size.width
        val x = (xOf(i, state.slotCount, view.scroll, main.width, view.slot) - width / 2f).coerceIn(0f, max(0f, main.right - width))
        val y = yOf(value, range, main.top, main.height)
        val top = if (isHigh) (y - lineH - 2f * density).coerceAtLeast(main.top) else (y + 2f * density).coerceAtMost(main.bottom - lineH)
        drawText(spec.measurer, text, Offset(x, top), style)
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
        val x = xOf(i, state.slotCount, view.scroll, main.width, view.slot)
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
private const val BODY_SHARE = 0.7f
private const val WICK_ONLY_SLOT = 3f
