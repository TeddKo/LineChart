package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SarTest {

    private fun assertValues(expected: List<Double>, actual: DoubleArray) {
        expected.forEachIndexed { k, value -> assertEquals(value, actual[k + 1], 1e-9) }
    }

    private val high = doubles(10.0, 11.0, 12.0, 13.0, 12.0, 10.0)
    private val low = doubles(9.0, 10.0, 11.0, 12.0, 11.0, 9.0)
    private val close = doubles(9.5, 10.5, 11.5, 12.5, 11.5, 9.5)

    @Test
    fun parabolicSar_followsWilderAndReversesWhenPriceCrossesTheStop() {
        val sar = parabolicSar(high, low, close, 0.02, 0.2)
        assertTrue(sar.values[0].isNaN())
        assertValues(listOf(9.0, 9.0, 9.18, 9.4856, 13.0), sar.values)
        assertEquals(listOf(false, false, false, false, false, true), sar.above.toList())
    }

    @Test
    fun parabolicSar_startsDownWhenTheFirstCloseFalls() {
        val sar = parabolicSar(DoubleArray(6) { 20.0 - low[it] }, DoubleArray(6) { 20.0 - high[it] }, DoubleArray(6) { 20.0 - close[it] }, 0.02, 0.2)
        assertValues(listOf(11.0, 11.0, 10.82, 10.5144, 7.0), sar.values)
        assertEquals(listOf(false, true, true, true, true, false), sar.above.toList())
    }

    @Test
    fun parabolicSar_capsTheAcceleration() {
        val n = 40
        val rising = DoubleArray(n) { 10.0 + it }
        val sar = parabolicSar(DoubleArray(n) { rising[it] + 1.0 }, DoubleArray(n) { rising[it] - 1.0 }, rising, 0.1, 0.2)
        val last = sar.values[n - 1]
        val previous = sar.values[n - 2]
        assertEquals(0.2 * (rising[n - 2] + 1.0 - previous) + previous, last, 1e-9)
    }

    @Test
    fun marks_areOneDotMarkPlacedByTrend() {
        val dots = ParabolicSar().marks(seriesOf(close, high, low), ChartSettings()).single() as DotMark
        assertEquals(1, dots.values.leadingNaN())
        assertTrue(dots.above[5])
        assertTrue(!dots.above[3])
    }

    @Test
    fun shorterThanTwoCandlesNeverThrows() {
        for (n in 0..1) {
            val dots = ParabolicSar().marks(seriesOf(DoubleArray(n) { 1.0 }), ChartSettings()).single() as DotMark
            assertTrue(dots.values.all { it.isNaN() })
        }
    }
}
