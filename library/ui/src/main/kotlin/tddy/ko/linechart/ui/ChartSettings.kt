package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable

/**
 * Price axis mapping, [Percent] showing the change against the base line value.
 */
public enum class PriceScale { Linear, Log, Percent }

/**
 * Side of the price axis column.
 */
public enum class AxisSide { Left, Right }

/**
 * Grid lines drawn behind the series.
 */
public enum class GridLines { None, Horizontal, Vertical, Both }

/**
 * Reference value drawn as a dashed line and used as the origin of [PriceScale.Percent].
 */
public sealed class BaseLine {
    /** No base line. */
    public data object None : BaseLine()

    /** Close of the previous session. */
    public data object PreviousClose : BaseLine()

    /** Open of the current session. */
    public data object TodayOpen : BaseLine()

    /** Pivot point of the previous session. */
    public data object Pivot : BaseLine()

    /**
     * Fixed [value] as the base line.
     */
    public class Custom(public val value: Double) : BaseLine() {
        init {
            require(value.isFinite()) { "value must be finite" }
        }

        override fun equals(other: Any?): Boolean = other is Custom && other.value == value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Custom($value)"
    }
}

/**
 * Display options of the panel, compared by value.
 *
 * @property scale price axis mapping
 * @property axisSide side of the price axis
 * @property rightMargin blank candle slots after the last candle, the larger of it and an indicator extension applies
 * @property grid grid lines
 * @property crosshair whether a long press shows the crosshair
 * @property legend whether the header and the pane legends are shown
 * @property currentPrice whether the last price line and axis tag are shown, the tag carrying the change percent when a base exists
 * @property baseLine reference line, percent scale uses the previous close while it is [BaseLine.None]
 * @property highLowMarkers whether the visible high and low values are marked
 * @property priceLimits whether limit lines are drawn at the previous close plus and minus [limitPercent]
 * @property limitPercent distance of the limit lines in percent
 * @property volumeMa whether the moving average lines of volume panes are shown
 * @property fontScale multiplier of the text size
 */
@Immutable
public class ChartSettings(
    public val scale: PriceScale = PriceScale.Linear,
    public val axisSide: AxisSide = AxisSide.Right,
    public val rightMargin: Int = 3,
    public val grid: GridLines = GridLines.Horizontal,
    public val crosshair: Boolean = true,
    public val legend: Boolean = true,
    public val currentPrice: Boolean = true,
    public val baseLine: BaseLine = BaseLine.None,
    public val highLowMarkers: Boolean = true,
    public val priceLimits: Boolean = false,
    public val limitPercent: Double = 30.0,
    public val volumeMa: Boolean = true,
    public val fontScale: Float = 1f,
) {
    init {
        require(rightMargin >= 0) { "rightMargin must not be negative" }
        require(limitPercent > 0.0) { "limitPercent must be positive" }
        require(fontScale > 0f) { "fontScale must be positive" }
    }

    private val fields: List<Any>
        get() = listOf(
            scale, axisSide, rightMargin, grid, crosshair, legend, currentPrice,
            baseLine, highLowMarkers, priceLimits, limitPercent, volumeMa, fontScale,
        )

    override fun equals(other: Any?): Boolean = other is ChartSettings && other.fields == fields

    override fun hashCode(): Int = fields.hashCode()

    override fun toString(): String = "ChartSettings$fields"
}
