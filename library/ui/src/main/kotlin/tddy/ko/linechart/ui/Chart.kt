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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first

private val PANE_HEIGHT = 80.dp
private val PANE_GAP = 4.dp
private const val ENTRY_MILLIS = 600
private const val PULSE_MILLIS = 1500
private const val TEXT_CACHE_SIZE = 128

private val NAME_RES: Map<String, Int> = mapOf(
    "bb" to R.string.linechart_name_bb,
    "env" to R.string.linechart_name_env,
    "pc" to R.string.linechart_name_pc,
    "ichimoku" to R.string.linechart_name_ichimoku,
    "sar" to R.string.linechart_name_sar,
    "pivot" to R.string.linechart_name_pivot,
    "vwap" to R.string.linechart_name_vwap,
    "vp" to R.string.linechart_name_vp,
    "macd" to R.string.linechart_name_macd,
    "macdo" to R.string.linechart_name_macdo,
    "trix" to R.string.linechart_name_trix,
    "sonar" to R.string.linechart_name_sonar,
    "rsi" to R.string.linechart_name_rsi,
    "stochf" to R.string.linechart_name_stochf,
    "stochs" to R.string.linechart_name_stochs,
    "willr" to R.string.linechart_name_willr,
    "mfi" to R.string.linechart_name_mfi,
    "psy" to R.string.linechart_name_psy,
    "cci" to R.string.linechart_name_cci,
    "dmi" to R.string.linechart_name_dmi,
    "atr" to R.string.linechart_name_atr,
    "tv" to R.string.linechart_name_tv,
    "obv" to R.string.linechart_name_obv,
    "vr" to R.string.linechart_name_vr,
    "vo" to R.string.linechart_name_vo,
    "mom" to R.string.linechart_name_mom,
    "roc" to R.string.linechart_name_roc,
    "disp" to R.string.linechart_name_disp,
    "mid" to R.string.linechart_name_mid,
    "upper" to R.string.linechart_name_upper,
    "lower" to R.string.linechart_name_lower,
    "signal" to R.string.linechart_name_signal,
    "osc" to R.string.linechart_name_osc,
    "conv" to R.string.linechart_name_conv,
    "base" to R.string.linechart_name_base,
    "lag" to R.string.linechart_name_lag,
    "span1" to R.string.linechart_name_span1,
    "span2" to R.string.linechart_name_span2,
)

private fun TimeframeUnit.labelRes(): Int = when (this) {
    TimeframeUnit.Tick -> R.string.linechart_unit_tick
    TimeframeUnit.Second -> R.string.linechart_unit_second
    TimeframeUnit.Minute -> R.string.linechart_unit_minute
    TimeframeUnit.Day -> R.string.linechart_unit_day
    TimeframeUnit.Week -> R.string.linechart_unit_week
    TimeframeUnit.Month -> R.string.linechart_unit_month
    TimeframeUnit.Year -> R.string.linechart_unit_year
}

@Composable
private fun defaultColors(): ChartColors = if (isSystemInDarkTheme()) ChartColors.KoreanDark else ChartColors.Korean

