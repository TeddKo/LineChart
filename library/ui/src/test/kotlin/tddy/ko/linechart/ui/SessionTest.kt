package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class SessionTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun candle(date: Long, high: Double, low: Double, close: Double, volume: Double = 1.0) =
        ChartData(close, date, close, high, low, volume)

    private val twoDays = Series(
        listOf(
            candle(10 * hour, 110.0, 95.0, 100.0),
            candle(11 * hour, 105.0, 90.0, 95.0),
            candle(12 * hour, 108.0, 92.0, 100.0),
            candle(day + 10 * hour, 101.0, 99.0, 100.0),
            candle(day + 11 * hour, 102.0, 98.0, 101.0),
        ),
    )

    @Test
    fun pivotLevels_intradayCandlesTakeThePreviousLocalDay() {
        val levels = pivotLevels(twoDays, utc)
        assertTrue(levels.all { it.take(3).all(Double::isNaN) })
        assertEquals(listOf(100.0, 110.0, 90.0, 120.0, 80.0), levels.map { it[3] })
        assertEquals(listOf(100.0, 110.0, 90.0, 120.0, 80.0), levels.map { it[4] })
    }

    @Test
    fun pivotLevels_groupByTheLocalDayOfTheZone() {
        val series = Series(
            listOf(
                candle(14 * hour, 110.0, 90.0, 100.0),
                candle(16 * hour, 120.0, 100.0, 110.0),
                candle(18 * hour, 105.0, 95.0, 100.0),
            ),
        )
        assertTrue(pivotLevels(series, utc)[0].all { it.isNaN() })
        val seoulLevels = pivotLevels(series, seoul)
        assertTrue(seoulLevels[0][0].isNaN() && !seoulLevels[0][1].isNaN())
        assertEquals((110.0 + 90.0 + 100.0) / 3.0, seoulLevels[0][1], 1e-9)
    }

    @Test
    fun pivotLevels_dailySeriesUseThePreviousCandle() {
        val series = Series(
            listOf(
                candle(0L, 110.0, 90.0, 100.0),
                candle(day, 120.0, 100.0, 110.0),
                candle(2 * day, 130.0, 105.0, 120.0),
            ),
        )
        val levels = pivotLevels(series, utc)
        assertTrue(levels[0][0].isNaN())
        assertEquals(100.0, levels[0][1], 1e-9)
        assertEquals(110.0, levels[1][1], 1e-9)
        assertEquals((120.0 + 100.0 + 110.0) / 3.0, levels[0][2], 1e-9)
    }

    @Test
    fun pivot_isStepLinesOutsideThePriceRange() {
        val pivot = Pivot()
        assertFalse(pivot.inScale)
        val lines = pivot.marks(twoDays, ChartSettings()).lines()
        assertEquals(listOf("P", "R1", "S1", "R2", "S2"), lines.map { it.label })
        assertTrue(lines.all { it.step })
        assertEquals(listOf(0, 1, -1, 1, -1), lines.map { it.direction })
    }

    @Test
    fun vwap_restartsEachLocalDay() {
        val series = Series(
            listOf(
                candle(10 * hour, 10.0, 10.0, 10.0, 1.0),
                candle(11 * hour, 20.0, 20.0, 20.0, 3.0),
                candle(day + 10 * hour, 30.0, 30.0, 30.0, 2.0),
                candle(day + 11 * hour, 10.0, 10.0, 10.0, 2.0),
            ),
        )
        val out = vwap(series, utc)
        assertEquals(10.0, out[0], 1e-9)
        assertEquals(17.5, out[1], 1e-9)
        assertEquals(30.0, out[2], 1e-9)
        assertEquals(20.0, out[3], 1e-9)
    }

    @Test
    fun vwap_isCumulativeOnDailySeries() {
        val series = Series(
            listOf(
                candle(0L, 10.0, 10.0, 10.0, 1.0),
                candle(day, 20.0, 20.0, 20.0, 3.0),
                candle(2 * day, 30.0, 30.0, 30.0, 2.0),
            ),
        )
        val out = vwap(series, utc)
        assertEquals(17.5, out[1], 1e-9)
        assertEquals((10.0 + 60.0 + 60.0) / 6.0, out[2], 1e-9)
    }

    @Test
    fun vwap_usesTheTypicalPriceAndIsNaNWithoutVolume() {
        val series = Series(listOf(candle(0L, 12.0, 6.0, 9.0, 0.0), candle(day, 15.0, 9.0, 12.0, 2.0)))
        val out = vwap(series, utc)
        assertTrue(out[0].isNaN())
        assertEquals(12.0, out[1], 1e-9)
    }

    @Test
    fun marks_neverThrowOnShortSeries() {
        for (n in 0..2) {
            val series = Series(List(n) { candle(it * day, 2.0, 1.0, 1.5) })
            Pivot().marks(series, ChartSettings())
            assertTrue(Vwap().needsVolume)
            Vwap().marks(series, ChartSettings())
        }
    }
}
