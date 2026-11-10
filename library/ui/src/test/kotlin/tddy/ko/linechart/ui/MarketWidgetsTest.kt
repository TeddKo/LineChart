package tddy.ko.linechart.ui

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketWidgetsTest {
    @Test
    fun tradeDateIsTwoDigitYearInTheGivenZone() {
        val utcMidnight = 1_700_000_000_000L - 1_700_000_000_000L % 86_400_000L
        assertEquals("23/11/14", tradeDate(utcMidnight, TimeZone.getTimeZone("UTC")))
        assertEquals("23/11/14", tradeDate(utcMidnight, TimeZone.getTimeZone("Asia/Seoul")))
        assertEquals("23/11/13", tradeDate(utcMidnight, TimeZone.getTimeZone("America/Los_Angeles")))
    }

    @Test
    fun tradeLabelAppendsQuantityOnlyAboveOne() {
        assertEquals("260", tradeLabel("260", 1))
        assertEquals("260 ×3", tradeLabel("260", 3))
    }
}
