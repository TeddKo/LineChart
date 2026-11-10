package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceScaleTest {

    @Test
    fun logRange_mapsPricesGeometrically() {
        val range = YRange(10.0, 1000.0, log = true)
        assertEquals(100f, yOf(10.0, range, 0f, 100f), 0.001f)
        assertEquals(50f, yOf(100.0, range, 0f, 100f), 0.001f)
        assertEquals(0f, yOf(1000.0, range, 0f, 100f), 0.001f)
    }

    @Test
    fun logRange_valueAtInvertsYOf() {
        val range = YRange(10.0, 1000.0, log = true)
        for (value in listOf(10.0, 31.6, 100.0, 777.0)) {
            assertEquals(value, valueAt(yOf(value, range, 20f, 80f), range, 20f, 80f), value * 1e-4)
        }
    }

    @Test
    fun visibleYRange_logPadsInLogSpaceKeepingTheGeometricCenter() {
        val items = listOf(ChartData(10.0, 1L), ChartData(1000.0, 2L))
        val range = visibleYRange(items, 0..1, ChartType.Line, log = true)
        assertTrue(range.log)
        assertEquals(10.0 * 1000.0, range.min * range.max, 1e-6)
        assertTrue(range.min < 10.0 && range.max > 1000.0)
    }

    @Test
    fun visibleYRange_logFallsBackToLinearForNonPositiveValues() {
        val items = listOf(ChartData(-5.0, 1L), ChartData(10.0, 2L))
        assertTrue(!visibleYRange(items, 0..1, ChartType.Line, log = true).log)
    }

    @Test
    fun visibleYRange_flatLogRangeStillHasASpan() {
        val range = visibleYRange(listOf(ChartData(10.0, 1L)), 0..0, ChartType.Line, log = true)
        assertTrue(range.max > range.min)
    }

    @Test
    fun logTicks_useOneTwoFiveStepsPerDecade() {
        assertEquals(listOf(1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0, 1000.0), logTicks(1.0, 1000.0, 5))
    }

    @Test
    fun logTicks_fallBackToNiceTicksInsideOneDecade() {
        val ticks = logTicks(101.0, 130.0, 5)
        assertTrue(ticks.size >= 2)
        assertTrue(ticks.all { it in 101.0..130.0 })
    }

    @Test
    fun percentTicks_areRoundPercentagesAroundTheBase() {
        val ticks = percentTicks(90.0, 110.0, 100.0, 5)
        assertEquals(listOf(90.0, 95.0, 100.0, 105.0, 110.0).size, ticks.size)
        listOf(90.0, 95.0, 100.0, 105.0, 110.0).forEachIndexed { i, expected -> assertEquals(expected, ticks[i], 1e-9) }
    }

    @Test
    fun percentTicks_nonPositiveBaseFallsBackToNiceTicks() {
        assertEquals(niceTicks(1.0, 5.0, 5), percentTicks(1.0, 5.0, 0.0, 5))
    }

    @Test
    fun percentText_isTheSignedChangeAgainstTheBase() {
        assertEquals("+5.00%", percentText(105.0, 100.0))
        assertEquals("-10.00%", percentText(90.0, 100.0))
        assertEquals("0.00%", percentText(100.0, 100.0).replace("+", ""))
    }
}
