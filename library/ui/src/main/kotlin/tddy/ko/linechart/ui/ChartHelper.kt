package tddy.ko.linechart.ui

import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

public val CHART_DATA_POINT_UNSPECIFIED: ChartDataPoint = ChartDataPoint(Long.MIN_VALUE, true)
public val DRAG_SELECTION_NONE: DragSelection = DragSelection(Offset.Zero, CHART_DATA_POINT_UNSPECIFIED)

@Stable
public data class ChartData(
    public val cumulativeProfit: Double,
    public val date: Long
)

@Stable
public data class ChartDataPoint(
    public val value: Long,
    public val isPositive: Boolean
)

@Stable
public data class DragSelection(
    public val position: Offset,
    public val item: ChartDataPoint,
    public val interpolatedValue: Long = 0L,
    public val index: Int = -1
)

@Stable
public data class CubicSegment(
    public val start: Offset,
    public val control1: Offset,
    public val control2: Offset,
    public val end: Offset
) {
    public fun getPointAt(t: Float): Offset {
        val t1 = 1f - t
        val t2 = t * t
        val t3 = t2 * t
        val t12 = t1 * t1
        val t13 = t12 * t1

        return Offset(
            x = t13 * start.x + 3f * t12 * t * control1.x + 3f * t1 * t2 * control2.x + t3 * end.x,
            y = t13 * start.y + 3f * t12 * t * control1.y + 3f * t1 * t2 * control2.y + t3 * end.y
        )
    }
}

public interface DragGestureListener {
    public fun onDrag(offset: Offset)
    public fun onDragStart(offset: Offset)
    public fun onDragEnd()
}

public abstract class DragHandler : DragGestureListener {
    public abstract fun selectedBy(offset: Offset): DragSelection

    private val _selected = MutableStateFlow(DRAG_SELECTION_NONE)
    public val selected: StateFlow<DragSelection> = _selected.asStateFlow()

    override fun onDrag(offset: Offset) {
        val newSelection = selectedBy(offset)
        if (newSelection != selected.value) {
            _selected.value = newSelection
        }
    }

    override fun onDragStart(offset: Offset) {
        val newSelection = selectedBy(offset)
        _selected.value = newSelection
    }

    override fun onDragEnd() {
        _selected.value = DRAG_SELECTION_NONE
    }
}

public fun Modifier.bindDragGestures(vararg listeners: DragGestureListener): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onPress = { offset ->
            listeners.forEach { it.onDragStart(offset) }
            awaitRelease()
            listeners.forEach { it.onDragEnd() }
        }
    )
}.pointerInput(Unit) {
    detectHorizontalDragGestures(
        onDragStart = { offset -> listeners.forEach { it.onDragStart(offset) } },
        onDragCancel = { listeners.forEach { it.onDragEnd() } },
        onDragEnd = { listeners.forEach { it.onDragEnd() } },
        onHorizontalDrag = { change, _ ->
            change.position.let { pos ->
                if (pos.x.isFinite() && pos.y.isFinite()) {
                    listeners.forEach { it.onDrag(pos) }
                }
            }
        }
    )
}

public class ChartCanvasManager(private val itemCount: Int) {
    private var canvasSize = Size.Unspecified
    private var offsets = listOf<Offset>()
    private var lineSegments = listOf<CubicSegment>()

    public fun getCurrentOffsets(): List<Offset> = offsets

    public fun calculateOffsets(
        scope: DrawScope,
        items: List<ChartDataPoint>,
        yFractionCalculator: (Long) -> Float
    ): List<Offset> {
        if (scope.size == canvasSize) return offsets

        canvasSize = scope.size
        val canvasWidth = canvasSize.width
        val canvasHeight = canvasSize.height
        val intervalX = canvasWidth / (itemCount - 1)

        offsets = items.mapIndexed { index, item ->
            Offset(
                x = intervalX * index,
                y = canvasHeight * yFractionCalculator(item.value)
            )
        }

        calculateLineSegments()
        return offsets
    }

    private fun calculateLineSegments() {
        lineSegments = offsets.zipWithNext { start, end ->
            val controlX = start.x + (end.x - start.x) * 0.5f
            CubicSegment(
                start = start,
                control1 = Offset(x = controlX, y = start.y),
                control2 = Offset(x = controlX, y = end.y),
                end = end
            )
        }
    }

    public fun getLineSegments(): List<CubicSegment> = lineSegments
    public fun getCanvasSize(): Size = canvasSize
}

