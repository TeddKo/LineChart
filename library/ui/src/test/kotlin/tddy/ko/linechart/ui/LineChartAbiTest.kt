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

    private fun type(name: String) = Class.forName("tddy.ko.linechart.ui.$name")

    @Test
    fun dragSelection_keepsConstructorCopyAndComponents() {
        val type = DragSelection::class.java
        val point = ChartDataPoint::class.java
        assertNotNull(type.getDeclaredConstructor(long, point, long, int, int, DefaultConstructorMarker::class.java))
        assertEquals(type, type.getDeclaredMethod("copy-M_7yMNQ", long, point, long, int).returnType)
        assertEquals(
            type,
            type.getDeclaredMethod("copy-M_7yMNQ\$default", type, long, point, long, int, int, Any::class.java).returnType,
        )
        assertEquals(long, type.getDeclaredMethod("getPosition-F1C5BW0").returnType)
        assertEquals(point, type.getDeclaredMethod("getItem").returnType)
        assertEquals(long, type.getDeclaredMethod("getInterpolatedValue").returnType)
        assertEquals(int, type.getDeclaredMethod("getIndex").returnType)
        assertEquals(long, type.getDeclaredMethod("component1-F1C5BW0").returnType)
        assertEquals(point, type.getDeclaredMethod("component2").returnType)
        assertEquals(long, type.getDeclaredMethod("component3").returnType)
        assertEquals(int, type.getDeclaredMethod("component4").returnType)
    }

    @Test
    fun chartDataPoint_keepsConstructorCopyAndComponents() {
        val type = ChartDataPoint::class.java
        assertNotNull(type.getDeclaredConstructor(long, boolean))
        assertEquals(long, type.getDeclaredMethod("getValue").returnType)
        assertEquals(boolean, type.getDeclaredMethod("isPositive").returnType)
        assertEquals(long, type.getDeclaredMethod("component1").returnType)
        assertEquals(boolean, type.getDeclaredMethod("component2").returnType)
        assertEquals(type, type.getDeclaredMethod("copy", long, boolean).returnType)
        assertEquals(type, type.getDeclaredMethod("copy\$default", type, long, boolean, int, Any::class.java).returnType)
    }

    @Test
    fun cubicSegment_keepsPublicMembers() {
        val type = type("CubicSegment")
        assertNotNull(type.getDeclaredConstructor(long, long, long, long, DefaultConstructorMarker::class.java))
        for (name in listOf("getStart-F1C5BW0", "getControl1-F1C5BW0", "getControl2-F1C5BW0", "getEnd-F1C5BW0")) {
            assertEquals(name, long, type.getDeclaredMethod(name).returnType)
        }
        assertEquals(long, type.getDeclaredMethod("getPointAt-tuRUvjQ", float).returnType)
        assertEquals(type, type.getDeclaredMethod("copy-zwwh4xc", long, long, long, long).returnType)
    }

    @Test
    fun chartCanvasManager_keepsPublicMembers() {
        val type = type("ChartCanvasManager")
        assertNotNull(type.getDeclaredConstructor(int))
        assertEquals(List::class.java, type.getDeclaredMethod("getCurrentOffsets").returnType)
        assertEquals(
            List::class.java,
            type.getDeclaredMethod("calculateOffsets", DrawScope::class.java, List::class.java, Function1::class.java).returnType,
        )
        assertEquals(List::class.java, type.getDeclaredMethod("getLineSegments").returnType)
        assertEquals(long, type.getDeclaredMethod("getCanvasSize-NH-jbRc").returnType)
    }

    @Test
    fun chartValueCalculator_keepsPublicMembers() {
        val type = type("ChartValueCalculator")
        assertNotNull(type.getDeclaredConstructor(List::class.java))
        assertEquals(Triple::class.java, type.getDeclaredMethod("getValueRange").returnType)
        assertEquals(float, type.getDeclaredMethod("yFractionOf", long).returnType)
        assertEquals(List::class.java, type.getDeclaredMethod("calculateYAxisTicks", int).returnType)
    }

    @Test
    fun chartDragManager_keepsPublicMembers() {
        val type = type("ChartDragManager")
        assertNotNull(type.getDeclaredConstructor(List::class.java, type("ChartCanvasManager"), type("ChartValueCalculator")))
        assertEquals(Triple::class.java, type.getDeclaredMethod("findClosestPoint-k-4lQ0M", long).returnType)
        assertEquals(void, type.getDeclaredMethod("updateLastSelectedIndex", int).returnType)
        assertEquals(int, type.getDeclaredMethod("getLastSelectedIndex").returnType)
    }

    @Test
    fun dragGestureListener_keepsAbstractMembers() {
        val type = DragGestureListener::class.java
        assertEquals(void, type.getDeclaredMethod("onDrag-k-4lQ0M", long).returnType)
        assertEquals(void, type.getDeclaredMethod("onDragStart-k-4lQ0M", long).returnType)
        assertEquals(void, type.getDeclaredMethod("onDragEnd").returnType)
        assertEquals(void, DragHandler::class.java.getDeclaredMethod("onDragEnd").returnType)
    }
}
