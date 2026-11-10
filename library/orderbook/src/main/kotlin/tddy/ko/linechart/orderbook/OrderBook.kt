package tddy.ko.linechart.orderbook

import java.util.TreeMap

internal class Resting(
    val id: Long,
    val option: String,
    val side: Side,
    val price: Long,
    var remaining: Int,
    val createdAt: Long,
    val expiresAt: Long,
    val owner: String?,
) {
    fun toOrder(): Order = Order(id, option, side, price, remaining, createdAt, expiresAt, owner)
}

/**
 * Price levels of one option, each level a FIFO queue.
 *
 * ponytail: removal is O(k) in the level queue, switch to an intrusive list if levels hold thousands of orders.
 */
internal class OrderBook(val option: String) {
    private val bids = TreeMap<Long, ArrayDeque<Resting>>(reverseOrder())
    private val asks = TreeMap<Long, ArrayDeque<Resting>>()

    fun levels(side: Side): TreeMap<Long, ArrayDeque<Resting>> = if (side == Side.Buy) bids else asks

    fun add(resting: Resting) {
        levels(resting.side).getOrPut(resting.price) { ArrayDeque() }.addLast(resting)
    }

    fun remove(resting: Resting): Boolean {
        val levels = levels(resting.side)
        val queue = levels[resting.price] ?: return false
        val removed = queue.remove(resting)
        if (queue.isEmpty()) levels.remove(resting.price)
        return removed
    }

    fun best(side: Side): Resting? = levels(side).firstEntry()?.value?.first()

    fun snapshot(depth: Int): BookSnapshot = BookSnapshot(option, aggregate(bids, depth), aggregate(asks, depth))

    /** Levels of [levels] up to [depth], each quantity summed in [Long] and capped at [Int.MAX_VALUE]. */
    private fun aggregate(levels: TreeMap<Long, ArrayDeque<Resting>>, depth: Int): List<BookLevel> =
        levels.entries.take(depth).map { (price, queue) -> BookLevel(price, queue.sumOf { it.remaining.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
}
