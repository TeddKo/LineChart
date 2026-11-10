package tddy.ko.linechart.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AveragesTest {

    private val closes = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)

    private fun series(vararg close: Double) = Series(close.mapIndexed { i, c -> ChartData(c, i + 1L, volume = 10.0 * (i + 1)) })

    @Test
    fun sma_isNaNDuringWarmUp() {
        val out = sma(closes, 3)
        assertTrue(out[0].isNaN() && out[1].isNaN())
        assertEquals(listOf(2.0, 3.0, 4.0), out.drop(2))
    }

    @Test
    fun ema_isSeededBySmaThenSmoothed() {
        val out = ema(closes, 3)
        assertTrue(out[1].isNaN())
        assertEquals(listOf(2.0, 3.0, 4.0), out.drop(2))
    }

    @Test
    fun wma_weightsNewestHighest() {
        val out = wma(closes, 3)
        assertEquals(14.0 / 6.0, out[2], 1e-9)
        assertEquals(20.0 / 6.0, out[3], 1e-9)
        assertEquals(26.0 / 6.0, out[4], 1e-9)
    }

    @Test
    fun averages_shorterThanPeriodNeverThrow() {
        assertTrue(sma(doubleArrayOf(1.0), 5).all { it.isNaN() })
        assertTrue(ema(DoubleArray(0), 5).isEmpty())
        assertTrue(wma(doubleArrayOf(1.0, 2.0), 5).all { it.isNaN() })
    }

    @Test
    fun movingAverage_buildsOneLineMarkPerVisiblePeriodWithColorsAndWidths() {
        val ma = MovingAverage(
            periods = listOf(2, 3, 4),
            colors = listOf(Color.Red, Color.Green, Color.Blue),
            widths = listOf(1.dp, 2.dp),
            visible = listOf(true, false, true),
        )
        val marks = ma.marks(series(1.0, 2.0, 3.0, 4.0, 5.0), ChartSettings()).map { it as LineMark }
        assertEquals(2, marks.size)
        assertEquals(Color.Red, marks[0].color)
        assertEquals(Color.Blue, marks[1].color)
        assertEquals(1.dp, marks[0].width)
        assertEquals(1.dp, marks[1].width)
        assertEquals(1.5, marks[0].values[1], 1e-9)
        assertTrue(marks[1].values[2].isNaN())
        assertEquals(2.5, marks[1].values[3], 1e-9)
    }

    @Test
    fun movingAverage_typeSelectsTheFormula() {
        val s = series(1.0, 2.0, 3.0, 4.0, 5.0)
        val wma = MovingAverage(listOf(3), MaType.Weighted).marks(s, ChartSettings()).single() as LineMark
        assertEquals(14.0 / 6.0, wma.values[2], 1e-9)
    }

    @Test
    fun movingAverage_colorsBeyondThePaletteFallBackToUnspecified() {
        val marks = MovingAverage(listOf(1, 2, 3, 4, 5)).marks(series(1.0, 2.0), ChartSettings()).map { it as LineMark }
        assertEquals(Color(0xFF2EB82E), marks[0].color)
        assertEquals(Color.Unspecified, marks[4].color)
    }

    @Test
    fun indicators_areEqualByTypeAndParams() {
        assertEquals(MovingAverage(), MovingAverage())
        assertEquals(MovingAverage().hashCode(), MovingAverage().hashCode())
        assertNotEquals(MovingAverage(), MovingAverage(listOf(5)))
        assertNotEquals(MovingAverage(), MovingAverage(type = MaType.Exponential))
        assertEquals(Volume(), Volume())
        assertNotEquals(Volume() as Indicator, MovingAverage())
    }

    @Test
    fun volume_marksBarsAndMovingAveragesOfVolume() {
        val marks = Volume(maPeriods = listOf(2)).marks(series(1.0, 2.0, 3.0), ChartSettings())
        assertEquals(2, marks.size)
        val bar = marks[0] as BarMark
        assertEquals(Tone.Direction, bar.tone)
        assertEquals(listOf(10.0, 20.0, 30.0), bar.values.toList())
        assertEquals(15.0, (marks[1] as LineMark).values[1], 1e-9)
    }

    @Test
    fun volume_hidesMovingAveragesWhenTheSettingIsOff() {
        val marks = Volume().marks(series(1.0, 2.0, 3.0), ChartSettings(volumeMa = false))
        assertEquals(1, marks.size)
    }

    @Test
    fun volume_needsVolumeAndScalesFromZero() {
        assertTrue(Volume().needsVolume)
        assertEquals(PaneScale.FromZero, Volume().scale)
    }
}
