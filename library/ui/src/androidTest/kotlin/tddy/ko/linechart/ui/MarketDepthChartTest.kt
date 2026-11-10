package tddy.ko.linechart.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.geometry.Offset
import org.junit.Rule
import org.junit.Test

class MarketDepthChartTest {

    @get:Rule
    val rule = createComposeRule()

    private val bids = listOf(MarketLevel(90_000, 2), MarketLevel(80_000, 3))
    private val asks = listOf(MarketLevel(100_000, 1), MarketLevel(110_000, 4))
    private val hasCursor = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

    @Test
    fun data_exposesBestPricesInDescription() {
        rule.setContent { DepthChart(bids, asks, Modifier.testTag("depth")) }
        rule.onNodeWithTag("depth").assertExists()
            .assert(
                SemanticsMatcher("best prices in description") { node ->
                    val text = node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
                    listOf("90,000", "100,000", "10,000").all { it in text }
                },
            )
    }

    @Test
    fun empty_drawsAnEmptyBoxWithoutCrash() {
        rule.setContent { DepthChart(emptyList(), emptyList(), Modifier.testTag("depth")) }
        rule.onNodeWithTag("depth").assertExists()
    }

    @Test
    fun oneSided_drawsWithoutCrash() {
        rule.setContent { DepthChart(bids, emptyList(), Modifier.testTag("depth")) }
        rule.onNodeWithTag("depth").assertExists()
    }

    @Test
    fun longPress_showsCursorAndHidesItOnRelease() {
        rule.setContent { DepthChart(bids, asks, Modifier.testTag("depth")) }
        rule.onNodeWithTag("depth").assert(hasCursor.not())
        rule.onNodeWithTag("depth").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(10f, 0f))
        }
        rule.onNodeWithTag("depth").assert(hasCursor)
        rule.onNodeWithTag("depth").performTouchInput { up() }
        rule.onNodeWithTag("depth").assert(hasCursor.not())
    }

    @Test
    fun narrowAndShort_longPressDoesNotCrash() {
        rule.setContent {
            Box(Modifier.width(40.dp)) { DepthChart(bids, asks, Modifier.testTag("depth"), height = 30.dp) }
        }
        rule.onNodeWithTag("depth").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(5f, 0f))
        }
        rule.onNodeWithTag("depth").assert(hasCursor)
        rule.onNodeWithTag("depth").performTouchInput { up() }
    }
}
