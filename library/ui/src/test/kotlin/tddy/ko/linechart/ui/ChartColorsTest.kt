package tddy.ko.linechart.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChartColorsTest {

    @Test
    fun chartType_hasLineAreaCandle() {
        assertEquals(listOf("Line", "Area", "Candle"), ChartType.entries.map { it.name })
    }

    @Test
    fun korean_isRedUpBlueDown() {
        assertEquals(Color(0xFFE5323B), ChartColors.Korean.up)
        assertEquals(Color(0xFF1E6FE8), ChartColors.Korean.down)
        assertEquals(Color(0xFF8B95A1), ChartColors.Korean.flat)
        assertEquals(Color(0xFFFFFFFF), ChartColors.Korean.background)
    }

    @Test
    fun international_isGreenUpRedDown() {
        assertEquals(Color(0xFF26A69A), ChartColors.International.up)
        assertEquals(Color(0xFFEF5350), ChartColors.International.down)
    }

    @Test
    fun darkPresets_changeUpDownTextGridAndBackground() {
        assertEquals(Color(0xFFFF5A5F), ChartColors.KoreanDark.up)
        assertEquals(Color(0xFF4C8DFF), ChartColors.KoreanDark.down)
        assertEquals(Color(0xFF121212), ChartColors.KoreanDark.background)
        assertEquals(Color(0xFF8B95A1), ChartColors.KoreanDark.text)
        assertEquals(Color(0x1FFFFFFF), ChartColors.KoreanDark.grid)
        assertEquals(ChartColors.International.up, ChartColors.InternationalDark.up)
        assertEquals(ChartColors.International.down, ChartColors.InternationalDark.down)
        assertEquals(Color(0xFF121212), ChartColors.InternationalDark.background)
        assertNotEquals(ChartColors.Korean.grid, ChartColors.KoreanDark.grid)
    }

    @Test
    fun forDirection_mapsSignToUpDownFlat() {
        val colors = ChartColors.Korean
        assertEquals(colors.up, colors.forDirection(1))
        assertEquals(colors.down, colors.forDirection(-1))
        assertEquals(colors.flat, colors.forDirection(0))
    }

    @Test
    fun presets_areSingletons() {
        assertSame(ChartColors.Korean, ChartColors.Korean)
    }
}
