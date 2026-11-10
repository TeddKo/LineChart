package tddy.ko.linechart.ui

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal enum class GestureKind { Undecided, Pan, Zoom, Vertical }

/**
 * Gesture decided by the accumulated movement ([dx], [dy]) of a touch beyond [slop] with [pointers] fingers down.
 */
internal fun classifyGesture(dx: Float, dy: Float, slop: Float, pointers: Int): GestureKind = when {
    pointers >= 2 -> GestureKind.Zoom
    abs(dx) > slop && abs(dx) >= abs(dy) -> GestureKind.Pan
    abs(dy) > slop -> GestureKind.Vertical
    else -> GestureKind.Undecided
}

/**
 * Pan with fling, pinch zoom at the focal point and long-press crosshair, leaving vertical drags unconsumed for the parent.
 */
internal fun Modifier.chartGestures(state: LineChartState): Modifier = pointerInput(state) {
    coroutineScope {
        var fling: Job? = null
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            fling?.cancel()
            var kind = GestureKind.Undecided
            val pending = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (kind == GestureKind.Undecided) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) return@withTimeoutOrNull
                    val moved = pressed[0].position - down.position
                    kind = classifyGesture(moved.x, moved.y, viewConfiguration.touchSlop, pressed.size)
                    if (kind == GestureKind.Pan || kind == GestureKind.Zoom) event.changes.forEach { it.consume() }
                }
            }
            when {
                pending == null -> {
                    state.onDragStart(down.position)
                    down.consume()
                    var last: PointerInputChange = down
                    do {
                        val event = awaitPointerEvent()
                        last = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (last.pressed) {
                            state.onDrag(last.position)
                            last.consume()
                        }
                    } while (last.pressed)
                    state.onDragEnd()
                }
                kind == GestureKind.Pan || kind == GestureKind.Zoom -> {
                    val tracker = VelocityTracker()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull { it.pressed }?.let {
                            val centroid = event.calculateCentroid()
                            if (centroid.isSpecified) state.transform(centroid.x, event.calculateZoom(), event.calculatePan().x)
                            tracker.addPosition(it.uptimeMillis, it.position)
                        }
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    val slot = state.slotPx()
                    val velocity = tracker.calculateVelocity().x
                    fling = launch {
                        var previous = 0f
                        AnimationState(0f, velocity).animateDecay(splineBasedDecay(this@pointerInput)) {
                            val target = state.scroll + (value - previous) / slot
                            previous = value
                            val clamped = clampScroll(target, state.itemCount, state.plot.width, slot)
                            state.scroll = clamped
                            if (clamped != target) cancelAnimation()
                        }
                    }
                }
            }
        }
    }
}
