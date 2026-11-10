package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllIndicatorsTest {

    @Test
    fun catalogHasThirtyDistinctIndicators() {
        assertEquals(30, allIndicators.map { it.javaClass }.toSet().size)
    }

    @Test
    fun marksNeverThrowForSeriesShorterThanAnyPeriod() {
        for (n in 0..4) {
            val series = seriesOf(DoubleArray(n) { 100.0 + it }, DoubleArray(n) { 101.0 + it }, DoubleArray(n) { 99.0 + it })
            for (indicator in allIndicators) indicator.marks(series, ChartSettings())
        }
    }

    @Test
    fun everyLineIsAsLongAsTheSeries() {
        val series = seriesOf(DoubleArray(130) { 100.0 + (it * 7) % 11 })
        for (indicator in allIndicators) {
            indicator.marks(series, ChartSettings()).lines().forEach { assertEquals(indicator.javaClass.simpleName, 130, it.values.size) }
        }
    }

    @Test
    fun rightPadIsOnlyAskedByIchimoku() {
        val padded = allIndicators.filterIsInstance<Overlay>().filter { it.rightPad > 0 }
        assertTrue(padded.single() is Ichimoku)
    }
}
