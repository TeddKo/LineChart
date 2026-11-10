package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {

    private val series = seriesOf(doubles(10.0, 12.0, 11.0, 20.0), volume = doubles(1.0, 2.0, 3.0, 4.0))

    @Test
    fun profileBins_putsEachVolumeInTheBinOfItsClose() {
        val bins = profileBins(series, 0..3, 2)
        assertEquals(10.0, bins.low, 1e-9)
        assertEquals(20.0, bins.high, 1e-9)
        assertEquals(listOf(6.0, 4.0), bins.volumes.toList())
        assertEquals(0, bins.poc)
    }

    @Test
    fun profileBins_usesOnlyTheVisibleRange() {
        val bins = profileBins(series, 2..3, 2)
        assertEquals(11.0, bins.low, 1e-9)
        assertEquals(listOf(3.0, 4.0), bins.volumes.toList())
        assertEquals(1, bins.poc)
    }

    @Test
    fun profileBins_rangeComesFromHighsAndLows() {
        val wide = seriesOf(doubles(10.0, 12.0), high = doubles(14.0, 16.0), low = doubles(6.0, 8.0), volume = doubles(1.0, 1.0))
        val bins = profileBins(wide, 0..1, 4)
        assertEquals(6.0, bins.low, 1e-9)
        assertEquals(16.0, bins.high, 1e-9)
        assertEquals(listOf(0.0, 1.0, 1.0, 0.0), bins.volumes.toList())
    }

    @Test
    fun profileBins_emptyRangeIsEmptyBins() {
        assertTrue(profileBins(series, IntRange.EMPTY, 3).volumes.all { it == 0.0 })
    }

    @Test
    fun profileBins_flatRangeFillsTheFirstBin() {
        val flat = seriesOf(doubles(5.0, 5.0), volume = doubles(2.0, 3.0))
        assertEquals(listOf(5.0, 0.0), profileBins(flat, 0..1, 2).volumes.toList())
    }

    @Test
    fun profileMark_memoizesPerSeriesAndVisibleRange() {
        val mark = ProfileMark(2)
        val first = mark.profile(series, 0..3)
        assertSame(first, mark.profile(series, 0..3))
        assertNotSame(first, mark.profile(series, 1..3))
        assertNotSame(first, mark.profile(seriesOf(doubles(1.0, 2.0)), 0..1))
    }

    @Test
    fun volumeProfile_isOneProfileMarkOutsideThePriceRange() {
        val profile = VolumeProfile(4)
        assertTrue(profile.marks(series, ChartSettings()).single() is ProfileMark)
        assertFalse(profile.inScale)
        assertTrue(profile.needsVolume)
        assertEquals(VolumeProfile(4), VolumeProfile(4))
    }
}
