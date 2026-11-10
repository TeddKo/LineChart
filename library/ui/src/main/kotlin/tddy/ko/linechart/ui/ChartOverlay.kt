package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.withStyle
import java.util.Locale
import kotlin.math.max

/**
 * Percent change of the close at [i] against the previous close, or against the open for the first item.
 */
internal fun changePercent(items: List<ChartData>, i: Int): Double {
    val reference = if (i == 0) items[0].open else items[i - 1].cumulativeProfit
    return if (reference == 0.0) 0.0 else (items[i].cumulativeProfit - reference) / reference * 100.0
}

internal fun formatChange(percent: Double): String = String.format(Locale.ROOT, "%+.2f%%", percent)

/**
 * Moving average legend name, the period followed by the timeframe [unit] word.
 */
internal fun maLegend(period: Int, unit: String): String = "MA$period$unit"

/**
 * Lines of [text] no wider than [maxWidth] through [measure], broken between its items that end in two spaces, an item wider than [maxWidth] standing alone on its line.
 */
internal fun wrapLegend(text: AnnotatedString, maxWidth: Float, measure: (AnnotatedString) -> Float): List<AnnotatedString> {
    val lines = ArrayList<AnnotatedString>()
    var start = 0
    var lineStart = 0
    while (start < text.length) {
        val found = text.text.indexOf("  ", start)
        val end = if (found < 0) text.length else found + 2
        if (start > lineStart && measure(text.subSequence(lineStart, end)) > maxWidth) {
            lines += text.subSequence(lineStart, start)
            lineStart = start
        }
        start = end
    }
    lines += text.subSequence(lineStart, text.length)
    return lines
}

internal fun DrawScope.drawOverlay(state: LineChartState, spec: ChartSpec, pulse: Float?) {
    val frame = state.frame ?: return
    if (state.series.items.size < 2) return
    translate(frame.plotLeft, 0f) { drawOverlayPlot(state, spec, pulse, frame) }
}

private fun DrawScope.drawOverlayPlot(state: LineChartState, spec: ChartSpec, pulse: Float?, frame: Frame) {
    val series = state.series
    val items = series.items
    val n = items.size
    val d = density
    val layout = frame.layout
    val computed = frame.computed
    val main = layout.main
    val slot = state.slotFor(main.width)
    val slots = state.slotCount
    val scroll = clampScroll(state.scroll, slots, main.width, slot)
    val cross = state.crosshair.let { if (it.isSpecified) Offset(it.x - frame.plotLeft, it.y) else it }
    val index = if (cross.isSpecified) indexAt(cross.x, slots, scroll, main.width, slot, n) else n - 1
    val colors = spec.colors
    val style = spec.textStyle.copy(color = colors.text)
    val lineH = spec.measurer.measure("0", spec.textStyle).size.height.toFloat()
    if (spec.cachedSeries !== series || spec.cachedComputed !== computed || spec.cachedIndex != index || spec.cachedWidth != main.width) {
        spec.cachedSeries = series
        spec.cachedComputed = computed
        spec.cachedIndex = index
        spec.cachedWidth = main.width
        spec.legends = spec.wrappedLegends(series, computed, index, main.width)
        spec.reservedLines = growReserved(spec.reservedLines, spec.legends.map { it.size })
        spec.paneLegends = computed.panes.mapIndexed { k, pane -> pane.legend(spec, series, computed.paneMarks[k], index) }
    }
    if (computed.settings.legend) {
        var row = 0
        spec.legends.forEachIndexed { k, lines ->
            for (line in lines.take(spec.reservedLines.getOrElse(k) { 1 })) {
                drawText(spec.measurer, line, Offset(0f, row * lineH), style, softWrap = false, maxLines = 1)
                row++
            }
            row += (spec.reservedLines.getOrElse(k) { 1 } - lines.size).coerceAtLeast(0)
        }
        spec.paneLegends.forEachIndexed { k, legend ->
            drawText(spec.measurer, legend, Offset(0f, layout.panes[k].top), style, softWrap = false, maxLines = 1)
        }
    }
    val separator = state.resizing
    if (separator in layout.panes.indices) {
        val above = if (separator == 0) main.bottom else layout.panes[separator - 1].bottom
        val y = (above + layout.panes[separator].top) / 2f
        drawLine(colors.crosshair.copy(alpha = RESIZE_ALPHA), Offset(0f, y), Offset(main.right, y), 2f * d)
    }
    if (cross.isSpecified) {
        val x = xOf(index, slots, scroll, main.width, slot)
        val paneIndex = layout.panes.indexOfFirst { cross.y in it.top..it.bottom }
        val rect = if (paneIndex >= 0) frame.bodies[paneIndex] else main
        val range = if (paneIndex >= 0) frame.paneRanges[paneIndex] else frame.range
        val y = cross.y.coerceIn(rect.top, rect.bottom)
        val dash = spec.dash(d)
        drawLine(colors.crosshair, Offset(x, main.top), Offset(x, layout.timeAxis.top), d, pathEffect = dash)
        drawLine(colors.crosshair, Offset(0f, y), Offset(main.right, y), d, pathEffect = dash)
        val tagStyle = style.copy(color = colors.background)
        val value = valueAt(y, range, rect.top, rect.height)
        val price = if (paneIndex >= 0) computed.panes[paneIndex].format(value) else spec.priceText(value, computed)
        val priceH = lineH + 2f * d
        val priceTop = (y - priceH / 2f).coerceIn(rect.top, max(rect.top, rect.bottom - priceH))
        drawRect(colors.crosshair, Offset(layout.axis.left, priceTop), Size(layout.axis.width, priceH))
        drawText(spec.measurer, price, Offset(layout.axis.left + 4f * d, priceTop + d), tagStyle)
        val time = spec.tagLabel(series, index)
        val timeW = spec.measurer.measure(time, tagStyle).size.width + 8f * d
        val timeLeft = (x - timeW / 2f).coerceIn(0f, max(0f, main.right - timeW))
        drawRect(colors.crosshair, Offset(timeLeft, layout.timeAxis.top), Size(timeW, lineH + 2f * d))
        drawText(spec.measurer, time, Offset(timeLeft + 4f * d, layout.timeAxis.top + d), tagStyle)
    } else if (pulse != null && state.entry.value >= 1f) {
        val x = xOf(n - 1, slots, scroll, main.width, slot)
        if (x in 0f..main.right) {
            val center = Offset(x, yOf(items.last().cumulativeProfit, frame.range, main.top, main.height))
            val color = colors.forDirection(items.last().cumulativeProfit.compareTo(items.first().cumulativeProfit))
            drawCircle(color, 3.5f * d + 15f * d * pulse, center, alpha = PULSE_ALPHA * (1f - pulse))
            drawCircle(color, 3.5f * d, center)
        }
    }
}

