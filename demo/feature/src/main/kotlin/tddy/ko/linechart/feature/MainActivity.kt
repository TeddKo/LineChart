package tddy.ko.linechart.feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import tddy.ko.linechart.desigh_system.theme.LineChartTheme
import tddy.ko.linechart.ui.ChartColors
import tddy.ko.linechart.ui.ChartData
import tddy.ko.linechart.ui.ChartType
import tddy.ko.linechart.ui.LineChartPanel
import tddy.ko.linechart.ui.Timeframe
import tddy.ko.linechart.ui.TimeframeUnit
import tddy.ko.linechart.ui.aggregate
import tddy.ko.linechart.ui.rememberLineChartState
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random

private const val TICK_MILLIS = 250L
private const val HISTORY_TICKS = 14_400
private const val HISTORY_STEP_MILLIS = 1_000L
private const val DAILY_YEARS = 8
private const val DAY_MILLIS = 86_400_000L
private const val BASE_PRICE = 71_000.0

private val Timeframes = listOf(
    "Tick" to Timeframe.Tick,
    "1s" to Timeframe.Second1,
    "5s" to Timeframe.Second5,
    "1m" to Timeframe.Minute1,
    "5m" to Timeframe.Minute5,
    "Day" to Timeframe.Day,
    "Week" to Timeframe.Week,
    "Month" to Timeframe.Month,
    "Year" to Timeframe.Year,
)

private fun nextTick(random: Random, previous: Double, date: Long): ChartData {
    val close = (previous * (1.0 + random.nextDouble(-0.0006, 0.0006))).roundToLong().coerceAtLeast(1L).toDouble()
    return ChartData(close, date, open = previous, high = maxOf(previous, close), low = minOf(previous, close), volume = random.nextDouble(1.0, 100.0))
}

private fun tickHistory(random: Random, end: Long): List<ChartData> {
    var price = BASE_PRICE
    return List(HISTORY_TICKS) { index ->
        nextTick(random, price, end - (HISTORY_TICKS - index) * HISTORY_STEP_MILLIS).also { price = it.cumulativeProfit }
    }
}

private fun dailySeries(random: Random, end: Long): List<ChartData> {
    var price = BASE_PRICE
    val days = DAILY_YEARS * 365
    return List(days) { index ->
        val open = price
        val close = (open * (1.0 + random.nextDouble(-0.025, 0.026))).roundToLong().coerceAtLeast(1L).toDouble()
        val high = (maxOf(open, close) * (1.0 + random.nextDouble(0.0, 0.01))).roundToLong().toDouble()
        val low = (minOf(open, close) * (1.0 - random.nextDouble(0.0, 0.01))).roundToLong().toDouble()
        price = close
        ChartData(close, end - (days - index) * DAY_MILLIS, open, high, low, random.nextDouble(1_000.0, 90_000.0))
    }
}

private fun formatChange(percent: Double): String = String.format(Locale.ROOT, "%+.2f%%", percent)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { LineChartTheme { Demo() } }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun Demo() {
    val random = remember { Random(7) }
    val now = remember { System.currentTimeMillis() }
    val daily = remember { dailySeries(random, now) }
    var ticks by remember { mutableStateOf(tickHistory(random, now)) }
    var timeframe by remember { mutableStateOf(Timeframe.Minute1) }
    var chartType by remember { mutableStateOf(ChartType.Candle) }
    var international by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(emptyList<ChartData>()) }
    val dark = isSystemInDarkTheme()

    LaunchedEffect(timeframe) {
        if (timeframe.unit == TimeframeUnit.Tick || timeframe.unit == TimeframeUnit.Second || timeframe.unit == TimeframeUnit.Minute) {
            while (true) {
                items = withContext(Dispatchers.Default) { ticks.aggregate(timeframe) }
                delay(TICK_MILLIS)
                ticks = ticks + nextTick(random, ticks.last().cumulativeProfit, System.currentTimeMillis())
            }
        } else {
            items = withContext(Dispatchers.Default) { daily.aggregate(timeframe) }
        }
    }

    val state = rememberLineChartState(items)
    val colors = when {
        international && dark -> ChartColors.InternationalDark
        international -> ChartColors.International
        dark -> ChartColors.KoreanDark
        else -> ChartColors.Korean
    }
    val last = items.lastOrNull()
    val previous = items.getOrNull(items.size - 2)
    val change = if (last != null && previous != null && previous.cumulativeProfit != 0.0) {
        (last.cumulativeProfit / previous.cumulativeProfit - 1.0) * 100.0
    } else {
        0.0
    }

    Column(
        Modifier.statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("DEMO/KRW", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(last?.let { String.format(Locale.ROOT, "%,.0f", it.cumulativeProfit) }.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            Text(formatChange(change), style = MaterialTheme.typography.titleMedium, color = if (change >= 0) colors.up else colors.down)
        }
        Segmented(listOf("Candle" to ChartType.Candle, "Line" to ChartType.Line, "Area" to ChartType.Area), chartType) { chartType = it }
        Segmented(listOf("Korean" to false, "International" to true), international) { international = it }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Timeframes.forEach { (label, value) ->
                FilterChip(selected = timeframe == value, onClick = { timeframe = value }, label = { Text(label) })
            }
        }
        LineChartPanel(
            chartHeight = 360.dp,
            state = state,
            chartType = chartType,
            colors = colors,
            timeframe = timeframe,
        )
    }
}
