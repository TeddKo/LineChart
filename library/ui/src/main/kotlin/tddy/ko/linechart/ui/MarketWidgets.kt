package tddy.ko.linechart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val RowHeight = 36.dp
private val RowPadding = 16.dp
private val ButtonShape = RoundedCornerShape(10.dp)
private val ChipShape = RoundedCornerShape(8.dp)

/**
 * Date of a trade as `yy/MM/dd` in [timeZone], independent of the default locale.
 */
internal fun tradeDate(time: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
    SimpleDateFormat("yy/MM/dd", Locale.ROOT).apply { this.timeZone = timeZone }.format(Date(time))

/**
 * [text] followed by `×quantity` when more than one unit traded.
 */
internal fun tradeLabel(text: String, quantity: Int): String = if (quantity > 1) "$text ×$quantity" else text

/**
 * Recent trades as size, trade price and trade date columns, newest first as given, at most [maxRows].
 *
 * A trade of more than one unit shows `×quantity` after its size, or after its price when [showOption] is false.
 *
 * @param showOption whether the size column is shown
 * @param priceFormatter text of a price
 * @param timeFormatter text of a trade time, `yy/MM/dd` by default
 */
@Composable
public fun TradeList(
    trades: List<MarketTrade>,
    modifier: Modifier = Modifier,
    maxRows: Int = 10,
    showOption: Boolean = true,
    colors: ChartColors = marketDefaultColors(),
    priceFormatter: (Long) -> String = ::defaultMarketPrice,
    timeFormatter: (Long) -> String = { tradeDate(it) },
    textStyle: TextStyle = MarketTextStyle,
) {
    val primary = textStyle.color.takeOrElse { colors.crosshair }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(RowHeight).padding(horizontal = RowPadding), verticalAlignment = Alignment.CenterVertically) {
            val header = textStyle.copy(color = colors.text)
            if (showOption) BasicText(stringResource(R.string.linechart_market_size), Modifier.weight(1f), style = header)
            BasicText(stringResource(R.string.linechart_market_trade_price), Modifier.weight(1f), style = header.copy(textAlign = TextAlign.Center))
            BasicText(stringResource(R.string.linechart_market_trade_date), Modifier.weight(1f), style = header.copy(textAlign = TextAlign.End))
        }
        trades.take(maxRows).forEach { trade ->
            Row(
                Modifier.fillMaxWidth().height(RowHeight).semantics(mergeDescendants = true) {}.padding(horizontal = RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val style = textStyle.copy(color = primary)
                val price = priceFormatter(trade.price)
                if (showOption) BasicText(tradeLabel(trade.option, trade.quantity), Modifier.weight(1f), style = style)
                BasicText(
                    if (showOption) price else tradeLabel(price, trade.quantity),
                    Modifier.weight(1f),
                    style = style.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                )
                BasicText(timeFormatter(trade.time), Modifier.weight(1f), style = style.copy(textAlign = TextAlign.End))
            }
        }
    }
}

/**
 * Horizontally scrolling single choice of market options, each with the price of [priceSide] or `-` when absent.
 *
 * @param selected key of the selected option
 * @param onSelect called with the key of the tapped option
 * @param priceSide [MarketSide.Ask] shows the lowest ask, [MarketSide.Bid] the highest bid
 */
@Composable
public fun OptionSelector(
    options: List<MarketOption>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    priceSide: MarketSide = MarketSide.Ask,
    colors: ChartColors = marketDefaultColors(),
    priceFormatter: (Long) -> String = ::defaultMarketPrice,
    textStyle: TextStyle = MarketTextStyle,
) {
    val primary = textStyle.color.takeOrElse { colors.crosshair }
    Row(
        modifier.horizontalScroll(rememberScrollState()).selectableGroup().padding(horizontal = RowPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.key == selected
            val price = if (priceSide == MarketSide.Ask) option.bestAsk else option.bestBid
            Column(
                Modifier
                    .width(88.dp)
                    .border(1.dp, if (isSelected) colors.crosshair else colors.grid, ChipShape)
                    .selectable(isSelected, role = Role.RadioButton) { onSelect(option.key) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(option.label, style = textStyle.copy(color = primary, fontWeight = FontWeight.Bold))
                BasicText(
                    price?.let(priceFormatter) ?: MARKET_EMPTY_PRICE,
                    style = textStyle.copy(color = colors.text, fontSize = 11.sp),
                )
            }
        }
    }
}

/**
 * Instant buy and sell buttons showing the lowest ask and the highest bid, always enabled.
 *
 * The buy fill uses [ChartColors.up] and the sell fill uses [ChartColors.down]. A missing price shows `-`.
 */
@Composable
public fun InstantTradeButtons(
    bestAsk: Long?,
    bestBid: Long?,
    onBuy: () -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ChartColors = marketDefaultColors(),
    priceFormatter: (Long) -> String = ::defaultMarketPrice,
    textStyle: TextStyle = MarketTextStyle,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = RowPadding), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InstantButton(stringResource(R.string.linechart_market_buy), bestAsk, stringResource(R.string.linechart_market_instant_buy_price), colors.up, onBuy, Modifier.weight(1f), colors, priceFormatter, textStyle)
        InstantButton(stringResource(R.string.linechart_market_sell), bestBid, stringResource(R.string.linechart_market_instant_sell_price), colors.down, onSell, Modifier.weight(1f), colors, priceFormatter, textStyle)
    }
}

@Composable
private fun InstantButton(
    title: String,
    price: Long?,
    caption: String,
    fill: Color,
    onClick: () -> Unit,
    modifier: Modifier,
    colors: ChartColors,
    priceFormatter: (Long) -> String,
    textStyle: TextStyle,
) {
    val onFill = colors.background
    Row(
        modifier
            .background(fill, ButtonShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(title, style = textStyle.copy(color = onFill, fontWeight = FontWeight.Bold))
        Box(Modifier.width(1.dp).height(28.dp).background(onFill.copy(alpha = 0.4f)))
        Column {
            BasicText(price?.let(priceFormatter) ?: MARKET_EMPTY_PRICE, style = textStyle.copy(color = onFill, fontWeight = FontWeight.Bold))
            BasicText(caption, style = textStyle.copy(color = onFill, fontSize = 11.sp))
        }
    }
}
