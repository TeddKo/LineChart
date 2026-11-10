package tddy.ko.linechart.ui

internal fun seriesOf(
    close: DoubleArray,
    high: DoubleArray = close,
    low: DoubleArray = close,
    volume: DoubleArray = DoubleArray(close.size) { 10.0 },
): Series = Series(close.indices.map { ChartData(close[it], it + 1L, close[it], high[it], low[it], volume[it]) })

internal fun doubles(vararg values: Double): DoubleArray = values

internal fun List<Mark>.lines(): List<LineMark> = filterIsInstance<LineMark>()

internal fun DoubleArray.leadingNaN(): Int = indexOfFirst { !it.isNaN() }.let { if (it < 0) size else it }
