package tddy.ko.linechart.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorStateTest {

    private fun items(n: Int, volume: Double = 10.0) =
        List(n) { ChartData(100.0 + it, it + 1L, volume = volume) }

    private fun state(n: Int = 30, volume: Double = 10.0) =
        LineChartState(2.dp, 5, 10_000).apply { update(items(n, volume), Int.MAX_VALUE) }

    private class Extending(private val pad: Int, private val scaled: Boolean = true) : Overlay() {
        override val params: List<Any> get() = listOf(pad, scaled)
        override val rightPad: Int get() = pad
        override val inScale: Boolean get() = scaled
        override fun marks(series: Series, settings: ChartSettings): List<Mark> = emptyList()
        override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int) = AnnotatedString("")
    }

    @Test
    fun setIndicators_computesMarksPerOverlayAndPane() {
        val state = state()
        state.setIndicators(listOf(MovingAverage(listOf(5, 20))), listOf(Volume(listOf(5))), ChartSettings())
        val computed = state.computed
        assertEquals(1, computed.overlays.size)
        assertEquals(2, computed.overlayMarks[0].size)
        assertEquals(1, computed.panes.size)
        assertEquals(2, computed.paneMarks[0].size)
        assertSame(computed.series, state.series)
    }

    @Test
    fun setIndicators_dropsVolumeIndicatorsWhenTheSeriesHasNoVolume() {
        val state = state(volume = 0.0)
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        assertTrue(state.computed.panes.isEmpty())
        assertTrue(state.computed.paneMarks.isEmpty())
        assertEquals(1, state.computed.overlays.size)
    }

    @Test
    fun setIndicators_equalInputsKeepTheComputedInstance() {
        val state = state()
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        val first = state.computed
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        assertSame(first, state.computed)
    }

    @Test
    fun setIndicators_reusesMarksOfUnchangedIndicators() {
        val state = state()
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        val before = state.computed
        state.setIndicators(listOf(MovingAverage(), MovingAverage(listOf(10))), listOf(Volume()), ChartSettings())
        assertNotSame(before, state.computed)
        assertSame(before.overlayMarks[0], state.computed.overlayMarks[0])
        assertSame(before.paneMarks[0], state.computed.paneMarks[0])
        assertEquals(2, state.computed.overlays.size)
    }

    @Test
    fun setIndicators_volumeMaToggleRecomputesVolumeMarks() {
        val state = state()
        state.setIndicators(emptyList(), listOf(Volume()), ChartSettings())
        assertEquals(3, state.computed.paneMarks[0].size)
        state.setIndicators(emptyList(), listOf(Volume()), ChartSettings(volumeMa = false))
        assertEquals(1, state.computed.paneMarks[0].size)
    }

    @Test
    fun update_recomputesAllMarksForTheNewSeries() {
        val state = state(30)
        state.setIndicators(listOf(MovingAverage(listOf(5))), emptyList(), ChartSettings())
        val before = state.computed
        state.update(items(31), Int.MAX_VALUE)
        assertNotSame(before.overlayMarks[0], state.computed.overlayMarks[0])
        assertSame(state.series, state.computed.series)
        assertEquals(31, (state.computed.overlayMarks[0][0] as LineMark).values.size)
    }

    @Test
    fun setIndicators_padIsTheLargerOfMarginAndOverlayExtension() {
        val state = state()
        state.setIndicators(emptyList(), emptyList(), ChartSettings(rightMargin = 3))
        assertEquals(3, state.pad)
        state.setIndicators(listOf(Extending(25)), emptyList(), ChartSettings(rightMargin = 3))
        assertEquals(25, state.pad)
        state.setIndicators(listOf(Extending(2)), emptyList(), ChartSettings(rightMargin = 0))
        assertEquals(2, state.pad)
    }

    @Test
    fun setIndicators_repeatedWithEqualInputsKeepsTheComputedInstance() {
        val state = state()
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        val first = state.computed
        state.setIndicators(listOf(MovingAverage()), listOf(Volume()), ChartSettings())
        assertSame(first, state.computed)
    }

    @Test
    fun scaleMarks_skipOverlaysOutsideTheScale() {
        val state = state()
        state.setIndicators(listOf(BollingerBands(), Extending(0, scaled = false)), emptyList(), ChartSettings())
        assertEquals(state.computed.overlayMarks[0], state.computed.scaleMarks)
    }

    @Test
    fun scaleMarks_excludeMovingAverageButKeepBands() {
        val state = state()
        state.setIndicators(listOf(MovingAverage(listOf(5, 20)), BollingerBands()), emptyList(), ChartSettings())
        val computed = state.computed
        assertEquals(computed.overlayMarks[1], computed.scaleMarks)
    }

    @Test
    fun legacyStates_keepZeroPadWithoutIndicators() {
        assertEquals(0, state().pad)
    }

    @Test
    fun paneFormat_usesCompactForVolumeAndTwoDecimalsOtherwise() {
        assertEquals("1.50K", Volume().format(1500.0))
        assertEquals("70.46", object : PaneIndicator(1f) {
            override val params: List<Any> get() = emptyList()
            override val scale: PaneScale get() = PaneScale.Auto
            override fun marks(series: Series, settings: ChartSettings): List<Mark> = emptyList()
            override fun legend(spec: ChartSpec, series: Series, marks: List<Mark>, i: Int) = AnnotatedString("")
        }.format(70.4637))
    }
}
