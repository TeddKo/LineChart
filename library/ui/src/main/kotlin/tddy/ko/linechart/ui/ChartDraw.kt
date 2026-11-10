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
import androidx.compose.ui.graphics.drawscope.translate
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
    val bodies: List<Rect>,
    val scroll: Float,
    val slot: Float,
    val computed: Computed,
    val plotLeft: Float,
)

/**
 * Display configuration and reusable draw resources of one panel, including the wrapped header and overlay legend lines of the last drawn [cachedIndex] of [cachedSeries] and [cachedComputed] at [cachedWidth],
 * and the line count [reservedLines] of each of them, grown to the largest seen, which sizes the header and caps the lines drawn.
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
    var legends: List<List<AnnotatedString>> = emptyList()
    var cachedWidth: Float = -1f
    var reservedSeries: Series? = null
    var reservedComputed: Computed? = null
    var reservedWidth: Float = -1f
    var reservedLines: List<Int> = emptyList()
    var paneLegends: List<AnnotatedString> = emptyList()
    private var dashDensity = 0f
    private var dashEffect: PathEffect? = null
    private val formats = HashMap<String, SimpleDateFormat>()

    fun lineColor(color: Color): Color = if (color == Color.Unspecified) colors.text else color

    fun markColor(mark: LineMark): Color =
        if (mark.direction != 0) colors.forDirection(mark.direction).copy(alpha = mark.alpha) else lineColor(mark.color)

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
    val names: Map<String, String> = emptyMap(),
) {
    fun name(key: String): String = names[key] ?: key
}

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

private fun ChartSpec.axisLabel(series: Series, i: Int, stride: Int): String {
    if (!series.timeAxis) return (i + 1).toString()
    val date = series.items[i].date
    timeFormatter?.let { return it(date) }
    val pattern = timePattern(timeframe, series.stepMs)
    val newDay = pattern.startsWith("HH") && i > 0 && time("yyyyMMdd", date) != time("yyyyMMdd", series.items[i - 1].date)
    return time(axisPattern(pattern, newDay, stride, series.stepMs), date)
}

/**
 * Price text of the axis, tags and markers, a signed change percent against the base on the percent scale.
 */
internal fun ChartSpec.priceText(value: Double, computed: Computed): String =
    if (computed.settings.scale == PriceScale.Percent && computed.base > 0.0) percentText(value, computed.base) else priceFormatter(value)

/**
 * Per-legend maximum of [reserved] and [lines].
 */
internal fun growReserved(reserved: List<Int>, lines: List<Int>): List<Int> =
    List(max(reserved.size, lines.size)) { max(reserved.getOrElse(it) { 0 }, lines.getOrElse(it) { 0 }) }

/**
 * Line count of the header and of each overlay legend wrapped to [width], starting from the last item and grown by [growReserved], reset when the series, the indicators or [width] change.
 */
private fun ChartSpec.reserveLegendLines(series: Series, computed: Computed, width: Float): List<Int> {
    if (reservedSeries !== series || reservedComputed !== computed || reservedWidth != width) {
        reservedSeries = series
        reservedComputed = computed
        reservedWidth = width
        reservedLines = wrappedLegends(series, computed, series.items.lastIndex, width).map { it.size }
    }
    return reservedLines
}

private fun mainTicks(range: YRange, computed: Computed, target: Int): List<Double> = when (computed.settings.scale) {
    PriceScale.Log -> if (range.log) logTicks(range.min, range.max, target) else niceTicks(range.min, range.max, target)
    PriceScale.Percent -> percentTicks(range.min, range.max, computed.base, target)
    PriceScale.Linear -> niceTicks(range.min, range.max, target)
}

/**
 * Lines of the last price axis tag: the price, and the change percent against the base when a base exists and the scale is not already percent.
 */
private fun ChartSpec.lastPriceLines(value: Double, computed: Computed): List<String> {
    val lines = arrayListOf(priceText(value, computed))
    if (computed.settings.scale != PriceScale.Percent && computed.base > 0.0) lines += percentText(value, computed.base)
    return lines
}

/**
 * Top of an axis tag of height [tagH] centered on [y], kept inside [mainTop] to [mainBottom].
 */
internal fun tagTop(y: Float, tagH: Float, mainTop: Float, mainBottom: Float): Float =
    (y - tagH / 2f).coerceIn(mainTop, max(mainTop, mainBottom - tagH))

/**
 * Whether a label box of height [labelH] at [labelTop] intersects a tag box of height [tagH] at [tagTop], touching edges not counting.
 */
