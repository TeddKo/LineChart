package tddy.ko.linechart.ui

import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.Composer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.internal.DefaultConstructorMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Guard for the 1.0.0 JVM entry points of LineChart.
 */
class LineChartAbiTest {

    private val int = Int::class.java
    private val long = Long::class.java
    private val float = Float::class.java
    private val double = Double::class.java
    private val boolean = Boolean::class.java
    private val void = Void.TYPE

    private fun method(owner: String, name: String, returns: Class<*>, vararg params: Class<*>) {
        val found = Class.forName("tddy.ko.linechart.ui.$owner").getDeclaredMethod(name, *params)
        assertNotNull("$owner.$name", found)
        assertEquals("$owner.$name return type", returns, found.returnType)
    }

    @Test
    fun chartFacade_keepsPanelAndChartEntries() {
        method("ChartKt", "LineChartPanel-uFdPcIQ", void, Modifier::class.java, float, LineChartState::class.java, Composer::class.java, int, int)
        method("ChartKt", "LineChart", void, Modifier::class.java, LineChartState::class.java, Composer::class.java, int)
    }

    @Test
    fun helperFacade_keepsTopLevelEntries() {
        method(
            "ChartHelperKt", "rememberLineChartState-FJfuzF0", LineChartState::class.java,
            List::class.java, int, int, int, float, Composer::class.java, int, int,
        )
        method("ChartHelperKt", "bindDragGestures", Modifier::class.java, Modifier::class.java, Array<DragGestureListener>::class.java)
        method(
            "ChartHelperKt", "infiniteTransitionValue", float,
            float, float, int, DurationBasedAnimationSpec::class.java, RepeatMode::class.java, Composer::class.java, int, int,
        )
        method("ChartHelperKt", "getCHART_DATA_POINT_UNSPECIFIED", ChartDataPoint::class.java)
        method("ChartHelperKt", "getDRAG_SELECTION_NONE", DragSelection::class.java)
    }

    @Test
    fun chartData_keepsLegacyConstructorCopyAndComponents() {
        val type = ChartData::class.java
        assertNotNull(type.getDeclaredConstructor(double, long))
        assertEquals(type, type.getDeclaredMethod("copy", double, long).returnType)
        assertEquals(
            type,
            type.getDeclaredMethod("copy\$default", type, double, long, int, Any::class.java).returnType,
        )
        assertEquals(double, type.getDeclaredMethod("component1").returnType)
        assertEquals(long, type.getDeclaredMethod("component2").returnType)
        assertEquals(double, type.getDeclaredMethod("getCumulativeProfit").returnType)
        assertEquals(long, type.getDeclaredMethod("getDate").returnType)
    }

    @Test
    fun lineChartState_keepsLegacyConstructorAndMembers() {
        val type = LineChartState::class.java
        assertNotNull(
            type.getDeclaredConstructor(List::class.java, boolean, List::class.java, float, int, DefaultConstructorMarker::class.java),
        )
        assertEquals(List::class.java, type.getDeclaredMethod("getLabels").returnType)
        assertEquals(float, type.getDeclaredMethod("getStrokeWidth-D9Ej5fM").returnType)
        assertEquals(int, type.getDeclaredMethod("getYAxisTicksCount").returnType)
        assertEquals(int, type.getDeclaredMethod("getItemCount").returnType)
        assertEquals(Function1::class.java, type.getDeclaredMethod("getFormatValue").returnType)
        assertEquals(List::class.java, type.getDeclaredMethod("getYAxisTicks").returnType)
        assertEquals(Stroke::class.java, type.getDeclaredMethod("stroke", DrawScope::class.java).returnType)
        assertEquals(List::class.java, type.getDeclaredMethod("getOffsets", DrawScope::class.java).returnType)
        assertEquals(boolean, type.getDeclaredMethod("isPositive").returnType)
        assertEquals(DragSelection::class.java, type.getDeclaredMethod("selectedBy-k-4lQ0M", long).returnType)
        assertEquals(void, type.getDeclaredMethod("onDragEnd").returnType)
    }

    @Test
    fun dragHandler_keepsSelectionMembers() {
        val type = DragHandler::class.java
        assertEquals(StateFlow::class.java, type.getDeclaredMethod("getSelected").returnType)
        assertEquals(void, type.getDeclaredMethod("onDrag-k-4lQ0M", long).returnType)
        assertEquals(void, type.getDeclaredMethod("onDragStart-k-4lQ0M", long).returnType)
    }
}
