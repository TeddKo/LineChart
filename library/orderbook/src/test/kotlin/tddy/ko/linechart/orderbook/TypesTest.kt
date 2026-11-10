package tddy.ko.linechart.orderbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TypesTest {
    private fun trade(quantity: Int) = Trade(1, "A", 100, quantity, 1, 2, Side.Buy, 0, 0, 0)

    @Test
    fun noFeeIsZero() {
        assertEquals(0L, FeePolicy.NoFee.fee(Side.Buy, 1_000, 5))
        assertEquals(0L, FeePolicy.NoFee.fee(Side.Sell, 1_000, 5))
    }

    @Test
    fun policyDefaults() {
        val policy = MatchingPolicy()
        assertEquals(true, policy.allowCrossing)
        assertEquals(1L, policy.tickSize)
        assertEquals(1L, policy.minPrice)
        assertEquals(1_000L, MatchingPolicy(tickSize = 1_000).minPrice)
    }

    @Test
    fun policyRejectsNonPositiveTickAndMinPrice() {
        assertThrows(IllegalArgumentException::class.java) { MatchingPolicy(tickSize = 0) }
        assertThrows(IllegalArgumentException::class.java) { MatchingPolicy(minPrice = 0) }
    }

    @Test
    fun snapshotBestPrices() {
        val snapshot = BookSnapshot("A", listOf(BookLevel(90, 1), BookLevel(80, 2)), listOf(BookLevel(100, 3)))
        assertEquals(90L, snapshot.bestBid)
        assertEquals(100L, snapshot.bestAsk)
        val empty = BookSnapshot("A", emptyList(), emptyList())
        assertNull(empty.bestBid)
        assertNull(empty.bestAsk)
    }

    @Test
    fun resultFilledQuantitySumsFills() {
        val result = OrderResult(1, listOf(trade(2), trade(3)), 0, 0, null)
        assertEquals(5, result.filledQuantity)
        assertEquals(0, OrderResult(null, emptyList(), 0, 0, RejectReason.InvalidQuantity).filledQuantity)
    }

    @Test
    fun levelQuantitySaturatesInsteadOfOverflowing() {
        val e = MatchingEngine(clock = { 1L })
        repeat(3) { e.place("A", Side.Sell, OrderType.Limit, Int.MAX_VALUE / 2 + 1, 100) }
        assertEquals(Int.MAX_VALUE, e.snapshot("A").asks.single().quantity)
    }

    @Test
    fun orderResultAndSnapshotHaveReadableToString() {
        val e = MatchingEngine(clock = { 1L })
        val result = e.place("A", Side.Sell, OrderType.Limit, 2, 100)
        assertEquals(true, "restingQuantity=2" in result.toString())
        assertEquals(true, "100" in e.snapshot("A").toString())
        assertEquals(true, "100" in checkNotNull(e.order(checkNotNull(result.orderId))).toString())
    }
}
