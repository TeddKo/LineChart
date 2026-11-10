package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class BaseLineTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val seoul = TimeZone.getTimeZone("Asia/Seoul")
    private val minute = 60_000L
    private val day = 86_400_000L

    private fun candle(date: Long, close: Double, open: Double = close, high: Double = close, low: Double = close) =
        ChartData(close, date, open, high, low)

    private val intraday = Series(
        listOf(
            candle(10 * 60 * minute, 11.0, open = 10.0, high = 110.0, low = 5.0),
            candle(10 * 60 * minute + minute, 12.0, open = 11.0, high = 90.0, low = 9.0),
            candle(10 * 60 * minute + 2 * minute, 13.0, open = 12.0, high = 14.0, low = 8.0),
            candle(day + 10 * 60 * minute, 20.0, open = 18.0),
            candle(day + 10 * 60 * minute + minute, 21.0, open = 20.0),
        ),
    )

    private val daily = Series(List(4) { candle(it * day, 100.0 + it, open = 99.0 + it) })

    @Test
    fun series_isIntradayOnlyBelowAboutADayStep() {
        assertTrue(intraday.intraday)
        assertTrue(!daily.intraday)
        assertTrue(!Series(emptyList()).intraday)
    }

    @Test
    fun previousClose_intradayIsTheLastCloseOfThePreviousLocalDay() {
        assertEquals(13.0, previousClose(intraday, utc), 0.0)
    }

    @Test
    fun previousClose_followsTheLocalDayBoundary() {
        val kst = Series(
            listOf(
                candle(Date_2024_01_01 + 14 * 60 * minute + 59 * minute, 5.0),
                candle(Date_2024_01_01 + 15 * 60 * minute, 6.0),
                candle(Date_2024_01_01 + 15 * 60 * minute + minute, 7.0),
            ),
        )
        assertEquals(5.0, previousClose(kst, seoul), 0.0)
        assertTrue(previousClose(kst, utc).isNaN())
    }

    @Test
    fun previousClose_dayAndAboveIsThePreviousCandleClose() {
        assertEquals(102.0, previousClose(daily, utc), 0.0)
    }

    @Test
    fun previousClose_isNaNWithoutAPreviousSession() {
        assertTrue(previousClose(Series(emptyList()), utc).isNaN())
        assertTrue(previousClose(Series(listOf(candle(0L, 1.0))), utc).isNaN())
        assertTrue(previousClose(Series(intraday.items.drop(3)), utc).isNaN())
    }

    @Test
    fun todayOpen_intradayIsTheOpenOfTheFirstCandleOfTheLastDay() {
        assertEquals(18.0, todayOpen(intraday, utc), 0.0)
        assertEquals(102.0, todayOpen(daily, utc), 0.0)
    }

    @Test
    fun pivotPoint_isTheTypicalPriceOfThePreviousSession() {
        assertEquals((110.0 + 5.0 + 13.0) / 3.0, pivotPoint(intraday, utc), 1e-9)
        assertEquals(102.0, pivotPoint(daily, utc), 1e-9)
    }

    @Test
    fun baseValue_resolvesEachBaseLine() {
        assertTrue(baseValue(intraday, BaseLine.None, utc).isNaN())
        assertEquals(13.0, baseValue(intraday, BaseLine.PreviousClose, utc), 0.0)
        assertEquals(18.0, baseValue(intraday, BaseLine.TodayOpen, utc), 0.0)
        assertEquals(42.5, baseValue(intraday, BaseLine.Custom(42.5), utc), 0.0)
        assertEquals((110.0 + 5.0 + 13.0) / 3.0, baseValue(intraday, BaseLine.Pivot, utc), 1e-9)
    }

    @Test
    fun limitLines_sitAtThePreviousCloseWithThePercent() {
        val (upper, lower) = limitLines(100.0, 30.0)
        assertEquals(130.0, upper, 1e-9)
        assertEquals(70.0, lower, 1e-9)
        assertTrue(limitLines(Double.NaN, 30.0).first.isNaN())
    }

    @Test
    fun computed_resolvesTheBaseAndFallsBackForPercentScale() {
        val state = LineChartState(androidx.compose.ui.unit.Dp.Hairline, 5, 10_000).apply { update(daily.items, Int.MAX_VALUE) }
        state.setIndicators(emptyList(), emptyList(), ChartSettings(baseLine = BaseLine.Custom(50.0)))
        assertEquals(50.0, state.computed.base, 0.0)
        state.setIndicators(emptyList(), emptyList(), ChartSettings(scale = PriceScale.Percent))
        assertEquals(102.0, state.computed.base, 0.0)
        assertEquals(102.0, state.computed.prevClose, 0.0)
        state.setIndicators(emptyList(), emptyList(), ChartSettings())
        assertTrue(state.computed.base.isNaN())
    }

    private companion object {
        const val Date_2024_01_01 = 1_704_067_200_000L
    }
}