public class ChartValueCalculator(private val items: List<ChartDataPoint>) {
    public val valueRange: Triple<Long, Long, Long> by lazy {
        val min = items.minOfOrNull { it.value } ?: 0L
        val max = items.maxOfOrNull { it.value } ?: 0L
        val range = (max - min).coerceAtLeast(1L)
        Triple(min, max, range)
    }

    private inner class LruCache<K, V>(private val maxSize: Int) :
        LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            return size > maxSize
        }
    }

    private val yFractionCache = LruCache<Long, Float>(100)

    public fun yFractionOf(value: Long): Float = yFractionCache.getOrPut(value) {
        val (min, max) = valueRange
        val paddedMin = min - (max - min) * 0.05
        val paddedMax = max + (max - min) * 0.05
        1f - ((value - paddedMin) / (paddedMax - paddedMin)).toFloat()
    }

    public fun calculateYAxisTicks(yAxisTicksCount: Int): List<Long> {
        val (min, max, range) = valueRange
        val paddedMin = min - (range * 0.05)
        val paddedMax = max + (range * 0.05)

        return List(yAxisTicksCount) { index ->
            val fraction = index / (yAxisTicksCount - 1).toFloat()
            (paddedMin + (paddedMax - paddedMin) * fraction).toLong()
        }.asReversed()
    }
}

public class ChartDragManager(
    private val items: List<ChartDataPoint>,
    private val canvasManager: ChartCanvasManager,
    private val valueCalculator: ChartValueCalculator
) {
    private var lastSelectedIndex = -1

    public fun findClosestPoint(point: Offset): Triple<Offset, Int, ChartDataPoint> {
        val lineSegments = canvasManager.getLineSegments()

        if (lineSegments.isEmpty()) {
            return Triple(Offset.Zero, -1, CHART_DATA_POINT_UNSPECIFIED)
        }

        when {
            point.x <= lineSegments.first().start.x -> {
                return Triple(lineSegments.first().start, 0, items.first())
            }

            point.x >= lineSegments.last().end.x -> {
                return Triple(lineSegments.last().end, lineSegments.size - 1, items.last())
            }
        }

        val segmentIndex = findSegmentIndex(point, lineSegments)
        val (closestPoint, interpolatedValue) = calculateClosestPoint(
            point,
            lineSegments[segmentIndex]
        )

        val currentPoint = items[segmentIndex]
        val nextPoint = items.getOrNull(segmentIndex + 1) ?: currentPoint

        val displayPoint = determineDisplayPoint(currentPoint, nextPoint, interpolatedValue)

        return Triple(closestPoint, segmentIndex, displayPoint)
    }

    private fun findSegmentIndex(point: Offset, segments: List<CubicSegment>): Int {
        return segments.binarySearchBy(point.x) { segment ->
            if (point.x < segment.start.x) segment.start.x
            else if (point.x > segment.end.x) segment.end.x
            else point.x
        }.let { if (it < 0) -(it + 1) else it }
            .coerceIn(0, segments.size - 1)
    }

    private fun calculateClosestPoint(point: Offset, segment: CubicSegment): Pair<Offset, Float> {
        val t = ((point.x - segment.start.x) / (segment.end.x - segment.start.x))
            .coerceIn(0f, 1f)
        val closestPoint = segment.getPointAt(t)
        val interpolatedValue = calculateInterpolatedValue(closestPoint.y)
        return Pair(closestPoint, interpolatedValue)
    }

    private fun calculateInterpolatedValue(y: Float): Float {
        val (min, max) = valueCalculator.valueRange
        val padding = (max - min) * 0.05f
        val paddedMin = min - padding
        val paddedMax = max + padding
        return paddedMin + (paddedMax - paddedMin) * (1f - y / canvasManager.getCanvasSize().height)
    }

    private fun determineDisplayPoint(
        currentPoint: ChartDataPoint,
        nextPoint: ChartDataPoint,
        interpolatedValue: Float
    ): ChartDataPoint {
        val valueDifference = abs(nextPoint.value - currentPoint.value)
        val threshold = valueDifference * 0.8

        val hasSignChange = currentPoint.isPositive != nextPoint.isPositive
        val shouldSwitchToNext = when {
            hasSignChange -> if (currentPoint.isPositive)
                interpolatedValue <= 0 else interpolatedValue >= 0

            currentPoint.isPositive -> shouldSwitchForPositive(
                currentPoint.value,
                nextPoint.value,
                interpolatedValue,
                threshold
            )

            else -> shouldSwitchForNegative(
                currentPoint.value,
                nextPoint.value,
                interpolatedValue,
                threshold
            )
        }
        return if (shouldSwitchToNext) nextPoint else currentPoint
    }

    private fun shouldSwitchForPositive(
        currentValue: Long,
        nextValue: Long,
        interpolatedValue: Float,
        threshold: Double
    ): Boolean = if (nextValue > currentValue) {
        abs(interpolatedValue - currentValue) > threshold
    } else {
        abs(currentValue - interpolatedValue) > threshold
    }

    private fun shouldSwitchForNegative(
        currentValue: Long,
        nextValue: Long,
        interpolatedValue: Float,
        threshold: Double
    ): Boolean = if (nextValue < currentValue) {
        abs(interpolatedValue - currentValue) > threshold
    } else {
        abs(currentValue - interpolatedValue) > threshold
    }

    public fun updateLastSelectedIndex(index: Int) {
        lastSelectedIndex = index
    }

    public fun getLastSelectedIndex(): Int = lastSelectedIndex
}

