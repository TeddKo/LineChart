package tddy.ko.linechart.desigh_system.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val purple200 = Color(0xFFBB86FC)
internal val purple500 = Color(0xFF6200EE)
internal val purple700 = Color(0xFF3700B3)
internal val teal200 = Color(0xFF03DAC5)
internal val teal700 = Color(0xFF018786)
internal val positive = Color(0xFF05CE62)
internal val warning = Color(0xFFFF5151)

internal val LightColorScheme = lightColorScheme(
    primary = purple200,
    secondary = purple500,
    tertiary = purple700,
    surface = Color.White,
    inversePrimary = positive,
    error = warning
)