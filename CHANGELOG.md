# Changelog

## 1.1.0

### Added
* `ChartType` (Line, Area, Candle) and OHLCV fields on `ChartData`
* Volume pane, moving averages, last price line, high/low markers
* Pan with fling, pinch zoom, long-press crosshair
* `Timeframe` and `List<ChartData>.aggregate`
* `ChartColors` with Korean and International presets and dark variants
* OHLC header and MA legend with the timeframe unit, English and Korean labels
* Live updates that keep the viewport, zoom and crosshair

### Changed
* Default look is Candle with MA 5/20/60/120 and the Korean palette
* Price axis on the right, canvas time axis with label stride
* `maxDataPoints` keeps the latest points
* `formatValue` uses `#,##0.##` independent of the default locale
* Fewer than 2 items draw an empty box
* The entry reveal plays once
* One persistent `LineChartState` updated in place
* Gray split right of the crosshair removed
* Built with Gradle 9.9.0, AGP 9.5.1, Kotlin 2.4.21 (API 2.2), compileSdk 37, Compose 1.12.1
* material3 removed from the library; the POM lists only Compose ui, foundation and kotlin-stdlib

### Compatibility
* 1.0.0 binary signatures are kept through hidden shims and verified by a reflection test
