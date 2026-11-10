package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChartDataTest {

    @Test
    fun lineValueDefaultsCollapseOhlcToClose() {
        val data = ChartData(10.0, 5L)
        assertEquals(10.0, data.open, 0.0)
        assertEquals(10.0, data.high, 0.0)
        assertEquals(10.0, data.low, 0.0)
        assertEquals(0.0, data.volume, 0.0)
        assertEquals(0.0, data.value, 0.0)
    }

    @Test
    fun highAndLowDefaultToOpenCloseEnvelope() {
        val rising = ChartData(10.0, 5L, open = 8.0)
        assertEquals(10.0, rising.high, 0.0)
        assertEquals(8.0, rising.low, 0.0)
        val falling = ChartData(7.0, 5L, open = 9.0)
        assertEquals(9.0, falling.high, 0.0)
        assertEquals(7.0, falling.low, 0.0)
    }

    @Test
    fun explicitOhlcvIsKept() {
        val data = ChartData(10.0, 5L, open = 8.0, high = 12.0, low = 7.0, volume = 300.0)
        assertEquals(12.0, data.high, 0.0)
        assertEquals(7.0, data.low, 0.0)
        assertEquals(300.0, data.volume, 0.0)
    }

    @Test
    fun valueFollowsVolumeAndSurvivesCopy() {
        val data = ChartData(10.0, 5L, volume = 3.0, value = 1_500.0)
        assertEquals(1_500.0, data.value, 0.0)
        assertEquals(1_500.0, data.copy(cumulativeProfit = 11.0).value, 0.0)
        assertEquals(1_500.0, data.component7(), 0.0)
        assertNotEquals(data, data.copy(value = 2.0))
    }

    @Test
    fun copyKeepsUntouchedFields() {
        val data = ChartData(10.0, 5L, open = 8.0, high = 12.0, low = 7.0, volume = 300.0)
        val copied = data.copy(cumulativeProfit = 11.0)
        assertEquals(11.0, copied.cumulativeProfit, 0.0)
        assertEquals(8.0, copied.open, 0.0)
        assertEquals(300.0, copied.volume, 0.0)
        assertNotEquals(data, copied)
        assertEquals(data, data.copy())
    }

    @Test
    fun componentsExposeLegacyPairFirst() {
        val (profit, date, open) = ChartData(10.0, 5L, open = 8.0)
        assertEquals(10.0, profit, 0.0)
        assertEquals(5L, date)
        assertEquals(8.0, open, 0.0)
    }
}
