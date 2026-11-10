package tddy.ko.linechart.orderbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchingTest {
    private var now = 1_000L

    private fun engine(
        policy: MatchingPolicy = MatchingPolicy(),
        fee: FeePolicy = FeePolicy.NoFee,
    ) = MatchingEngine(policy, fee, { now })

    private fun MatchingEngine.limit(side: Side, price: Long, quantity: Int, owner: String? = null, option: String = "A") =
        place(option, side, OrderType.Limit, quantity, price, owner = owner)

    private fun MatchingEngine.instant(side: Side, quantity: Int, price: Long? = null, owner: String? = null) =
        place("A", side, OrderType.Instant, quantity, price, owner = owner)

    @Test
    fun betterPriceFillsFirst() {
        val e = engine()
        e.limit(Side.Sell, 110, 1)
        val cheap = e.limit(Side.Sell, 100, 1)
        val result = e.limit(Side.Buy, 120, 1)
        assertEquals(cheap.orderId, result.fills.single().sellOrderId)
        assertEquals(100L, result.fills.single().price)
    }

    @Test
    fun fifoWithinPrice() {
        val e = engine()
        val first = e.limit(Side.Sell, 100, 1)
        val second = e.limit(Side.Sell, 100, 1)
        val result = e.limit(Side.Buy, 100, 2)
        assertEquals(listOf(first.orderId, second.orderId), result.fills.map { it.sellOrderId })
    }

    @Test
    fun partialFillKeepsPriority() {
        val e = engine()
        val first = e.limit(Side.Sell, 100, 3)
        val second = e.limit(Side.Sell, 100, 1)
        e.limit(Side.Buy, 100, 1)
        val result = e.limit(Side.Buy, 100, 2)
        assertEquals(listOf(first.orderId), result.fills.map { it.sellOrderId })
        assertEquals(2, result.filledQuantity)
        val rest = e.snapshot("A").asks.single()
        assertEquals(BookLevel(100, 1), rest)
        val next = e.limit(Side.Buy, 100, 1)
        assertEquals(second.orderId, next.fills.single().sellOrderId)
    }

    @Test
    fun crossingTradesAtMakerPriceAndRemainderRests() {
        val e = engine()
        e.limit(Side.Sell, 100, 2)
        val result = e.limit(Side.Buy, 130, 5)
        val trade = result.fills.single()
        assertEquals(100L, trade.price)
        assertEquals(Side.Buy, trade.takerSide)
        assertEquals(2, result.filledQuantity)
        assertEquals(3, result.restingQuantity)
        assertEquals(listOf(BookLevel(130, 3)), e.snapshot("A").bids)
        assertEquals(1_000L, trade.time)
    }

    @Test
    fun sellTakerFillsHighestBidAtMakerPrice() {
        val e = engine()
        e.limit(Side.Buy, 90, 1)
        e.limit(Side.Buy, 95, 1)
        val result = e.limit(Side.Sell, 80, 1)
        assertEquals(95L, result.fills.single().price)
        assertEquals(Side.Sell, result.fills.single().takerSide)
    }

    @Test
    fun crossingRejectedWhenPolicyForbids() {
        val e = engine(MatchingPolicy(allowCrossing = false))
        e.limit(Side.Sell, 100, 1)
        val buy = e.limit(Side.Buy, 100, 1)
        assertNull(buy.orderId)
        assertEquals(RejectReason.Crossing, buy.reason)
        assertEquals(1, e.limit(Side.Sell, 90, 1).restingQuantity)
        assertEquals(1, e.limit(Side.Buy, 85, 1).restingQuantity)
        assertEquals(RejectReason.Crossing, e.limit(Side.Sell, 80, 1).reason)
        assertEquals(RejectReason.Crossing, e.limit(Side.Buy, 120, 1).reason)
    }

    @Test
    fun instantIgnoresNoCrossingPolicy() {
        val e = engine(MatchingPolicy(allowCrossing = false))
        e.limit(Side.Sell, 100, 1)
        assertEquals(1, e.instant(Side.Buy, 1).filledQuantity)
    }

    @Test
    fun validationRejectsWithoutConsumingId() {
        val e = engine(MatchingPolicy(tickSize = 10, minPrice = 50))
        assertEquals(RejectReason.InvalidQuantity, e.limit(Side.Buy, 100, 0).reason)
        assertEquals(RejectReason.InvalidQuantity, e.instant(Side.Buy, -1).reason)
        assertEquals(RejectReason.InvalidPrice, e.limit(Side.Buy, 40, 1).reason)
        assertEquals(RejectReason.InvalidPrice, e.limit(Side.Buy, 105, 1).reason)
        assertEquals(RejectReason.InvalidPrice, e.place("A", Side.Buy, OrderType.Limit, 1).reason)
        val past = e.place("A", Side.Buy, OrderType.Limit, 1, 100, expiresAt = now)
        assertEquals(RejectReason.InvalidExpiry, past.reason)
        assertEquals(1L, e.limit(Side.Buy, 100, 1).orderId)
    }

    @Test
    fun optionsAreIsolated() {
        val e = engine()
        e.limit(Side.Sell, 100, 1, option = "A")
        val other = e.limit(Side.Buy, 100, 1, option = "B")
        assertEquals(0, other.filledQuantity)
        assertEquals(1, e.snapshot("A").asks.size)
        assertEquals(1, e.snapshot("B").bids.size)
        assertEquals(0, e.snapshot("C").bids.size)
    }

    @Test
    fun snapshotAggregatesWithDepth() {
        val e = engine()
        e.limit(Side.Buy, 90, 1)
        e.limit(Side.Buy, 90, 2)
        e.limit(Side.Buy, 80, 4)
        assertEquals(listOf(BookLevel(90, 3)), e.snapshot("A", 1).bids)
        assertEquals(listOf(BookLevel(90, 3), BookLevel(80, 4)), e.snapshot("A").bids)
    }

    @Test
    fun feesPerSideWithCapInsidePolicy() {
        val fee = FeePolicy { side, price, quantity ->
            val rate = if (side == Side.Buy) 3 else 6
            minOf(Math.multiplyExact(price, quantity.toLong()) * rate / 100, 300L)
        }
        val e = engine(fee = fee)
        e.limit(Side.Sell, 1_000, 1)
        val small = e.limit(Side.Buy, 1_000, 1).fills.single()
        assertEquals(30L, small.buyerFee)
        assertEquals(60L, small.sellerFee)
        e.limit(Side.Sell, 10_000, 1)
        val capped = e.limit(Side.Buy, 10_000, 1).fills.single()
        assertEquals(300L, capped.buyerFee)
        assertEquals(300L, capped.sellerFee)
    }

    @Test
    fun instantOnEmptyBookIsNoLiquidity() {
        val result = engine().instant(Side.Buy, 2)
        assertNull(result.orderId)
        assertEquals(RejectReason.NoLiquidity, result.reason)
        assertEquals(2, result.canceledQuantity)
    }

    @Test
    fun instantPartialCancelsRemainder() {
        val e = engine()
        e.limit(Side.Sell, 100, 2)
        val result = e.instant(Side.Buy, 5)
        assertEquals(2, result.filledQuantity)
        assertEquals(3, result.canceledQuantity)
        assertEquals(0, result.restingQuantity)
        assertEquals(RejectReason.NoLiquidity, result.reason)
        assertEquals(0, e.snapshot("A").bids.size)
    }

    @Test
    fun instantFullFillHasNoReason() {
        val e = engine()
        e.limit(Side.Buy, 100, 2)
        val result = e.instant(Side.Sell, 2)
        assertNull(result.reason)
        assertEquals(0, result.canceledQuantity)
    }

    @Test
    fun instantPriceBoundStopsFilling() {
        val e = engine()
        e.limit(Side.Sell, 100, 1)
        e.limit(Side.Sell, 120, 1)
        val buy = e.instant(Side.Buy, 2, price = 110)
        assertEquals(1, buy.filledQuantity)
        assertEquals(1, buy.canceledQuantity)
        e.limit(Side.Buy, 90, 1)
        assertEquals(0, e.instant(Side.Sell, 1, price = 95).filledQuantity)
    }

    @Test
    fun selfTradeStopsAndCancelsRemainderKeepingEarlierFills() {
        val e = engine()
        e.limit(Side.Sell, 100, 1, owner = "bob")
        e.limit(Side.Sell, 101, 1, owner = "me")
        e.limit(Side.Sell, 102, 1, owner = "bob")
        val result = e.limit(Side.Buy, 105, 3, owner = "me")
        assertEquals(1, result.filledQuantity)
        assertEquals(2, result.canceledQuantity)
        assertEquals(RejectReason.SelfTrade, result.reason)
        assertEquals(0, result.restingQuantity)
        assertEquals(0, e.snapshot("A").bids.size)
    }

    @Test
    fun selfTradeNeedsOwner() {
        val e = engine()
        e.limit(Side.Sell, 100, 1)
        assertEquals(1, e.limit(Side.Buy, 100, 1).filledQuantity)
        e.limit(Side.Sell, 100, 1, owner = "me")
        assertEquals(1, e.limit(Side.Buy, 100, 1).filledQuantity)
    }

    @Test
    fun throwingFeePolicyLeavesTheBookAndIdsUntouched() {
        var calls = 0
        val e = engine(fee = FeePolicy { _, _, _ -> if (++calls > 2) error("fee") else 0L })
        val first = e.limit(Side.Sell, 100, 1)
        val second = e.limit(Side.Sell, 100, 1)
        val before = e.snapshot("A").asks
        var thrown = false
        try {
            e.limit(Side.Buy, 100, 2)
        } catch (_: IllegalStateException) {
            thrown = true
        }
        assertEquals(true, thrown)
        assertEquals(before, e.snapshot("A").asks)
        assertEquals(1, checkNotNull(e.order(checkNotNull(first.orderId))).quantity)
        assertEquals(1, checkNotNull(e.order(checkNotNull(second.orderId))).quantity)
        assertEquals(checkNotNull(second.orderId) + 1, e.limit(Side.Buy, 50, 1).orderId)
    }
}
