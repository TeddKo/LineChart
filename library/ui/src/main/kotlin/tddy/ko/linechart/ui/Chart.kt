/*
 * Copyright (c) 2024 TeddKo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tddy.ko.linechart.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Internal component that renders Y-axis labels
 *
 * @param yAxisTicks List of values to display on Y-axis
 * @param formatValue Function to format the numeric values
 * @param textColor Color for the axis labels
 * @param paddingValues Padding around the labels
 */
@Composable
private fun YAxisLabels(
    yAxisTicks: List<Long>,
    formatValue: (Long) -> String,
    textColor: Color,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(paddingValues = paddingValues),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        yAxisTicks.forEach { value ->
            Text(
                text = formatValue(value),
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

/**
 * Internal component that renders X-axis labels
 *
 * @param modifier Modifier for customizing the layout
 * @param labels List of labels to display on X-axis
 * @param textColor Color for the axis labels
 * @param paddingValues Padding around the labels
 */
@Composable
private fun XAxisLabels(
    modifier: Modifier = Modifier,
    labels: List<String>,
    textColor: Color,
    paddingValues: PaddingValues
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(paddingValues = paddingValues),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
        }
    }
}

/**
 * Internal component that shows tooltip when user interacts with the chart
 *
 * @param selectedValue Currently selected data point
 * @param formatValue Function to format the numeric values
 * @param surfaceColor Background color of the tooltip
 * @param textColor Text color of the tooltip
 * @param paddingValues Padding around the tooltip
 */
@Composable
private fun ChartTooltip(
    selectedValue: DragSelection,
    formatValue: (Long) -> String,
    surfaceColor: Color,
    textColor: Color,
    paddingValues: PaddingValues
) {
    val tooltipText = formatValue(selectedValue.item.value)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelMedium

    val textSize = remember(tooltipText) {
        textMeasurer.measure(
            text = tooltipText,
            style = textStyle
        )
    }

    val tooltipPadding = with(density) { 12.dp.toPx() }
    val tooltipWidth = textSize.size.width + (tooltipPadding * 2)
    val tooltipHeight = textSize.size.height + (tooltipPadding * 2)

    Column(
        modifier = Modifier
            .padding(paddingValues = paddingValues)
            .offset {
                val xOffset = selectedValue.position.x - (tooltipWidth / 2)
                val yOffset = selectedValue.position.y - tooltipHeight
                IntOffset(
                    x = xOffset.toInt(),
                    y = yOffset.toInt()
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = tooltipText,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp),
                    clip = true
                )
                .background(
                    color = surfaceColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .size(12.dp)
                .offset(y = (-6).dp)
        ) {
            val path = Path().apply {
                val width = size.width
                val height = size.height

                moveTo(0f, 0f)
                lineTo(width, 0f)
                quadraticTo(
                    width / 2f + 4.dp.toPx(), height / 2f,
                    width / 2f, height
                )
                quadraticTo(
                    width / 2f - 4.dp.toPx(), height / 2f,
                    0f, 0f
                )
                close()
            }

            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.1f),
                style = Fill,
                blendMode = BlendMode.SrcOver
            )

            drawPath(
                path = path,
                color = surfaceColor,
                style = Fill
            )
        }
    }
}

/**
 * Core chart drawing component that handles the actual line chart rendering
 *
 * @param modifier Modifier for customizing the layout
 * @param state LineChartState containing chart data and configuration
 * @param yAnim Animation state for Y-axis values
 * @param transitionValue Animation progress for visual effects
 * @param surfaceColor Background color of the chart
 * @param positiveColor Color used for positive values
 * @param negativeColor Color used for negative values
 * @param paddingValues Padding around the chart
 */
