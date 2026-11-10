package tddy.ko.linechart.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneLayoutTest {

    private fun layout(vararg weights: Float, height: Float = 300f, axisLeft: Boolean = false) =
        paneLayout(400f, height, 30f, 20f, 60f, 4f, 80f, weights.toList(), axisLeft)

    @Test
    fun withoutPanes_mainTakesTheWholeBody() {
        val layout = layout()
        assertEquals(30f, layout.main.top, 0f)
        assertEquals(280f, layout.main.bottom, 0f)
        assertEquals(340f, layout.main.right, 0f)
        assertTrue(layout.panes.isEmpty())
        assertEquals(280f, layout.timeAxis.top, 0f)
        assertEquals(300f, layout.timeAxis.bottom, 0f)
        assertEquals(340f, layout.axis.left, 0f)
        assertEquals(400f, layout.axis.right, 0f)
    }

    @Test
    fun eachPaneTakesItsWeightTimesPaneHeightAboveTheTimeAxis() {
        val layout = layout(1f, 2f, height = 300f + 80f + 4f + 160f + 4f)
        assertEquals(2, layout.panes.size)
        assertEquals(80f, layout.panes[0].height, 0.01f)
        assertEquals(160f, layout.panes[1].height, 0.01f)
        assertEquals(layout.main.bottom + 4f, layout.panes[0].top, 0.01f)
        assertEquals(layout.panes[0].bottom + 4f, layout.panes[1].top, 0.01f)
        assertEquals(layout.timeAxis.top, layout.panes[1].bottom, 0.01f)
    }

    @Test
    fun panesAddHeightSoTheMainAreaStaysChartHeightMinusChrome() {
        val total = 300f + totalPaneHeight(80f, listOf(1f, 1f), 4f)
        assertEquals(168f, totalPaneHeight(80f, listOf(1f, 1f), 4f), 0f)
        val layout = layout(1f, 1f, height = total)
        assertEquals(250f, layout.main.height, 0.01f)
    }

    @Test
    fun tinyHeightNeverGivesTheMainAreaANegativeHeight() {
        val layout = paneLayout(10f, 10f, 30f, 20f, 60f, 4f, 80f, listOf(1f), false)
        assertEquals(0f, layout.main.height, 0f)
        assertEquals(0f, layout.main.width, 0f)
    }

    @Test
    fun leftAxisSitsLeftOfThePlotInPlotCoordinates() {
        val layout = layout(1f, axisLeft = true)
        assertEquals(-60f, layout.axis.left, 0f)
        assertEquals(0f, layout.axis.right, 0f)
        assertEquals(340f, layout.main.width, 0f)
    }

    @Test
    fun axisWidth_addsEightDpAndRoundsUpToFourDp() {
        assertEquals(48f, axisWidth(labelWidth = 37f, density = 1f), 0f)
        assertEquals(44f, axisWidth(labelWidth = 36f, density = 1f), 0f)
        assertEquals(96f, axisWidth(labelWidth = 74f, density = 2f), 0f)
    }

    @Test
    fun paneBody_startsBelowTheLegendStripAndKeepsTheOtherEdges() {
        val body = paneBody(Rect(0f, 100f, 340f, 180f), 12f, 2f)
        assertEquals(114f, body.top, 0f)
        assertEquals(0f, body.left, 0f)
        assertEquals(340f, body.right, 0f)
        assertEquals(180f, body.bottom, 0f)
    }

    @Test
    fun paneBody_withoutLegendIsTheWholePane() {
        val pane = Rect(0f, 100f, 340f, 180f)
        assertEquals(pane, paneBody(pane, 0f, 0f))
    }

    @Test
    fun paneBody_neverInvertsInAPaneShorterThanTheStrip() {
        val body = paneBody(Rect(0f, 100f, 340f, 108f), 12f, 2f)
        assertEquals(108f, body.top, 0f)
        assertEquals(0f, body.height, 0f)
    }

    @Test
    fun deltas_resizePanesAndLetTheMainAreaAbsorbThem() {
        val base = paneLayout(400f, 400f, 30f, 20f, 60f, 4f, 80f, listOf(1f, 1f), false)
        val resized = paneLayout(400f, 400f, 30f, 20f, 60f, 4f, 80f, listOf(1f, 1f), false, listOf(20f, -10f))
        assertEquals(100f, resized.panes[0].height, 0.01f)
        assertEquals(70f, resized.panes[1].height, 0.01f)
        assertEquals(base.main.height - 10f, resized.main.height, 0.01f)
        assertEquals(base.timeAxis.top, resized.panes[1].bottom, 0.01f)
    }

    @Test
    fun totalPaneHeight_includesTheDeltas() {
        assertEquals(168f + 5f, totalPaneHeight(80f, listOf(1f, 1f), 4f, listOf(10f, -5f)), 0f)
    }

    @Test
    fun separatorAt_findsTheBoundaryWithinTheZone() {
        val layout = paneLayout(400f, 400f, 30f, 20f, 60f, 4f, 80f, listOf(1f, 1f), false)
        val first = (layout.main.bottom + layout.panes[0].top) / 2f
        val second = (layout.panes[0].bottom + layout.panes[1].top) / 2f
        assertEquals(0, separatorAt(layout, first + 11f, 12f))
        assertEquals(1, separatorAt(layout, second - 12f, 12f))
        assertEquals(-1, separatorAt(layout, first + 40f, 12f))
        assertEquals(-1, separatorAt(layout, layout.main.top + 5f, 12f))
    }

    @Test
    fun separatorAt_withoutPanesIsNone() {
        assertEquals(-1, separatorAt(layout(), 280f, 12f))
    }

    @Test
    fun resizeBoundary_growsTheUpperAreaAndShrinksTheLowerByTheSameAmount() {
        val next = resizeBoundary(listOf(200f, 80f, 80f), 0, 30f, listOf(120f, 40f, 40f))
        assertEquals(listOf(230f, 50f, 80f), next)
        assertEquals(360f, next.sum(), 0f)
    }

    @Test
    fun resizeBoundary_upwardDragShrinksTheUpperArea() {
        assertEquals(listOf(200f, 60f, 100f), resizeBoundary(listOf(200f, 80f, 80f), 1, -20f, listOf(120f, 40f, 40f)))
    }

    @Test
    fun resizeBoundary_clampsAtTheMinimumHeights() {
        val mins = listOf(120f, 40f, 40f)
        assertEquals(listOf(240f, 40f, 80f), resizeBoundary(listOf(200f, 80f, 80f), 0, 500f, mins))
        assertEquals(listOf(120f, 160f, 80f), resizeBoundary(listOf(200f, 80f, 80f), 0, -500f, mins))
        assertEquals(listOf(200f, 40f, 120f), resizeBoundary(listOf(200f, 80f, 80f), 1, -500f, mins))
    }

    @Test
    fun resizeBoundary_aBelowMinimumAreaIsNeverShrunkFurther() {
        assertEquals(listOf(100f, 100f), resizeBoundary(listOf(100f, 100f), 0, -30f, listOf(120f, 40f)))
    }

    @Test
    fun resizeBoundary_outOfRangeIndexChangesNothing() {
        val heights = listOf(200f, 80f)
        assertEquals(heights, resizeBoundary(heights, 1, 10f, listOf(120f, 40f)))
        assertEquals(heights, resizeBoundary(heights, -1, 10f, listOf(120f, 40f)))
    }
}
