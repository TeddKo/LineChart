package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MacdTest {

    private val closes = doubles(1.0, 2.0, 4.0, 7.0, 11.0)

    @Test
    fun ema_startsAtTheFirstNonNaNInput() {
        val out = ema(doubles(Double.NaN, Double.NaN, 1.0, 2.0, 4.0, 7.0), 2)
        assertEquals(3, out.leadingNaN())
        assertEquals(1.5, out[3], 1e-9)
        assertEquals(4.0 * 2.0 / 3.0 + 1.5 / 3.0, out[4], 1e-9)
    }

    @Test
    fun macd_isEmaDifferenceWithSignalAndOscillator() {
        val marks = Macd(2, 3, 2).marks(seriesOf(closes), ChartSettings())
        val bar = marks[0] as BarMark
        val line = marks[1] as LineMark
        val signal = marks[2] as LineMark
        assertEquals(0.8333333, line.values[2], 1e-6)
        assertEquals(1.0555556, line.values[3], 1e-6)
        assertEquals(1.4074074, line.values[4], 1e-6)
        assertEquals(0.9444444, signal.values[3], 1e-6)
        assertEquals(1.2530864, signal.values[4], 1e-6)
        assertEquals(Tone.Sign, bar.tone)
        assertEquals(0.1111111, bar.values[3], 1e-6)
        assertEquals(0.1543210, bar.values[4], 1e-6)
    }

    @Test
    fun macdOscillator_isOnlyTheHistogram() {
        val marks = MacdOscillator(2, 3, 2).marks(seriesOf(closes), ChartSettings())
        assertEquals(1, marks.size)
        assertEquals(0.1543210, (marks[0] as BarMark).values[4], 1e-6)
    }

    @Test
    fun trix_isPercentChangeOfTripleEma() {
        val lines = Trix(2, 2).marks(seriesOf(closes), ChartSettings()).lines()
        assertEquals(4, lines[0].values.leadingNaN())
        assertEquals(81.4022579, lines[0].values[4], 1e-5)
    }

    @Test
    fun sonar_isPercentChangeOfEmaAgainstItsLag() {
        val lines = Sonar(2, 1, 2).marks(seriesOf(doubles(1.0, 2.0, 4.0, 7.0, 11.0, 16.0)), ChartSettings()).lines()
        assertEquals(2, lines[0].values.leadingNaN())
        assertEquals(111.1111111, lines[0].values[2], 1e-6)
        assertEquals(80.7017544, lines[0].values[3], 1e-6)
        assertEquals(95.9064327, lines[1].values[3], 1e-6)
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val series = seriesOf(DoubleArray(80) { 100.0 + it % 9 })
        val macd = Macd().marks(series, ChartSettings())
        assertEquals(25, (macd[1] as LineMark).values.leadingNaN())
        assertEquals(33, (macd[2] as LineMark).values.leadingNaN())
        assertEquals(33, (macd[0] as BarMark).values.leadingNaN())
        assertEquals(34, Trix().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(42, Trix().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
        assertEquals(28, Sonar().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(36, Sonar().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        val series = seriesOf(doubles(1.0, 2.0))
        for (pane in listOf(Macd(), MacdOscillator(), Trix(), Sonar())) {
            pane.marks(series, ChartSettings()).forEach { mark ->
                if (mark is LineMark) assertTrue(mark.values.all { it.isNaN() })
            }
        }
    }
}
