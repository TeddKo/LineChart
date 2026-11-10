package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleTest {

    private fun candle(close: Double, open: Double, high: Double, low: Double) =
        ChartData(close, 0L, open, high, low)

    @Test
    fun visibleYRange_candleUsesLowHighWithFivePercentPadding() {
        val items = listOf(candle(10.0, 12.0, 20.0, 0.0), candle(15.0, 10.0, 100.0, 5.0), candle(1.0, 1.0, 1.0, 1.0))
        val range = visibleYRange(items, 0..1, ChartType.Candle)
        assertEquals(-5.0, range.min, 1e-9)
        assertEquals(105.0, range.max, 1e-9)
    }

    @Test
    fun visibleYRange_includeWidensRangeToTheValue() {
        val items = listOf(candle(10.0, 10.0, 20.0, 10.0), candle(20.0, 10.0, 20.0, 10.0))
        val above = visibleYRange(items, 0..1, ChartType.Line, include = 30.0)
        assertEquals(10.0 - 1.0, above.min, 1e-9)
        assertEquals(30.0 + 1.0, above.max, 1e-9)
        val below = visibleYRange(items, 0..1, ChartType.Line, include = 0.0)
        assertEquals(-1.0, below.min, 1e-9)
        assertEquals(21.0, below.max, 1e-9)
        val inside = visibleYRange(items, 0..1, ChartType.Line, include = 15.0)
        assertEquals(9.5, inside.min, 1e-9)
        assertEquals(20.5, inside.max, 1e-9)
        val none = visibleYRange(items, 0..1, ChartType.Line, include = Double.NaN)
        assertEquals(9.5, none.min, 1e-9)
    }

    @Test
    fun visibleYRange_lineAndAreaUseClose() {
        val items = listOf(candle(10.0, 12.0, 20.0, 0.0), candle(20.0, 10.0, 100.0, 5.0))
        for (type in listOf(ChartType.Line, ChartType.Area)) {
            val range = visibleYRange(items, 0..1, type)
            assertEquals(9.5, range.min, 1e-9)
            assertEquals(20.5, range.max, 1e-9)
        }
    }

    @Test
    fun visibleYRange_flatRangeGetsOnePercentOrOne() {
        val flat = visibleYRange(listOf(ChartData(200.0, 0L)), 0..0, ChartType.Line)
        assertEquals(198.0, flat.min, 1e-9)
        assertEquals(202.0, flat.max, 1e-9)
        val zero = visibleYRange(listOf(ChartData(0.0, 0L)), 0..0, ChartType.Candle)
        assertEquals(-1.0, zero.min, 1e-9)
        assertEquals(1.0, zero.max, 1e-9)
    }

    @Test
    fun visibleYRange_emptyRangeFallsBackToUnitRange() {
        val range = visibleYRange(emptyList(), IntRange.EMPTY, ChartType.Candle)
        assertEquals(0.0, range.min, 0.0)
        assertEquals(1.0, range.max, 0.0)
    }

    @Test
    fun yOfAndValueAt_areInverseAndTopIsMax() {
        val range = YRange(0.0, 100.0)
        assertEquals(10f, yOf(100.0, range, 10f, 200f), 1e-4f)
        assertEquals(210f, yOf(0.0, range, 10f, 200f), 1e-4f)
        assertEquals(60.0, valueAt(yOf(60.0, range, 10f, 200f), range, 10f, 200f), 1e-4)
    }

    @Test
    fun niceTicks_roundStepsInsideRange() {
        assertEquals(listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0), niceTicks(0.0, 100.0, 6))
        assertEquals(listOf(0.0, 1.0), niceTicks(0.0, 1.0, 2))
        assertEquals(
            listOf(71000.0, 71200.0, 71400.0, 71600.0, 71800.0, 72000.0),
            niceTicks(71000.0, 72000.0, 5),
        )
    }

    @Test
    fun niceTicks_targetBelowTwoActsAsTwoAndFlatRangeIsSingleTick() {
        assertEquals(niceTicks(0.0, 10.0, 2), niceTicks(0.0, 10.0, 1))
        assertEquals(listOf(5.0), niceTicks(5.0, 5.0, 5))
    }

    @Test
    fun niceTicks_fractionalStepsStayAscendingWithinRange() {
        val ticks = niceTicks(0.12, 0.47, 5)
        assertTrue(ticks.size >= 2)
        assertTrue(ticks.first() >= 0.12 - 1e-9 && ticks.last() <= 0.47 + 1e-9)
        assertEquals(ticks.sorted(), ticks)
    }

    @Test
    fun visibleYRange_includesOverlayMarksInTheVisibleRange() {
        val items = listOf(candle(10.0, 12.0, 20.0, 0.0), candle(15.0, 10.0, 100.0, 5.0))
        val mark = LineMark("", doubleArrayOf(Double.NaN, 200.0), androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.unit.Dp.Hairline)
        val range = visibleYRange(items, 0..1, ChartType.Candle, listOf(mark))
        assertEquals(200.0 + 200.0 * 0.05, range.max, 1e-9)
    }

    @Test
    fun direction_candleComparesCloseWithOpen() {
        val items = listOf(candle(12.0, 10.0, 12.0, 10.0), candle(8.0, 10.0, 10.0, 8.0), candle(10.0, 10.0, 10.0, 10.0))
        assertEquals(1, direction(items, 0, ChartType.Candle))
        assertEquals(-1, direction(items, 1, ChartType.Candle))
        assertEquals(0, direction(items, 2, ChartType.Candle))
    }

    @Test
    fun direction_lineComparesCloseWithPreviousClose() {
        val items = listOf(5.0, 7.0, 6.0, 6.0).map { ChartData(it, 0L) }
        assertEquals(0, direction(items, 0, ChartType.Line))
        assertEquals(1, direction(items, 1, ChartType.Area))
        assertEquals(-1, direction(items, 2, ChartType.Line))
        assertEquals(0, direction(items, 3, ChartType.Line))
    }
}
