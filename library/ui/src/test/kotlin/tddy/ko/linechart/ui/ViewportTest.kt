package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTest {

    private fun items(vararg dates: Long) = dates.map { ChartData(1.0, it) }

    @Test
    fun effectiveSlot_usesDefaultWhenNaNAndClamps() {
        assertEquals(8f, effectiveSlot(Float.NaN, 8f, 2f, 40f), 0f)
        assertEquals(2f, effectiveSlot(1f, 8f, 2f, 40f), 0f)
        assertEquals(40f, effectiveSlot(90f, 8f, 2f, 40f), 0f)
        assertEquals(12f, effectiveSlot(12f, 8f, 2f, 40f), 0f)
    }

    @Test
    fun minSlot_fitsAllItemsButStaysWithinBounds() {
        assertEquals(5f, minSlot(500f, 100, 2f, 40f), 0f)
        assertEquals(2f, minSlot(100f, 100, 2f, 40f), 0f)
        assertEquals(40f, minSlot(500f, 2, 2f, 40f), 0f)
        assertEquals(40f, minSlot(500f, 0, 2f, 40f), 0f)
    }

    @Test
    fun maxScroll_isZeroWhenEverythingFits() {
        assertEquals(0f, maxScroll(10, 100f, 8f), 0f)
        assertEquals(90f, maxScroll(100, 80f, 8f), 0.0001f)
    }

    @Test
    fun clampScroll_staysInsideZeroAndMax() {
        assertEquals(0f, clampScroll(-3f, 100, 80f, 8f), 0f)
        assertEquals(90f, clampScroll(500f, 100, 80f, 8f), 0.0001f)
        assertEquals(12f, clampScroll(12f, 100, 80f, 8f), 0f)
    }

    @Test
    fun xOf_latestCandleCentersHalfSlotFromRightEdge() {
        assertEquals(96f, xOf(9, 10, 0f, 100f, 8f), 0.0001f)
        assertEquals(88f, xOf(8, 10, 0f, 100f, 8f), 0.0001f)
        assertEquals(80f, xOf(8, 10, -1f, 100f, 8f), 0.0001f)
        assertEquals(96f, xOf(7, 10, 2f, 100f, 8f), 0.0001f)
    }

    @Test
    fun indexAt_isInverseOfXOfAndCoerced() {
        for (i in 0 until 10) {
            assertEquals(i, indexAt(xOf(i, 10, 1.5f, 100f, 8f), 10, 1.5f, 100f, 8f))
        }
        assertEquals(9, indexAt(500f, 10, 0f, 100f, 8f))
        assertEquals(0, indexAt(-500f, 10, 0f, 100f, 8f))
        assertEquals(-1, indexAt(10f, 0, 0f, 100f, 8f))
    }

    @Test
    fun visibleRange_coversItemsIntersectingPlot() {
        assertEquals(0..9, visibleRange(10, 0f, 100f, 8f))
        assertEquals(87..99, visibleRange(100, 0f, 100f, 8f))
        assertEquals(84..96, visibleRange(100, 3f, 100f, 8f))
        assertEquals(83..96, visibleRange(100, 3.8f, 100f, 8f))
        assertTrue(visibleRange(0, 0f, 100f, 8f).isEmpty())
    }

    @Test
    fun zoomScroll_keepsIndexUnderFocalPointFixed() {
        val n = 100
        val plotW = 200f
        val fx = 120f
        val before = indexAtFraction(n, 7f, plotW, 8f, fx)
        val scroll = zoomScroll(7f, plotW, 8f, 16f, fx)
        assertEquals(before, indexAtFraction(n, scroll, plotW, 16f, fx), 0.0001f)
    }

    private fun indexAtFraction(n: Int, scroll: Float, plotW: Float, slot: Float, x: Float) =
        n - scroll - (plotW - x) / slot

    @Test
    fun appendedCount_resetsWhenOldEmptyOrNotFound() {
        assertEquals(-1, appendedCount(emptyList(), items(1, 2)))
        assertEquals(-1, appendedCount(items(1, 2), items(5, 6)))
        assertEquals(-1, appendedCount(items(1, 2), emptyList()))
    }

    @Test
    fun appendedCount_countsAppendedTailAndZeroForInPlaceUpdates() {
        assertEquals(0, appendedCount(items(1, 2, 3), items(1, 2, 3)))
        assertEquals(0, appendedCount(items(2, 3), items(1, 2, 3)))
        assertEquals(1, appendedCount(items(1, 2, 3), items(1, 2, 3, 4)))
        assertEquals(3, appendedCount(items(1, 2, 3), items(1, 2, 3, 4, 5, 6)))
        assertEquals(2, appendedCount(items(1, 2, 3), items(3, 4, 5)))
    }

    @Test
    fun nextScroll_followsRightEdgeAndHoldsScrolledBackPosition() {
        assertEquals(0f, nextScroll(0f, 2, false), 0f)
        assertEquals(0.4f, nextScroll(0.4f, 2, false), 0f)
        assertEquals(12f, nextScroll(10f, 2, false), 0f)
        assertEquals(2f, nextScroll(0f, 2, true), 0f)
        assertEquals(0f, nextScroll(30f, -1, false), 0f)
        assertEquals(10f, nextScroll(10f, 0, false), 0f)
    }

    @Test
    fun totalPad_isTheLargerOfMarginAndExtension() {
        assertEquals(3, totalPad(3, 0))
        assertEquals(25, totalPad(3, 25))
        assertEquals(0, totalPad(0, 0))
    }

    @Test
    fun slotCount_placesTheLastItemPadSlotsLeftOfTheRightEdge() {
        assertEquals(100f - 3.5f * 8f, xOf(9, 13, 0f, 100f, 8f), 0.0001f)
    }

    @Test
    fun indexAt_withPadCoercesIntoItemCount() {
        assertEquals(9, indexAt(100f, 13, 0f, 100f, 8f, count = 10))
        assertEquals(9, indexAt(xOf(9, 13, 0f, 100f, 8f), 13, 0f, 100f, 8f, count = 10))
        assertEquals(0, indexAt(-500f, 13, 0f, 100f, 8f, count = 10))
    }

    @Test
    fun visibleRange_withPadStopsAtTheLastItem() {
        assertEquals(3..9, visibleRange(13, 0f, 80f, 8f, count = 10))
        assertEquals(0..9, visibleRange(13, 3f, 100f, 8f, count = 10))
        assertEquals(9..9, visibleRange(13, 0f, 24f, 8f, count = 10))
    }

    @Test
    fun maxScroll_countsPadSlots() {
        assertEquals(3f, maxScroll(13, 80f, 8f), 0.0001f)
    }
}
