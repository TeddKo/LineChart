package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Pane rectangles of the chart in plot coordinates (x from the plot's left edge) in px, [axis] being the price axis column of the main pane, right of the plot or left of it at negative x.
 */
internal class PaneLayout(val main: Rect, val panes: List<Rect>, val timeAxis: Rect, val axis: Rect)

/**
 * Split of [width] x [height] into header, main, one pane of [paneHeight] times its weight plus its entry of [deltas] per entry of [weights] and time axis, the panes taking their height from the main pane.
 */
internal fun paneLayout(
    width: Float,
    height: Float,
    headerH: Float,
    timeH: Float,
    axisW: Float,
    gap: Float,
    paneHeight: Float,
    weights: List<Float>,
    axisLeft: Boolean,
    deltas: List<Float> = emptyList(),
): PaneLayout {
    val plotW = (width - axisW).coerceAtLeast(0f)
    val bottom = (height - timeH).coerceAtLeast(headerH)
    val mainBottom = (bottom - totalPaneHeight(paneHeight, weights, gap, deltas)).coerceAtLeast(headerH)
    var top = mainBottom + gap
    val panes = weights.mapIndexed { k, weight ->
        Rect(0f, top, plotW, top + paneHeight * weight + deltas.getOrElse(k) { 0f }).also { top = it.bottom + gap }
    }
    return PaneLayout(
        main = Rect(0f, headerH, plotW, mainBottom),
        panes = panes,
        timeAxis = Rect(0f, bottom, plotW, bottom + timeH),
        axis = if (axisLeft) Rect(-axisW, headerH, 0f, mainBottom) else Rect(plotW, headerH, width, mainBottom),
    )
}

/**
 * Plot area of [pane] below its legend strip of [strip] plus [gap], the strip being zero when the legend is hidden.
 */
internal fun paneBody(pane: Rect, strip: Float, gap: Float): Rect = Rect(pane.left, minOf(pane.top + strip + gap, pane.bottom), pane.right, pane.bottom)

/**
 * Height the panes of [weights] add below the main pane, each [paneHeight] times its weight plus its entry of [deltas] and a [gap] above it.
 */
internal fun totalPaneHeight(paneHeight: Float, weights: List<Float>, gap: Float, deltas: List<Float> = emptyList()): Float =
    weights.withIndex().sumOf { (k, weight) -> (paneHeight * weight + deltas.getOrElse(k) { 0f } + gap).toDouble() }.toFloat()

/**
 * Index of the separator whose center, the middle of the gap between two areas, lies within [zone] px of [y], where 0 divides the main pane from the first pane, or -1 for none.
 */
internal fun separatorAt(layout: PaneLayout, y: Float, zone: Float): Int = layout.panes.indices.firstOrNull { k ->
    val above = if (k == 0) layout.main.bottom else layout.panes[k - 1].bottom
    abs(y - (above + layout.panes[k].top) / 2f) <= zone
} ?: -1

/**
 * [heights] with the separator [index] moved by [deltaPx], the area above growing and the one below shrinking by the same amount, limited so neither goes below its entry of [mins] or, when already below it, shrinks at all.
 */
internal fun resizeBoundary(heights: List<Float>, index: Int, deltaPx: Float, mins: List<Float>): List<Float> {
    if (index !in 0 until heights.lastIndex) return heights
    val shift = deltaPx.coerceIn(-maxOf(0f, heights[index] - mins[index]), maxOf(0f, heights[index + 1] - mins[index + 1]))
    return heights.mapIndexed { k, h -> if (k == index) h + shift else if (k == index + 1) h - shift else h }
}

/**
 * Price axis column width: [labelWidth] plus 8dp, rounded up to a multiple of 4dp.
 */
internal fun axisWidth(labelWidth: Float, density: Float): Float {
    val step = 4f * density
    return ceil((labelWidth + 8f * density) / step) * step
}

/**
 * Index and top of each label of height [lineH] centered on its entry of [ys], kept inside [top] to [bottom] and dropped when closer than [lineH] to the label kept before it.
 */
internal fun labelTops(ys: List<Float>, lineH: Float, top: Float, bottom: Float): List<Pair<Int, Float>> {
    val kept = ArrayList<Pair<Int, Float>>()
    ys.forEachIndexed { k, y ->
        val labelTop = (y - lineH / 2f).coerceIn(top, maxOf(top, bottom - lineH))
        if (kept.isEmpty() || abs(labelTop - kept.last().second) >= lineH) kept += k to labelTop
    }
    return kept
}
