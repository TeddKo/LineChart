package tddy.ko.linechart.feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.lifecycle.ViewModelProvider
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
import tddy.ko.linechart.ui.ChartData
import tddy.ko.linechart.ui.LineChartPanel
import tddy.ko.linechart.ui.Timeframe
import tddy.ko.linechart.ui.TimeframeUnit
import tddy.ko.linechart.ui.aggregate
import tddy.ko.linechart.ui.rememberLineChartState
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random

private const val TICK_MILLIS = 250L
private const val HISTORY_TICKS = 20_000
private const val HISTORY_STEP_MILLIS = 30_000L
private const val DAILY_YEARS = 8
private const val DAY_MILLIS = 86_400_000L
private const val BASE_PRICE = 71_000.0
private const val SPLIT_DAYS_AGO = 400L

private val Counts = listOf(1, 3, 5, 10, 15, 30, 60, 120, 240)

private fun TimeframeUnit.label(): String = when (this) {
    TimeframeUnit.Tick -> "틱"
    TimeframeUnit.Second -> "초"
    TimeframeUnit.Minute -> "분"
    TimeframeUnit.Day -> "일"
    TimeframeUnit.Week -> "주"
    TimeframeUnit.Month -> "월"
    TimeframeUnit.Year -> "년"
}

private fun TimeframeUnit.sheetTitle(): String = when (this) {
    TimeframeUnit.Tick -> "틱 간격"
    TimeframeUnit.Second -> "초봉 간격"
    else -> "분봉 간격"
}

private val TimeframeUnit.intraday: Boolean get() = ordinal <= TimeframeUnit.Minute.ordinal

private fun nextTick(random: Random, previous: Double, date: Long): ChartData {
    val close = (previous * (1.0 + random.nextDouble(-0.0006, 0.0006))).roundToLong().coerceAtLeast(1L).toDouble()
    return ChartData(close, date, open = previous, high = maxOf(previous, close), low = minOf(previous, close), volume = random.nextLong(1L, 100L).toDouble())
}

private fun tickHistory(random: Random, end: Long): List<ChartData> {
    var price = BASE_PRICE
    return List(HISTORY_TICKS) { index ->
        nextTick(random, price, end - (HISTORY_TICKS - index) * HISTORY_STEP_MILLIS).also { price = it.cumulativeProfit }
    }
}

private fun ChartData.unadjusted(): ChartData =
    copy(cumulativeProfit = cumulativeProfit * 2, open = open * 2, high = high * 2, low = low * 2, volume = volume / 2)

private fun dailySeries(random: Random, end: Long): List<ChartData> {
    var price = BASE_PRICE
    val days = DAILY_YEARS * 365
    return List(days) { index ->
        val open = price
        val close = (open * (1.0 + random.nextDouble(-0.025, 0.026))).roundToLong().coerceAtLeast(1L).toDouble()
        val high = (maxOf(open, close) * (1.0 + random.nextDouble(0.0, 0.01))).roundToLong().toDouble()
        val low = (minOf(open, close) * (1.0 - random.nextDouble(0.0, 0.01))).roundToLong().toDouble()
        price = close
        ChartData(close, end - (days - index) * DAY_MILLIS, open, high, low, random.nextLong(1_000L, 90_000L).toDouble())
    }
}

private fun formatChange(percent: Double): String = String.format(Locale.ROOT, "%+.2f%%", percent)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val vm = ViewModelProvider(this)[DemoViewModel::class.java]
        setContent { LineChartTheme(darkTheme = vm.prefs.theme.isDark()) { Screens(vm) } }
    }
}

@Composable
private fun Screens(vm: DemoViewModel) {
    var settings by rememberSaveable { mutableStateOf(false) }
    if (settings) SettingsScreen(vm, onBack = { settings = false }) else Demo(vm, onSettings = { settings = true })
}

@Composable
private fun Demo(vm: DemoViewModel, onSettings: () -> Unit) {
    val random = remember { Random(7) }
    val now = remember { System.currentTimeMillis() }
    val adjustedDaily = remember { dailySeries(random, now) }
    val rawDaily = remember { adjustedDaily.map { if (it.date < now - SPLIT_DAYS_AGO * DAY_MILLIS) it.unadjusted() else it } }
    var ticks by remember { mutableStateOf(tickHistory(random, now)) }
    var unit by rememberSaveable { mutableStateOf(TimeframeUnit.Minute) }
    var counts by rememberSaveable {
        mutableStateOf(mapOf(TimeframeUnit.Tick to 1, TimeframeUnit.Second to 1, TimeframeUnit.Minute to 5))
    }
    var sheetUnit by remember { mutableStateOf<TimeframeUnit?>(null) }
    var showIndicators by rememberSaveable { mutableStateOf(false) }
    val timeframe = Timeframe(unit, counts[unit] ?: 1)
    var items by remember { mutableStateOf(emptyList<ChartData>()) }
    val prefs = vm.prefs
    val daily = if (prefs.adjusted) adjustedDaily else rawDaily

    LaunchedEffect(timeframe, daily) {
        if (timeframe.unit.intraday) {
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
    val overlays = remember(vm.overlays, vm.maLines) { vm.builtOverlays() }
    val panes = remember(vm.panes) { vm.builtPanes() }
    val colors = prefs.colors(prefs.theme.isDark())
    val settings = remember(prefs) { prefs.settings() }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DEMO/KRW", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { showIndicators = true }) { Text("지표") }
            TextButton(onClick = onSettings) { Text("설정") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(last?.let { String.format(Locale.ROOT, "%,.0f", it.cumulativeProfit) }.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            Text(formatChange(change), style = MaterialTheme.typography.titleMedium, color = if (change >= 0) colors.up else colors.down)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeframeUnit.entries.forEach { value ->
                val count = counts[value]
                FilterChip(
                    selected = unit == value,
                    onClick = {
                        unit = value
                        if (value.intraday) sheetUnit = value
                    },
                    label = { Text(if (count != null) "$count${value.label()} ▾" else value.label()) },
                )
            }
        }
        LineChartPanel(
            state = state,
            chartType = prefs.chartType,
            colors = colors,
            overlays = overlays,
            panes = panes,
            settings = settings,
            timeframe = timeframe,
            priceFormatter = { String.format(Locale.ROOT, "%,.0f", it) },
        )
    }

    if (showIndicators) IndicatorSheet(vm, onDismiss = { showIndicators = false })
    sheetUnit?.let { target ->
        CountSheet(target, counts[target] ?: 1, onSelect = { counts = counts + (target to it) }, onDismiss = { sheetUnit = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountSheet(unit: TimeframeUnit, selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(unit.sheetTitle(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            Counts.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { count ->
                        val onClick = {
                            onSelect(count)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            Unit
                        }
                        val modifier = Modifier.weight(1f).height(56.dp)
                        if (count == selected) {
                            Button(onClick, modifier) { Text("$count${unit.label()}") }
                        } else {
                            FilledTonalButton(onClick, modifier) { Text("$count${unit.label()}") }
                        }
                    }
                }
            }
        }
    }
}
