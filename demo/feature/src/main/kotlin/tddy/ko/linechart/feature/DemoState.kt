package tddy.ko.linechart.feature

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import tddy.ko.linechart.ui.AxisSide
import tddy.ko.linechart.ui.BaseLine
import tddy.ko.linechart.ui.ChartColors
import tddy.ko.linechart.ui.ChartSettings
import tddy.ko.linechart.ui.ChartType
import tddy.ko.linechart.ui.GridLines
import tddy.ko.linechart.ui.MaType
import tddy.ko.linechart.ui.MovingAverage
import tddy.ko.linechart.ui.Overlay
import tddy.ko.linechart.ui.PaneIndicator
import tddy.ko.linechart.ui.PriceScale

enum class ThemeMode(val label: String) { System("시스템"), Light("라이트"), Dark("다크") }

data class Prefs(
    val chartType: ChartType = ChartType.Candle,
    val international: Boolean = false,
    val up: Color? = null,
    val down: Color? = null,
    val theme: ThemeMode = ThemeMode.System,
    val fontScale: Float = 1f,
    val scale: PriceScale = PriceScale.Linear,
    val axisSide: AxisSide = AxisSide.Right,
    val rightMargin: Int = 3,
    val adjusted: Boolean = false,
    val grid: GridLines = GridLines.Horizontal,
    val crosshair: Boolean = true,
    val legend: Boolean = true,
    val currentPrice: Boolean = true,
    val baseLine: BaseLine = BaseLine.None,
    val highLow: Boolean = true,
    val priceLimits: Boolean = false,
    val volumeMa: Boolean = true,
) {
    fun settings(): ChartSettings = ChartSettings(
        scale = scale, axisSide = axisSide, rightMargin = rightMargin, grid = grid, crosshair = crosshair, legend = legend,
        currentPrice = currentPrice, baseLine = baseLine, highLowMarkers = highLow, priceLimits = priceLimits,
        volumeMa = volumeMa, fontScale = fontScale,
    )

    fun colors(dark: Boolean): ChartColors {
        val base = when {
            international && dark -> ChartColors.InternationalDark
            international -> ChartColors.International
            dark -> ChartColors.KoreanDark
            else -> ChartColors.Korean
        }
        return ChartColors(up ?: base.up, down ?: base.down, base.flat, base.text, base.grid, base.crosshair, base.background)
    }
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

data class MaLine(val period: Int, val type: MaType, val color: Color, val width: Float, val visible: Boolean)

val Swatches = listOf(
    Color(0xFFE5323B), Color(0xFF1E6FE8), Color(0xFF2EB82E), Color(0xFFFF9800),
    Color(0xFF9C27B0), Color(0xFF8D6E63), Color(0xFF00897B), Color(0xFF333D4B),
)

private val DefaultMaLines = MovingAverage().let { ma ->
    ma.periods.mapIndexed { index, period -> MaLine(period, MaType.Simple, ma.colors[index], 1f, true) }
}

class DemoViewModel : ViewModel() {
    var overlays by mutableStateOf(listOf(Catalog.spec("ma").entry()))
    var panes by mutableStateOf(listOf(Catalog.spec("vol").entry()))
    var maLines by mutableStateOf(DefaultMaLines)
    var prefs by mutableStateOf(Prefs())

    fun reset() {
        overlays = listOf(Catalog.spec("ma").entry())
        panes = listOf(Catalog.spec("vol").entry())
        maLines = DefaultMaLines
        prefs = Prefs()
    }

    fun editMa(index: Int, change: (MaLine) -> MaLine) {
        maLines = maLines.mapIndexed { i, line -> if (i == index) change(line) else line }
    }

    fun edit(overlay: Boolean, index: Int, entry: Entry) {
        update(overlay, entries(overlay).mapIndexed { i, old -> if (i == index) entry else old })
    }

    fun move(overlay: Boolean, index: Int, delta: Int) {
        val list = entries(overlay).toMutableList()
        val target = index + delta
        if (target !in list.indices) return
        list.add(target, list.removeAt(index))
        update(overlay, list)
    }

    fun remove(overlay: Boolean, index: Int) {
        update(overlay, entries(overlay).filterIndexed { i, _ -> i != index })
    }

    fun entries(overlay: Boolean): List<Entry> = if (overlay) overlays else panes

    fun isOn(spec: IndicatorSpec): Boolean = entries(spec.overlay).any { it.key == spec.key }

    fun toggle(spec: IndicatorSpec, on: Boolean) {
        val current = entries(spec.overlay)
        val next = if (on) current + spec.entry() else current.filterNot { it.key == spec.key }
        update(spec.overlay, next)
    }

    fun update(overlay: Boolean, entries: List<Entry>) {
        if (overlay) overlays = entries else panes = entries
    }

    fun summary(entry: Entry): String = when {
        entry.key == "ma" -> maLines.joinToString(" · ") { it.period.toString() }
        entry.values.isEmpty() -> "-"
        else -> entry.values.joinToString(" · ") { formatParam(it) }
    }

    fun builtOverlays(): List<Overlay> = overlays.flatMap { entry ->
        if (entry.key == "ma") maOverlays(maLines) else listOf(entry.build() as Overlay)
    }

    fun builtPanes(): List<PaneIndicator> = panes.map { it.build() as PaneIndicator }
}

private fun maOverlays(lines: List<MaLine>): List<Overlay> = lines.groupBy { it.type }.map { (type, group) ->
    MovingAverage(group.map { it.period }, type, group.map { it.color }, group.map { it.width.dp }, group.map { it.visible })
}
