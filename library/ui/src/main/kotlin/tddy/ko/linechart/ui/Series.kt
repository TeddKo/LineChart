package tddy.ko.linechart.ui

/**
 * Immutable snapshot of the chart items with the derived values shared by drawing and gestures.
 *
 * @property timeAxis whether the dates are strictly increasing
 * @property stepMs smallest positive date step, 0 without a time axis
 * @property hasVolume whether any item carries a volume
 * @property intraday whether the step is shorter than a day, with margin for daylight saving days
 */
internal class Series(val items: List<ChartData>) {
    val timeAxis: Boolean
    val stepMs: Long
    val hasVolume: Boolean = items.any { it.volume > 0.0 }
    val intraday: Boolean get() = timeAxis && stepMs < INTRADAY_MAX_STEP_MS
    val close: DoubleArray by lazy { DoubleArray(items.size) { items[it].cumulativeProfit } }
    val high: DoubleArray by lazy { DoubleArray(items.size) { items[it].high } }
    val low: DoubleArray by lazy { DoubleArray(items.size) { items[it].low } }
    val volume: DoubleArray by lazy { DoubleArray(items.size) { items[it].volume } }
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

private const val INTRADAY_MAX_STEP_MS = 20 * 3_600_000L
