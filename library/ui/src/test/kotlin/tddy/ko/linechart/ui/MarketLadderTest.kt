package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketLadderTest {
    private val levels = listOf(MarketLevel(100, 2), MarketLevel(90, 0), MarketLevel(110, 4), MarketLevel(105, 1))

    @Test
    fun askRowsAscendFromBestAndDropEmptyLevels() {
        assertEquals(listOf(100L, 105L, 110L), ladderRows(levels, MarketSide.Ask, 10).map { it.price })
    }

    @Test
    fun bidRowsDescendFromBest() {
        assertEquals(listOf(110L, 105L, 100L), ladderRows(levels, MarketSide.Bid, 10).map { it.price })
    }

    @Test
    fun rowsAreLimitedToMaxRows() {
        assertEquals(listOf(100L, 105L), ladderRows(levels, MarketSide.Ask, 2).map { it.price })
        assertTrue(ladderRows(levels, MarketSide.Ask, 0).isEmpty())
    }

    @Test
    fun fractionsAreQuantityOverLargestShown() {
        val rows = ladderRows(levels, MarketSide.Ask, 10)
        assertEquals(listOf(0.5f, 0.25f, 1f), ladderFractions(rows))
    }

    @Test
    fun fractionsOfEmptyOrNonPositiveAreZero() {
        assertTrue(ladderFractions(emptyList()).isEmpty())
        assertEquals(listOf(0f, 0f), ladderFractions(listOf(MarketLevel(1, 0), MarketLevel(2, -1))))
    }
}
