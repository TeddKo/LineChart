package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatTest {

    private val second = 1_000L
    private val minute = 60 * second
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun formatPrice_groupsThousandsAndTrimsToTwoDecimals() {
        assertEquals("1,234,567.89", formatPrice(1234567.891))
        assertEquals("71,200", formatPrice(71200.0))
        assertEquals("0.5", formatPrice(0.5))
        assertEquals("-1,234.5", formatPrice(-1234.5))
        assertEquals("0", formatPrice(-0.001))
    }

    @Test
    fun formatPrice_ignoresDefaultLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1,234.5", formatPrice(1234.5))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun timePattern_followsTimeframeUnit() {
        assertEquals("HH:mm:ss", timePattern(Timeframe.Tick, 0L))
        assertEquals("HH:mm:ss", timePattern(Timeframe.Second5, 0L))
        assertEquals("HH:mm", timePattern(Timeframe.Minute5, 0L))
        assertEquals("MM/dd", timePattern(Timeframe.Day, 0L))
        assertEquals("MM/dd", timePattern(Timeframe.Week, 0L))
        assertEquals("yyyy/MM", timePattern(Timeframe.Month, 0L))
        assertEquals("yyyy", timePattern(Timeframe.Year, 0L))
    }

    @Test
    fun timePattern_infersFromStepWhenTimeframeIsNull() {
        assertEquals("HH:mm:ss", timePattern(null, 5 * second))
        assertEquals("HH:mm", timePattern(null, 5 * minute))
        assertEquals("MM/dd", timePattern(null, day))
        assertEquals("MM/dd", timePattern(null, 7 * day))
        assertEquals("yyyy/MM", timePattern(null, 31 * day))
        assertEquals("yyyy", timePattern(null, 366 * day))
        assertEquals("HH:mm:ss", timePattern(null, 0L))
    }

    @Test
    fun tagPattern_isLongerFormOfTimePattern() {
        assertEquals("yyyy/MM/dd HH:mm:ss", tagPattern(Timeframe.Tick, 0L))
        assertEquals("yyyy/MM/dd HH:mm:ss", tagPattern(Timeframe.Second1, 0L))
        assertEquals("yyyy/MM/dd HH:mm", tagPattern(Timeframe.Minute1, 0L))
        assertEquals("yyyy/MM/dd", tagPattern(Timeframe.Day, 0L))
        assertEquals("yyyy/MM/dd", tagPattern(Timeframe.Week, 0L))
        assertEquals("yyyy/MM", tagPattern(Timeframe.Month, 0L))
        assertEquals("yyyy", tagPattern(Timeframe.Year, 0L))
        assertEquals("yyyy/MM/dd HH:mm", tagPattern(null, 5 * minute))
    }

    @Test
    fun labelStride_picksSmallestRoundStrideThatFitsLabel() {
        assertEquals(1, labelStride(100f, 30f))
        assertEquals(5, labelStride(8f, 30f))
        assertEquals(2, labelStride(20f, 30f))
        assertEquals(50, labelStride(1f, 30f))
        assertEquals(10, labelStride(4f, 40f))
        assertEquals(100, labelStride(0.4f, 30f))
        assertEquals(1, labelStride(0f, 30f))
        assertEquals(1, labelStride(Float.NaN, 30f))
    }
}
