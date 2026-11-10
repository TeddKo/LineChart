package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Rect
import kotlin.math.ceil

/**
 * Pane rectangles of the chart in px, the price axis column sitting right of the main and volume panes.
 */
internal class PaneLayout(val main: Rect, val volume: Rect?, val timeAxis: Rect, val axis: Rect)

/**
 * Splits [width] x [height] into header, main, optional volume (20% of the body below the main pane), time axis and price axis.
 */
internal fun paneLayout(
    width: Float,
    height: Float,
    headerH: Float,
    timeH: Float,
    axisW: Float,
    gap: Float,
    hasVolume: Boolean,
): PaneLayout {
    val plotW = (width - axisW).coerceAtLeast(0f)
    val bottom = (height - timeH).coerceAtLeast(headerH)
    val body = bottom - headerH
    val volumeH = if (hasVolume) (body - gap).coerceAtLeast(0f) * VOLUME_SHARE else 0f
    val mainBottom = if (hasVolume) bottom - volumeH - gap else bottom
    val main = Rect(0f, headerH, plotW, mainBottom.coerceAtLeast(headerH))
    return PaneLayout(
        main = main,
        volume = if (hasVolume) Rect(0f, mainBottom + gap, plotW, bottom) else null,
        timeAxis = Rect(0f, bottom, plotW, bottom + timeH),
        axis = Rect(plotW, headerH, width, mainBottom.coerceAtLeast(headerH)),
    )
}

/**
 * Price axis column width: [labelWidth] plus 8dp, rounded up to a multiple of 4dp.
 */
internal fun axisWidth(labelWidth: Float, density: Float): Float {
    val step = 4f * density
    return ceil((labelWidth + 8f * density) / step) * step
}

private const val VOLUME_SHARE = 0.2f
