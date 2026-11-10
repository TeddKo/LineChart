package tddy.ko.linechart.orderbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderBookTest {
    private var seq = 0L

    private fun resting(side: Side, price: Long, quantity: Int) =
        Resting(++seq, "A", side, price, quantity, 0L, Long.MAX_VALUE, null)

    @Test
    fun snapshotAggregatesPerPriceAndSortsBothSides() {
        val book = OrderBook("A")
        book.add(resting(Side.Buy, 90, 1))
        book.add(resting(Side.Buy, 95, 2))
        book.add(resting(Side.Buy, 90, 3))
        book.add(resting(Side.Sell, 110, 4))
        book.add(resting(Side.Sell, 100, 5))
        book.add(resting(Side.Sell, 100, 6))
        val snapshot = book.snapshot(Int.MAX_VALUE)
        assertEquals(listOf(BookLevel(95, 2), BookLevel(90, 4)), snapshot.bids)
        assertEquals(listOf(BookLevel(100, 11), BookLevel(110, 4)), snapshot.asks)
        assertEquals(95L, snapshot.bestBid)
        assertEquals(100L, snapshot.bestAsk)
    }

    @Test
    fun snapshotDepthLimitsEachSide() {
        val book = OrderBook("A")
        for (p in 91L..95L) book.add(resting(Side.Buy, p, 1))
        for (p in 100L..104L) book.add(resting(Side.Sell, p, 1))
        val snapshot = book.snapshot(2)
        assertEquals(listOf(95L, 94L), snapshot.bids.map { it.price })
        assertEquals(listOf(100L, 101L), snapshot.asks.map { it.price })
    }

    @Test
    fun bestIsFifoHeadOfBestLevel() {
        val book = OrderBook("A")
        val first = resting(Side.Sell, 100, 1)
        val second = resting(Side.Sell, 100, 1)
        book.add(second)
        book.add(first)
        assertEquals(second.id, book.best(Side.Sell)?.id)
        assertNull(book.best(Side.Buy))
    }

    @Test
    fun removeDropsEmptyLevel() {
        val book = OrderBook("A")
        val a = resting(Side.Buy, 90, 1)
        val b = resting(Side.Buy, 90, 1)
        book.add(a)
        book.add(b)
        assertTrue(book.remove(a))
        assertEquals(listOf(BookLevel(90, 1)), book.snapshot(10).bids)
        assertTrue(book.remove(b))
        assertTrue(book.snapshot(10).bids.isEmpty())
        assertFalse(book.remove(b))
    }
}
