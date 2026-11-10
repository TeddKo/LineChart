package tddy.ko.linechart.orderbook

import java.util.PriorityQueue

/**
 * Price-time priority matching engine over independent books per option.
 *
 * Better price fills first, FIFO within a price, trades execute at the maker price. Money is KRW as [Long],
 * fee policies own notional math. Time and ids are injected so runs are deterministic, order and trade ids
 * share one sequence. Not thread-safe, the caller serializes access.
 */
public class MatchingEngine(
    private val policy: MatchingPolicy = MatchingPolicy(),
    private val feePolicy: FeePolicy = FeePolicy.NoFee,
    private val clock: () -> Long = System::currentTimeMillis,
    private val nextId: () -> Long = counter(),
) {
    private val books = HashMap<String, OrderBook>()
    private val byId = HashMap<Long, Resting>()
    private val expiries = PriorityQueue<Resting>(compareBy({ it.expiresAt }, { it.id }))

    /**
     * Places an order. A rejected order consumes no id and returns a null [OrderResult.orderId].
     *
     * [OrderType.Limit] needs [price] and rests its remainder, [OrderType.Instant] fills up to the optional
     * [price] bound and cancels the rest. [expiresAt] is absolute epoch milliseconds.
     */
    public fun place(
        option: String,
        side: Side,
        type: OrderType,
        quantity: Int,
        price: Long? = null,
        expiresAt: Long = Long.MAX_VALUE,
        owner: String? = null,
    ): OrderResult {
        val now = clock()
        if (quantity <= 0) return rejected(RejectReason.InvalidQuantity)
        if (type == OrderType.Limit) {
            val invalid = if (price == null) RejectReason.InvalidPrice else validateLimit(price, expiresAt, now)
            if (invalid != null) return rejected(invalid)
        }
        val book = books.getOrPut(option) { OrderBook(option) }
        if (type == OrderType.Limit && !policy.allowCrossing && price != null && crosses(book, side, price, now)) {
            return rejected(RejectReason.Crossing)
        }
        return match(book, side, type, quantity, price, expiresAt, owner, now)
    }

    /** Removes an open order and returns it, null when unknown. */
    public fun cancel(orderId: Long): Order? {
        val resting = byId.remove(orderId) ?: return null
        books[resting.option]?.remove(resting)
        return resting.toOrder()
    }

    /**
     * Replaces [price] and [expiresAt] of an open order, null keeps the current value.
     *
     * Cancel and replace with the same id and the remaining quantity: the order loses time priority and may
     * match. A rejected modify keeps the original order untouched.
     */
    public fun modify(orderId: Long, price: Long? = null, expiresAt: Long? = null): OrderResult {
        val now = clock()
        val resting = byId[orderId]?.takeIf { it.expiresAt > now } ?: return rejected(RejectReason.NotFound)
        val newPrice = price ?: resting.price
        val newExpiry = expiresAt ?: resting.expiresAt
        val book = books.getValue(resting.option)
        validateLimit(newPrice, newExpiry, now)?.let { return rejected(it) }
        if (!policy.allowCrossing && crosses(book, resting.side, newPrice, now)) return rejected(RejectReason.Crossing)
        cancel(orderId)
        return match(book, resting.side, OrderType.Limit, resting.remaining, newPrice, newExpiry, resting.owner, now, orderId)
    }

    /**
     * Reclaims every order with `expiresAt <= now` and returns them sorted by expiry then id.
     *
     * Matching skips expired orders on its own, but they stay known to [order] and [snapshot] until this runs,
     * so the caller invokes it on a timer.
     */
    public fun expire(now: Long = clock()): List<Order> {
        val expired = ArrayList<Order>()
        while (true) {
            val top = expiries.peek()?.takeIf { it.expiresAt <= now } ?: break
            expiries.poll()
            if (byId[top.id] !== top) continue
            byId.remove(top.id)
            books[top.option]?.remove(top)
            expired += top.toOrder()
        }
        return expired
    }

    /** Open order by id, null when unknown or fully filled. */
    public fun order(orderId: Long): Order? = byId[orderId]?.toOrder()

    /** Aggregated book of [option] limited to [depth] levels per side. */
    public fun snapshot(option: String, depth: Int = Int.MAX_VALUE): BookSnapshot =
        books[option]?.snapshot(depth) ?: BookSnapshot(option, emptyList(), emptyList())

    private fun validateLimit(price: Long, expiresAt: Long, now: Long): RejectReason? = when {
        price < policy.minPrice || price % policy.tickSize != 0L -> RejectReason.InvalidPrice
        expiresAt <= now -> RejectReason.InvalidExpiry
        else -> null
    }

    private fun liveBest(book: OrderBook, side: Side, now: Long): Resting? {
        while (true) {
            val best = book.best(side) ?: return null
            if (best.expiresAt > now) return best
            book.remove(best)
        }
    }

    private fun crosses(book: OrderBook, side: Side, price: Long, now: Long): Boolean {
        val best = liveBest(book, side.opposite(), now)?.price ?: return false
        return if (side == Side.Buy) price >= best else price <= best
    }

    private fun match(
        book: OrderBook,
        side: Side,
        type: OrderType,
        quantity: Int,
        price: Long?,
        expiresAt: Long,
        owner: String?,
        now: Long,
        existingId: Long? = null,
    ): OrderResult {
        var orderId: Long? = existingId
        var remaining = quantity
        var reason: RejectReason? = null
        val fills = ArrayList<Trade>()
        while (remaining > 0) {
            val maker = liveBest(book, side.opposite(), now) ?: break
            if (price != null && (if (side == Side.Buy) maker.price > price else maker.price < price)) break
            if (owner != null && maker.owner == owner) {
                reason = RejectReason.SelfTrade
                break
            }
            val taken = minOf(remaining, maker.remaining)
            val takerId = orderId ?: nextId().also { orderId = it }
            val buyId = if (side == Side.Buy) takerId else maker.id
            val sellId = if (side == Side.Buy) maker.id else takerId
            fills += Trade(
                id = nextId(),
                option = book.option,
                price = maker.price,
                quantity = taken,
                buyOrderId = buyId,
                sellOrderId = sellId,
                takerSide = side,
                time = now,
                buyerFee = feePolicy.fee(Side.Buy, maker.price, taken),
                sellerFee = feePolicy.fee(Side.Sell, maker.price, taken),
            )
            maker.remaining -= taken
            if (maker.remaining == 0) {
                book.remove(maker)
                byId.remove(maker.id)
            }
            remaining -= taken
        }
        if (remaining == 0) return OrderResult(orderId, fills, 0, 0, null)
        if (type == OrderType.Limit && reason == null && price != null) {
            val id = orderId ?: nextId().also { orderId = it }
            val resting = Resting(id, book.option, side, price, remaining, now, expiresAt, owner)
            book.add(resting)
            byId[id] = resting
            if (expiresAt != Long.MAX_VALUE) expiries += resting
            return OrderResult(id, fills, remaining, 0, null)
        }
        return OrderResult(orderId, fills, 0, remaining, reason ?: RejectReason.NoLiquidity)
    }

    private fun rejected(reason: RejectReason) = OrderResult(null, emptyList(), 0, 0, reason)

    private fun Side.opposite() = if (this == Side.Buy) Side.Sell else Side.Buy

    private companion object {
        fun counter(): () -> Long {
            var n = 0L
            return { ++n }
        }
    }
}
