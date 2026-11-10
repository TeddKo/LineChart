package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedTest {

    private val high = doubles(10.0, 12.0, 11.0, 13.0)
    private val low = doubles(8.0, 9.0, 7.0, 10.0)
    private val close = doubles(9.0, 11.0, 8.0, 12.0)
    private val ranged = seriesOf(close, high, low)

    @Test
    fun wilder_isSeededByTheAverageThenSmoothed() {
        val out = wilder(doubles(Double.NaN, 1.0, 1.0, 0.0, 2.0), 3, 1)
        assertEquals(3, out.leadingNaN())
        assertEquals(2.0 / 3.0, out[3], 1e-9)
        assertEquals(10.0 / 9.0, out[4], 1e-9)
    }

    @Test
    fun rsi_followsWilderSmoothing() {
        val out = rsi(doubles(10.0, 11.0, 12.0, 11.0, 13.0), 3)
        assertEquals(3, out.leadingNaN())
        assertEquals(66.6666667, out[3], 1e-6)
        assertEquals(83.3333333, out[4], 1e-6)
    }

    @Test
    fun rsi_matchesTheFourteenPeriodReferenceSeries() {
        val closes = doubles(
            44.3389, 44.0902, 44.1497, 43.6124, 44.3278, 44.8264, 45.0955, 45.4245, 45.8433,
            46.0826, 45.8931, 46.0328, 45.6140, 46.2820, 46.2820,
        )
        val out = rsi(closes, 14)
        assertEquals(14, out.leadingNaN())
        assertEquals(70.5328, out[14], 1e-3)
    }

    @Test
    fun rsi_isHundredWithoutLossesAndFiftyWhenFlat() {
        assertEquals(100.0, rsi(doubles(1.0, 2.0, 3.0, 4.0), 2)[3], 1e-9)
        assertEquals(50.0, rsi(doubles(5.0, 5.0, 5.0, 5.0), 2)[3], 1e-9)
    }

    @Test
    fun rsi_addsASimpleSignalLine() {
        val lines = Rsi(3, 2).marks(seriesOf(doubles(10.0, 11.0, 12.0, 11.0, 13.0)), ChartSettings()).lines()
        assertEquals(4, lines[1].values.leadingNaN())
        assertEquals((66.6666667 + 83.3333333) / 2.0, lines[1].values[4], 1e-6)
    }

    @Test
    fun stochasticFast_isCloseWithinTheRangeAndSmoothedD() {
        val lines = StochasticFast(3, 2).marks(ranged, ChartSettings()).lines()
        assertEquals(2, lines[0].values.leadingNaN())
        assertEquals(20.0, lines[0].values[2], 1e-9)
        assertEquals(83.3333333, lines[0].values[3], 1e-6)
        assertEquals(51.6666667, lines[1].values[3], 1e-6)
    }

    @Test
    fun stochasticFast_isFiftyOnAFlatRange() {
        val lines = StochasticFast(3, 2).marks(seriesOf(doubles(5.0, 5.0, 5.0, 5.0)), ChartSettings()).lines()
        assertEquals(50.0, lines[0].values[2], 1e-9)
    }

    @Test
    fun stochasticSlow_smoothsFastKThenD() {
        val lines = StochasticSlow(3, 2, 2).marks(ranged, ChartSettings()).lines()
        assertEquals(3, lines[0].values.leadingNaN())
        assertEquals(51.6666667, lines[0].values[3], 1e-6)
        assertTrue(lines[1].values.all { it.isNaN() })
    }

    @Test
    fun williamsR_isNegativeHundredScaled() {
        val values = WilliamsR(3).marks(ranged, ChartSettings()).lines()[0].values
        assertEquals(2, values.leadingNaN())
        assertEquals(-80.0, values[2], 1e-9)
        assertEquals(-16.6666667, values[3], 1e-6)
    }

    @Test
    fun mfi_isTheShareOfPositiveMoneyFlow() {
        val series = seriesOf(doubles(10.0, 11.0, 10.0, 12.0), volume = doubles(1.0, 2.0, 3.0, 4.0))
        val values = Mfi(3).marks(series, ChartSettings()).lines()[0].values
        assertEquals(3, values.leadingNaN())
        assertEquals(70.0, values[3], 1e-9)
    }

    @Test
    fun psychological_countsRisingClosesInTheWindow() {
        val values = Psychological(3).marks(seriesOf(doubles(1.0, 2.0, 3.0, 4.0, 3.0)), ChartSettings()).lines()[0].values
        assertEquals(3, values.leadingNaN())
        assertEquals(100.0, values[3], 1e-9)
        assertEquals(66.6666667, values[4], 1e-6)
    }

    @Test
    fun scalesAndLevelsFollowTheCatalog() {
        assertTrue(Rsi().levels.contentEquals(doubleArrayOf(30.0, 70.0)))
        assertTrue(StochasticSlow().levels.contentEquals(doubleArrayOf(20.0, 80.0)))
        assertTrue(WilliamsR().levels.contentEquals(doubleArrayOf(-20.0, -80.0)))
        assertTrue(Mfi().needsVolume)
        assertTrue(Psychological().levels.contentEquals(doubleArrayOf(25.0, 75.0)))
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val series = seriesOf(DoubleArray(60) { 100.0 + (it * 7) % 11 })
        assertEquals(14, Rsi().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(19, Rsi().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
        assertEquals(4, StochasticFast().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(6, StochasticFast().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
        assertEquals(6, StochasticSlow().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(8, StochasticSlow().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
        assertEquals(13, WilliamsR().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(14, Mfi().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(10, Psychological().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        val series = seriesOf(doubles(1.0, 2.0))
        for (pane in listOf(Rsi(), StochasticFast(), StochasticSlow(), WilliamsR(), Mfi(), Psychological())) {
            pane.marks(series, ChartSettings()).lines().forEach { assertTrue(it.values.all { v -> v.isNaN() }) }
            pane.marks(seriesOf(DoubleArray(0)), ChartSettings())
        }
    }
}
