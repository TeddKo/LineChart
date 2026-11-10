package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendTest {

    private val high = doubles(10.0, 12.0, 11.0, 13.0, 14.0, 13.0, 15.0)
    private val low = doubles(8.0, 9.0, 8.0, 10.0, 12.0, 11.0, 12.0)
    private val close = doubles(9.0, 11.0, 9.0, 12.0, 13.0, 12.0, 14.0)

    @Test
    fun trueRange_usesThePreviousCloseWhenItWidensTheRange() {
        val out = trueRange(doubles(11.0, 12.0, 16.0), doubles(9.0, 10.0, 12.0), doubles(10.0, 11.0, 12.0))
        assertEquals(listOf(2.0, 2.0, 5.0), out.toList())
    }

    @Test
    fun meanDeviation_isAbsoluteDistanceFromTheWindowMean() {
        val out = meanDeviation(doubles(1.0, 2.0, 3.0, 4.0), 3)
        assertEquals(2, out.leadingNaN())
        assertEquals(2.0 / 3.0, out[2], 1e-9)
        assertEquals(2.0 / 3.0, out[3], 1e-9)
    }

    @Test
    fun cci_isHundredForALinearRamp() {
        val values = Cci(3).marks(seriesOf(doubles(1.0, 2.0, 3.0, 4.0)), ChartSettings()).lines()[0].values
        assertEquals(2, values.leadingNaN())
        assertEquals(100.0, values[2], 1e-9)
        assertEquals(100.0, values[3], 1e-9)
    }

    @Test
    fun cci_isZeroForAFlatWindow() {
        assertEquals(0.0, Cci(3).marks(seriesOf(doubles(5.0, 5.0, 5.0)), ChartSettings()).lines()[0].values[2], 1e-9)
    }

    @Test
    fun atr_isWilderAverageOfTheTrueRange() {
        val series = seriesOf(doubles(10.0, 11.0, 12.0, 13.0), doubles(11.0, 12.0, 13.0, 16.0), doubles(9.0, 10.0, 11.0, 12.0))
        val values = Atr(2).marks(series, ChartSettings()).lines()[0].values
        assertEquals(1, values.leadingNaN())
        assertEquals(2.0, values[1], 1e-9)
        assertEquals(2.0, values[2], 1e-9)
        assertEquals(3.0, values[3], 1e-9)
    }

    @Test
    fun dmi_matchesTheWilderReferenceValues() {
        val lines = Dmi(3, 2).marks(seriesOf(close, high, low), ChartSettings()).lines()
        assertEquals(listOf("+DI", "-DI", "ADX"), lines.map { it.label })
        assertEquals(3, lines[0].values.leadingNaN())
        assertEquals(40.0, lines[0].values[3], 1e-6)
        assertEquals(10.0, lines[1].values[3], 1e-6)
        assertEquals(42.3076923, lines[0].values[4], 1e-6)
        assertEquals(7.6923077, lines[1].values[4], 1e-6)
        assertEquals(4, lines[2].values.leadingNaN())
        assertEquals(64.6153846, lines[2].values[4], 1e-6)
        assertEquals(45.1648352, lines[2].values[5], 1e-6)
        assertEquals(51.6146756, lines[2].values[6], 1e-6)
    }

    @Test
    fun dmi_isHundredAdxOnAPureUptrend() {
        val up = DoubleArray(8) { 2.0 + it }
        val lines = Dmi(2, 2).marks(seriesOf(DoubleArray(8) { 1.5 + it }, up, DoubleArray(8) { 1.0 + it }), ChartSettings()).lines()
        assertEquals(100.0 * 1.0 / 1.5, lines[0].values[7], 1e-6)
        assertEquals(0.0, lines[1].values[7], 1e-9)
        assertEquals(100.0, lines[2].values[7], 1e-6)
    }

    @Test
    fun dmi_colorsPlusDiUpAndMinusDiDown() {
        val lines = Dmi().marks(seriesOf(close, high, low), ChartSettings()).lines()
        assertEquals(listOf(1, -1, 0), lines.map { it.direction })
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val series = seriesOf(DoubleArray(60) { 100.0 + (it * 7) % 11 }, DoubleArray(60) { 102.0 + (it * 7) % 11 }, DoubleArray(60) { 98.0 + (it * 7) % 11 })
        assertEquals(19, Cci().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(13, Atr().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        val dmi = Dmi().marks(series, ChartSettings()).lines()
        assertEquals(14, dmi[0].values.leadingNaN())
        assertEquals(27, dmi[2].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        for (n in 0..3) {
            val series = seriesOf(DoubleArray(n) { 1.0 + it })
            for (pane in listOf(Cci(), Dmi(), Atr())) {
                pane.marks(series, ChartSettings()).lines().forEach { assertTrue(it.values.all { v -> v.isNaN() }) }
            }
        }
    }
}
