package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDrawTest {

    private val items = listOf(
        ChartData(10.0, 1, open = 9.0, high = 12.0, low = 8.0),
        ChartData(11.0, 2, open = 10.0, high = 15.0, low = 9.5),
        ChartData(9.0, 3, open = 11.0, high = 11.5, low = 5.0),
        ChartData(10.0, 4, open = 9.0, high = 15.0, low = 5.0),
    )

    @Test
    fun extremeIndices_candleUsesHighAndLowKeepingFirstTie() {
        assertEquals(1 to 2, extremeIndices(items, 0..3, ChartType.Candle))
        assertEquals(3 to 3, extremeIndices(items, 3..3, ChartType.Candle))
    }

    @Test
    fun extremeIndices_emptyRangeIsNone() {
        assertEquals(-1 to -1, extremeIndices(items, IntRange.EMPTY, ChartType.Candle))
        assertEquals(-1 to -1, extremeIndices(items, 4..3, ChartType.Line))
    }

    @Test
    fun extremeIndices_lineUsesClose() {
        assertEquals(1 to 2, extremeIndices(items, 0..3, ChartType.Line))
    }

    @Test
    fun candleBodyWidth_isSeventyPercentOfSlotWithOnePxMinimum() {
        assertEquals(14f, candleBodyWidth(20f), 0.001f)
        assertEquals(1f, candleBodyWidth(1f), 0f)
    }
}

class ChartHeaderTest {

    private val items = listOf(
        ChartData(100.0, 1, open = 90.0),
        ChartData(110.0, 2, open = 100.0),
        ChartData(99.0, 3, open = 110.0),
    )

    @Test
    fun changePercent_comparesWithPreviousCloseAndFirstWithOpen() {
        assertEquals(10.0, changePercent(items, 1), 0.0001)
        assertEquals(-10.0, changePercent(items, 2), 0.0001)
        assertEquals(100.0 / 90.0 * 100.0 - 100.0, changePercent(items, 0), 0.0001)
    }

    @Test
    fun changePercent_zeroReferenceIsZero() {
        assertEquals(0.0, changePercent(listOf(ChartData(0.0, 1), ChartData(5.0, 2)), 1), 0.0)
    }

    @Test
    fun formatChange_signsTwoDecimalsAndIgnoresLocale() {
        assertEquals("+1.23%", formatChange(1.234))
        assertEquals("-0.50%", formatChange(-0.5))
        assertEquals("+0.00%", formatChange(0.0))
    }

    @Test
    fun maLegend_appendsTheTimeframeUnitOrNothing() {
        assertEquals("MA5일", maLegend(5, "일"))
        assertEquals("MA20", maLegend(20, ""))
    }
}

class TagOverlapTest {

    @Test
    fun overlapsTag_detectsIntersectingBoxes() {
        assertTrue(overlapsTag(95f, 12f, 100f, 26f))
        assertTrue(overlapsTag(120f, 12f, 100f, 26f))
        assertTrue(overlapsTag(100f, 12f, 100f, 26f))
    }

    @Test
    fun overlapsTag_touchingOrDistantBoxesDoNotCollide() {
        assertFalse(overlapsTag(88f, 12f, 100f, 26f))
        assertFalse(overlapsTag(126f, 12f, 100f, 26f))
        assertFalse(overlapsTag(10f, 12f, 100f, 26f))
    }

    @Test
    fun tagTop_centersOnYAndStaysInsideTheMainPane() {
        assertEquals(87f, tagTop(100f, 26f, 30f, 280f), 0f)
        assertEquals(30f, tagTop(32f, 26f, 30f, 280f), 0f)
        assertEquals(254f, tagTop(279f, 26f, 30f, 280f), 0f)
    }

    @Test
    fun lineReference_prefersBaseThenPreviousCloseThenFirstVisibleClose() {
        assertEquals(100.0, lineReference(base = 100.0, prevClose = 90.0, firstVisible = 80.0), 0.0)
        assertEquals(90.0, lineReference(base = Double.NaN, prevClose = 90.0, firstVisible = 80.0), 0.0)
        assertEquals(80.0, lineReference(base = Double.NaN, prevClose = Double.NaN, firstVisible = 80.0), 0.0)
    }
}
