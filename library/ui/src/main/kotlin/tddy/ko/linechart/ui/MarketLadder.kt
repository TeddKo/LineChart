package tddy.ko.linechart.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val RowHeight = 36.dp
private val RowPadding = 16.dp
private const val BAR_ALPHA = 0.12f

/**
 * Levels of [side] ordered from the best price outward without empty levels, at most [maxRows].
 */
internal fun ladderRows(levels: List<MarketLevel>, side: MarketSide, maxRows: Int): List<MarketLevel> {
    val live = levels.filter { it.quantity > 0 }
    val sorted = if (side == MarketSide.Ask) live.sortedBy { it.price } else live.sortedByDescending { it.price }
    return sorted.take(maxRows)
}

/**
 * Bar length of every row as its quantity over the largest quantity, 0 when that is not positive.
 */
internal fun ladderFractions(rows: List<MarketLevel>): List<Float> {
    val max = rows.maxOfOrNull { it.quantity } ?: 0
    return rows.map { if (max <= 0) 0f else it.quantity.toFloat() / max }
}

/**
 * Order book ladder of one [side] with a quantity bar behind every row and the best price in bold.
 *
 * Asks list the lowest price first and bids the highest price first. Rows are clickable when [onLevelClick] is set.
 *
 * @param levels levels of the side in any order
 * @param maxRows number of rows shown
 * @param onLevelClick called with the price of the tapped row
 * @param colors bids use [ChartColors.up] and asks use [ChartColors.down]
 * @param priceFormatter text of a price
 */
@Composable
public fun OrderBookLadder(
    levels: List<MarketLevel>,
    side: MarketSide,
    modifier: Modifier = Modifier,
    maxRows: Int = 10,
    onLevelClick: ((Long) -> Unit)? = null,
    colors: ChartColors = marketDefaultColors(),
    priceFormatter: (Long) -> String = ::defaultMarketPrice,
    textStyle: TextStyle = MarketTextStyle,
) {
    val rows = remember(levels, side, maxRows) { ladderRows(levels, side, maxRows) }
    val fractions = remember(rows) { ladderFractions(rows) }
    val sideColor = if (side == MarketSide.Bid) colors.up else colors.down
    val primary = textStyle.color.takeOrElse { colors.crosshair }
    val select = stringResource(R.string.market_ladder_select)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(RowHeight).padding(horizontal = RowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BasicText(stringResource(R.string.market_price), style = textStyle.copy(color = colors.text))
            BasicText(stringResource(R.string.market_quantity), style = textStyle.copy(color = colors.text))
        }
        rows.forEachIndexed { index, level ->
            val fraction = fractions[index]
            val click = onLevelClick
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(RowHeight)
                    .drawBehind {
                        val width = size.width * fraction
                        drawRect(sideColor.copy(alpha = BAR_ALPHA), Offset(size.width - width, 0f), Size(width, size.height))
                    }
                    .then(if (click == null) Modifier else Modifier.clickable(role = Role.Button, onClickLabel = select) { click(level.price) })
                    .semantics(mergeDescendants = true) {}
                    .padding(horizontal = RowPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(
                    priceFormatter(level.price),
                    style = if (index == 0) textStyle.copy(color = sideColor, fontWeight = FontWeight.Bold) else textStyle.copy(color = primary),
                )
                BasicText(level.quantity.toString(), style = textStyle.copy(color = primary, textAlign = TextAlign.End))
            }
        }
    }
}
