package tddy.ko.linechart.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Side of an order book: [Bid] are buy orders, [Ask] are sell orders.
 */
public enum class MarketSide { Bid, Ask }

/**
 * Aggregated [quantity] waiting at one [price].
 */
@Immutable
public data class MarketLevel(public val price: Long, public val quantity: Int)

/**
 * Executed trade of an [option] at [price], with [time] in epoch millis.
 */
@Immutable
public data class MarketTrade(
    public val option: String,
    public val price: Long,
    public val quantity: Int,
    public val time: Long,
)

/**
 * Selectable option of a market such as a size, with the lowest ask and the highest bid or null when absent.
 */
@Immutable
public data class MarketOption(
    public val key: String,
    public val label: String,
    public val bestAsk: Long?,
    public val bestBid: Long?,
)

internal val MarketTextStyle = TextStyle(fontSize = 13.sp, fontFeatureSettings = "tnum")

internal const val MARKET_EMPTY_PRICE = "-"

@Composable
internal fun marketDefaultColors(): ChartColors = if (isSystemInDarkTheme()) ChartColors.KoreanDark else ChartColors.Korean

internal fun defaultMarketPrice(value: Long): String = formatPrice(value.toDouble())
