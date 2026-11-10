package tddy.ko.linechart.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.up
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LineChartPanelTest {

    @get:Rule
    val rule = createComposeRule()

    private fun series(count: Int) = List(count) {
        val close = 100.0 + it % 17
        ChartData(close, 1_700_000_000_000L + it * 60_000L, open = close - 1, high = close + 2, low = close - 2, volume = 10.0 + it)
    }

    private lateinit var state: LineChartState

    private fun show(count: Int, type: ChartType = ChartType.Candle) {
        rule.setContent {
            state = rememberLineChartState(series(count))
            LineChartPanel(Modifier.testTag("chart"), 300.dp, state, type, timeframe = Timeframe.Minute1)
        }
        rule.waitForIdle()
    }

    @Test
    fun fewerThanTwoItems_drawsAnEmptyBoxWithoutCrash() {
        show(1)
        rule.onNodeWithTag("chart").assertExists()
        assertEquals(0f, rule.runOnIdle { state.plot.width })
    }

    @Test
    fun chartNarrowerThanThePriceAxis_drawsWithoutCrash() {
        rule.setContent {
            state = rememberLineChartState(series(200))
            Box(Modifier.width(10.dp)) {
                LineChartPanel(Modifier.testTag("chart"), 300.dp, state)
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("chart").assertExists()
        assertEquals(0f, rule.runOnIdle { state.plot.width })
    }

    @Test
    fun everyChartType_laysOutThePlotAfterTheEntryReveal() {
        val type = mutableStateOf(ChartType.Candle)
        rule.setContent {
            state = rememberLineChartState(series(200))
            LineChartPanel(Modifier.testTag("chart"), 300.dp, state, type.value)
        }
        for (each in ChartType.entries) {
            type.value = each
            rule.runOnIdle {
                assertTrue(state.plot.width > 0f)
                assertEquals(1f, state.entry.value, 0f)
            }
        }
    }

    @Test
    fun longPress_showsCrosshairAndHidesItOnRelease() {
        show(200)
        rule.onNodeWithTag("chart").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(10f, 0f))
        }
        rule.runOnIdle { assertTrue(state.crosshair.isSpecified) }
        rule.onNodeWithTag("chart").performTouchInput { up() }
        rule.runOnIdle { assertFalse(state.crosshair.isSpecified) }
    }

    private fun separatorY(index: Int): Float = rule.runOnIdle {
        val layout = checkNotNull(state.frame).layout
        val above = if (index == 0) layout.main.bottom else layout.panes[index - 1].bottom
        (above + layout.panes[index].top) / 2f
    }

    @Test
    fun longPressOnSeparatorAndDrag_resizesTheMainAndPaneHeights() {
        show(200)
        val before = rule.runOnIdle { checkNotNull(state.frame).layout }
        val y = separatorY(0)
        rule.onNodeWithTag("chart").performTouchInput {
            down(Offset(center.x, y))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, -40f))
        }
        rule.runOnIdle {
            assertEquals(0, state.resizing)
            assertFalse(state.crosshair.isSpecified)
        }
        rule.onNodeWithTag("chart").performTouchInput { up() }
        rule.waitForIdle()
        val after = rule.runOnIdle { checkNotNull(state.frame).layout }
        assertEquals(before.main.height - 40f, after.main.height, 1f)
        assertEquals(before.panes[0].height + 40f, after.panes[0].height, 1f)
        rule.runOnIdle { assertEquals(-1, state.resizing) }
    }

    @Test
    fun longPressAwayFromSeparators_stillShowsTheCrosshair() {
        show(200)
        val before = rule.runOnIdle { checkNotNull(state.frame).layout.main.height }
        rule.onNodeWithTag("chart").performTouchInput {
            down(Offset(center.x, 60f))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(0f, 30f))
        }
        rule.runOnIdle {
            assertTrue(state.crosshair.isSpecified)
            assertEquals(-1, state.resizing)
        }
        rule.onNodeWithTag("chart").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(before, rule.runOnIdle { checkNotNull(state.frame).layout.main.height }, 0.5f)
    }

    @Test
    fun horizontalSwipe_pansBackInTime() {
        show(300)
        rule.onNodeWithTag("chart").performTouchInput { swipeRight(startX = 100f, endX = 600f) }
        rule.runOnIdle { assertTrue("scroll=${state.scroll} slot=${state.slot} plot=${state.plot} n=${state.itemCount}", state.scroll > 0f) }
    }

    @Test
    fun pinch_changesTheSlotWidth() {
        show(300)
        val before = rule.runOnIdle { state.slotPx() }
        rule.onNodeWithTag("chart").performTouchInput {
            pinch(Offset(400f, 300f), Offset(500f, 300f), Offset(250f, 300f), Offset(650f, 300f))
        }
        rule.runOnIdle { assertNotEquals(before, state.slotPx()) }
    }

    @Test
    fun verticalDrag_isLeftToTheParentScroll() {
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            state = rememberLineChartState(series(200))
            Column(Modifier.height(400.dp).verticalScroll(scrollState)) {
                LineChartPanel(Modifier.testTag("chart").fillMaxWidth(), 300.dp, state)
                Spacer(Modifier.height(800.dp))
            }
        }
        rule.onNodeWithTag("chart").performTouchInput { swipeUp() }
        rule.runOnIdle {
            assertTrue(scrollState.value > 0)
            assertEquals(0f, state.scroll, 0f)
        }
    }

    @Test
    fun horizontalPan_isNotLeakedToAHorizontalParent() {
        lateinit var scrollState: ScrollState
        rule.setContent {
            scrollState = rememberScrollState()
            state = rememberLineChartState(series(300))
            Row(Modifier.horizontalScroll(scrollState)) {
                LineChartPanel(Modifier.testTag("chart").width(400.dp), 300.dp, state)
                Spacer(Modifier.width(800.dp))
            }
        }
        rule.onNodeWithTag("chart").performTouchInput { swipeRight(startX = 100f, endX = 600f) }
        rule.runOnIdle {
            assertTrue(state.scroll > 0f)
            assertEquals(0, scrollState.value)
        }
    }

    @Test
    fun liveAppend_followsTheRightEdgeAndKeepsAScrolledBackView() {
        val data = mutableStateOf(series(100))
        rule.setContent {
            state = rememberLineChartState(data.value)
            LineChartPanel(Modifier.testTag("chart"), 300.dp, state)
        }
        rule.waitForIdle()
        data.value = series(101)
        rule.runOnIdle { assertEquals(0f, state.scroll, 0f) }
        rule.runOnIdle { state.scroll = 10f }
        data.value = series(102)
        rule.runOnIdle { assertEquals(11f, state.scroll, 0f) }
    }

    @Test
    fun volumePane_addsItsHeightBelowTheChartHeight() {
        show(100)
        rule.onNodeWithTag("chart").assertHeightIsEqualTo(300.dp + 80.dp + 4.dp)
    }

    @Test
    fun crosshairOff_longPressShowsNothing() {
        rule.setContent {
            state = rememberLineChartState(series(200))
            LineChartPanel(Modifier.testTag("chart"), 300.dp, state, settings = ChartSettings(crosshair = false))
        }
        rule.waitForIdle()
        rule.onNodeWithTag("chart").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
            moveBy(Offset(10f, 0f))
        }
        rule.runOnIdle { assertFalse(state.crosshair.isSpecified) }
        rule.onNodeWithTag("chart").performTouchInput { up() }
    }
}
