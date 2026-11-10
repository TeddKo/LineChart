package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeFamilyTest {

    @Test
    fun tradingValue_usesTheItemValueWhenPositiveElseCloseTimesVolume() {
        val series = Series(
            listOf(
                ChartData(10.0, 1L, volume = 5.0),
                ChartData(11.0, 2L, volume = 5.0, value = 1000.0),
                ChartData(12.0, 3L, volume = 2.0, value = 0.0),
            ),
        )
        val marks = TradingValue(listOf(2)).marks(series, ChartSettings())
        val bars = marks[0] as BarMark
        assertEquals(listOf(50.0, 1000.0, 24.0), bars.values.toList())
        assertEquals(Tone.Direction, bars.tone)
        assertEquals(525.0, (marks[1] as LineMark).values[1], 1e-9)
    }

    @Test
    fun tradingValue_hidesMovingAveragesWhenTheSettingIsOff() {
        assertEquals(1, TradingValue().marks(seriesOf(doubles(1.0, 2.0)), ChartSettings(volumeMa = false)).size)
    }

    @Test
    fun obv_addsVolumeOnRisesAndSubtractsOnFalls() {
        val series = seriesOf(doubles(10.0, 11.0, 10.0, 10.0, 12.0), volume = doubles(5.0, 3.0, 2.0, 4.0, 6.0))
        val lines = Obv(2).marks(series, ChartSettings()).lines()
        assertEquals(listOf(0.0, 3.0, 1.0, 1.0, 7.0), lines[0].values.toList())
        assertEquals(1, lines[1].values.leadingNaN())
        assertEquals(2.0, lines[1].values[2], 1e-9)
        assertEquals(4.0, lines[1].values[4], 1e-9)
    }

    @Test
    fun volumeRatio_weighsFlatVolumeHalfOnEachSide() {
        val series = seriesOf(doubles(10.0, 11.0, 10.0, 10.0, 10.0), volume = doubles(1.0, 4.0, 5.0, 0.0, 6.0))
        val values = VolumeRatio(3).marks(series, ChartSettings()).lines()[0].values
        assertEquals(3, values.leadingNaN())
        assertEquals(80.0, values[3], 1e-9)
    }

    @Test
    fun volumeRatio_secondWindowCountsFlatCandles() {
        val series = seriesOf(doubles(10.0, 11.0, 10.0, 10.0, 10.0), volume = doubles(1.0, 4.0, 5.0, 2.0, 6.0))
        assertEquals(100.0 * 4.0 / 9.0, VolumeRatio(3).marks(series, ChartSettings()).lines()[0].values[4], 1e-9)
    }

    @Test
    fun volumeOscillator_isPercentDifferenceOfEmas() {
        val series = seriesOf(DoubleArray(5) { 1.0 }, volume = doubles(1.0, 2.0, 3.0, 4.0, 5.0))
        val values = VolumeOscillator(2, 3).marks(series, ChartSettings()).lines()[0].values
        assertEquals(2, values.leadingNaN())
        assertEquals(25.0, values[2], 1e-9)
    }

    @Test
    fun volumeIndicators_needVolumeAndKeepTheirLevels() {
        assertTrue(listOf(TradingValue(), Obv(), VolumeRatio(), VolumeOscillator()).all { it.needsVolume })
        assertTrue(VolumeRatio().levels.contentEquals(doubleArrayOf(70.0, 450.0)))
    }

    @Test
    fun warmUp_countsLeadingNaNs() {
        val series = seriesOf(DoubleArray(60) { 100.0 + (it * 7) % 11 }, volume = DoubleArray(60) { 10.0 + it % 5 })
        assertEquals(0, Obv().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(8, Obv().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
        assertEquals(20, VolumeRatio().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(19, VolumeOscillator().marks(series, ChartSettings()).lines()[0].values.leadingNaN())
        assertEquals(19, TradingValue().marks(series, ChartSettings()).lines()[1].values.leadingNaN())
    }

    @Test
    fun shorterThanThePeriodsNeverThrows() {
        for (n in 0..3) {
            val series = seriesOf(DoubleArray(n) { 1.0 + it })
            for (pane in listOf(TradingValue(), Obv(), VolumeRatio(), VolumeOscillator())) pane.marks(series, ChartSettings())
        }
    }
}
