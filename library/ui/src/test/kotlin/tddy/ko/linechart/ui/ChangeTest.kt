package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeTest {

    private val ascending = seriesOf(doubles(1.0, 2.0, 3.0, 4.0))

    @Test
    fun closeRatio_dividesByTheLaggedClose() {
        val out = closeRatio(doubles(1.0, 2.0, 3.0, 4.0), 2)
        assertEquals(2, out.leadingNaN())
        assertEquals(3.0, out[2], 1e-9)
        assertEquals(2.0, out[3], 1e-9)
    }

    @Test
    fun momentum_isHundredTimesTheRatio() {
        val lines = Momentum(2, 2).marks(ascending, ChartSettings()).lines()
        assertEquals(300.0, lines[0].values[2], 1e-9)
        assertEquals(200.0, lines[0].values[3], 1e-9)
        assertEquals(250.0, lines[1].values[3], 1e-9)
    }

    @Test
    fun roc_isPercentChange() {
        val values = Roc(2).marks(ascending, ChartSettings()).lines()[0].values
        assertEquals(200.0, values[2], 1e-9)
        assertEquals(100.0, values[3], 1e-9)
    }

    @Test
    fun disparity_isCloseOverItsAverage() {
        val out = disparity(doubles(1.0, 2.0, 3.0, 4.0), 3)
        assertEquals(2, out.leadingNaN())
        assertEquals(150.0, out[2], 1e-9)
        assertEquals(400.0 / 3.0, out[3], 1e-9)
    }

    @Test
    fun disparity_buildsOneLinePerPeriod() {
        val lines = Disparity(listOf(2, 3)).marks(ascending, ChartSettings()).lines()
        assertEquals(2, lines.size)
        assertEquals(1, lines[0].values.leadingNaN())
        assertEquals(2, lines[1].values.leadingNaN())
    }

    @Test
    fun levelsAndScalesFollowTheCatalog() {
        assertTrue(Momentum().levels.contentEquals(doubleArrayOf(100.0)))
        assertTrue(Roc().levels.contentEquals(doubleArrayOf(0.0)))
        assertTrue(Disparity().levels.contentEquals(doubleArrayOf(100.0)))
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val series = seriesOf(DoubleArray(80) { 100.0 + (it * 7) % 11 })
        val momentum = Momentum().marks(series, ChartSettings()).lines()
        assertEquals(10, momentum[0].values.leadingNaN())
        assertEquals(18, momentum[1].values.leadingNaN())
        assertEquals(12, Roc().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        val disparity = Disparity().marks(series, ChartSettings()).lines()
        assertEquals(19, disparity[0].values.leadingNaN())
        assertEquals(59, disparity[1].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        for (n in 0..3) {
            val series = seriesOf(DoubleArray(n) { 1.0 + it })
            for (pane in listOf(Momentum(), Roc(), Disparity())) {
                pane.marks(series, ChartSettings()).lines().forEach { assertTrue(it.values.all { v -> v.isNaN() }) }
            }
        }
    }

    @Test
    fun closeRatio_zeroLaggedCloseIsNaN() {
        val out = closeRatio(doubles(0.0, 1.0, 2.0), 1)
        assertTrue(out[1].isNaN())
    }

    @Test
    fun roc_overASeriesCrossingZeroStaysFinite() {
        val series = seriesOf(doubles(2.0, 1.0, 0.0, 1.0, 2.0, 3.0))
        val marks = Roc(1).marks(series, ChartSettings())
        assertTrue(marks.lines()[0].values.none { it.isInfinite() })
        val range = paneYRange(marks, 0..5, PaneScale.Around(0.0), doubleArrayOf(0.0))
        assertTrue(range.min.isFinite() && range.max.isFinite())
    }

    @Test
    fun disparity_zeroAverageIsNaN() {
        val out = disparity(doubles(-1.0, 1.0, 3.0), 2)
        assertTrue(out[1].isNaN())
    }
}
