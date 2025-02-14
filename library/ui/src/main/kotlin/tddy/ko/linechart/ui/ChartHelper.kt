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
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

val CHART_DATA_POINT_UNSPECIFIED = ChartDataPoint(Long.MIN_VALUE, true)
val DRAG_SELECTION_NONE = DragSelection(Offset.Zero, CHART_DATA_POINT_UNSPECIFIED)

@Stable
data class ChartData(
    val cumulativeProfit: Double,
    val date: Long
)

@Stable
data class ChartDataPoint(
    val value: Long,
    val isPositive: Boolean
)

@Stable
data class DragSelection(
    val position: Offset,
    val item: ChartDataPoint,
    val interpolatedValue: Long = 0L,
    val index: Int = -1
)

@Stable
data class CubicSegment(
    val start: Offset,
    val control1: Offset,
    val control2: Offset,
    val end: Offset
) {
    fun getPointAt(t: Float): Offset {
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

interface DragGestureListener {
    fun onDrag(offset: Offset)
    fun onDragStart(offset: Offset)
    fun onDragEnd()
}

abstract class DragHandler : DragGestureListener {
    abstract fun selectedBy(offset: Offset): DragSelection

    private val _selected = MutableStateFlow(DRAG_SELECTION_NONE)
    val selected = _selected.asStateFlow()

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

fun Modifier.bindDragGestures(vararg listeners: DragGestureListener) = pointerInput(Unit) {
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

class ChartCanvasManager(private val itemCount: Int) {
    private var canvasSize = Size.Unspecified
    private var offsets = listOf<Offset>()
    private var lineSegments = listOf<CubicSegment>()

    fun getCurrentOffsets(): List<Offset> = offsets

    fun calculateOffsets(
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

    fun getLineSegments() = lineSegments
    fun getCanvasSize() = canvasSize
}

class ChartValueCalculator(private val items: List<ChartDataPoint>) {
    val valueRange: Triple<Long, Long, Long> by lazy {
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

    fun yFractionOf(value: Long): Float = yFractionCache.getOrPut(value) {
        val (min, max) = valueRange
        val paddedMin = min - (max - min) * 0.05
        val paddedMax = max + (max - min) * 0.05
        1f - ((value - paddedMin) / (paddedMax - paddedMin)).toFloat()
    }

    fun calculateYAxisTicks(yAxisTicksCount: Int): List<Long> {
        val (min, max, range) = valueRange
        val paddedMin = min - (range * 0.05)
        val paddedMax = max + (range * 0.05)

        return List(yAxisTicksCount) { index ->
            val fraction = index / (yAxisTicksCount - 1).toFloat()
            (paddedMin + (paddedMax - paddedMin) * fraction).toLong()
        }.asReversed()
    }
}

class ChartDragManager(
    private val items: List<ChartDataPoint>,
    private val canvasManager: ChartCanvasManager,
    private val valueCalculator: ChartValueCalculator
) {
    private var lastSelectedIndex = -1

    fun findClosestPoint(point: Offset): Triple<Offset, Int, ChartDataPoint> {
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

    fun updateLastSelectedIndex(index: Int) {
        lastSelectedIndex = index
    }

    fun getLastSelectedIndex() = lastSelectedIndex
}

class LineChartState(
    private val items: List<ChartDataPoint>,
    private val defaultPositive: Boolean,
    val labels: List<String>,
    val strokeWidth: Dp,
    val yAxisTicksCount: Int,
) : DragHandler() {
    val itemCount = items.size
    private val valueFormatter: (Long) -> String = { value -> "%.1f".format(value / 10000.0) }
    val formatValue: (Long) -> String = valueFormatter

    private val canvasManager = ChartCanvasManager(itemCount)
    private val valueCalculator = ChartValueCalculator(items)
    private val dragManager = ChartDragManager(items, canvasManager, valueCalculator)

    val yAxisTicks by lazy { valueCalculator.calculateYAxisTicks(yAxisTicksCount) }

    fun stroke(scope: DrawScope) = with(scope) {
        Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
    }

    fun getOffsets(scope: DrawScope): List<Offset> =
        canvasManager.calculateOffsets(scope, items, valueCalculator::yFractionOf)

    val isPositive: Boolean get() = if (selected.value == DRAG_SELECTION_NONE) defaultPositive else selected.value.item.isPositive

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
fun rememberLineChartState(
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
fun infiniteTransitionValue(
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