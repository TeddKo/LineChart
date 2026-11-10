package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.jvm.internal.DefaultConstructorMarker

class LineChartStateTest {

    private fun items(vararg dates: Long) = dates.map { ChartData(it.toDouble(), it) }

    private fun state(vararg dates: Long, maxDataPoints: Int = Int.MAX_VALUE, scaleFactor: Int = 10_000) =
        LineChartState(2.dp, 5, scaleFactor).apply { update(items(*dates), maxDataPoints) }

    @Test
    fun update_exposesItemCountAndSequentialLabels() {
        val state = state(1, 2, 3)
        assertEquals(3, state.itemCount)
        assertEquals(listOf("1", "2", "3"), state.labels)
    }

    @Test
    fun update_keepsOnlyLatestMaxDataPointsAndTreatsNonPositiveAsUnlimited() {
        assertEquals(2, state(1, 2, 3, 4, 5, maxDataPoints = 2).itemCount)
        assertEquals(5, state(1, 2, 3, 4, 5, maxDataPoints = 0).itemCount)
        assertEquals(5, state(1, 2, 3, 4, 5, maxDataPoints = 99).itemCount)
        val limited = state(1, 2, 3, 4, 5, maxDataPoints = 2)
        assertEquals(listOf(4.0, 5.0), limited.items.map { it.cumulativeProfit })
    }

    @Test
    fun update_followsRightEdgeWhileAtLatest() {
        val state = state(1, 2, 3)
        state.update(items(1, 2, 3, 4), Int.MAX_VALUE)
        assertEquals(0f, state.scroll, 0f)
    }

    @Test
    fun update_keepsScrolledBackViewAnchoredOnAppend() {
        val state = state(1, 2, 3)
        state.scroll = 5f
        state.update(items(1, 2, 3, 4, 5), Int.MAX_VALUE)
        assertEquals(7f, state.scroll, 0f)
    }

    @Test
    fun update_inPlaceLastCandleChangeKeepsScroll() {
        val state = state(1, 2, 3)
        state.scroll = 5f
        state.update(listOf(ChartData(1.0, 1), ChartData(2.0, 2), ChartData(99.0, 3)), Int.MAX_VALUE)
        assertEquals(5f, state.scroll, 0f)
        assertEquals(99.0, state.items.last().cumulativeProfit, 0.0)
    }

    @Test
    fun update_crosshairFreezesFollowing() {
        val state = state(1, 2, 3)
        state.onDragStart(Offset(10f, 10f))
        state.update(items(1, 2, 3, 4), Int.MAX_VALUE)
        assertEquals(1f, state.scroll, 0f)
    }

    @Test
    fun update_crosshairReleaseResumesFollowingWhenItStartedAtLatest() {
        val state = state(1, 2, 3)
        state.onDragStart(Offset(10f, 10f))
        state.update(items(1, 2, 3, 4), Int.MAX_VALUE)
        state.update(items(1, 2, 3, 4, 5), Int.MAX_VALUE)
        assertEquals(2f, state.scroll, 0f)
        state.onDragEnd()
        assertEquals(0f, state.scroll, 0f)
    }

    @Test
    fun update_crosshairReleaseKeepsTheAnchorWhenItStartedScrolledBack() {
        val state = state(1, 2, 3)
        state.scroll = 5f
        state.onDragStart(Offset(10f, 10f))
        state.update(items(1, 2, 3, 4), Int.MAX_VALUE)
        state.onDragEnd()
        assertEquals(6f, state.scroll, 0f)
    }

    @Test
    fun update_clampsStoredScrollWhenEverythingFits() {
        val state = state(1, 2, 3)
        state.plot = Rect(0f, 0f, 1000f, 200f)
        state.density = 1f
        state.onDragStart(Offset(10f, 10f))
        state.update(items(1, 2, 3, 4), Int.MAX_VALUE)
        state.update(items(1, 2, 3, 4, 5), Int.MAX_VALUE)
        assertEquals(0f, state.scroll, 0f)
    }

    @Test
    fun transform_ignoresNonFiniteInput() {
        val state = state(1, 2, 3)
        state.plot = Rect(0f, 0f, 20f, 200f)
        state.density = 1f
        state.scroll = 1f
        state.transform(Float.NaN, 1f, 5f)
        state.transform(10f, Float.NaN, 5f)
        state.transform(10f, 1f, Float.POSITIVE_INFINITY)
        assertEquals(1f, state.scroll, 0f)
    }

    @Test
    fun update_resetsScrollButKeepsSlotWhenSeriesIsReplaced() {
        val state = state(1, 2, 3)
        state.scroll = 5f
        state.slot = 12f
        state.update(items(100, 200), Int.MAX_VALUE)
        assertEquals(0f, state.scroll, 0f)
        assertEquals(12f, state.slot, 0f)
    }

