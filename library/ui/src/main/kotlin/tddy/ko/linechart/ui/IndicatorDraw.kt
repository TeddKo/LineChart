package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.abs
import kotlin.math.min

/**
 * Geometry of one pane for the mark renderers, [slots] counting the items and the blank slots after them.
 */
internal class PaneContext(val rect: Rect, val range: YRange, val view: View, val slots: Int, val series: Series) {
    fun x(slot: Int): Float = xOf(slot, slots, view.scroll, rect.width, view.slot)

    fun y(value: Double): Float = yOf(value, range, rect.top, rect.height)

    val from: Int get() = (view.window.first - 1).coerceAtLeast(0)

    val to: Int get() = (view.window.last + 1).coerceAtMost(slots - 1)
}

internal fun DrawScope.drawMark(mark: Mark, spec: ChartSpec, pane: PaneContext) {
    if (pane.view.window.isEmpty()) return
    when (mark) {
        is LineMark -> drawLineMark(mark, spec, pane)
        is BarMark -> drawBarMark(mark, spec, pane)
        is FillMark -> drawFillMark(mark, spec, pane)
        is DotMark -> drawDotMark(mark, spec, pane)
    }
}

/**
 * Polyline broken at NaN values, steps first moving horizontally when the mark is a step line.
 */
private fun DrawScope.drawLineMark(mark: LineMark, spec: ChartSpec, pane: PaneContext) {
    val path = spec.path
    path.reset()
    var pen = false
    var previousY = 0f
    for (j in pane.from..pane.to) {
        val value = mark.values.getOrElse(j - mark.offset) { Double.NaN }
        if (value.isNaN()) {
            pen = false
            continue
        }
        val x = pane.x(j)
        val y = pane.y(value)
        if (!pen) {
            path.moveTo(x, y)
        } else {
            if (mark.step) path.lineTo(x, previousY)
            path.lineTo(x, y)
        }
        pen = true
        previousY = y
    }
    drawPath(path, spec.markColor(mark), style = Stroke(mark.width.toPx(), join = StrokeJoin.Round))
}

private const val BAR_DIRECTION_ALPHA = 0.5f
private const val BAR_SIGN_ALPHA = 0.6f
private const val DOT_RADIUS_DP = 1.5f

private fun DrawScope.drawBarMark(mark: BarMark, spec: ChartSpec, pane: PaneContext) {
    val body = candleBodyWidth(pane.view.slot)
    val zero = pane.y(0.0)
    for (j in pane.view.visible) {
        val value = mark.values.getOrElse(j) { Double.NaN }
        if (value.isNaN()) continue
        val color = when (mark.tone) {
            Tone.Direction -> spec.colors.forDirection(direction(pane.series.items, j, spec.type)).copy(alpha = BAR_DIRECTION_ALPHA)
            Tone.Sign -> spec.colors.forDirection(value.compareTo(0.0)).copy(alpha = BAR_SIGN_ALPHA)
        }
        val y = pane.y(value)
        drawRect(color, Offset(pane.x(j) - body / 2f, min(y, zero)), Size(body, abs(zero - y)))
    }
}

/**
 * Area between the two arrays, one polygon per run of values sharing a sign when the mark has no color.
 */
private fun DrawScope.drawFillMark(mark: FillMark, spec: ChartSpec, pane: PaneContext) {
    val path = spec.fillPath
    var start = -1
    var tone = 0
    fun a(j: Int) = mark.a.getOrElse(j - mark.offset) { Double.NaN }
    fun b(j: Int) = mark.b?.getOrElse(j - mark.offset) { Double.NaN } ?: 0.0
    fun flush(end: Int) {
        if (start < 0) return
        path.reset()
        path.moveTo(pane.x(start), pane.y(a(start)))
        for (j in start + 1..end) path.lineTo(pane.x(j), pane.y(a(j)))
        for (j in end downTo start) path.lineTo(pane.x(j), pane.y(b(j)))
        path.close()
        drawPath(path, mark.color ?: spec.colors.forDirection(tone).copy(alpha = mark.alpha))
        start = -1
    }
    for (j in pane.from..pane.to) {
        val av = a(j)
        val bv = b(j)
        if (av.isNaN() || bv.isNaN()) {
            flush(j - 1)
            continue
        }
        val sign = if (mark.color == null) av.compareTo(bv) else 0
        if (start >= 0 && sign != tone) flush(j - 1)
        if (start < 0) {
            start = j
            tone = sign
        }
    }
    flush(pane.to)
}

private fun DrawScope.drawDotMark(mark: DotMark, spec: ChartSpec, pane: PaneContext) {
    for (j in pane.from..pane.to) {
        val value = mark.values.getOrElse(j - mark.offset) { Double.NaN }
        if (value.isNaN()) continue
        val color = if (mark.above.getOrElse(j - mark.offset) { false }) spec.colors.down else spec.colors.up
        drawCircle(color, DOT_RADIUS_DP * density, Offset(pane.x(j), pane.y(value)))
    }
}

/**
 * Horizontal reference lines at [levels], dashed in twice the grid opacity.
 */
internal fun DrawScope.drawLevels(levels: DoubleArray, spec: ChartSpec, pane: PaneContext) {
    val grid = spec.colors.grid
    val color = grid.copy(alpha = min(1f, grid.alpha * 2f))
    for (level in levels) {
        val y = pane.y(level)
        if (y in pane.rect.top..pane.rect.bottom) {
            drawLine(color, Offset(0f, y), Offset(pane.rect.right, y), density, pathEffect = spec.dash(density))
        }
    }
}
