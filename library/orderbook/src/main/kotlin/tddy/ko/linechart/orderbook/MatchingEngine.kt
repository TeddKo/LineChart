package tddy.ko.linechart.orderbook

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
            if (price == null || price < policy.minPrice || price % policy.tickSize != 0L) {
                return rejected(RejectReason.InvalidPrice)
            }
            if (expiresAt <= now) return rejected(RejectReason.InvalidExpiry)
        }
        val book = books.getOrPut(option) { OrderBook(option) }
        if (type == OrderType.Limit && !policy.allowCrossing && price != null && crosses(book, side, price)) {
            return rejected(RejectReason.Crossing)
        }
        return match(book, side, type, quantity, price, expiresAt, owner, now)
    }

    /** Aggregated book of [option] limited to [depth] levels per side. */
    public fun snapshot(option: String, depth: Int = Int.MAX_VALUE): BookSnapshot =
        books[option]?.snapshot(depth) ?: BookSnapshot(option, emptyList(), emptyList())

    private fun crosses(book: OrderBook, side: Side, price: Long): Boolean {
        val best = book.best(side.opposite())?.price ?: return false
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
    ): OrderResult {
        var orderId: Long? = null
        var remaining = quantity
        var reason: RejectReason? = null
        val fills = ArrayList<Trade>()
        while (remaining > 0) {
            val maker = book.best(side.opposite()) ?: break
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
            if (maker.remaining == 0) book.remove(maker)
            remaining -= taken
        }
        if (remaining == 0) return OrderResult(orderId, fills, 0, 0, null)
        if (type == OrderType.Limit && reason == null && price != null) {
            val id = orderId ?: nextId().also { orderId = it }
            book.add(Resting(id, book.option, side, price, remaining, now, expiresAt, owner))
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