private fun AnnotatedString.Builder.pair(label: String, value: String, labelColor: Color, valueColor: Color) {
    withStyle(SpanStyle(color = labelColor)) { append("$label ") }
    withStyle(SpanStyle(color = valueColor)) { append("$value  ") }
}

/**
 * Header and overlay legend of the item [i], each wrapped to [width].
 */
internal fun ChartSpec.wrappedLegends(series: Series, computed: Computed, i: Int, width: Float): List<List<AnnotatedString>> {
    val measure = { text: AnnotatedString -> measurer.measure(text, textStyle).size.width.toFloat() }
    val legends = listOf(header(this, series, i)) + computed.overlays.mapIndexed { k, overlay -> overlay.legend(this, series, computed.overlayMarks[k], i) }
    return legends.map { wrapLegend(it, width, measure) }
}

private fun header(spec: ChartSpec, series: Series, i: Int): AnnotatedString {
    val items = series.items
    val item = items[i]
    val colors = spec.colors
    val tone = colors.forDirection(direction(items, i, spec.type))
    val change = formatChange(changePercent(items, i))
    val labels = spec.labels
    return buildAnnotatedString {
        if (spec.type == ChartType.Candle) {
            pair(labels.open, spec.priceFormatter(item.open), colors.text, tone)
            pair(labels.high, spec.priceFormatter(item.high), colors.text, tone)
            pair(labels.low, spec.priceFormatter(item.low), colors.text, tone)
            pair(labels.close, spec.priceFormatter(item.cumulativeProfit), colors.text, tone)
            withStyle(SpanStyle(color = tone)) { append("$change  ") }
            if (series.hasVolume) pair(labels.volume, spec.priceFormatter(item.volume), colors.text, tone)
        } else {
            withStyle(SpanStyle(color = colors.text)) { append("${spec.tagLabel(series, i)}  ") }
            withStyle(SpanStyle(color = tone)) { append("${spec.priceFormatter(item.cumulativeProfit)}  $change") }
        }
    }
}

private const val PULSE_ALPHA = 0.6f
private const val RESIZE_ALPHA = 0.6f
