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
        is ProfileMark -> drawProfile(mark, spec, pane)
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
        if (!value.isFinite()) {
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
private const val PROFILE_WIDTH = 0.3f
private const val PROFILE_ALPHA = 0.15f
private const val PROFILE_POC_ALPHA = 0.3f

private fun DrawScope.drawBarMark(mark: BarMark, spec: ChartSpec, pane: PaneContext) {
    val body = candleBodyWidth(pane.view.slot)
    val zero = pane.y(0.0)
    for (j in pane.view.visible) {
        val value = mark.values.getOrElse(j) { Double.NaN }
        if (!value.isFinite()) continue
        val color = when (mark.tone) {
            Tone.Direction -> spec.colors.forDirection(direction(pane.series.items, j, spec.type)).copy(alpha = BAR_DIRECTION_ALPHA)
            Tone.Sign -> spec.colors.forDirection(value.compareTo(0.0)).copy(alpha = BAR_SIGN_ALPHA)
        }
        val y = pane.y(value)
        drawRect(color, Offset(pane.x(j) - body / 2f, min(y, zero)), Size(body, abs(zero - y)))
    }
}

/**
 * Contiguous slots [start] to [end] of a fill sharing the [tone] of the sign of a minus b, 0 for an unsigned fill.
 */
internal class FillRun(val start: Int, val end: Int, val tone: Int)

/**
 * Runs of slots from [from] to [to] where both values exist, split where the sign of their difference changes when [bySign].
 */
internal fun fillRuns(a: (Int) -> Double, b: (Int) -> Double, from: Int, to: Int, bySign: Boolean): List<FillRun> {
    val runs = ArrayList<FillRun>()
    var start = -1
    var tone = 0
    for (j in from..to) {
        val av = a(j)
        val bv = b(j)
        if (!av.isFinite() || !bv.isFinite()) {
            if (start >= 0) runs += FillRun(start, j - 1, tone)
            start = -1
            continue
        }
        val sign = if (bySign) av.compareTo(bv) else 0
        if (start >= 0 && sign != tone) {
            runs += FillRun(start, j - 1, tone)
            start = -1
        }
        if (start < 0) {
            start = j
            tone = sign
        }
    }
    if (start >= 0) runs += FillRun(start, to, tone)
    return runs
}

/**
 * Area between the two arrays, one polygon per run of values sharing a sign when the mark has no color.
 */
private fun DrawScope.drawFillMark(mark: FillMark, spec: ChartSpec, pane: PaneContext) {
    val path = spec.fillPath
    fun a(j: Int) = mark.a.getOrElse(j - mark.offset) { Double.NaN }
    fun b(j: Int) = mark.b?.getOrElse(j - mark.offset) { Double.NaN } ?: 0.0
    for (run in fillRuns(::a, ::b, pane.from, pane.to, mark.color == null)) {
        path.reset()
        path.moveTo(pane.x(run.start), pane.y(a(run.start)))
        for (j in run.start + 1..run.end) path.lineTo(pane.x(j), pane.y(a(j)))
        for (j in run.end downTo run.start) path.lineTo(pane.x(j), pane.y(b(j)))
        path.close()
        drawPath(path, mark.color ?: spec.colors.forDirection(run.tone).copy(alpha = mark.alpha))
    }
}

/**
 * Slot of a dot with whether it lies above the candle.
 */
internal class DotSpot(val slot: Int, val above: Boolean)

internal fun dotSpots(mark: DotMark, from: Int, to: Int): List<DotSpot> = (from..to).mapNotNull { j ->
    if (mark.values.getOrElse(j - mark.offset) { Double.NaN }.isFinite().not()) null else DotSpot(j, mark.above.getOrElse(j - mark.offset) { false })
}

private fun DrawScope.drawDotMark(mark: DotMark, spec: ChartSpec, pane: PaneContext) {
    for (spot in dotSpots(mark, pane.from, pane.to)) {
        val color = if (spot.above) spec.colors.down else spec.colors.up
        drawCircle(color, DOT_RADIUS_DP * density, Offset(pane.x(spot.slot), pane.y(mark.values[spot.slot - mark.offset])))
    }
}

/**
 * Y of the [levels] that fall inside the pane.
 */
internal fun levelYs(levels: DoubleArray, pane: PaneContext): List<Float> =
    levels.map { pane.y(it) }.filter { it in pane.rect.top..pane.rect.bottom }

/**
 * Horizontal reference lines at [levels], dashed in twice the grid opacity.
 */
internal fun DrawScope.drawLevels(levels: DoubleArray, spec: ChartSpec, pane: PaneContext) {
    val grid = spec.colors.grid
    val color = grid.copy(alpha = min(1f, grid.alpha * 2f))
    for (y in levelYs(levels, pane)) {
        drawLine(color, Offset(0f, y), Offset(pane.rect.right, y), density, pathEffect = spec.dash(density))
    }
}

/**
 * Horizontal volume bars of the visible range growing from the left edge up to 30% of the pane width, the busiest price in the up color.
 */
private fun DrawScope.drawProfile(mark: ProfileMark, spec: ChartSpec, pane: PaneContext) {
    val profile = mark.profile(pane.series, pane.view.visible)
    val peak = profile.volumes.maxOrNull() ?: return
    val step = (profile.high - profile.low) / profile.volumes.size
    if (peak <= 0.0 || step <= 0.0) return
    profile.volumes.forEachIndexed { bin, volume ->
        if (volume <= 0.0) return@forEachIndexed
        val top = pane.y(profile.low + (bin + 1) * step)
        val bottom = pane.y(profile.low + bin * step)
        val color = if (bin == profile.poc) spec.colors.up.copy(alpha = PROFILE_POC_ALPHA) else spec.colors.text.copy(alpha = PROFILE_ALPHA)
        drawRect(color, Offset(pane.rect.left, top), Size((volume / peak * PROFILE_WIDTH * pane.rect.width).toFloat(), bottom - top))
    }
}