    @Test
    fun update_appliesScalarSettings() {
        val state = state(1, 2)
        state.update(items(1, 2), Int.MAX_VALUE, 4.dp, 7, 100)
        assertEquals(4.dp, state.strokeWidth)
        assertEquals(7, state.yAxisTicksCount)
        assertEquals("1,234.56", state.formatValue(123456L))
    }

    @Test
    fun yAxisTicks_areNiceTicksScaledAndDescending() {
        val state = LineChartState(2.dp, 5, 10_000).apply {
            update(listOf(ChartData(10.0, 1), ChartData(20.0, 2)), Int.MAX_VALUE)
            plot = Rect(0f, 0f, 300f, 200f)
            density = 1f
        }
        assertEquals(listOf(200_000L, 180_000L, 160_000L, 140_000L, 120_000L, 100_000L), state.yAxisTicks)
    }

    private fun selectable(vararg closes: Double) = LineChartState(2.dp, 5, 10_000).apply {
        update(closes.mapIndexed { i, c -> ChartData(c, i + 1L) }, Int.MAX_VALUE)
        plot = Rect(0f, 0f, 100f, 100f)
        density = 1f
    }

    @Test
    fun selectedBy_snapsToCandleCenterWithScaledCloseAndDirection() {
        val rising = selectable(1.0, 2.0, 3.0)
        val selection = rising.selectedBy(Offset(55f, 10f))
        assertEquals(1, selection.index)
        assertEquals(50f, selection.position.x, 0.01f)
        assertEquals(20_000L, selection.item.value)
        assertTrue(selection.item.isPositive)
        val falling = selectable(3.0, 2.0)
        assertFalse(falling.selectedBy(Offset(90f, 10f)).item.isPositive)
    }

    @Test
    fun selectedBy_isNoneWithoutPlotOrItems() {
        assertEquals(DRAG_SELECTION_NONE, LineChartState(2.dp, 5, 10_000).selectedBy(Offset(1f, 1f)))
        assertEquals(DRAG_SELECTION_NONE, state(1, 2).selectedBy(Offset(1f, 1f)))
    }

    @Test
    fun drag_setsCrosshairEmitsOnIndexChangeOnlyAndHidesOnEnd() {
        val state = selectable(1.0, 2.0, 3.0)
        state.onDragStart(Offset(55f, 10f))
        assertEquals(Offset(55f, 10f), state.crosshair)
        val first = state.selected.value
        state.onDrag(Offset(48f, 30f))
        assertEquals(first, state.selected.value)
        state.onDrag(Offset(90f, 30f))
        assertEquals(2, state.selected.value.index)
        state.onDragEnd()
        assertEquals(Offset.Unspecified, state.crosshair)
        assertEquals(DRAG_SELECTION_NONE, state.selected.value)
    }

    @Test
    fun isPositive_followsLastVersusFirstCloseUntilSelected() {
        assertTrue(selectable(1.0, 3.0).isPositive)
        assertFalse(selectable(3.0, 1.0).isPositive)
    }

    @Test
    fun legacyConstructor_mapsDataPointsToScaledSeries() {
        val constructor = LineChartState::class.java.getDeclaredConstructor(
            List::class.java,
            Boolean::class.java,
            List::class.java,
            Float::class.java,
            Int::class.java,
            DefaultConstructorMarker::class.java,
        )
        val legacy = constructor.newInstance(
            listOf(ChartDataPoint(10_000L, true), ChartDataPoint(25_000L, true)),
            true,
            listOf("a", "b"),
            2f,
            5,
            null,
        )
        assertEquals(2, legacy.itemCount)
        assertEquals(listOf(1.0, 2.5), legacy.items.map { it.cumulativeProfit })
        assertEquals("1", legacy.formatValue(10_000L))
    }

    @Test
    fun slotCount_addsThePadToTheItemCount() {
        val state = selectable(1.0, 2.0, 3.0)
        assertEquals(3, state.slotCount)
        state.pad = 4
        assertEquals(7, state.slotCount)
    }

    @Test
    fun selectedBy_withPadNeverExceedsTheLastItem() {
        val state = selectable(1.0, 2.0, 3.0)
        state.pad = 3
        assertEquals(2, state.selectedBy(Offset(99f, 10f)).index)
    }

    @Test
    fun transform_withPadAllowsScrollingToTheFirstPadSlot() {
        val state = selectable(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0)
        state.slot = 10f
        state.pad = 5
        state.transform(50f, 1f, 100_000f)
        assertEquals(15f, state.scroll, 0.0001f)
    }
}
