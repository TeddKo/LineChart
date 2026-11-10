package tddy.ko.linechart.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class BandsTest {

    private val ascending = seriesOf(doubles(1.0, 2.0, 3.0, 4.0, 5.0))

    @Test
    fun stdev_isPopulationDeviation() {
        val out = stdev(doubles(1.0, 2.0, 3.0, 4.0, 5.0), 3)
        assertEquals(2, out.leadingNaN())
        assertEquals(sqrt(2.0 / 3.0), out[2], 1e-9)
        assertEquals(sqrt(2.0 / 3.0), out[4], 1e-9)
    }

    @Test
    fun highestAndLowest_coverTheTrailingWindow() {
        val values = doubles(3.0, 1.0, 4.0, 1.0, 5.0)
        assertEquals(listOf(4.0, 4.0, 5.0), highest(values, 3).drop(2))
        assertEquals(listOf(1.0, 1.0, 1.0), lowest(values, 3).drop(2))
        assertEquals(2, highest(values, 3).leadingNaN())
    }

    @Test
    fun bollinger_bandsAreMeanPlusMinusKDeviations() {
        val lines = BollingerBands(3, 2.0).marks(ascending, ChartSettings()).lines()
        assertEquals(listOf("mid", "upper", "lower"), lines.map { it.label })
        val sd = sqrt(2.0 / 3.0)
        assertEquals(2.0, lines[0].values[2], 1e-9)
        assertEquals(2.0 + 2.0 * sd, lines[1].values[2], 1e-9)
        assertEquals(2.0 - 2.0 * sd, lines[2].values[2], 1e-9)
        assertEquals(4.0 + 2.0 * sd, lines[1].values[4], 1e-9)
    }

    @Test
    fun bollinger_addsAFillBetweenTheBandsInTheBandColor() {
        val fill = BollingerBands(3).marks(ascending, ChartSettings()).filterIsInstance<FillMark>().single()
        assertEquals(0.08f, fill.color!!.alpha, 1e-2f)
        assertEquals(BollingerColors[1].red, fill.color!!.red, 1e-2f)
    }

    @Test
    fun envelope_isPercentAroundTheAverage() {
        val lines = Envelope(3, 10.0).marks(ascending, ChartSettings()).lines()
        assertEquals(2.2, lines[1].values[2], 1e-9)
        assertEquals(1.8, lines[2].values[2], 1e-9)
        assertEquals(4.4, lines[1].values[4], 1e-9)
    }

    @Test
    fun priceChannel_excludesTheCurrentCandle() {
        val series = seriesOf(doubles(9.0, 11.0, 10.0, 8.0), high = doubles(10.0, 12.0, 11.0, 9.0), low = doubles(8.0, 9.0, 7.0, 6.0))
        val lines = PriceChannel(2).marks(series, ChartSettings()).lines()
        assertEquals(2, lines[0].values.leadingNaN())
        assertEquals(12.0, lines[0].values[2], 1e-9)
        assertEquals(8.0, lines[1].values[2], 1e-9)
        assertEquals(12.0, lines[0].values[3], 1e-9)
        assertEquals(7.0, lines[1].values[3], 1e-9)
    }

    @Test
    fun warmUp_leavesPeriodMinusOneNaNs() {
        val series = seriesOf(DoubleArray(60) { 100.0 + it % 7 })
        val bb = BollingerBands().marks(series, ChartSettings()).lines()
        val env = Envelope().marks(series, ChartSettings()).lines()
        assertTrue(bb.all { it.values.leadingNaN() == 19 })
        assertTrue(env.all { it.values.leadingNaN() == 19 })
        assertTrue(PriceChannel().marks(series, ChartSettings()).lines().all { it.values.leadingNaN() == 20 })
    }

    @Test
    fun colors_overrideTheDefaults() {
        val lines = BollingerBands(colors = listOf(Color.Red, Color.Blue)).marks(ascending, ChartSettings()).lines()
        assertEquals(Color.Red, lines[0].color)
        assertEquals(Color.Blue, lines[1].color)
    }
}
