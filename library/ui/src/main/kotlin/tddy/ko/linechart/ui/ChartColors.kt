package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Series rendering style of the chart.
 */
public enum class ChartType { Line, Area, Candle }

/**
 * Color palette of the chart with Korean (red up, blue down) and international (green up, red down) presets.
 *
 * @property up color of rising candles and values
 * @property down color of falling candles and values
 * @property flat color of unchanged candles and values
 * @property text color of axis labels
 * @property grid color of grid lines
 * @property crosshair color of the crosshair lines and tags
 * @property background color of the chart background
 */
@Immutable
public class ChartColors(
    public val up: Color = Color(0xFFE5323B),
    public val down: Color = Color(0xFF1E6FE8),
    public val flat: Color = Color(0xFF8B95A1),
    public val text: Color = Color(0xFF6B7684),
    public val grid: Color = Color(0x14000000),
    public val crosshair: Color = Color(0xFF333D4B),
    public val background: Color = Color(0xFFFFFFFF),
) {
    internal fun forDirection(direction: Int): Color = when {
        direction > 0 -> up
        direction < 0 -> down
        else -> flat
    }

    public companion object {
        public val Korean: ChartColors = ChartColors()

        public val KoreanDark: ChartColors = ChartColors(
            up = Color(0xFFFF5A5F),
            down = Color(0xFF4C8DFF),
            text = Color(0xFF8B95A1),
            grid = Color(0x1FFFFFFF),
            crosshair = Color(0xFFD1D6DB),
            background = Color(0xFF121212),
        )

        public val International: ChartColors = ChartColors(
            up = Color(0xFF26A69A),
            down = Color(0xFFEF5350),
        )

        public val InternationalDark: ChartColors = ChartColors(
            up = Color(0xFF26A69A),
            down = Color(0xFFEF5350),
            text = Color(0xFF8B95A1),
            grid = Color(0x1FFFFFFF),
            crosshair = Color(0xFFD1D6DB),
            background = Color(0xFF121212),
        )
    }
}
