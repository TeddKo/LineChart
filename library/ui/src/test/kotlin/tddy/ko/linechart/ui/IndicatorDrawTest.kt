package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorDrawTest {

    private val nan = Double.NaN

    private fun runs(a: DoubleArray, b: DoubleArray, bySign: Boolean = true, from: Int = 0, to: Int = a.lastIndex) =
        fillRuns({ a.getOrElse(it) { nan } }, { b.getOrElse(it) { nan } }, from, to, bySign).map { Triple(it.start, it.end, it.tone) }

    @Test
    fun fillRuns_splitWhereTheSignOfTheDifferenceChanges() {
        val a = doubles(3.0, 3.0, 1.0, 1.0, 1.0, 5.0)
        val b = doubles(2.0, 2.0, 2.0, 2.0, 2.0, 2.0)
        assertEquals(listOf(Triple(0, 1, 1), Triple(2, 4, -1), Triple(5, 5, 1)), runs(a, b))
    }

    @Test
    fun fillRuns_breakAtNaNOnEitherSide() {
        val a = doubles(3.0, nan, 3.0, 3.0)
        val b = doubles(2.0, 2.0, 2.0, nan)
        assertEquals(listOf(Triple(0, 0, 1), Triple(2, 2, 1)), runs(a, b))
    }

    @Test
    fun fillRuns_unsignedFillIsOneRunWithTheFlatTone() {
        val a = doubles(3.0, 1.0, 3.0)
        val b = doubles(2.0, 2.0, 2.0)
        assertEquals(listOf(Triple(0, 2, 0)), runs(a, b, bySign = false))
    }

    @Test
    fun fillRuns_equalValuesAreTheFlatTone() {
        assertEquals(listOf(Triple(0, 0, 0), Triple(1, 1, 1)), runs(doubles(2.0, 3.0), doubles(2.0, 2.0)))
    }

    @Test
    fun fillRuns_onlyCoverTheRequestedSlots() {
        val a = doubles(3.0, 3.0, 3.0, 3.0)
        val b = doubles(2.0, 2.0, 2.0, 2.0)
        assertEquals(listOf(Triple(1, 2, 1)), runs(a, b, from = 1, to = 2))
        assertTrue(runs(a, b, from = 5, to = 4).isEmpty())
    }

    @Test
    fun dotSpots_skipNaNAndApplyTheOffset() {
        val mark = DotMark(doubles(nan, 1.0, 2.0), booleanArrayOf(false, true, false), offset = 1)
        val spots = dotSpots(mark, 0, 4)
        assertEquals(listOf(2, 3), spots.map { it.slot })
        assertEquals(listOf(true, false), spots.map { it.above })
    }

    private fun pane() = PaneContext(
        Rect(0f, 0f, 100f, 100f), YRange(0.0, 100.0), View(0f, 10f, 0..9, 0..9), 10, Series(emptyList()),
    )

    @Test
    fun levelYs_mapLevelsIntoThePaneAndDropTheOnesOutside() {
        val ys = levelYs(doubleArrayOf(30.0, 70.0, 150.0, -10.0), pane())
        assertEquals(listOf(70f, 30f), ys)
    }

    @Test
    fun levelYs_emptyLevelsGiveNoLines() {
        assertTrue(levelYs(DoubleArray(0), pane()).isEmpty())
    }
}