internal fun overlapsTag(labelTop: Float, labelH: Float, tagTop: Float, tagH: Float): Boolean =
    labelTop < tagTop + tagH && tagTop < labelTop + labelH

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
    val settings = computed.settings
    val style = spec.textStyle.copy(color = colors.text)
    val lineH = spec.measurer.measure("0", spec.textStyle).size.height.toFloat()
    val timeH = lineH + 4f * d
    if (n < 2) {
        state.plot = Rect.Zero
        state.frame = null
        return
    }
    val log = settings.scale == PriceScale.Log
    val guessW = size.width - if (state.axisW > 0f) state.axisW else 48f * d
    val guessView = state.view(guessW)
    val guessRange = visibleYRange(items, guessView.visible, spec.type, computed.scaleMarks, log, computed.baseInclude)
    val lastClose = items.last().cumulativeProfit
    var labelW = (mainTicks(guessRange, computed, state.yAxisTicksCount) + lastClose)
        .maxOf { spec.measurer.measure(spec.priceText(it, computed), style).size.width }
    for (line in spec.lastPriceLines(lastClose, computed)) labelW = max(labelW, spec.measurer.measure(line, style).size.width)
    computed.panes.forEachIndexed { k, pane ->
        val range = paneYRange(computed.paneMarks[k], guessView.visible, pane.scale, pane.levels)
        for (tick in paneTicks(pane, range)) labelW = max(labelW, spec.measurer.measure(pane.format(tick), style).size.width)
    }
    state.axisW = axisWidth(labelW.toFloat(), d)
    val headerH = (if (settings.legend) spec.reserveLegendLines(series, computed, size.width - state.axisW).sum() else 0) * lineH + 6f * d
    val axisLeft = settings.axisSide == AxisSide.Left
    val layout = paneLayout(
        size.width, size.height, headerH, timeH, state.axisW, 4f * d,
        spec.paneHeight.toPx(), computed.panes.map { it.weight }, axisLeft, state.paneDeltas,
    )
    val main = layout.main
    val plotLeft = if (axisLeft) state.axisW else 0f
    state.plot = main.translate(plotLeft, 0f)
    val view = state.view(main.width)
    if (main.width <= 0f || view.visible.isEmpty()) {
        state.frame = null
        return
    }
    val range = visibleYRange(items, view.visible, spec.type, computed.scaleMarks, log, computed.baseInclude)
    val paneRanges = computed.panes.mapIndexed { k, pane ->
        paneYRange(computed.paneMarks[k], view.visible, pane.scale, pane.levels)
    }
    val strip = if (settings.legend) lineH else 0f
    val bodies = layout.panes.map { paneBody(it, strip, if (settings.legend) 2f * d else 0f) }
    val frame = Frame(layout, range, paneRanges, bodies, view.scroll, view.slot, computed, plotLeft)
    state.frame = frame
    translate(plotLeft, 0f) { drawPlot(state, spec, frame, view, lineH, style) }
}

