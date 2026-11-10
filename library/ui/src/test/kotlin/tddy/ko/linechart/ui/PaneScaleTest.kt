package tddy.ko.linechart.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneScaleTest {

    private fun line(vararg v: Double, offset: Int = 0) = LineMark("", v, Color.Red, 1.dp, offset)

    @Test
    fun fromZero_runsZeroToVisibleMax() {
        val r = paneYRange(listOf(BarMark(doubleArrayOf(5.0, 20.0, 10.0), Tone.Direction)), 0..2, PaneScale.FromZero, DoubleArray(0))
        assertEquals(0.0, r.min, 0.0)
        assertEquals(20.0, r.max, 0.0)
    }

    @Test
    fun fixed_ignoresTheData() {
        val r = paneYRange(listOf(line(1.0, 2.0)), 0..1, PaneScale.Fixed(0.0, 100.0), DoubleArray(0))
        assertEquals(0.0, r.min, 0.0)
        assertEquals(100.0, r.max, 0.0)
    }

    @Test
    fun around_isSymmetricAndIncludesLevels() {
        val r = paneYRange(listOf(line(1.0, 3.0, -2.0)), 0..2, PaneScale.Around(0.0), doubleArrayOf(5.0))
        assertEquals(-r.max, r.min, 1e-9)
        assertTrue(r.max >= 5.0)
    }

    @Test
    fun auto_padsTheVisibleExtentAndSkipsNaN() {
        val r = paneYRange(listOf(line(Double.NaN, 10.0, 20.0)), 0..2, PaneScale.Auto, DoubleArray(0))
        assertEquals(9.5, r.min, 1e-9)
        assertEquals(20.5, r.max, 1e-9)
    }

    @Test
    fun auto_flatDataGetsASpan() {
        val r = paneYRange(listOf(line(5.0, 5.0)), 0..1, PaneScale.Auto, DoubleArray(0))
        assertTrue(r.max > r.min)
    }

    @Test
    fun markOffsetShiftsValuesIntoSlots() {
        val r = paneYRange(listOf(line(1.0, 2.0, offset = 2)), 0..1, PaneScale.FromZero, DoubleArray(0))
        assertEquals(1.0, r.max, 0.0)
    }
}
