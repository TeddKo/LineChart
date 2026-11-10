package tddy.ko.linechart.ui

import java.util.TimeZone

/**
 * Marks of the indicators computed for [series] with [settings], the overlays and panes that need volume dropped when the series has none.
 *
 * @property scaleMarks marks of the overlays that take part in the main Y range
 * @property cache marks by indicator, reused while the series stays the same
 * @property base value of the base line, the previous close or the first close standing in for it on the percent scale, NaN when there is none
 * @property baseInclude value the main Y range must contain, the base while a base line is selected and NaN otherwise
 * @property prevClose close of the previous session, NaN when there is none
 */
internal class Computed(
    val series: Series,
    val settings: ChartSettings,
    val overlays: List<Overlay>,
    val overlayMarks: List<List<Mark>>,
    val panes: List<PaneIndicator>,
    val paneMarks: List<List<Mark>>,
    val cache: Map<Indicator, List<Mark>>,
    val base: Double,
    val prevClose: Double,
) {
    val baseInclude: Double = if (settings.baseLine == BaseLine.None) Double.NaN else base

    val scaleMarks: List<Mark> = overlays.indices.filter { overlays[it].inScale }.flatMap { overlayMarks[it] }

    companion object {
        val Empty = Computed(Series(emptyList()), ChartSettings(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap(), Double.NaN, Double.NaN)
    }
}

/**
 * Marks of [overlays] and [panes] computed over [series], reusing the marks of [previous] for equal indicators while the series and the volume moving average setting are unchanged.
 *
 * ponytail: every indicator is recomputed in full on each series change, about 2 to 4 ms at 10k candles and 20 lines, move it to Dispatchers.Default with conflation or resume from the tail if profiling shows more than 4 ms.
 */
internal fun computeIndicators(
    series: Series,
    overlays: List<Overlay>,
    panes: List<PaneIndicator>,
    settings: ChartSettings,
    previous: Computed,
): Computed {
    val reusable = previous.series === series && previous.settings.volumeMa == settings.volumeMa
    val cache = HashMap<Indicator, List<Mark>>()
    fun marksOf(indicator: Indicator): List<Mark> = cache.getOrPut(indicator) {
        (if (reusable) previous.cache[indicator] else null) ?: indicator.marks(series, settings)
    }
    val shownOverlays = overlays.filter { !it.needsVolume || series.hasVolume }
    val shownPanes = panes.filter { !it.needsVolume || series.hasVolume }
    val zone = TimeZone.getDefault()
    val percent = settings.scale == PriceScale.Percent
    var base = baseValue(series, if (percent && settings.baseLine == BaseLine.None) BaseLine.PreviousClose else settings.baseLine, zone)
    if (percent && !(base > 0.0)) base = series.items.firstOrNull()?.cumulativeProfit ?: Double.NaN
    return Computed(
        series, settings, shownOverlays, shownOverlays.map(::marksOf), shownPanes, shownPanes.map(::marksOf), cache,
        base, previousClose(series, zone),
    )
}
