package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * Volume per price bin between [low] and [high], the bin of the largest volume being the point of control.
 */
internal class ProfileBins(val low: Double, val high: Double, val volumes: DoubleArray) {
    val poc: Int = volumes.indices.maxByOrNull { volumes[it] } ?: -1

    companion object {
        val Empty = ProfileBins(0.0, 0.0, DoubleArray(0))
    }
}

/**
 * Volume of the candles in [visible] summed into [bins] equal price bins between their lowest low and highest high, each candle into the bin of its close.
 */
internal fun profileBins(series: Series, visible: IntRange, bins: Int): ProfileBins {
    if (visible.isEmpty()) return ProfileBins(0.0, 0.0, DoubleArray(bins))
    val low = visible.minOf { series.low[it] }
    val high = visible.maxOf { series.high[it] }
    val volumes = DoubleArray(bins)
    for (i in visible) {
        val bin = if (high > low) ((series.close[i] - low) / (high - low) * bins).toInt().coerceIn(0, bins - 1) else 0
        volumes[bin] += series.volume[i]
    }
    return ProfileBins(low, high, volumes)
}

/**
 * Volume profile of the visible range drawn from the left edge of the main pane, outside the price range.
 *
 * @property bins number of price bins
 */
@Immutable
public class VolumeProfile(public val bins: Int = 10) : Overlay() {
    init {
        requirePositive(bins)
    }

    override val params: List<Any> get() = listOf(bins)
    override val needsVolume: Boolean get() = true
    override val inScale: Boolean get() = false

    override fun marks(series: Series, settings: ChartSettings): List<Mark> = listOf(ProfileMark(bins))

    override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString =
        legendLine(spec, "vp", "($bins)", marks, i, spec.priceFormatter)
}
