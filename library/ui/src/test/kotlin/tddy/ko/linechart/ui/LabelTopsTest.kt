package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LabelTopsTest {

    @Test
    fun labelTops_centersEachLabelOnItsTick() {
        assertEquals(listOf(95f, 45f), labelTops(listOf(100f, 50f), 10f, 0f, 200f).map { it.second })
    }

    @Test
    fun labelTops_skipsALabelThatCollidesWithTheKeptOne() {
        val kept = labelTops(listOf(100f, 96f, 50f), 10f, 0f, 200f)
        assertEquals(listOf(0, 2), kept.map { it.first })
    }

    @Test
    fun labelTops_keepsLabelsInsideTheBounds() {
        assertEquals(listOf(0f, 40f), labelTops(listOf(2f, 99f), 10f, 0f, 50f).map { it.second })
    }

    @Test
    fun labelTops_touchingLabelsAreKept() {
        assertEquals(2, labelTops(listOf(100f, 90f), 10f, 0f, 200f).size)
    }

    @Test
    fun labelTops_shortPaneCollapsesToOneLabel() {
        assertEquals(1, labelTops(listOf(10f, 20f, 30f), 20f, 0f, 30f).size)
    }
}
