package tddy.ko.linechart.orderbook

/** Order kind: [Limit] rests at a price, [Instant] fills immediately or is canceled. */
public enum class OrderType { Limit, Instant }

/** Reason an order was rejected or its remainder canceled. */
public enum class RejectReason { InvalidQuantity, InvalidPrice, InvalidExpiry, Crossing, NoLiquidity, SelfTrade, NotFound }

/**
 * Open order seen from outside.
 *
 * [quantity] is the remaining quantity, [price] is in KRW, [expiresAt] is absolute epoch milliseconds
 * with [Long.MAX_VALUE] meaning no expiry.
 */
public class Order internal constructor(
    public val id: Long,
    public val option: String,
    public val side: Side,
    public val price: Long,
    public val quantity: Int,
    public val createdAt: Long,
    public val expiresAt: Long,
    public val owner: String?,
) {
    override fun toString(): String = "Order(id=$id, option=$option, side=$side, price=$price, quantity=$quantity, expiresAt=$expiresAt, owner=$owner)"
}

/** Executed trade at the maker price. Fees are in KRW. */
public data class Trade(
    val id: Long,
    val option: String,
    val price: Long,
    val quantity: Int,
    val buyOrderId: Long,
    val sellOrderId: Long,
    val takerSide: Side,
    val time: Long,
    val buyerFee: Long,
    val sellerFee: Long,
)

/** Aggregated quantity at one price. */
public data class BookLevel(val price: Long, val quantity: Int)

/** Aggregated book of one option, [bids] descending and [asks] ascending by price. */
public class BookSnapshot internal constructor(
    public val option: String,
    public val bids: List<BookLevel>,
    public val asks: List<BookLevel>,
) {
    /** Highest bid price, null without bids. */
    public val bestBid: Long? get() = bids.firstOrNull()?.price

    /** Lowest ask price, null without asks. */
    public val bestAsk: Long? get() = asks.firstOrNull()?.price

    override fun toString(): String = "BookSnapshot(option=$option, bids=$bids, asks=$asks)"
}

/**
 * Fee of one side of a trade, caps live inside the policy, with a no-throw requirement for implementations.
 */
public fun interface FeePolicy {
    /** Fee in KRW charged to [side] for [quantity] filled at [price]. */
    public fun fee(side: Side, price: Long, quantity: Int): Long

    public companion object {
        /** Policy charging no fee. */
        public val NoFee: FeePolicy = FeePolicy { _, _, _ -> 0L }
    }
}

/**
 * Matching rules.
 *
 * With [allowCrossing] false a limit order that would match is rejected with [RejectReason.Crossing].
 * Limit prices must be multiples of [tickSize] and at least [minPrice].
 */
public class MatchingPolicy(
    public val allowCrossing: Boolean = true,
    public val tickSize: Long = 1L,
    public val minPrice: Long = tickSize,
) {
    init {
        require(tickSize > 0) { "tickSize must be positive" }
        require(minPrice > 0) { "minPrice must be positive" }
    }
}

/**
 * Outcome of placing or modifying an order.
 *
 * [orderId] is null when rejected. [restingQuantity] stays in the book, [canceledQuantity] was dropped
 * and explained by [reason].
 */
public class OrderResult internal constructor(
    public val orderId: Long?,
    public val fills: List<Trade>,
    public val restingQuantity: Int,
    public val canceledQuantity: Int,
    public val reason: RejectReason?,
) {
    /** Quantity executed by [fills]. */
    public val filledQuantity: Int get() = fills.sumOf { it.quantity }

    override fun toString(): String =
        "OrderResult(orderId=$orderId, fills=$fills, restingQuantity=$restingQuantity, canceledQuantity=$canceledQuantity, reason=$reason)"
}
