package tddy.ko.linechart.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.max

/**
 * Drawable result of an indicator, indexed by item with an optional [offset] in slots.
 */
internal sealed class Mark

/**
 * Polyline of [values], broken at NaN and drawn as steps when [step]; an unspecified [color] resolves to the text color and a non-zero [direction] to the up or down color at [alpha].
 * An empty [label] leaves the line unnamed in the legend.
 */
internal class LineMark(
    val label: String,
    val values: DoubleArray,
    val color: Color,
    val width: Dp,
    val offset: Int = 0,
    val step: Boolean = false,
    val direction: Int = 0,
    val alpha: Float = 1f,
) : Mark()

internal enum class Tone { Sign, Direction }

/**
 * Bars of [values] from zero, colored by the sign of the value or by the candle direction.
 */
internal class BarMark(val values: DoubleArray, val tone: Tone) : Mark()

/**
 * Area between [a] and [b], or between [a] and zero when [b] is null, in [color] or in up and down colors at [alpha] when it is null.
 */
internal class FillMark(val a: DoubleArray, val b: DoubleArray?, val color: Color?, val offset: Int = 0, val alpha: Float = 0.6f) : Mark()

/**
 * Dots of [values], above the candle (down color) where [above] is set and below it (up color) otherwise.
 */
internal class DotMark(val values: DoubleArray, val above: BooleanArray, val offset: Int = 0) : Mark()

/**
 * Volume profile of the visible range, a draw-time mark whose bins are memoized per series and visible range.
 */
internal class ProfileMark(val bins: Int) : Mark() {
    private var memoSeries: Series? = null
    private var memoFirst = -1
    private var memoLast = -1
    private var memo = ProfileBins.Empty

    fun profile(series: Series, visible: IntRange): ProfileBins {
        if (memoSeries !== series || memoFirst != visible.first || memoLast != visible.last) {
            memo = profileBins(series, visible, bins)
            memoSeries = series
            memoFirst = visible.first
            memoLast = visible.last
        }
        return memo
    }
}

internal sealed interface PaneScale {
    data object Auto : PaneScale
    data object FromZero : PaneScale
    class Fixed(val min: Double, val max: Double) : PaneScale
    class Around(val center: Double) : PaneScale
}

/**
 * Moving average family of the [MovingAverage] indicator.
 */
public enum class MaType { Simple, Exponential, Weighted }

/**
 * Computation and drawing recipe of a chart indicator, equal to another of the same class with the same parameters.
 */
public abstract class Indicator internal constructor() {
    internal abstract val params: List<Any>
    internal open val needsVolume: Boolean get() = false
    internal abstract fun marks(series: Series, settings: ChartSettings): List<Mark>
    internal abstract fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int): AnnotatedString

    final override fun equals(other: Any?): Boolean =
        other is Indicator && other.javaClass == javaClass && other.params == params

    final override fun hashCode(): Int = 31 * javaClass.hashCode() + params.hashCode()
}

/**
 * Indicator drawn on the main price pane.
 */
public abstract class Overlay internal constructor() : Indicator() {
    internal open val inScale: Boolean get() = true
    internal open val rightPad: Int get() = 0
}

/**
 * Indicator drawn in its own pane below the main pane, [weight] times the panel pane height tall.
 */
public abstract class PaneIndicator internal constructor(public val weight: Float) : Indicator() {
    internal abstract val scale: PaneScale
    internal open val levels: DoubleArray get() = NO_LEVELS
    internal open val compact: Boolean get() = false
}

private val NO_LEVELS = DoubleArray(0)

/**
 * Value text of the pane axis, legend and crosshair tag, compact for volume-type panes and two decimals for oscillators.
 */
internal fun PaneIndicator.format(value: Double): String = if (compact) formatCompact(value) else formatOscillator(value)

internal fun Mark.eachValue(slot: Int, action: (Double) -> Unit) {
    fun DoubleArray?.at(offset: Int) {
        val v = this?.getOrNull(slot - offset) ?: return
        if (v.isFinite()) action(v)
    }
    when (this) {
        is LineMark -> values.at(offset)
        is BarMark -> values.at(0)
        is FillMark -> {
            a.at(offset)
            b.at(offset)
        }
        is DotMark -> values.at(offset)
        is ProfileMark -> Unit
    }
}

/**
 * Value range of a pane over the slots in [visible], padded 5%, from zero, fixed or symmetric around a center according to [scale], including [levels] unless fixed.
 */
internal fun paneYRange(marks: List<Mark>, visible: IntRange, scale: PaneScale, levels: DoubleArray): YRange {
    if (scale is PaneScale.Fixed) return YRange(scale.min, scale.max)
    var min = Double.POSITIVE_INFINITY
    var max = Double.NEGATIVE_INFINITY
    for (mark in marks) {
        for (i in visible) {
            mark.eachValue(i) {
                if (it < min) min = it
                if (it > max) max = it
            }
        }
    }
    for (level in levels) {
        if (level < min) min = level
        if (level > max) max = level
    }
    if (scale is PaneScale.FromZero) {
        if (max.isInfinite()) return YRange(0.0, 1.0)
        return YRange(0.0, if (max > 0.0) max else 1.0)
    }
    if (min > max) return YRange(0.0, 1.0)
    if (scale is PaneScale.Around) {
        val reach = max(abs(max - scale.center), abs(min - scale.center)).let { if (it > 0.0) it else 1.0 }
        return YRange(scale.center - reach * 1.05, scale.center + reach * 1.05)
    }
    val span = max - min
    if (span <= 0.0) {
        val delta = if (min == 0.0) 1.0 else abs(min) * 0.01
        return YRange(min - delta, max + delta)
    }
    return YRange(min - span * 0.05, max + span * 0.05)
}

/**
 * One-line legend of an indicator named by [key] followed by [args], each line and sign histogram of [marks] with its value at [i] through [format], in the color it is drawn in.
 */
internal fun legendLine(spec: ChartSpec, key: String, args: String, marks: List<Mark>, i: Int, format: (Double) -> String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = spec.colors.text)) { append("${spec.labels.name(key)}$args  ") }
    for (mark in marks) {
        when {
            mark is LineMark -> {
                val value = mark.values.getOrElse(i - mark.offset) { Double.NaN }
                val name = if (mark.label.isEmpty()) "" else "${spec.labels.name(mark.label)} "
                withStyle(SpanStyle(color = spec.markColor(mark))) { append("$name${if (value.isFinite()) format(value) else "-"}  ") }
            }
            mark is BarMark && mark.tone == Tone.Sign -> {
                val value = mark.values.getOrElse(i) { Double.NaN }
                withStyle(SpanStyle(color = spec.colors.forDirection(value.compareTo(0.0)))) {
                    append("${spec.labels.name("osc")} ${if (value.isFinite()) format(value) else "-"}  ")
                }
            }
        }
    }
}

/**
 * This value, NaN when it is infinite.
 */
internal fun Double.finiteOrNaN(): Double = if (isFinite()) this else Double.NaN

internal fun requirePositive(vararg values: Int) = require(values.all { it >= 1 }) { "periods must be positive" }

internal fun requireWeight(weight: Float) = require(weight > 0f) { "weight must be positive" }

internal fun List<Color>.at(k: Int): Color = getOrElse(k) { Color.Unspecified }
