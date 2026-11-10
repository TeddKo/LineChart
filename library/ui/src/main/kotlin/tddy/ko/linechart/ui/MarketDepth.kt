package tddy.ko.linechart.ui

import kotlin.math.max

private const val X_MARGIN = 1.05
private const val MIN_HALF_WIDTH_RATIO = 0.05

/**
 * Cumulative depth of both sides, bid prices descending and ask prices ascending, each with the running quantity from the best price outward.
 */
internal class Depth(
    val bidPrices: LongArray,
    val bidCum: LongArray,
    val askPrices: LongArray,
    val askCum: LongArray,
) {
    val maxCum: Long = max(bidCum.lastOrNull() ?: 0L, askCum.lastOrNull() ?: 0L)

    val mid: Double? = when {
        bidPrices.isNotEmpty() && askPrices.isNotEmpty() -> (bidPrices[0] + askPrices[0]) / 2.0
        bidPrices.isNotEmpty() -> bidPrices[0].toDouble()
        askPrices.isNotEmpty() -> askPrices[0].toDouble()
        else -> null
    }

    val spread: Long? = if (bidPrices.isNotEmpty() && askPrices.isNotEmpty()) askPrices[0] - bidPrices[0] else null

    /**
     * Quantity cumulated at [price]: bids at or above it left of the best bid, asks at or below it right of the best ask, null in the gap and on an empty side.
     */
    fun cumulativeAt(price: Double): Long? {
        if (bidPrices.isNotEmpty() && price <= bidPrices[0]) {
            var lo = 0
            var hi = bidPrices.size
            while (lo < hi) {
                val m = (lo + hi) ushr 1
                if (bidPrices[m] >= price) lo = m + 1 else hi = m
            }
            return bidCum[lo - 1]
        }
        if (askPrices.isNotEmpty() && price >= askPrices[0]) {
            var lo = 0
            var hi = askPrices.size
            while (lo < hi) {
                val m = (lo + hi) ushr 1
                if (askPrices[m] <= price) lo = m + 1 else hi = m
            }
            return askCum[lo - 1]
        }
        return null
    }
}

/**
 * Depth of the levels with a positive quantity, sorted regardless of the input order.
 */
internal fun depthOf(bids: List<MarketLevel>, asks: List<MarketLevel>): Depth {
    val bidLevels = bids.filter { it.quantity > 0 }.sortedByDescending { it.price }
    val askLevels = asks.filter { it.quantity > 0 }.sortedBy { it.price }
    return Depth(
        LongArray(bidLevels.size) { bidLevels[it].price },
        cumulative(bidLevels),
        LongArray(askLevels.size) { askLevels[it].price },
        cumulative(askLevels),
    )
}

private fun cumulative(levels: List<MarketLevel>): LongArray {
    var sum = 0L
    return LongArray(levels.size) { sum += levels[it].quantity; sum }
}

/**
 * Visible price range centered on the mid price and widened by 5 percent past the farthest level, null for an empty book.
 */
internal fun depthXRange(depth: Depth): Pair<Double, Double>? {
    val mid = depth.mid ?: return null
    val lowest = depth.bidPrices.lastOrNull()?.toDouble() ?: mid
    val highest = depth.askPrices.lastOrNull()?.toDouble() ?: mid
    val half = max(max(mid - lowest, highest - mid) * X_MARGIN, mid * MIN_HALF_WIDTH_RATIO)
    return mid - half to mid + half
}
