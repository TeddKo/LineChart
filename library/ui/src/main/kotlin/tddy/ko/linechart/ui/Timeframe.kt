package tddy.ko.linechart.ui

import androidx.compose.runtime.Immutable
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Time unit of a candle.
 */
public enum class TimeframeUnit { Tick, Second, Minute, Day, Week, Month, Year }

/**
 * Candle interval made of [count] units, where [count] applies to Tick, Second and Minute only.
 */
@Immutable
public class Timeframe(public val unit: TimeframeUnit, public val count: Int = 1) {
    init {
        require(count >= 1) { "count must be at least 1" }
    }

    override fun equals(other: Any?): Boolean =
        other is Timeframe && other.unit == unit && other.count == count

    override fun hashCode(): Int = 31 * unit.hashCode() + count

    override fun toString(): String = "Timeframe($unit, $count)"

    public companion object {
        public val Tick: Timeframe = Timeframe(TimeframeUnit.Tick)
        public val Second1: Timeframe = Timeframe(TimeframeUnit.Second, 1)
        public val Second5: Timeframe = Timeframe(TimeframeUnit.Second, 5)
        public val Second10: Timeframe = Timeframe(TimeframeUnit.Second, 10)
        public val Second30: Timeframe = Timeframe(TimeframeUnit.Second, 30)
        public val Minute1: Timeframe = Timeframe(TimeframeUnit.Minute, 1)
        public val Minute3: Timeframe = Timeframe(TimeframeUnit.Minute, 3)
        public val Minute5: Timeframe = Timeframe(TimeframeUnit.Minute, 5)
        public val Minute10: Timeframe = Timeframe(TimeframeUnit.Minute, 10)
        public val Minute15: Timeframe = Timeframe(TimeframeUnit.Minute, 15)
        public val Minute30: Timeframe = Timeframe(TimeframeUnit.Minute, 30)
        public val Minute60: Timeframe = Timeframe(TimeframeUnit.Minute, 60)
        public val Day: Timeframe = Timeframe(TimeframeUnit.Day)
        public val Week: Timeframe = Timeframe(TimeframeUnit.Week)
        public val Month: Timeframe = Timeframe(TimeframeUnit.Month)
        public val Year: Timeframe = Timeframe(TimeframeUnit.Year)
    }
}

/**
 * Groups items in time order into candles of [timeframe] with open of the first item, high and low extremes, close of the last item and summed volume.
 *
 * The candle date is the bucket start in [timeZone], and for Tick the date of the first item of each group of [Timeframe.count] items.
 * Aggregating the full list on every update suits modest histories, while apps with large histories keep the aggregated history and re-aggregate only the last bucket.
 */
public fun List<ChartData>.aggregate(
    timeframe: Timeframe,
    timeZone: TimeZone = TimeZone.getDefault(),
): List<ChartData> {
    if (isEmpty()) return emptyList()
    val calendar = GregorianCalendar(timeZone)
    val result = ArrayList<ChartData>()
    var groupStart = 0
    var groupKey = bucketKey(this[0], 0, timeframe, timeZone, calendar)
    for (i in 1..size) {
        val key = if (i < size) bucketKey(this[i], i, timeframe, timeZone, calendar) else Long.MIN_VALUE
        if (i == size || key != groupKey) {
            result += merge(groupStart, i, if (timeframe.unit == TimeframeUnit.Tick) this[groupStart].date else groupKey)
            groupStart = i
            groupKey = key
        }
    }
    return result
}

private fun List<ChartData>.merge(from: Int, to: Int, date: Long): ChartData {
    val first = this[from]
    var high = first.high
    var low = first.low
    var volume = 0.0
    for (i in from until to) {
        val item = this[i]
        if (item.high > high) high = item.high
        if (item.low < low) low = item.low
        volume += item.volume
    }
    return ChartData(this[to - 1].cumulativeProfit, date, first.open, high, low, volume)
}

private fun bucketKey(item: ChartData, index: Int, timeframe: Timeframe, timeZone: TimeZone, calendar: Calendar): Long =
    when (timeframe.unit) {
        TimeframeUnit.Tick -> (index / timeframe.count).toLong()
        TimeframeUnit.Second -> floorLocal(item.date, 1_000L * timeframe.count, timeZone)
        TimeframeUnit.Minute -> floorLocal(item.date, 60_000L * timeframe.count, timeZone)
        else -> calendarBucket(item.date, timeframe.unit, calendar)
    }

private const val DAY_MILLIS = 86_400_000L

private fun floorLocal(date: Long, unitMillis: Long, timeZone: TimeZone): Long {
    val offset = timeZone.getOffset(date)
    val local = date + offset
    val dayStart = Math.floorDiv(local, DAY_MILLIS) * DAY_MILLIS
    return dayStart + Math.floorDiv(local - dayStart, unitMillis) * unitMillis - offset
}

private fun calendarBucket(date: Long, unit: TimeframeUnit, calendar: Calendar): Long {
    calendar.timeInMillis = date
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    when (unit) {
        TimeframeUnit.Week -> calendar.add(
            Calendar.DAY_OF_MONTH,
            -((calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7),
        )
        TimeframeUnit.Month -> calendar.set(Calendar.DAY_OF_MONTH, 1)
        TimeframeUnit.Year -> {
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        else -> Unit
    }
    return calendar.timeInMillis
}
