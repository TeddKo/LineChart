package tddy.ko.linechart.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MarketLadderComponentTest {

    @get:Rule
    val rule = createComposeRule()

    private val levels = listOf(MarketLevel(110_000, 4), MarketLevel(100_000, 2), MarketLevel(105_000, 1))

    @Test
    fun rows_showFormattedPricesLimitedToMaxRows() {
        rule.setContent { OrderBookLadder(levels, MarketSide.Ask, Modifier.testTag("ladder"), maxRows = 2) }
        rule.onNodeWithText("100,000").assertExists()
        rule.onNodeWithText("105,000").assertExists()
        rule.onAllNodesWithText("110,000").assertCountEquals(0)
    }

    @Test
    fun click_reportsThePriceOfTheRow() {
        val clicked = mutableListOf<Long>()
        rule.setContent { OrderBookLadder(levels, MarketSide.Bid, onLevelClick = { clicked += it }) }
        rule.onNodeWithText("105,000").performClick()
        assertEquals(listOf(105_000L), clicked)
    }

    @Test
    fun withoutClickHandler_rowsStillRender() {
        rule.setContent { OrderBookLadder(levels, MarketSide.Ask) }
        rule.onNodeWithText("110,000").assertExists()
    }
}
