package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChartSettingsTest {

    @Test
    fun equality_isByValueIncludingTheCustomBaseLine() {
        assertEquals(ChartSettings(), ChartSettings())
        assertEquals(ChartSettings().hashCode(), ChartSettings().hashCode())
        assertNotEquals(ChartSettings(), ChartSettings(scale = PriceScale.Log))
        assertEquals(ChartSettings(baseLine = BaseLine.Custom(5.0)), ChartSettings(baseLine = BaseLine.Custom(5.0)))
        assertNotEquals(ChartSettings(baseLine = BaseLine.Custom(5.0)), ChartSettings(baseLine = BaseLine.Custom(6.0)))
    }

    @Test
    fun defaults_matchTheDesign() {
        val s = ChartSettings()
        assertEquals(3, s.rightMargin)
        assertEquals(GridLines.Horizontal, s.grid)
        assertEquals(AxisSide.Right, s.axisSide)
        assertEquals(30.0, s.limitPercent, 0.0)
    }

    @Test
    fun invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ChartSettings(rightMargin = -1) }
        assertThrows(IllegalArgumentException::class.java) { ChartSettings(limitPercent = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { ChartSettings(fontScale = 0f) }
    }
}
