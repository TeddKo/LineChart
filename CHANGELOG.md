# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## 1.1.1

### Changed
* Line and Area charts split their color at a reference price: up color above, down color below. The reference is the base line, else the previous session close, else the first visible close. The pulse dot follows the same reference

## 1.1.0

### Added
* `ChartType` (Line, Area, Candle) and OHLCV plus `value` fields on `ChartData`, with the 1.0 constructor and `copy` kept
* Volume pane, last price line, high/low markers
* Pan with fling, pinch zoom, long-press crosshair that hides on release
* `Timeframe` (tick, second, minute, day, week, month, year, any count) and `List<ChartData>.aggregate`
* `ChartColors` with Korean and International presets and dark variants
* OHLC header and legends with the timeframe unit, English and Korean labels, header legend wrap
* Live updates that keep the viewport, zoom and crosshair
* `overlays`, `panes`, `settings` and `paneHeight` parameters on `LineChartPanel`
* 9 overlay indicators: `MovingAverage` (simple, exponential, weighted), `BollingerBands`, `Envelope`, `Ichimoku`, `ParabolicSar`, `PriceChannel`, `Pivot`, `Vwap`, `VolumeProfile`
* 21 pane indicators: `Volume`, `TradingValue`, `Macd`, `MacdOscillator`, `Rsi` (Wilder), `StochasticFast`, `StochasticSlow` (5, 3, 3), `WilliamsR`, `Cci`, `Dmi`, `Atr`, `Obv`, `Mfi`, `Momentum`, `Roc`, `Trix`, `Disparity`, `Psychological`, `VolumeRatio`, `VolumeOscillator`, `Sonar`; Ichimoku shifts by shift−1 and extends the chart 25 slots to the right, `TradingValue` uses `ChartData.value` when above 0 else close times volume, volume-based indicators are hidden without volume
* Multi-pane layout with per-pane Y axes, legends, level lines, crosshair value tags and formats (K/M/B, two decimals with `Locale.ROOT`)
* Pane resize by long press on a separator (within 12dp, haptic, minimum 120dp for the main area and 40dp for a pane), kept in `LineChartState` and not saved across recreation
* `ChartSettings`: price scale (linear, log, percent), axis side, right margin, grid, crosshair, legend, current price tag with percent, base lines (previous close, today open, pivot, custom), high/low markers, price limits, volume MA, font scale
* Time axis shows `MM/dd` for an intraday label at a new day or when the label stride spans a day
* Market components: `DepthChart`, `OrderBookLadder`, `TradeList`, `OptionSelector`, `InstantTradeButtons`, models `MarketLevel`, `MarketTrade`, `MarketOption`, `MarketSide`, and `List<MarketTrade>.toDailyChartData`
* New artifact `linechart-orderbook`: pure Kotlin matching engine (price-time priority, limit and instant orders, lazy expiry, modify, fee policy, self-trade prevention, deterministic clock and ids)
* Demo app: indicator sheet, settings screen, market screen with a live order book simulator

### Changed
* **Height:** `chartHeight` is the main chart area; every pane adds `paneHeight × weight + 4.dp`, so the default Volume pane makes the total 384.dp instead of 300.dp
* Default look is Candle with MA 5/20/60/120, a Volume pane and the Korean palette
* Price axis on the right, canvas time axis with label stride
* `maxDataPoints` keeps the latest points
* `formatValue` uses `#,##0.##` independent of the default locale
* Fewer than 2 items draw an empty box
* The entry reveal plays once
* One persistent `LineChartState` updated in place
* An opaque `colors.background` is drawn behind the chart
* A tap no longer selects a sample, a long press does
* The `labels` argument of the legacy `LineChartState` constructor is ignored
* `DragSelection.position` snaps to the candle center and `interpolatedValue` equals the value
* The default palette follows the system dark mode
* Main Y range includes bands, channel, Ichimoku, SAR, VWAP and the base line, and excludes moving averages, pivot and volume profile
* Built with Gradle 9.9.0, AGP 9.5.1, Kotlin 2.4.21 (API 2.2), compileSdk 37, Compose 1.12.1
* material3 removed from the library; the POM lists only Compose ui, foundation and kotlin-stdlib
* Library resources use the `linechart_` prefix, market strings are `linechart_market_*`

### Fixed
* Gray split right of the crosshair removed
* Crosshair snaps to the candle center and its tags are clamped to the plot
* Division by zero in change-based indicators (Roc, Disparity, Trix, Sonar) no longer produces infinite values, gaps are drawn and the legend shows `-`
* Header legend no longer jumps between wrap heights while the crosshair moves, and the plot lays out again when an overlay legend grows to another wrapped line
* Pane heights follow their indicator when another pane is removed or inserted, matching an equal indicator before position and kind
* `DepthChart` no longer crashes on a narrow or very short layout, including a narrow first frame, and stops recomposing on every drag event
* `BaseLine.Custom` rejects non-finite values
* Pane legend no longer overlaps the bars
* Engine: a throwing fee policy leaves the book unchanged, including on modify where the original order is kept, a level quantity above `Int.MAX_VALUE` is capped

### Removed
* The 1.1 preview parameter `maPeriods` and `ChartColors.ma`; use `MovingAverage(periods, colors)`
* material3 as a transitive dependency

### Compatibility
* 1.0.0 binary signatures are kept through hidden shims and verified by a reflection test; a `javap` diff against the 1.0 jar reports no missing member
* The `linechart-orderbook` javadoc jar is empty, accepted by Maven Central