public class LineChartState(
    private val items: List<ChartDataPoint>,
    private val defaultPositive: Boolean,
    public val labels: List<String>,
    public val strokeWidth: Dp,
    public val yAxisTicksCount: Int,
) : DragHandler() {
    public val itemCount: Int = items.size
    private val valueFormatter: (Long) -> String = { value -> "%.1f".format(value / 10000.0) }
    public val formatValue: (Long) -> String = valueFormatter

    private val canvasManager = ChartCanvasManager(itemCount)
    private val valueCalculator = ChartValueCalculator(items)
    private val dragManager = ChartDragManager(items, canvasManager, valueCalculator)

    public val yAxisTicks: List<Long> by lazy { valueCalculator.calculateYAxisTicks(yAxisTicksCount) }

    public fun stroke(scope: DrawScope): Stroke = with(scope) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    public fun getOffsets(scope: DrawScope): List<Offset> =
        canvasManager.calculateOffsets(scope, items, valueCalculator::yFractionOf)

    public val isPositive: Boolean get() = if (selected.value == DRAG_SELECTION_NONE) defaultPositive else selected.value.item.isPositive

    override fun selectedBy(offset: Offset): DragSelection {
        val currentOffsets = canvasManager.getCurrentOffsets()
        if (currentOffsets.isEmpty() || offset.x !in currentOffsets.first().x..currentOffsets.last().x) {
            return DRAG_SELECTION_NONE
        }

        val (closestPoint, segmentIndex, displayPoint) = dragManager.findClosestPoint(offset)
        val currentIndex =
            if (displayPoint == items[segmentIndex]) segmentIndex else segmentIndex + 1

        if (currentIndex != dragManager.getLastSelectedIndex()) {
            dragManager.updateLastSelectedIndex(currentIndex)
            return DragSelection(
                position = closestPoint,
                item = displayPoint,
                interpolatedValue = displayPoint.value,
                index = currentIndex
            )
        }

        return DragSelection(
            position = closestPoint,
            item = displayPoint,
            interpolatedValue = displayPoint.value,
            index = dragManager.getLastSelectedIndex()
        )
    }

    override fun onDragEnd() {
        super.onDragEnd()
        dragManager.updateLastSelectedIndex(-1)
    }
}

@Composable
public fun rememberLineChartState(
    items: List<ChartData>,
    maxDataPoints: Int = Int.MAX_VALUE,
    yAxisTicksCount: Int = 5,
    scaleFactor: Int = 10000,
    strokeWidth: Dp = 2.dp
): LineChartState {
    val limitedItems = remember(items) {
        if (items.size <= maxDataPoints) items
        else items.filterIndexed { index, _ ->
            index % (items.size / maxDataPoints) == 0
        }.take(maxDataPoints)
    }

    return remember(limitedItems) {

        val defaultPositive = limitedItems.lastOrNull()?.cumulativeProfit?.let { it > 0 } ?: true

        val chartData = limitedItems.map {
            ChartDataPoint(
                value = (it.cumulativeProfit * scaleFactor).toLong(),
                isPositive = it.cumulativeProfit > 0
            )
        }

        val labels = List(chartData.size) { (it + 1).toString() }

        LineChartState(
            items = chartData,
            labels = labels,
            strokeWidth = strokeWidth,
            yAxisTicksCount = yAxisTicksCount,
            defaultPositive = defaultPositive
        )
    }
}

@Composable
public fun infiniteTransitionValue(
    initialValue: Float = 0f,
    targetValue: Float,
    durationMillis: Int,
    animation: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = durationMillis,
        easing = LinearEasing
    ),
    repeatMode: RepeatMode = RepeatMode.Restart
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")
    return infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = animation,
            repeatMode = repeatMode
        ),
        label = "infiniteTransitionValue"
    ).value
}