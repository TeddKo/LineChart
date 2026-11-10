package tddy.ko.linechart.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import tddy.ko.linechart.ui.MaType
import tddy.ko.linechart.ui.MovingAverage
import tddy.ko.linechart.ui.Overlay
import tddy.ko.linechart.ui.PaneIndicator

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
