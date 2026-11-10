package tddy.ko.linechart.ui

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Slot width in px used by the viewport, falling back to [defaultSlot] while [slot] is NaN.
 */
internal fun effectiveSlot(slot: Float, defaultSlot: Float, minSlot: Float, maxSlot: Float): Float =
    (if (slot.isNaN()) defaultSlot else slot).coerceIn(minSlot, maxSlot)

/**
 * Smallest slot width in px that still fits every item into [plotW], capped at [maxSlot].
 */
internal fun minSlot(plotW: Float, n: Int, minPx: Float, maxSlot: Float): Float =
    (if (n > 0) max(minPx, plotW / n) else maxSlot).coerceAtMost(maxSlot)

/**
 * Largest scroll in slots from the right edge, keeping the first slot inside the plot.
 */
internal fun maxScroll(n: Int, plotW: Float, slot: Float): Float = max(0f, n - plotW / slot)

internal fun clampScroll(scroll: Float, n: Int, plotW: Float, slot: Float): Float =
    scroll.coerceIn(0f, maxScroll(n, plotW, slot))

/**
 * Center x in px of slot [i] among [n] slots with scroll counted in slots from the right edge.
 */
internal fun xOf(i: Int, n: Int, scroll: Float, plotW: Float, slot: Float): Float =
    plotW - (n - scroll - i - 0.5f) * slot

/**
 * Item index under [x] among [n] slots, coerced into the [count] items, or -1 for an empty series.
 */
internal fun indexAt(x: Float, n: Int, scroll: Float, plotW: Float, slot: Float, count: Int = n): Int {
    if (count <= 0) return -1
    return floor(n - scroll - (plotW - x) / slot).toInt().coerceIn(0, count - 1)
}

/**
 * Indices of the [count] items intersecting the plot of [n] slots, empty for an empty series.
 */
internal fun visibleRange(n: Int, scroll: Float, plotW: Float, slot: Float, count: Int = n): IntRange {
    if (count <= 0) return IntRange.EMPTY
    val first = floor(n - scroll - plotW / slot).toInt().coerceIn(0, count - 1)
    val last = (ceil(n - scroll).toInt() - 1).coerceAtMost(count - 1)
    return first..last
}

/**
 * Blank slots after the last item, the larger of the user right margin and the extension an indicator needs.
 */
internal fun totalPad(rightMargin: Int, rightPad: Int): Int = max(rightMargin, rightPad)

/**
 * [pad] limited to half of the slots of width [slot] that fit [plotW], so that candles keep at least about half the plot, unless all [count] items and the pad fit anyway.
 */
internal fun clampPad(pad: Int, count: Int, plotW: Float, slot: Float): Int {
    val fit = plotW / slot
    return if (fit >= count + pad - FIT_EPSILON) pad else min(pad, floor(fit / 2f).toInt())
}

private const val FIT_EPSILON = 0.01f

/**
 * Scroll that keeps the item under focal x [fx] fixed while the slot changes from [old] to [new].
 */
internal fun zoomScroll(scroll: Float, plotW: Float, old: Float, new: Float, fx: Float): Float =
    scroll + (plotW - fx) * (1f / old - 1f / new)

/**
 * Number of items appended to [old] in [new] by `date` identity, or -1 when the series must reset.
 */
internal fun appendedCount(old: List<ChartData>, new: List<ChartData>): Int {
    if (old.isEmpty() || new.isEmpty()) return -1
    val lastDate = old.last().date
    if (new.last().date == lastDate) return 0
    for (k in new.lastIndex downTo 0) {
        if (new[k].date == lastDate) return new.lastIndex - k
    }
    return -1
}

/**
 * Scroll after an update: reset on a negative [appended], hold while following the right edge, otherwise keep the view anchored.
 */
internal fun nextScroll(scroll: Float, appended: Int, crosshairActive: Boolean): Float = when {
    appended < 0 -> 0f
    scroll <= FOLLOW_SCROLL && !crosshairActive -> scroll
    else -> scroll + appended
}

internal const val FOLLOW_SCROLL = 0.5f
