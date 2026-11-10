package tddy.ko.linechart.ui

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
}
