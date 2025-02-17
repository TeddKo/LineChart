# 📈 LineChart

<p align="center">
  <a href="https://search.maven.org/artifact/io.github.teddko/linechart"><img alt="MavenCentral" src="https://img.shields.io/maven-central/v/io.github.teddko/linechart.svg"/></a>
  <a href="https://android-arsenal.com/api?level=23"><img alt="API" src="https://img.shields.io/badge/API-23%2B-brightgreen.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
</p>
<p align="center">
  <strong>Jetpack Compose</strong> library for creating customizable line charts with smooth animations.
</p>

# 🚀 Features
* 🎯 Smooth animations using Jetpack Compose
* 📊 Supports multiple data points and real-time updates
* 🎨 Customizable styling, axis labels, and grid lines
* 📱 Responsive and adaptive layout

<p align="center">
  <img src="https://github.com/user-attachments/assets/2e54e9c0-27a5-4381-b012-ce9675fb1f0d" width="480"/>
</p>

# 📦 Installation

```kotlin
dependencies {
    implementation("io.github.teddko:linechart:1.0.0")
}
```

# 📖 Basic Usage

```kotlin
@Composable
fun LineChartDemo() {
    // Create sample data points
    val chartData = listOf(
        ChartData(cumulativeProfit = 100.0, date = System.currentTimeMillis()),
        ChartData(cumulativeProfit = 150.0, date = System.currentTimeMillis() + 86400000),
        ChartData(cumulativeProfit = 130.0, date = System.currentTimeMillis() + 172800000),
        ChartData(cumulativeProfit = 200.0, date = System.currentTimeMillis() + 259200000)
    )

    val state = rememberLineChartState(
        items = chartData,
        maxDataPoints = 20,  // Optional: limit number of points
        yAxisTicksCount = 5  // Optional: number of y-axis labels
    )

    LineChartPanel(
        modifier = Modifier.fillMaxWidth(),
        chartHeight = 300.dp,
        state = state
    )
}
```

# 🎨 Customization

```kotlin
@Composable
fun CustomizedLineChart() {
    val chartData = listOf(
        ChartData(cumulativeProfit = 50.0, date = System.currentTimeMillis()),
        ChartData(cumulativeProfit = 75.0, date = System.currentTimeMillis() + 86400000),
        ChartData(cumulativeProfit = 60.0, date = System.currentTimeMillis() + 172800000),
        ChartData(cumulativeProfit = 90.0, date = System.currentTimeMillis() + 259200000)
    )

    val state = rememberLineChartState(
        items = chartData,
        maxDataPoints = 10,        // Show maximum 10 points
        yAxisTicksCount = 6,       // Show 6 labels on Y-axis
        scaleFactor = 10000,       // Scale factor for values
        strokeWidth = 3.dp         // Custom line thickness
    )

    LineChartPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        chartHeight = 400.dp,      // Custom chart height
        state = state
    )
}
```

# 🛠️ Development Environment
* **Kotlin 2.0+**
* **Android Studio Hedgehog+**
* **Jetpack Compose 1.7+**
* **Gradle Kotlin DSL applied**

# 📝 License
This project is licensed under the Apache 2.0 License.
