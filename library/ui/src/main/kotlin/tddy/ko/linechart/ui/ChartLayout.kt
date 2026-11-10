package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil

/**
 * Pane rectangles of the chart in plot coordinates (x from the plot's left edge) in px, [axis] being the price axis column of the main pane, right of the plot or left of it at negative x.
 */
internal class PaneLayout(val main: Rect, val panes: List<Rect>, val timeAxis: Rect, val axis: Rect)

/**
 * Split of [width] x [height] into header, main, one pane of [paneHeight] times its weight per entry of [weights] and time axis, the panes taking their height from the main pane.
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
): PaneLayout {
    val plotW = (width - axisW).coerceAtLeast(0f)
    val bottom = (height - timeH).coerceAtLeast(headerH)
    val mainBottom = (bottom - totalPaneHeight(paneHeight, weights, gap)).coerceAtLeast(headerH)
    var top = mainBottom + gap
    val panes = weights.map { weight ->
        Rect(0f, top, plotW, top + paneHeight * weight).also { top = it.bottom + gap }
    }
    return PaneLayout(
        main = Rect(0f, headerH, plotW, mainBottom),
        panes = panes,
        timeAxis = Rect(0f, bottom, plotW, bottom + timeH),
        axis = if (axisLeft) Rect(-axisW, headerH, 0f, mainBottom) else Rect(plotW, headerH, width, mainBottom),
    )
}

/**
 * Height the panes of [weights] add below the main pane, each [paneHeight] times its weight plus a [gap] above it.
 */
internal fun totalPaneHeight(paneHeight: Float, weights: List<Float>, gap: Float): Float =
    weights.sumOf { (paneHeight * it + gap).toDouble() }.toFloat()

/**
 * Price axis column width: [labelWidth] plus 8dp, rounded up to a multiple of 4dp.
 */
internal fun axisWidth(labelWidth: Float, density: Float): Float {
    val step = 4f * density
    return ceil((labelWidth + 8f * density) / step) * step
}
