# LineChart

<p align="center">
  <a href="https://search.maven.org/artifact/io.github.teddko/linechart"><img alt="MavenCentral" src="https://img.shields.io/maven-central/v/io.github.teddko/linechart.svg"/></a>
  <a href="https://android-arsenal.com/api?level=23"><img alt="API" src="https://img.shields.io/badge/API-23%2B-brightgreen.svg"/></a>
  <a href="https://opensource.org/licenses/Apache-2.0"><img alt="License" src="https://img.shields.io/badge/License-Apache%202.0-blue.svg"/></a>
</p>
<p align="center">
A <strong>Jetpack Compose</strong> library for creating customizable line charts with smooth animations.
</p>

## 🚀 Features
- 🎯 Smooth animations using Jetpack Compose
- 📊 Supports multiple data points and real-time updates
- 🎨 Customizable styling, axis labels, and grid lines
- 📱 Responsive and adaptive layout

## 📦 Installation
```kotlin
dependencies {
    implementation("io.github.teddko:linechart:1.0.0")
}
```

## 📖 Basic Usage
```kotlin
@Composable
fun LineChartDemo() {
    val data = remember { listOf(10f, 20f, 15f, 25f, 30f) }

    LineChart(
        modifier = Modifier.fillMaxSize(),
        data = data,
        onValueSelected = { value ->
            println("Selected Value: $value")
        }
    )
}
```

## 🎨 Customization
### Chart Styling Options
```kotlin
LineChart(
    data = listOf(5f, 15f, 10f, 20f, 25f),
    lineColor = Color.Blue,
    gridColor = Color.Gray,
    showGrid = true,
    animateChanges = true
)
```

### Axis Configuration
```kotlin
LineChart(
    data = listOf(5f, 10f, 15f, 20f),
    xAxisLabels = listOf("Q1", "Q2", "Q3", "Q4"),
    yAxisStep = 5f,
    showXAxis = true,
    showYAxis = true
)
```

## 🛠️ Development Environment
- **Kotlin 1.8+**
- **Android Studio Hedgehog+**
- **Jetpack Compose 1.4+**
- **Gradle Kotlin DSL applied**

## 📝 License
This project is licensed under the Apache 2.0 License.

