package tddy.ko.linechart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToLong

private const val Y_MARGIN = 1.05
private const val FILL_ALPHA = 0.15f
private val StrokeWidth = 1.5.dp
private val TextGap = 4.dp
private val TagPadding = 4.dp

/**
 * Market depth chart of cumulative quantity against price, bids stepping up to the left and asks to the right of a dashed mid line.
 *
 * Long press and drag shows a crosshair with the price and the cumulated quantity under it. An empty book draws an empty box.
 *
 * @param bids buy levels in any order
 * @param asks sell levels in any order
 * @param height height of the chart
 * @param colors bids use [ChartColors.up] and asks use [ChartColors.down]
 * @param priceFormatter text of a price
 */
@Composable
public fun DepthChart(
    bids: List<MarketLevel>,
    asks: List<MarketLevel>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    colors: ChartColors = marketDefaultColors(),
    priceFormatter: (Long) -> String = ::defaultMarketPrice,
    textStyle: TextStyle = ChartTextStyle,
) {
    val depth = remember(bids, asks) { depthOf(bids, asks) }
    val range = remember(depth) { depthXRange(depth) }
    val box = modifier.fillMaxWidth().height(height)
    if (range == null) {
        Box(box)
        return
    }
    val measurer = rememberTextMeasurer()
    val yMax = max(depth.maxCum, 1L) * Y_MARGIN
    val yTicks = remember(yMax) { niceTicks(0.0, yMax, 4).map { it.roundToLong() }.distinct() }
    val density = LocalDensity.current
    val axisWidth = remember(yTicks, textStyle, density) {
        yTicks.maxOf { measurer.measure(it.toString(), textStyle).size.width } + with(density) { TextGap.roundToPx() * 2 }
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var cursor by remember { mutableFloatStateOf(Float.NaN) }
    val plotWidth = (size.width - axisWidth).coerceAtLeast(0)
    val description = depthDescription(depth, priceFormatter)
    val resources = LocalResources.current
    val cursorText = {
        val x = cursor
        if (x.isNaN() || plotWidth == 0) {
            null
        } else {
            val price = (range.first + x.coerceIn(0f, plotWidth.toFloat()) / plotWidth * (range.second - range.first)).roundToLong()
            depth.cumulativeAt(price.toDouble())?.let { cum ->
                resources.getString(R.string.linechart_market_depth_cursor, priceFormatter(price), cum)
            } ?: resources.getString(R.string.linechart_market_depth_cursor_gap, priceFormatter(price))
        }
    }
    val paths = remember(depth) { DepthPaths() }
    Canvas(
        box
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { cursor = it.x },
                    onDrag = { change, _ ->
                        change.consume()
                        cursor = change.position.x
                    },
                    onDragEnd = { cursor = Float.NaN },
                    onDragCancel = { cursor = Float.NaN },
                )
            }
            .semantics(mergeDescendants = true) {
                contentDescription = description
                cursorText()?.let { stateDescription = it }
            },
    ) {
        drawDepth(depth, paths, range, yMax, yTicks, axisWidth.toFloat(), cursor, cursorText(), measurer, colors, priceFormatter, textStyle)
    }
}

@Composable
private fun depthDescription(depth: Depth, priceFormatter: (Long) -> String): String {
    val bid = depth.bidPrices.firstOrNull()
    val ask = depth.askPrices.firstOrNull()
    val spread = depth.spread
    return if (bid != null && ask != null && spread != null) {
        stringResource(R.string.linechart_market_depth_description, priceFormatter(bid), priceFormatter(ask), priceFormatter(spread))
    } else {
        stringResource(R.string.linechart_market_depth_description_one_side, priceFormatter(checkNotNull(bid ?: ask)))
    }
}

/**
 * This value limited to zero through [max], zero when [max] is negative.
 */
internal fun Float.coerceWithin(max: Float): Float = coerceAtMost(max).coerceAtLeast(0f)

private class DepthGeometry(val plotWidth: Float, val plotHeight: Float, val xMin: Double, val xMax: Double, val yMax: Double) {
    fun x(price: Double): Float = ((price - xMin) / (xMax - xMin) * plotWidth).toFloat()

    fun y(cumulative: Long): Float = (plotHeight - cumulative / yMax * plotHeight).toFloat()
}