@Composable
private fun ChartBody(
    modifier: Modifier,
    state: LineChartState,
    chartType: ChartType,
    colors: ChartColors,
    chartHeight: Dp?,
    overlays: List<Overlay>,
    panes: List<PaneIndicator>,
    settings: ChartSettings,
    paneHeight: Dp,
    timeframe: Timeframe?,
    priceFormatter: (Double) -> String,
    timeFormatter: ((Long) -> String)?,
    textStyle: TextStyle,
) {
    val measurer = rememberTextMeasurer(TEXT_CACHE_SIZE)
    val scaledStyle = if (settings.fontScale == 1f || !textStyle.fontSize.isSpecified) textStyle else textStyle.copy(fontSize = textStyle.fontSize * settings.fontScale)
    val labels = ChartLabels(
        stringResource(R.string.linechart_open),
        stringResource(R.string.linechart_high),
        stringResource(R.string.linechart_low),
        stringResource(R.string.linechart_close),
        stringResource(R.string.linechart_volume),
        timeframe?.let { stringResource(it.unit.labelRes()) }.orEmpty(),
        NAME_RES.mapValues { stringResource(it.value) },
    )
    val spec = remember(chartType, colors, timeframe, priceFormatter, timeFormatter, scaledStyle, measurer, labels, paneHeight) {
        ChartSpec(chartType, colors, timeframe, priceFormatter, timeFormatter, scaledStyle, measurer, labels, paneHeight)
    }
    val pulse = if (chartType == ChartType.Candle) {
        null
    } else {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(PULSE_MILLIS, easing = LinearEasing)),
            label = "pulseValue",
        )
    }
    remember(state) { state.setIndicators(overlays, panes, settings) }
    SideEffect { state.setIndicators(overlays, panes, settings) }
    LaunchedEffect(state) {
        snapshotFlow { state.itemCount }.first { it > 0 }
        if (state.entry.value < 1f) state.entry.animateTo(1f, tween(ENTRY_MILLIS))
    }
    val hasVolume by remember(state) { derivedStateOf { state.series.hasVolume } }
    val shown = panes.filter { !it.needsVolume || hasVolume }
    val sized = if (chartHeight == null) modifier else {
        modifier.fillMaxWidth().height(chartHeight + paneHeight * shown.sumOf { it.weight.toDouble() }.toFloat() + PANE_GAP * shown.size)
    }
    Box(sized.chartGestures(state, LocalHapticFeedback.current)) {
        Spacer(Modifier.fillMaxSize().graphicsLayer().drawBehind { drawMain(state, spec) })
        Spacer(Modifier.fillMaxSize().drawBehind { drawOverlay(state, spec, pulse?.value) })
    }
}

/**
 * Chart with the default Candle type, Korean palette, moving averages and volume pane, filling the size given by [modifier].
 */
@Composable
public fun LineChart(
    modifier: Modifier,
    state: LineChartState,
) {
    ChartBody(modifier, state, ChartType.Candle, defaultColors(), null, DEFAULT_OVERLAYS, DEFAULT_PANES, DEFAULT_SETTINGS, PANE_HEIGHT, null, ::formatPrice, null, ChartTextStyle)
}

/**
 * Interactive line, area or candle chart with pan, fling, pinch zoom, long-press crosshair, pane resizing and live updates.
 *
 * A long press away from the separators shows the crosshair. A long press within 12dp of a separator between two areas instead highlights it with a haptic tick, and dragging then moves it, growing the area above and shrinking the one below by the same amount down to 120dp for the main area and 40dp for a pane. The resized heights live in [state], surviving data updates and indicator parameter changes and being dropped for removed panes, but not saved across recreation.
 *
 * Feed new data by passing a new list to [rememberLineChartState]; the viewport, zoom and crosshair survive updates.
 * [timeframe] only labels the time axis and the moving average legend, use [aggregate] to build the candles.
 *
 * @param modifier layout modifier
 * @param chartHeight height of the main chart area with header and time axis, the panes adding their height below it
 * @param state chart state holding the series and the viewport
 * @param chartType series style
 * @param colors palette
 * @param overlays indicators drawn on the main pane
 * @param panes indicators drawn in panes below the main pane
 * @param settings display options
 * @param paneHeight height of a pane of weight 1, each pane adding its height below [chartHeight]
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
    overlays: List<Overlay> = DEFAULT_OVERLAYS,
    panes: List<PaneIndicator> = DEFAULT_PANES,
    settings: ChartSettings = DEFAULT_SETTINGS,
    paneHeight: Dp = PANE_HEIGHT,
    timeframe: Timeframe? = null,
    priceFormatter: (Double) -> String = ::formatPrice,
    timeFormatter: ((Long) -> String)? = null,
    textStyle: TextStyle = ChartTextStyle,
) {
    ChartBody(
        modifier,
        state, chartType, colors, chartHeight, overlays, panes, settings, paneHeight, timeframe, priceFormatter, timeFormatter, textStyle,
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

private val DEFAULT_OVERLAYS = listOf<Overlay>(MovingAverage())
private val DEFAULT_PANES = listOf<PaneIndicator>(Volume())
private val DEFAULT_SETTINGS = ChartSettings()