private fun DrawScope.drawPlot(state: LineChartState, spec: ChartSpec, frame: Frame, view: View, lineH: Float, style: TextStyle) {
    val d = density
    val colors = spec.colors
    val series = state.series
    val items = series.items
    val computed = frame.computed
    val settings = computed.settings
    val layout = frame.layout
    val main = layout.main
    val range = frame.range
    val gridHorizontal = settings.grid == GridLines.Horizontal || settings.grid == GridLines.Both
    val gridVertical = settings.grid == GridLines.Vertical || settings.grid == GridLines.Both
    val labelX = layout.axis.left + 4f * d

    val tagBox = if (settings.currentPrice) lastPriceBox(state, spec, frame, lineH) else null
    for (tick in mainTicks(range, computed, state.yAxisTicksCount)) {
        val y = yOf(tick, range, main.top, main.height)
        if (gridHorizontal) drawLine(colors.grid, Offset(0f, y), Offset(main.right, y), d)
        if (tagBox != null && overlapsTag(y - lineH / 2f, lineH, tagBox.first, tagBox.second)) continue
        drawText(spec.measurer, spec.priceText(tick, computed), Offset(labelX, y - lineH / 2f), style)
    }
    computed.panes.forEachIndexed { k, pane ->
        val rect = frame.bodies[k]
        val ticks = paneTicks(pane, frame.paneRanges[k])
        val ys = ticks.map { yOf(it, frame.paneRanges[k], rect.top, rect.height) }
        if (pane.levels.isEmpty() && gridHorizontal) for (y in ys) drawLine(colors.grid, Offset(0f, y), Offset(rect.right, y), d)
        for ((i, top) in labelTops(ys, lineH, rect.top, rect.bottom)) drawText(spec.measurer, pane.format(ticks[i]), Offset(labelX, top), style)
    }

    val widest = spec.axisLabel(series, items.lastIndex, 1)
    val labelWidth = spec.measurer.measure(widest, style).size.width
    val stride = labelStride(view.slot, labelWidth + 8f * d)
    val gridBottom = layout.panes.lastOrNull()?.bottom ?: main.bottom
    for (i in view.visible) {
        if (i % stride != 0) continue
        val x = xOf(i, state.slotCount, view.scroll, main.width, view.slot)
        if (x < 0f || x > main.right) continue
        if (gridVertical) drawLine(colors.grid, Offset(x, main.top), Offset(x, gridBottom), d)
        val text = spec.axisLabel(series, i, stride)
        val w = spec.measurer.measure(text, style).size.width
        drawText(spec.measurer, text, Offset((x - w / 2f).coerceIn(0f, max(0f, main.right - w)), layout.timeAxis.top + 2f * d), style)
    }

    clipRect(0f, 0f, main.width * state.entry.value, size.height) {
        computed.panes.forEachIndexed { k, pane ->
            val rect = frame.bodies[k]
            val context = PaneContext(rect, frame.paneRanges[k], view, state.slotCount, series)
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
    drawReferences(spec, frame, lineH, style)
    if (settings.currentPrice) drawLastPrice(state, spec, frame, lineH, style)
    if (settings.highLowMarkers) drawExtremes(state, spec, view, frame, lineH, style)
}

/**
 * Dashed base line and price limit lines that fall inside the main pane, each labeled on the axis column.
 */
private fun DrawScope.drawReferences(spec: ChartSpec, frame: Frame, lineH: Float, style: TextStyle) {
    val computed = frame.computed
    val settings = computed.settings
    val main = frame.layout.main
    val references = ArrayList<Pair<Double, Color>>(3)
    if (settings.baseLine != BaseLine.None && !computed.base.isNaN()) references += computed.base to spec.colors.text
    if (settings.priceLimits && !computed.prevClose.isNaN()) {
        val (upper, lower) = limitLines(computed.prevClose, settings.limitPercent)
        references += upper to spec.colors.up
        references += lower to spec.colors.down
    }
    for ((value, color) in references) {
        val y = yOf(value, frame.range, main.top, main.height)
        if (y !in main.top..main.bottom) continue
        drawLine(color, Offset(0f, y), Offset(main.right, y), density, pathEffect = spec.dash(density))
        val top = (y - lineH / 2f).coerceIn(main.top, max(main.top, main.bottom - lineH))
        drawText(spec.measurer, spec.priceText(value, computed), Offset(frame.layout.axis.left + 4f * density, top), style.copy(color = color))
    }
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

private fun DrawScope.lastPriceY(state: LineChartState, frame: Frame): Float {
    val main = frame.layout.main
    return yOf(state.items.last().cumulativeProfit, frame.range, main.top, main.height).coerceIn(main.top, main.bottom)
}

private fun DrawScope.lastPriceBox(state: LineChartState, spec: ChartSpec, frame: Frame, lineH: Float): Pair<Float, Float> {
    val main = frame.layout.main
    val tagH = spec.lastPriceLines(state.items.last().cumulativeProfit, frame.computed).size * lineH + 2f * density
    return tagTop(lastPriceY(state, frame), tagH, main.top, main.bottom) to tagH
}

private fun DrawScope.drawLastPrice(state: LineChartState, spec: ChartSpec, frame: Frame, lineH: Float, style: TextStyle) {
    val items = state.items
    val layout = frame.layout
    val main = layout.main
    val last = items.lastIndex
    val color = spec.colors.forDirection(direction(items, last, spec.type))
    val y = lastPriceY(state, frame)
    drawLine(color, Offset(0f, y), Offset(main.right, y), density, pathEffect = spec.dash(density))
    val lines = spec.lastPriceLines(items[last].cumulativeProfit, frame.computed)
    val (top, tagH) = lastPriceBox(state, spec, frame, lineH)
    drawRect(color, Offset(layout.axis.left, top), Size(layout.axis.width, tagH))
    lines.forEachIndexed { k, line ->
        drawText(spec.measurer, line, Offset(layout.axis.left + 4f * density, top + density + k * lineH), style.copy(color = spec.colors.background))
    }
}

private fun DrawScope.drawExtremes(state: LineChartState, spec: ChartSpec, view: View, frame: Frame, lineH: Float, style: TextStyle) {
    val items = state.items
    val main = frame.layout.main
    val (high, low) = extremeIndices(items, view.visible, spec.type)
    val candle = spec.type == ChartType.Candle
    for ((i, isHigh) in listOf(high to true, low to false)) {
        val value = if (candle) (if (isHigh) items[i].high else items[i].low) else items[i].cumulativeProfit
        val text = spec.priceText(value, frame.computed)
        val width = spec.measurer.measure(text, style).size.width
        val x = (xOf(i, state.slotCount, view.scroll, main.width, view.slot) - width / 2f).coerceIn(0f, max(0f, main.right - width))
        val y = yOf(value, frame.range, main.top, main.height)
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
