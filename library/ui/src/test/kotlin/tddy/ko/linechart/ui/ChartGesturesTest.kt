package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGesturesTest {

    @Test
    fun classifyGesture_twoPointersZoom() {
        assertEquals(GestureKind.Zoom, classifyGesture(0f, 0f, 8f, 2))
    }

    @Test
    fun classifyGesture_horizontalBeyondSlopPans() {
        assertEquals(GestureKind.Pan, classifyGesture(20f, 5f, 8f, 1))
        assertEquals(GestureKind.Pan, classifyGesture(-20f, -20f, 8f, 1))
    }

    @Test
    fun classifyGesture_verticalBeyondSlopIsVertical() {
        assertEquals(GestureKind.Vertical, classifyGesture(2f, 30f, 8f, 1))
    }

    @Test
    fun classifyGesture_withinSlopIsUndecided() {
        assertEquals(GestureKind.Undecided, classifyGesture(3f, -4f, 8f, 1))
    }

    private fun state(n: Int) = LineChartState(2.dp, 5, 10_000).apply {
        update(List(n) { ChartData(it.toDouble(), it + 1L) }, Int.MAX_VALUE)
        plot = Rect(0f, 0f, 400f, 200f)
        density = 1f
    }

    @Test
    fun transform_panMovesScrollInSlotsAndClamps() {
        val state = state(100)
        state.transform(200f, 1f, 80f)
        assertEquals(10f, state.scroll, 0.001f)
        state.transform(200f, 1f, 1_000_000f)
        assertEquals(maxScroll(100, 400f, 8f), state.scroll, 0.001f)
        state.transform(200f, 1f, -1_000_000f)
        assertEquals(0f, state.scroll, 0f)
    }

    @Test
    fun transform_zoomKeepsItemUnderFocalPointAndClampsSlot() {
        val state = state(100)
        val before = indexAt(300f, 100, state.scroll, 400f, state.slotPx())
        state.transform(300f, 2f, 0f)
        assertEquals(16f, state.slot, 0.001f)
        assertEquals(before, indexAt(300f, 100, state.scroll, 400f, state.slotPx()))
        state.transform(300f, 100f, 0f)
        assertEquals(40f, state.slot, 0.001f)
        state.transform(300f, 0.0001f, 0f)
        assertTrue(state.slot >= 4f)
    }
}
