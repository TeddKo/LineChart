package tddy.ko.linechart.feature

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun <T> Segmented(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (label, value) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(1f))
        FilledTonalIconButton(onMinus) { Text("−") }
        Text(value, Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center)
        FilledTonalIconButton(onPlus) { Text("+") }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
fun SwatchRow(label: String, selected: Color?, onSelect: (Color?) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            TextButton({ onSelect(null) }) { Text("기본") }
        }
        var picking by remember { mutableStateOf(false) }
        val custom = selected?.takeIf { it !in Swatches }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Swatches.forEach { color ->
                Box(Modifier.size(28.dp).clip(CircleShape).background(color).border(swatchBorder(color == selected), CircleShape).clickable { onSelect(color) })
            }
            val fill = custom?.let { Modifier.background(it) } ?: Modifier.background(Brush.sweepGradient(Rainbow))
            Box(Modifier.size(28.dp).clip(CircleShape).then(fill).border(swatchBorder(custom != null), CircleShape).clickable { picking = true })
        }
        if (picking) ColorPickerDialog(selected ?: Swatches.first(), { picking = false }) { onSelect(it); picking = false }
    }
}

private val Rainbow = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it, 1f, 1f) }

@Composable
private fun swatchBorder(selected: Boolean): BorderStroke =
    if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

/**
 * Color wheel picking hue by angle and saturation by distance from the center, a value slider and a live preview.
 */
@Composable
private fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val color = Color.hsv(hue, saturation, value)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton({ onPick(color) }) { Text("적용") } },
        dismissButton = { TextButton(onDismiss) { Text("취소") } },
        title = { Text("색 선택") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorWheel(hue, saturation, value, Modifier.size(240.dp)) { h, s -> hue = h; saturation = s }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(color).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Slider(value, { value = it }, Modifier.weight(1f))
                }
            }
        },
    )
}

/**
 * Hue and saturation disc with a thumb at the current color, dimmed by [value].
 */
@Composable
private fun ColorWheel(hue: Float, saturation: Float, value: Float, modifier: Modifier, onChange: (Float, Float) -> Unit) {
    fun pick(position: Offset, size: IntSize) {
        val radius = min(size.width, size.height) / 2f
        val dx = position.x - size.width / 2f
        val dy = position.y - size.height / 2f
        val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        onChange((degrees + 360f) % 360f, (hypot(dx, dy) / radius).coerceIn(0f, 1f))
    }
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { pick(it, size) } }
            .pointerInput(Unit) { detectDragGestures { change, _ -> pick(change.position, size) } },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(Rainbow, center), radius)
        drawCircle(Brush.radialGradient(listOf(Color.hsv(0f, 0f, 1f), Color.Transparent), center, radius), radius)
        drawCircle(Color.hsv(0f, 0f, 0f, 1f - value), radius)
        val angle = Math.toRadians(hue.toDouble())
        val thumb = center + Offset((cos(angle) * saturation * radius).toFloat(), (sin(angle) * saturation * radius).toFloat())
        drawCircle(Color.hsv(hue, saturation, value), 12.dp.toPx(), thumb)
        drawCircle(Color.hsv(0f, 0f, 1f), 12.dp.toPx(), thumb, style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
}
