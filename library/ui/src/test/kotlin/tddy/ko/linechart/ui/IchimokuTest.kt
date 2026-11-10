package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IchimokuTest {

    private val series = seriesOf(doubles(1.0, 2.0, 3.0, 4.0, 5.0, 6.0))
    private val ichimoku = Ichimoku(2, 3, 4, 3)

    @Test
    fun midpoint_isHalfTheWindowExtremes() {
        val out = midpoint(doubles(3.0, 5.0, 4.0), doubles(1.0, 2.0, 0.0), 2)
        assertEquals(1, out.leadingNaN())
        assertEquals(3.0, out[1], 1e-9)
        assertEquals(2.5, out[2], 1e-9)
    }

    @Test
    fun marks_buildTheFiveLinesAndTheCloud() {
        val marks = ichimoku.marks(series, ChartSettings())
        assertTrue(marks[0] is FillMark)
        assertEquals(listOf("conv", "base", "lag", "span1", "span2"), marks.lines().map { it.label })
    }

    @Test
    fun lines_matchTheLinearRampValues() {
        val lines = ichimoku.marks(series, ChartSettings()).lines()
        assertEquals(1.5, lines[0].values[1], 1e-9)
        assertEquals(2.5, lines[0].values[2], 1e-9)
        assertEquals(2.0, lines[1].values[2], 1e-9)
        assertEquals(2.25, lines[3].values[2], 1e-9)
        assertEquals(2.5, lines[4].values[3], 1e-9)
    }

    @Test
    fun leadingSpans_shiftForwardAndTheLaggingSpanBackward() {
        val lines = ichimoku.marks(series, ChartSettings()).lines()
        assertEquals(listOf(0, 0, -2, 2, 2), lines.map { it.offset })
        assertEquals(ichimoku.shift - 1, ichimoku.rightPad)
        assertEquals(6.0, lines[2].values[5], 1e-9)
    }

    @Test
    fun defaults_plotTheSpansTwentyFiveSlotsAhead() {
        assertEquals(25, Ichimoku().rightPad)
        assertEquals(25, Ichimoku().marks(series, ChartSettings()).lines()[3].offset)
    }

    @Test
    fun cloud_isFilledByTheSignOfSpanAMinusSpanB() {
        val fill = ichimoku.marks(series, ChartSettings())[0] as FillMark
        assertEquals(null, fill.color)
        assertEquals(0.12f, fill.alpha, 1e-6f)
        assertEquals(2, fill.offset)
        assertEquals(2.25, fill.a[2], 1e-9)
        assertEquals(2.5, checkNotNull(fill.b)[3], 1e-9)
    }

    @Test
    fun spans_areDrawnInUpAndDownColors() {
        val lines = ichimoku.marks(series, ChartSettings()).lines()
        assertEquals(1, lines[3].direction)
        assertEquals(-1, lines[4].direction)
        assertEquals(0.6f, lines[3].alpha, 1e-6f)
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val long = seriesOf(DoubleArray(120) { 100.0 + (it * 7) % 11 })
        val lines = Ichimoku().marks(long, ChartSettings()).lines()
        assertEquals(8, lines[0].values.leadingNaN())
        assertEquals(25, lines[1].values.leadingNaN())
        assertEquals(0, lines[2].values.leadingNaN())
        assertEquals(25, lines[3].values.leadingNaN())
        assertEquals(51, lines[4].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        for (n in 0..3) {
            val marks = Ichimoku().marks(seriesOf(DoubleArray(n) { 1.0 + it }), ChartSettings())
            marks.lines().filter { it.label != "lag" }.forEach { assertTrue(it.values.all { v -> v.isNaN() }) }
        }
    }
}
