package tddy.ko.linechart.ui

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketHistoryTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val day = 86_400_000L
    private val base = 1_700_000_000_000L - 1_700_000_000_000L % day

    private fun trade(price: Long, quantity: Int, time: Long) = MarketTrade("260", price, quantity, time)

    @Test
    fun emptyTradesGiveEmptySeries() {
        assertTrue(emptyList<MarketTrade>().toDailyChartData(utc).isEmpty())
    }

    @Test
    fun unsortedTradesFormOneOhlcCandlePerDay() {
        val trades = listOf(
            trade(120, 1, base + 3 * 3_600_000L),
            trade(100, 1, base + 1 * 3_600_000L),
            trade(150, 1, base + 5 * 3_600_000L),
            trade(90, 1, base + 7 * 3_600_000L),
        )
        val candle = trades.toDailyChartData(utc).single()
        assertEquals(base, candle.date)
        assertEquals(100.0, candle.open, 0.0)
        assertEquals(150.0, candle.high, 0.0)
        assertEquals(90.0, candle.low, 0.0)
        assertEquals(90.0, candle.cumulativeProfit, 0.0)
    }

    @Test
    fun volumeSumsQuantityAndValueSumsPriceTimesQuantity() {
        val trades = listOf(trade(100, 2, base + 1_000), trade(150, 3, base + 2_000))
        val candle = trades.toDailyChartData(utc).single()
        assertEquals(5.0, candle.volume, 0.0)
        assertEquals(650.0, candle.value, 0.0)
    }

    @Test
    fun dayBoundaryFollowsTheTimeZone() {
        val trades = listOf(trade(100, 1, base + 14 * 3_600_000L), trade(110, 1, base + 16 * 3_600_000L))
        assertEquals(1, trades.toDailyChartData(utc).size)
        assertEquals(2, trades.toDailyChartData(seoul).size)
    }

    @Test
    fun separateDaysStayInTimeOrder() {
        val trades = listOf(trade(110, 1, base + day + 1), trade(100, 1, base + 1))
        assertEquals(listOf(base, base + day), trades.toDailyChartData(utc).map { it.date })
    }
}
