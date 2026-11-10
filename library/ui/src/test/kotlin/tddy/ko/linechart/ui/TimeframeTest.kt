package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class TimeframeTest {

    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val utc = TimeZone.getTimeZone("UTC")
    private val kolkata = TimeZone.getTimeZone("Asia/Kolkata")

    private fun ts(zone: TimeZone, y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0): Long =
        GregorianCalendar(zone).apply {
            clear()
            set(y, mo - 1, d, h, mi, s)
        }.timeInMillis

    private fun tick(date: Long, close: Double, open: Double = close, high: Double = maxOf(open, close), low: Double = minOf(open, close), volume: Double = 0.0) =
        ChartData(close, date, open, high, low, volume)

    @Test
    fun timeframe_equalsByValueAndRejectsNonPositiveCount() {
        assertEquals(Timeframe(TimeframeUnit.Minute, 5), Timeframe.Minute5)
        assertEquals(Timeframe(TimeframeUnit.Minute, 5).hashCode(), Timeframe.Minute5.hashCode())
        assertNotEquals(Timeframe.Minute5, Timeframe.Second5)
        assertEquals(Timeframe(TimeframeUnit.Tick, 1), Timeframe.Tick)
        assertThrows(IllegalArgumentException::class.java) { Timeframe(TimeframeUnit.Minute, 0) }
    }

    @Test
    fun emptyAndSingleItem() {
        assertEquals(emptyList<ChartData>(), emptyList<ChartData>().aggregate(Timeframe.Minute1, seoul))
        val t = ts(seoul, 2026, 11, 10, 9, 0, 40)
        val one = listOf(tick(t, 10.0, open = 9.0, high = 11.0, low = 8.0, volume = 5.0))
        val result = one.aggregate(Timeframe.Minute1, seoul)
        assertEquals(listOf(ChartData(10.0, ts(seoul, 2026, 11, 10, 9, 0, 0), 9.0, 11.0, 8.0, 5.0)), result)
    }

    @Test
    fun minute5_mergesOhlcvIntoBucketStart() {
        val items = listOf(
            tick(ts(seoul, 2026, 11, 10, 9, 0, 10), 10.0, open = 10.0, high = 10.5, low = 9.8, volume = 1.0),
            tick(ts(seoul, 2026, 11, 10, 9, 2, 0), 12.0, open = 10.0, high = 12.5, low = 9.9, volume = 2.0),
            tick(ts(seoul, 2026, 11, 10, 9, 4, 59), 11.0, open = 12.0, high = 12.0, low = 10.5, volume = 3.0),
            tick(ts(seoul, 2026, 11, 10, 9, 5, 0), 13.0, open = 11.0, high = 13.0, low = 11.0, volume = 4.0),
        )
        val result = items.aggregate(Timeframe.Minute5, seoul)
        assertEquals(
            listOf(
                ChartData(11.0, ts(seoul, 2026, 11, 10, 9, 0, 0), 10.0, 12.5, 9.8, 6.0),
                ChartData(13.0, ts(seoul, 2026, 11, 10, 9, 5, 0), 11.0, 13.0, 11.0, 4.0),
            ),
            result,
        )
    }

    @Test
    fun second10_alignsToLocalSeconds() {
        val items = listOf(
            tick(ts(utc, 2026, 11, 10, 0, 0, 9), 1.0),
            tick(ts(utc, 2026, 11, 10, 0, 0, 10), 2.0),
            tick(ts(utc, 2026, 11, 10, 0, 0, 19), 3.0),
        )
        val result = items.aggregate(Timeframe.Second10, utc)
        assertEquals(listOf(ts(utc, 2026, 11, 10, 0, 0, 0), ts(utc, 2026, 11, 10, 0, 0, 10)), result.map { it.date })
        assertEquals(listOf(1.0, 3.0), result.map { it.cumulativeProfit })
    }

    @Test
    fun minute60_alignsToLocalHourInHalfHourZone() {
        val items = listOf(
            tick(ts(kolkata, 2026, 11, 10, 9, 59, 0), 1.0),
            tick(ts(kolkata, 2026, 11, 10, 10, 0, 0), 2.0),
        )
        val result = items.aggregate(Timeframe.Minute60, kolkata)
        assertEquals(listOf(ts(kolkata, 2026, 11, 10, 9, 0, 0), ts(kolkata, 2026, 11, 10, 10, 0, 0)), result.map { it.date })
    }

    @Test
    fun day_followsTimeZoneCalendarDate() {
        val items = listOf(
            tick(ts(utc, 2026, 11, 10, 23, 30, 0), 1.0),
            tick(ts(utc, 2026, 11, 11, 0, 30, 0), 2.0),
        )
        assertEquals(listOf(ts(utc, 2026, 11, 10), ts(utc, 2026, 11, 11)), items.aggregate(Timeframe.Day, utc).map { it.date })
        val inSeoul = items.aggregate(Timeframe.Day, seoul)
        assertEquals(listOf(ts(seoul, 2026, 11, 11)), inSeoul.map { it.date })
        assertEquals(2.0, inSeoul.single().cumulativeProfit, 0.0)
        assertEquals(1.0, inSeoul.single().open, 0.0)
    }

    @Test
    fun week_startsOnMonday() {
        val items = listOf(
            tick(ts(seoul, 2026, 11, 8, 12), 1.0),
            tick(ts(seoul, 2026, 11, 9, 0), 2.0),
            tick(ts(seoul, 2026, 11, 15, 23, 59, 59), 3.0),
            tick(ts(seoul, 2026, 11, 16, 0), 4.0),
        )
        val result = items.aggregate(Timeframe.Week, seoul)
        assertEquals(
            listOf(ts(seoul, 2026, 11, 2), ts(seoul, 2026, 11, 9), ts(seoul, 2026, 11, 16)),
            result.map { it.date },
        )
        assertEquals(3.0, result[1].cumulativeProfit, 0.0)
        assertEquals(2.0, result[1].open, 0.0)
        assertEquals(Calendar.MONDAY, GregorianCalendar(seoul).apply { timeInMillis = result[1].date }.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun monthAndYear_bucketOnCalendarBoundaries() {
        val items = listOf(
            tick(ts(seoul, 2026, 10, 31, 23), 1.0),
            tick(ts(seoul, 2026, 11, 1, 0), 2.0),
            tick(ts(seoul, 2026, 12, 31, 23), 3.0),
            tick(ts(seoul, 2027, 1, 1, 0), 4.0),
        )
        assertEquals(
            listOf(ts(seoul, 2026, 10, 1), ts(seoul, 2026, 11, 1), ts(seoul, 2026, 12, 1), ts(seoul, 2027, 1, 1)),
            items.aggregate(Timeframe.Month, seoul).map { it.date },
        )
        val years = items.aggregate(Timeframe.Year, seoul)
        assertEquals(listOf(ts(seoul, 2026, 1, 1), ts(seoul, 2027, 1, 1)), years.map { it.date })
        assertEquals(3.0, years[0].cumulativeProfit, 0.0)
        assertEquals(1.0, years[0].open, 0.0)
    }

    @Test
    fun tick_groupsConsecutiveItemsAndKeepsLastDate() {
        val items = (1..7).map { tick(it * 1000L, close = it.toDouble(), volume = 1.0) }
        val result = items.aggregate(Timeframe(TimeframeUnit.Tick, 3), seoul)
        assertEquals(listOf(3000L, 6000L, 7000L), result.map { it.date })
        assertEquals(listOf(3.0, 6.0, 7.0), result.map { it.cumulativeProfit })
        assertEquals(listOf(1.0, 4.0, 7.0), result.map { it.open })
        assertEquals(listOf(3.0, 6.0, 7.0), result.map { it.high })
        assertEquals(listOf(1.0, 4.0, 7.0), result.map { it.low })
        assertEquals(listOf(3.0, 3.0, 1.0), result.map { it.volume })
        assertEquals(items, items.aggregate(Timeframe.Tick, seoul))
    }
}
