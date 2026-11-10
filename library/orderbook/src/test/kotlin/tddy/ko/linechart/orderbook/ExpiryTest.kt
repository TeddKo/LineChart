package tddy.ko.linechart.orderbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiryTest {
    private var now = 1_000L

    private fun engine(policy: MatchingPolicy = MatchingPolicy()) = MatchingEngine(policy, clock = { now })

    private fun MatchingEngine.limit(side: Side, price: Long, quantity: Int, expiresAt: Long = Long.MAX_VALUE) =
        place("A", side, OrderType.Limit, quantity, price, expiresAt)

    @Test
    fun expireReclaimsSortedByExpiryThenId() {
        val e = engine()
        val late = checkNotNull(e.limit(Side.Buy, 90, 1, expiresAt = 3_000).orderId)
        val early = checkNotNull(e.limit(Side.Sell, 100, 1, expiresAt = 2_000).orderId)
        val tie = checkNotNull(e.limit(Side.Buy, 80, 1, expiresAt = 2_000).orderId)
        val keep = checkNotNull(e.limit(Side.Buy, 70, 1).orderId)
        assertTrue(e.expire(1_999).isEmpty())
        now = 3_000
        assertEquals(listOf(early, tie, late), e.expire().map { it.id })
        assertNull(e.order(late))
        assertEquals(listOf(BookLevel(70, 1)), e.snapshot("A").bids)
        assertEquals(70L, e.order(keep)?.price)
        assertTrue(e.expire().isEmpty())
    }

    @Test
    fun expireSkipsCanceledFilledAndReplacedEntries() {
        val e = engine()
        val canceled = checkNotNull(e.limit(Side.Buy, 90, 1, expiresAt = 2_000).orderId)
        val filled = checkNotNull(e.limit(Side.Sell, 100, 1, expiresAt = 2_000).orderId)
        val modified = checkNotNull(e.limit(Side.Buy, 80, 1, expiresAt = 2_000).orderId)
        e.cancel(canceled)
        e.limit(Side.Buy, 100, 1)
        e.modify(modified, expiresAt = 9_000)
        now = 2_500
        assertTrue(e.expire().isEmpty())
        assertEquals(modified, e.order(modified)?.id)
        assertNull(e.order(filled))
    }

    @Test
    fun matchingSkipsExpiredRestingLazily() {
        val e = engine()
        e.limit(Side.Sell, 100, 1, expiresAt = 2_000)
        val live = checkNotNull(e.limit(Side.Sell, 110, 1).orderId)
        now = 2_000
        val result = e.limit(Side.Buy, 120, 1)
        assertEquals(live, result.fills.single().sellOrderId)
        assertEquals(110L, result.fills.single().price)
        assertEquals(0, e.snapshot("A").asks.size)
    }

    @Test
    fun instantAgainstOnlyExpiredIsNoLiquidity() {
        val e = engine()
        e.limit(Side.Sell, 100, 1, expiresAt = 2_000)
        now = 2_000
        assertEquals(RejectReason.NoLiquidity, e.place("A", Side.Buy, OrderType.Instant, 1).reason)
    }

    @Test
    fun noCrossingPolicyIgnoresExpiredBest() {
        val e = engine(MatchingPolicy(allowCrossing = false))
        e.limit(Side.Sell, 100, 1, expiresAt = 2_000)
        now = 2_000
        assertEquals(1, e.limit(Side.Buy, 100, 1).restingQuantity)
    }

    @Test
    fun pastExpiryIsInvalid() {
        assertEquals(RejectReason.InvalidExpiry, engine().limit(Side.Buy, 90, 1, expiresAt = 999).reason)
    }

    @Test
    fun sameInputsGiveIdenticalResults() {
        fun run(): List<Any> {
            val e = engine()
            val out = mutableListOf<Any>()
            e.limit(Side.Sell, 100, 2)
            e.limit(Side.Sell, 100, 1)
            out.addAll(e.limit(Side.Buy, 100, 3).fills)
            out.add(e.place("A", Side.Buy, OrderType.Instant, 1).reason ?: "none")
            out.addAll(e.snapshot("A").bids)
            return out
        }
        assertEquals(run(), run())
    }

    @Test
    fun orderAndTradeIdsShareOneSequence() {
        val e = engine()
        val sell = e.limit(Side.Sell, 100, 1)
        val buy = e.limit(Side.Buy, 100, 1)
        assertEquals(1L, sell.orderId)
        assertEquals(2L, buy.orderId)
        assertEquals(3L, buy.fills.single().id)
    }

    @Test
    fun injectedIdSource() {
        var n = 100L
        val e = MatchingEngine(clock = { now }, nextId = { n++ })
        assertEquals(100L, e.limit(Side.Buy, 90, 1).orderId)
    }
}
