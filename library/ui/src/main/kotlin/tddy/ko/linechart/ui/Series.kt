package tddy.ko.linechart.ui

/**
 * Immutable snapshot of the chart items with the derived values shared by drawing and gestures.
 *
 * @property timeAxis whether the dates are strictly increasing
 * @property stepMs smallest positive date step, 0 without a time axis
 * @property hasVolume whether any item carries a volume
 */
internal class Series(val items: List<ChartData>) {
    val prefix: DoubleArray = prefixSums(items)
    val timeAxis: Boolean
    val stepMs: Long
    val hasVolume: Boolean = items.any { it.volume > 0.0 }
    val labels: List<String> by lazy { List(items.size) { (it + 1).toString() } }

    init {
        var increasing = true
        var step = Long.MAX_VALUE
        for (i in 1 until items.size) {
            val diff = items[i].date - items[i - 1].date
            if (diff <= 0L) {
                increasing = false
                break
            }
            if (diff < step) step = diff
        }
        timeAxis = increasing && items.size > 1
        stepMs = if (timeAxis) step else 0L
    }
}
