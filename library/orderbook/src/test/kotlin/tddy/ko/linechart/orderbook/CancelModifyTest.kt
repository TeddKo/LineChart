package tddy.ko.linechart.orderbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CancelModifyTest {
    private var now = 1_000L

    private fun engine(policy: MatchingPolicy = MatchingPolicy()) = MatchingEngine(policy, clock = { now })

    private fun MatchingEngine.limit(side: Side, price: Long, quantity: Int, expiresAt: Long = Long.MAX_VALUE) =
        place("A", side, OrderType.Limit, quantity, price, expiresAt)

    @Test
    fun cancelRemovesOrderAndReturnsIt() {
        val e = engine()
        val id = e.limit(Side.Buy, 90, 3).orderId!!
        val canceled = e.cancel(id)
        assertEquals(id, canceled?.id)
        assertEquals(3, canceled?.quantity)
        assertEquals(0, e.snapshot("A").bids.size)
        assertNull(e.order(id))
        assertNull(e.cancel(id))
    }

    @Test
    fun cancelUnknownIsNull() {
        assertNull(engine().cancel(99))
    }

    @Test
    fun orderExposesRemainingQuantity() {
        val e = engine()
        val id = e.limit(Side.Sell, 100, 5).orderId!!
        e.limit(Side.Buy, 100, 2)
        val order = e.order(id)
        assertNotNull(order)
        assertEquals(3, order?.quantity)
        assertEquals(Side.Sell, order?.side)
        assertEquals(100L, order?.price)
        assertEquals("A", order?.option)
        assertNull(e.order(12345))
    }

    @Test
    fun modifyKeepsIdUsesRemainingAndLosesPriority() {
        val e = engine()
        val first = e.limit(Side.Sell, 100, 3).orderId!!
        val second = e.limit(Side.Sell, 100, 1).orderId!!
        e.limit(Side.Buy, 100, 1)
        val result = e.modify(first, price = 100, expiresAt = 5_000)
        assertEquals(first, result.orderId)
        assertEquals(2, result.restingQuantity)
        assertEquals(5_000L, e.order(first)?.expiresAt)
        val fill = e.limit(Side.Buy, 100, 1).fills.single()
        assertEquals(second, fill.sellOrderId)
    }

    @Test
    fun modifyPriceMovesLevel() {
        val e = engine()
        val id = e.limit(Side.Buy, 90, 2).orderId!!
        e.modify(id, price = 95)
        assertEquals(listOf(BookLevel(95, 2)), e.snapshot("A").bids)
    }

    @Test
    fun modifyCanCrossAndMatch() {
        val e = engine()
        e.limit(Side.Sell, 100, 1)
        val id = e.limit(Side.Buy, 90, 3).orderId!!
        val result = e.modify(id, price = 100)
        assertEquals(1, result.filledQuantity)
        assertEquals(2, result.restingQuantity)
        assertEquals(id, result.fills.single().buyOrderId)
        assertEquals(2, e.order(id)?.quantity)
    }

    @Test
    fun rejectedModifyKeepsOriginal() {
        val e = engine(MatchingPolicy(allowCrossing = false, tickSize = 10))
        e.limit(Side.Sell, 100, 1)
        val id = e.limit(Side.Buy, 90, 2).orderId!!
        assertEquals(RejectReason.InvalidPrice, e.modify(id, price = 95).reason)
        assertEquals(RejectReason.Crossing, e.modify(id, price = 100).reason)
        assertEquals(RejectReason.InvalidExpiry, e.modify(id, expiresAt = now).reason)
        assertEquals(90L, e.order(id)?.price)
        assertEquals(listOf(BookLevel(90, 2)), e.snapshot("A").bids)
    }

    @Test
    fun modifyUnknownIsNotFound() {
        val result = engine().modify(7, price = 100)
        assertNull(result.orderId)
        assertEquals(RejectReason.NotFound, result.reason)
    }
}
