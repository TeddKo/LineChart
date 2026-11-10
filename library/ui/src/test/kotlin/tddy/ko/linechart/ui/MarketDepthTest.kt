package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDepthTest {
    private val bids = listOf(MarketLevel(90, 2), MarketLevel(95, 1), MarketLevel(80, 4))
    private val asks = listOf(MarketLevel(110, 3), MarketLevel(100, 5), MarketLevel(120, 0))

    @Test
    fun depthSortsAndAccumulatesBothSides() {
        val depth = depthOf(bids, asks)
        assertEquals(listOf(95L, 90L, 80L), depth.bidPrices.toList())
        assertEquals(listOf(1L, 3L, 7L), depth.bidCum.toList())
        assertEquals(listOf(100L, 110L), depth.askPrices.toList())
        assertEquals(listOf(5L, 8L), depth.askCum.toList())
        assertEquals(8L, depth.maxCum)
    }

    @Test
    fun depthDropsNonPositiveQuantities() {
        val depth = depthOf(listOf(MarketLevel(90, 0), MarketLevel(80, -3)), emptyList())
        assertTrue(depth.bidPrices.isEmpty())
        assertEquals(0L, depth.maxCum)
    }

    @Test
    fun midAndSpread() {
        val depth = depthOf(bids, asks)
        assertEquals(97.5, checkNotNull(depth.mid), 0.0)
        assertEquals(5L, depth.spread)
    }

    @Test
    fun oneSidedBookHasMidButNoSpread() {
        val onlyBids = depthOf(bids, emptyList())
        assertEquals(95.0, checkNotNull(onlyBids.mid), 0.0)
        assertNull(onlyBids.spread)
        val onlyAsks = depthOf(emptyList(), asks)
        assertEquals(100.0, checkNotNull(onlyAsks.mid), 0.0)
        assertNull(onlyAsks.spread)
    }

    @Test
    fun emptyBookHasNothing() {
        val depth = depthOf(emptyList(), emptyList())
        assertNull(depth.mid)
        assertNull(depth.spread)
        assertEquals(0L, depth.maxCum)
        assertNull(depthXRange(depth))
    }

    @Test
    fun xRangeIsCenteredOnMidWithMargin() {
        val range = checkNotNull(depthXRange(depthOf(bids, asks)))
        val half = (97.5 - 80.0) * 1.05
        assertEquals(97.5 - half, range.first, 1e-9)
        assertEquals(97.5 + half, range.second, 1e-9)
    }

    @Test
    fun xRangeUsesFarthestSide() {
        val range = checkNotNull(depthXRange(depthOf(listOf(MarketLevel(99, 1)), listOf(MarketLevel(101, 1), MarketLevel(140, 1)))))
        val half = (140.0 - 100.0) * 1.05
        assertEquals(100.0 - half, range.first, 1e-9)
        assertEquals(100.0 + half, range.second, 1e-9)
    }

    @Test
    fun xRangeOfSingleLevelStillHasWidth() {
        val range = checkNotNull(depthXRange(depthOf(listOf(MarketLevel(100, 1)), emptyList())))
        assertTrue(range.second > range.first)
        assertEquals(100.0, (range.first + range.second) / 2, 1e-9)
    }

    @Test
    fun xRangeOfOneSideSpansItsLevels() {
        val range = checkNotNull(depthXRange(depthOf(emptyList(), listOf(MarketLevel(100, 1), MarketLevel(120, 1)))))
        assertTrue(range.first < 100.0 && range.second > 120.0)
    }

    @Test
    fun cumulativeAtBidSide() {
        val depth = depthOf(bids, asks)
        assertEquals(1L, depth.cumulativeAt(95.0))
        assertEquals(3L, depth.cumulativeAt(90.0))
        assertEquals(1L, depth.cumulativeAt(92.0))
        assertEquals(7L, depth.cumulativeAt(80.0))
        assertEquals(7L, depth.cumulativeAt(10.0))
    }

    @Test
    fun cumulativeAtAskSide() {
        val depth = depthOf(bids, asks)
        assertEquals(5L, depth.cumulativeAt(100.0))
        assertEquals(5L, depth.cumulativeAt(105.0))
        assertEquals(8L, depth.cumulativeAt(110.0))
        assertEquals(8L, depth.cumulativeAt(500.0))
    }

    @Test
    fun cumulativeAtGapIsNull() {
        val depth = depthOf(bids, asks)
        assertNull(depth.cumulativeAt(96.0))
        assertNull(depth.cumulativeAt(99.9))
    }

    @Test
    fun cumulativeAtWithEmptySideIsNull() {
        val onlyBids = depthOf(bids, emptyList())
        assertNull(onlyBids.cumulativeAt(120.0))
        assertEquals(7L, onlyBids.cumulativeAt(70.0))
        val onlyAsks = depthOf(emptyList(), asks)
        assertNull(onlyAsks.cumulativeAt(50.0))
        assertEquals(5L, onlyAsks.cumulativeAt(100.0))
    }
}
