package tddy.ko.linechart.feature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import tddy.ko.linechart.desigh_system.theme.LineChartTheme
import tddy.ko.linechart.ui.ChartData
import tddy.ko.linechart.ui.LineChartPanel
import tddy.ko.linechart.ui.rememberLineChartState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {

            val previewData = listOf(
                ChartData(10.0, 0),
                ChartData(12.5, 0),
                ChartData(11.0, 0),
                ChartData(11.8, 0),
                ChartData(-2.0, 0),
                ChartData(8.0, 0),
                ChartData(9.5, 0),
                ChartData(7.0, 0),
                ChartData(8.2, 0),
                ChartData(15.0, 0),
                ChartData(13.5, 0),
                ChartData(14.0, 0),
                ChartData(13.8, 0),
                ChartData(13.9, 0),
                ChartData(14.2, 0),
                ChartData(14.1, 0),
                ChartData(14.5, 0),
                ChartData(14.3, 0),
                ChartData(14.0, 0),
                ChartData(13.5, 0),
                ChartData(12.8, 0),
                ChartData(12.0, 0),
                ChartData(11.5, 0),
                ChartData(10.8, 0),
                ChartData(-3.0, 0)
            )

            LineChartTheme {
                LineChartPanel(
                    state = rememberLineChartState(
                        items = previewData
                    )
                )
            }
        }
    }
}