@Composable
private fun ChartCanvas(
    modifier: Modifier = Modifier,
    state: LineChartState,
    yAnim: Animatable<Float, *>,
    transitionValue: Float,
    surfaceColor: Color,
    positiveColor: Color,
    negativeColor: Color,
    paddingValues: PaddingValues
) {
    val chartPath = remember { Path() }
    val selectedValue by state.selected.collectAsState()

    val animateChartColor by animateColorAsState(
        targetValue = if (state.isPositive) positiveColor else negativeColor,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "animate chart color"
    )

    Box {
        Canvas(
            modifier = modifier
                .padding(paddingValues = paddingValues)
                .bindDragGestures(state)
        ) {
            val offsets = state.getOffsets(this)
            val end = offsets.last()
            val strokeWidthPx = state.strokeWidth.toPx()

            // 전체 차트 경로 생성
            chartPath.reset()
            if (offsets.size >= 2) {
                chartPath.moveTo(offsets[0].x, lerp(size.height, offsets[0].y, yAnim.value))

                for (i in 1 until offsets.size) {
                    val prevPoint = offsets[i - 1]
                    val currentPoint = offsets[i]

                    val controlPoint1X = prevPoint.x + (currentPoint.x - prevPoint.x) * 0.5f
                    val controlPoint1Y = lerp(size.height, prevPoint.y, yAnim.value)
                    val controlPoint2X = prevPoint.x + (currentPoint.x - prevPoint.x) * 0.5f
                    val controlPoint2Y = lerp(size.height, currentPoint.y, yAnim.value)

                    chartPath.cubicTo(
                        controlPoint1X, controlPoint1Y,
                        controlPoint2X, controlPoint2Y,
                        currentPoint.x, lerp(size.height, currentPoint.y, yAnim.value)
                    )
                }
            }

            val stroke = state.stroke(this)

            if (selectedValue != DRAG_SELECTION_NONE) {
                val selectedX = selectedValue.position.x

                clipRect(
                    left = -selectedX,
                    top = 0f,
                    right = selectedX,
                    bottom = size.height
                ) {
                    drawPath(
                        path = chartPath,
                        color = animateChartColor,
                        style = stroke
                    )
                }

                clipRect(
                    left = selectedX,
                    right = size.width + strokeWidthPx,
                    bottom = size.height,
                    top = 0f
                ) {
                    drawPath(
                        path = chartPath,
                        color = Color.Gray,
                        style = stroke
                    )
                }

                drawCircle(
                    color = animateChartColor,
                    radius = 3.5.dp.toPx() + (15.dp.toPx() * transitionValue),
                    alpha = .6f - (.6f * transitionValue),
                    center = selectedValue.position
                )
                drawCircle(
                    color = animateChartColor,
                    radius = 3.5.dp.toPx() + (.5.dp.toPx() * transitionValue),
                    center = selectedValue.position
                )
            } else {
                drawPath(
                    path = chartPath,
                    color = animateChartColor,
                    style = stroke
                )

                drawCircle(
                    color = animateChartColor,
                    radius = 3.5.dp.toPx() + (15.dp.toPx() * transitionValue),
                    alpha = .6f - (.6f * transitionValue),
                    center = end
                )
                drawCircle(
                    color = animateChartColor,
                    radius = 3.5.dp.toPx() + (.5.dp.toPx() * transitionValue),
                    center = end
                )
            }
        }
        if (selectedValue != DRAG_SELECTION_NONE) {
            ChartTooltip(
                selectedValue = selectedValue,
                formatValue = state.formatValue,
                surfaceColor = surfaceColor,
                textColor = animateChartColor,
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
fun LineChart(
    modifier: Modifier,
    state: LineChartState,
) {
    val transitionValue = infiniteTransitionValue(targetValue = 1f, durationMillis = 1500)
    val positiveColor = MaterialTheme.colorScheme.inversePrimary
    val negativeColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onSurface
    val yAnim = remember { Animatable(.1f) }
    val surfaceColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(state) {
        yAnim.snapTo(.0f)
        yAnim.animateTo(1f)
    }

    Row(modifier = modifier) {
        YAxisLabels(
            yAxisTicks = state.yAxisTicks,
            formatValue = state.formatValue,
            textColor = textColor,
            paddingValues = PaddingValues(vertical = 20.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            ChartCanvas(
                modifier = Modifier.fillMaxSize(),
                state = state,
                yAnim = yAnim,
                transitionValue = transitionValue,
                surfaceColor = surfaceColor,
                positiveColor = positiveColor,
                negativeColor = negativeColor,
                paddingValues = PaddingValues(top = 20.dp, bottom = 20.dp, end = 20.dp)
            )

            XAxisLabels(
                modifier = Modifier.align(Alignment.BottomCenter),
                labels = state.labels,
                textColor = textColor,
                paddingValues = PaddingValues(end = 20.dp)
            )
        }
    }
}

/**
 * LineChart Component for Jetpack Compose
 *
 * This component provides an interactive line chart with features like:
 * - Smooth animations
 * - Touch interaction with tooltips
 * - Customizable colors and styles
 * - Automatic scaling
 * - Y-axis and X-axis labels
 * - Support for positive and negative values
 *
 * Basic usage example:
 * ```
 * LineChartPanel(
 *     state = rememberLineChartState(
 *         items = yourDataList,
 *         maxDataPoints = 20,  // Optional: limit number of points
 *         yAxisTicksCount = 5   // Optional: number of y-axis labels
 *     )
 * )
 * ```
 */

/**
 * Main container component for the line chart
 *
 * @param modifier Modifier for customizing the layout
 * @param chartHeight Height of the chart (default: 300.dp)
 * @param state LineChartState that contains the chart data and configuration
 */
@Composable
fun LineChartPanel(
    modifier: Modifier = Modifier,
    chartHeight: Dp = 300.dp,
    state: LineChartState
) {
    if (state.itemCount < 2) return

    LineChart(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
        state = state
    )
}