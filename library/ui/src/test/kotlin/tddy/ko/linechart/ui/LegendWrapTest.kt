package tddy.ko.linechart.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class LegendWrapTest {

    private val width: (AnnotatedString) -> Float = { it.length.toFloat() }

    private fun lines(text: String, max: Float) = wrapLegend(AnnotatedString(text), max, width).map { it.text }

    @Test
    fun wrapLegend_fitsOnOneLine() {
        assertEquals(listOf("aa  bb  "), lines("aa  bb  ", 8f))
    }

    @Test
    fun wrapLegend_breaksBeforeTheItemThatOverflows() {
        assertEquals(listOf("aaa  bbb  ", "ccc  "), lines("aaa  bbb  ccc  ", 10f))
    }

    @Test
    fun wrapLegend_keepsAnItemWiderThanTheWidthOnItsOwnLine() {
        assertEquals(listOf("aa  ", "bbbbbbbbbb  ", "cc  "), lines("aa  bbbbbbbbbb  cc  ", 6f))
    }

    @Test
    fun wrapLegend_emptyTextIsOneEmptyLine() {
        assertEquals(listOf(""), lines("", 5f))
    }

    @Test
    fun wrapLegend_keepsSpanStylesOfTheSplitItems() {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.Red)) { append("aaa  ") }
            withStyle(SpanStyle(color = Color.Blue)) { append("bbb  ") }
        }
        val wrapped = wrapLegend(text, 5f, width)
        assertEquals(2, wrapped.size)
        assertEquals(Color.Blue, wrapped[1].spanStyles.single().item.color)
    }
}
