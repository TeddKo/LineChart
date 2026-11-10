package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaneLayoutTest {

    @Test
    fun paneLayout_withoutVolumeGivesMainTheWholeBody() {
        val layout = paneLayout(width = 400f, height = 300f, headerH = 30f, timeH = 20f, axisW = 60f, gap = 4f, hasVolume = false)
        assertEquals(30f, layout.main.top, 0f)
        assertEquals(280f, layout.main.bottom, 0f)
        assertEquals(340f, layout.main.right, 0f)
        assertNull(layout.volume)
        assertEquals(280f, layout.timeAxis.top, 0f)
        assertEquals(300f, layout.timeAxis.bottom, 0f)
        assertEquals(340f, layout.axis.left, 0f)
        assertEquals(400f, layout.axis.right, 0f)
    }

    @Test
    fun paneLayout_withVolumeSplitsBodyIntoMainGapAndTwentyPercentVolume() {
        val layout = paneLayout(width = 400f, height = 300f, headerH = 30f, timeH = 20f, axisW = 60f, gap = 4f, hasVolume = true)
        val volume = checkNotNull(layout.volume)
        assertEquals(49.2f, volume.height, 0.01f)
        assertEquals(layout.main.bottom + 4f, volume.top, 0.01f)
        assertEquals(280f, volume.bottom, 0.01f)
        assertEquals(196.8f, layout.main.height, 0.01f)
    }

    @Test
    fun paneLayout_tinyHeightNeverGoesNegative() {
        val layout = paneLayout(width = 10f, height = 10f, headerH = 30f, timeH = 20f, axisW = 60f, gap = 4f, hasVolume = true)
        assertEquals(0f, layout.main.height.coerceAtLeast(0f), 0f)
        assertEquals(0f, layout.main.width, 0f)
    }

    @Test
    fun axisWidth_addsEightDpAndRoundsUpToFourDp() {
        assertEquals(48f, axisWidth(labelWidth = 37f, density = 1f), 0f)
        assertEquals(44f, axisWidth(labelWidth = 36f, density = 1f), 0f)
        assertEquals(96f, axisWidth(labelWidth = 74f, density = 2f), 0f)
    }
}