private fun DrawScope.drawDepth(
    depth: Depth,
    paths: DepthPaths,
    range: Pair<Double, Double>,
    yMax: Double,
    yTicks: List<Long>,
    axisWidth: Float,
    cursor: Float,
    cursorText: String?,
    measurer: TextMeasurer,
    colors: ChartColors,
    priceFormatter: (Long) -> String,
    textStyle: TextStyle,
) {
    val axisStyle = textStyle.copy(color = colors.text)
    val primary = textStyle.color.takeOrElse { colors.crosshair }
    val gap = TextGap.toPx()
    val lineHeight = measurer.measure("0", axisStyle).size.height
    val plotWidth = size.width - axisWidth
    val plotHeight = size.height - lineHeight - gap
    if (plotWidth <= 0f || plotHeight <= 0f) return
    val g = DepthGeometry(plotWidth, plotHeight, range.first, range.second, yMax)
    val stroke = Stroke(StrokeWidth.toPx())

    for (tick in yTicks) {
        val y = g.y(tick)
        drawLine(colors.grid, Offset(0f, y), Offset(plotWidth, y), 1f)
        val label = measurer.measure(tick.toString(), axisStyle)
        drawText(label, topLeft = Offset(plotWidth + gap, (y - label.size.height / 2f).coerceWithin(plotHeight - label.size.height)))
    }
    var lastRight = Float.NEGATIVE_INFINITY
    for (tick in niceTicks(range.first, range.second, 5)) {
        val label = measurer.measure(priceFormatter(tick.roundToLong()), axisStyle)
        val left = (g.x(tick) - label.size.width / 2f).coerceWithin(plotWidth - label.size.width)
        if (left < lastRight + gap) continue
        drawText(label, topLeft = Offset(left, plotHeight + gap))
        lastRight = left + label.size.width
    }

    paths.update(depth, g)
    drawSide(paths.bid, paths.bidFill, colors.up, stroke)
    drawSide(paths.ask, paths.askFill, colors.down, stroke)

    depth.mid?.let { mid ->
        val x = g.x(mid)
        drawLine(
            colors.crosshair, Offset(x, 0f), Offset(x, plotHeight), 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        depth.spread?.let { spread ->
            val label = measurer.measure(priceFormatter(spread), axisStyle.copy(color = primary))
            drawText(label, topLeft = Offset((x - label.size.width / 2f).coerceWithin(plotWidth - label.size.width), 0f))
        }
    }

    if (!cursor.isNaN() && cursorText != null) {
        val x = cursor.coerceWithin(plotWidth)
        drawLine(colors.crosshair, Offset(x, 0f), Offset(x, plotHeight), 1f)
        val label = measurer.measure(cursorText, axisStyle.copy(color = colors.background))
        val pad = TagPadding.toPx()
        val left = (x - label.size.width / 2f).coerceWithin(plotWidth - label.size.width)
        drawRect(
            colors.crosshair, Offset(left - pad, 0f), Size(label.size.width + 2 * pad, label.size.height + 2 * pad), style = Fill,
        )
        drawText(label, topLeft = Offset(left, pad))
    }
}

private fun DrawScope.drawSide(path: Path, fill: Path, color: Color, stroke: Stroke) {
    drawPath(fill, color.copy(alpha = FILL_ALPHA))
    drawPath(path, color, style = stroke)
}

/**
 * Step paths and fills of both sides of one [Depth], rebuilt only when the geometry they were built for changes.
 */
private class DepthPaths {
    val bid = Path()
    val ask = Path()
    val bidFill = Path()
    val askFill = Path()
    private var key: List<Any>? = null

    fun update(depth: Depth, g: DepthGeometry) {
        val next = listOf(g.plotWidth, g.plotHeight, g.xMin, g.xMax, g.yMax)
        if (next == key) return
        key = next
        bidPath(bid, depth, g)
        askPath(ask, depth, g)
        close(bid, bidFill, g)
        close(ask, askFill, g)
    }

    private fun close(line: Path, fill: Path, g: DepthGeometry) {
        fill.rewind()
        fill.addPath(line)
        val bounds = fill.getBounds()
        fill.lineTo(bounds.right, g.plotHeight)
        fill.lineTo(bounds.left, g.plotHeight)
        fill.close()
    }
}

private fun bidPath(path: Path, depth: Depth, g: DepthGeometry) = path.apply {
    rewind()
    val n = depth.bidPrices.size
    if (n == 0) return@apply
    moveTo(0f, g.y(depth.bidCum[n - 1]))
    for (j in n - 1 downTo 0) {
        lineTo(g.x(depth.bidPrices[j].toDouble()), g.y(depth.bidCum[j]))
        lineTo(g.x(depth.bidPrices[j].toDouble()), g.y(if (j == 0) 0L else depth.bidCum[j - 1]))
    }
}

private fun askPath(path: Path, depth: Depth, g: DepthGeometry) = path.apply {
    rewind()
    val n = depth.askPrices.size
    if (n == 0) return@apply
    moveTo(g.x(depth.askPrices[0].toDouble()), g.plotHeight)
    for (i in 0 until n) {
        lineTo(g.x(depth.askPrices[i].toDouble()), g.y(depth.askCum[i]))
        lineTo(if (i < n - 1) g.x(depth.askPrices[i + 1].toDouble()) else g.plotWidth, g.y(depth.askCum[i]))
    }
}
