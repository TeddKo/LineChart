# LineChart

<p align="center">
  <a href="https://search.maven.org/artifact/io.github.teddko/linechart"><img alt="MavenCentral" src="https://img.shields.io/maven-central/v/io.github.teddko/linechart.svg"/></a>
  <a href="https://android-arsenal.com/api?level=23"><img alt="API" src="https://img.shields.io/badge/API-23%2B-brightgreen.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
</p>

Jetpack Compose chart for trading-style screens: candle, line and area series with volume, moving averages, pan, fling, pinch zoom, a long-press crosshair and live updates.

<!-- screenshots -->

## Features

* Candle, Line and Area series (`ChartType`)
* Volume pane, moving averages (default MA 5/20/60/120), last price line and visible-range high/low markers
* Pan with fling, pinch zoom at the focal point, long-press crosshair that hides on release
* Vertical drags are left to the parent, so the chart works inside a scrolling column
* Real-time updates that keep the viewport, zoom and crosshair and follow the right edge
* Korean (red up, blue down) and International (green up, red down) palettes with dark variants, or a custom `ChartColors`
* Canvas price axis (right) and time axis with adaptive label stride
* English and Korean header labels
* Entry reveal animation, played once

## Installation

```kotlin
dependencies {
    implementation("io.github.teddko:linechart:1.1.0")
}
```

## Usage

```kotlin
val state = rememberLineChartState(items)

LineChartPanel(
    modifier = Modifier.fillMaxWidth(),
    chartHeight = 360.dp,
    state = state,
    chartType = ChartType.Candle,
    colors = ChartColors.Korean,
    maPeriods = listOf(5, 20, 60, 120),
    timeframe = Timeframe.Minute1,
)
```

`ChartData(cumulativeProfit, date, open, high, low, volume)` carries one sample. `cumulativeProfit` is the line value and the candle close, `date` is epoch millis and the identity of the sample. `open`, `high`, `low` and `volume` are optional.

## Timeframes

`Timeframe` is a unit with a count: `Tick`, `Second1/5/10/30`, `Minute1/3/5/10/15/30/60`, `Day`, `Week`, `Month`, `Year`, or `Timeframe(TimeframeUnit.Minute, 7)`. Second and minute counts align per day from local midnight, so any count works.

```kotlin
val candles = ticks.aggregate(Timeframe.Minute5)
```

`aggregate` builds candles (first open, max high, min low, last close, summed volume, bucket start date) in the device time zone, weeks starting Monday. A `Tick` group takes the date of its first item, so a growing last group keeps its identity. Moving averages count candles of the current timeframe, so MA5 on a weekly chart is five weeks, and the legend shows the unit (`MA5D`, `MA20분`). Aggregating the full list on every tick suits modest histories; with a large history keep the aggregated list and re-aggregate only the last bucket.

Axis labels and the crosshair time tag are formatted in the device time zone. To show another zone, pass `timeFormatter`, for example `timeFormatter = { millis -> format.format(Date(millis)) }` with a `SimpleDateFormat` whose `timeZone` is set; `aggregate(timeframe, timeZone)` takes the zone for the buckets.

## Palettes

| Palette | Up | Down |
|---|---|---|
| `ChartColors.Korean` (default) | red | blue |
| `ChartColors.International` | green | red |
| `ChartColors.KoreanDark`, `ChartColors.InternationalDark` | dark-surface variants | |

The default follows `isSystemInDarkTheme()`.

## Gestures

| Gesture | Result |
|---|---|
| Horizontal drag | Pan, with fling |
| Two-finger pinch | Zoom around the fingers |
| Long press, then drag | Crosshair with price and time tags, header shows the item under it; hides on release |
| Vertical drag | Passed to the parent scroll |

## State

`rememberLineChartState(items, maxDataPoints, yAxisTicksCount, scaleFactor, strokeWidth)` keeps one `LineChartState` for the lifetime of the composable. Pass a new list on every update.

* The last sample changed or was replaced in place: viewport unchanged
* Samples appended: the chart follows the right edge while it is at the latest sample, and stays anchored when scrolled back or while the crosshair is on
* The crosshair keeps the anchor while it is on; when it is released, the chart jumps back to the latest sample if it was following when the crosshair started, otherwise the anchor stays
* A series that no longer contains the previous last `date` (for example a timeframe switch): the viewport resets to the latest sample and the zoom is kept
* `maxDataPoints > 0` keeps the latest points only
* `selected` emits only when the crosshair moves to another sample

## Upgrade notes from 1.0.0

The 1.0.0 API and binary signatures stay (`LineChartPanel`, `LineChart`, `rememberLineChartState`, `ChartData(cumulativeProfit, date)`). Behavior changes:

* The default look is now Candle with MA 5/20/60/120; pass `chartType = ChartType.Line, maPeriods = emptyList()` for a plain line
* Pan, zoom and crosshair gestures are on; the crosshair hides on release
* The price axis is on the right, the time axis is drawn by the library
* The default palette is Korean (red up, blue down)
* A header shows OHLC, change and the MA legend
* `maxDataPoints` now keeps the latest points (`takeLast`)
* `formatValue` uses `#,##0.##` and a locale independent format
* Fewer than 2 items draw an empty box instead of nothing
* The gray split right of the crosshair is gone
* The entry reveal plays once, not on every state change
* An opaque `colors.background` is drawn behind the chart; use `Color.Transparent` to show the parent
* A tap no longer selects a sample, a long press does
* The `labels` argument of the legacy `LineChartState` constructor is ignored, the axis shows the item number or the time
* `DragSelection.position` snaps to the candle center and `interpolatedValue` equals the value
* The default palette follows the system dark mode
* material3 is no longer a transitive dependency; the POM lists only Compose ui, foundation and kotlin-stdlib

## Requirements

* minSdk 23, compileSdk 37
* Compose ui and foundation 1.12.1
* Kotlin 2.2 API and language level, stdlib 2.2.21

## License

Apache License 2.0
