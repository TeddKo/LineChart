package tddy.ko.linechart.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MarketWidgetsComponentTest {

    @get:Rule
    val rule = createComposeRule()

    private val options = listOf(
        MarketOption("250", "250", 120_000, 100_000),
        MarketOption("260", "260", null, null),
    )

    @Test
    fun optionSelector_marksSelectedAndReportsTaps() {
        var selected by mutableStateOf("250")
        rule.setContent { OptionSelector(options, selected, { selected = it }) }
        rule.onNodeWithText("250").assertIsSelected()
        rule.onNodeWithText("260").assertIsNotSelected()
        rule.onNodeWithText("260").performClick()
        rule.onNodeWithText("260").assertIsSelected()
        rule.onNodeWithText("250").assertIsNotSelected()
    }

    @Test
    fun optionSelector_showsPriceOfTheChosenSideOrDash() {
        rule.setContent { OptionSelector(options, "250", {}, priceSide = MarketSide.Bid) }
        rule.onNodeWithText("100,000").assertExists()
        rule.onNodeWithText("-").assertExists()
    }

    @Test
    fun tradeList_showsQuantityMarkerAndFormattedDate() {
        val trades = listOf(MarketTrade("260", 135_000, 3, 1_700_000_000_000L), MarketTrade("250", 120_000, 1, 1_700_000_000_000L))
        rule.setContent { TradeList(trades, timeFormatter = { "d" }) }
        rule.onNodeWithText("260 ×3").assertExists()
        rule.onNodeWithText("135,000").assertExists()
        rule.onNodeWithText("250").assertExists()
    }

    @Test
    fun tradeList_withoutOptionPutsQuantityAfterPrice() {
        rule.setContent { TradeList(listOf(MarketTrade("260", 135_000, 2, 0)), showOption = false) }
        rule.onNodeWithText("135,000 ×2").assertExists()
    }

    @Test
    fun instantButtons_showPricesAndAreClickableEvenWithoutPrice() {
        var buys = 0
        var sells = 0
        rule.setContent { InstantTradeButtons(120_000, null, { buys++ }, { sells++ }) }
        rule.onNodeWithText("120,000").performClick()
        rule.onNodeWithText("-").performClick()
        assertEquals(1, buys)
        assertEquals(1, sells)
    }
}
