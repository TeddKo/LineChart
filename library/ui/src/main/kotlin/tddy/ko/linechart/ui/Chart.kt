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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

private const val ENTRY_MILLIS = 600
private const val TEXT_CACHE_SIZE = 128

@Composable
private fun defaultColors(): ChartColors = if (isSystemInDarkTheme()) ChartColors.KoreanDark else ChartColors.Korean

@Composable
private fun ChartBody(
    modifier: Modifier,
    state: LineChartState,
    chartType: ChartType,
    colors: ChartColors,
    maPeriods: List<Int>,
    timeframe: Timeframe?,
    priceFormatter: (Double) -> String,
    timeFormatter: ((Long) -> String)?,
    textStyle: TextStyle,
) {
    val measurer = rememberTextMeasurer(TEXT_CACHE_SIZE)
    val spec = remember(chartType, colors, maPeriods, timeframe, priceFormatter, timeFormatter, textStyle, measurer) {
        ChartSpec(chartType, colors, maPeriods, timeframe, priceFormatter, timeFormatter, textStyle, measurer)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.itemCount }.first { it > 0 }
        if (state.entry.value < 1f) state.entry.animateTo(1f, tween(ENTRY_MILLIS))
    }
    Box(modifier) {
        Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind { drawMain(state, spec) })
    }
}

/**
 * Chart with the default Candle type, Korean palette and moving averages, filling the size given by [modifier].
 */
@Composable
public fun LineChart(
    modifier: Modifier,
    state: LineChartState,
) {
    ChartBody(modifier, state, ChartType.Candle, defaultColors(), DEFAULT_MA_PERIODS, null, ::formatPrice, null, ChartTextStyle)
}

/**
 * Interactive line, area or candle chart with pan, fling, pinch zoom, long-press crosshair and live updates.
 *
 * Feed new data by passing a new list to [rememberLineChartState]; the viewport, zoom and crosshair survive updates.
 * [timeframe] only labels the time axis and the moving average legend, use [aggregate] to build the candles.
 *
 * @param modifier layout modifier
 * @param chartHeight chart height
 * @param state chart state holding the series and the viewport
 * @param chartType series style
 * @param colors palette
 * @param maPeriods moving average periods counted in candles of the current timeframe
 * @param timeframe timeframe of the items, null to infer the label pattern from the data step
 * @param priceFormatter price text of axis labels, tags and header
 * @param timeFormatter time text overriding the timeframe pattern
 * @param textStyle style of axis and header text
 */
@Composable
public fun LineChartPanel(
    modifier: Modifier = Modifier,
    chartHeight: Dp = 300.dp,
    state: LineChartState,
    chartType: ChartType = ChartType.Candle,
    colors: ChartColors = defaultColors(),
    maPeriods: List<Int> = DEFAULT_MA_PERIODS,
    timeframe: Timeframe? = null,
    priceFormatter: (Double) -> String = ::formatPrice,
    timeFormatter: ((Long) -> String)? = null,
    textStyle: TextStyle = ChartTextStyle,
) {
    ChartBody(
        modifier.fillMaxWidth().height(chartHeight),
        state, chartType, colors, maPeriods, timeframe, priceFormatter, timeFormatter, textStyle,
    )
}

/**
 * Legacy 1.0.0 entry point kept for binary compatibility.
 */
@Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
public fun LineChartPanel(
    modifier: Modifier = Modifier,
    chartHeight: Dp = 300.dp,
    state: LineChartState,
) {
    LineChartPanel(modifier, chartHeight, state, ChartType.Candle)
}

private val DEFAULT_MA_PERIODS = listOf(5, 20, 60, 120)
