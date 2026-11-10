package tddy.ko.linechart.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class IndicatorNamesTest {

    private fun names(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("name").nodeValue.startsWith("linechart_name_") }
            .associate { it.attributes.getNamedItem("name").nodeValue.removePrefix("linechart_name_") to it.textContent }
    }

    @Test
    fun everyIndicatorHasAnEnglishAndAKoreanName() {
        val english = names("values")
        val korean = names("values-ko")
        assertEquals(english.keys, korean.keys)
        for (key in listOf("bb", "env", "pc", "ichimoku", "sar", "pivot", "vwap", "vp", "macd", "macdo", "trix", "sonar", "rsi", "stochf", "stochs", "willr", "mfi", "psy", "cci", "dmi", "atr", "tv", "obv", "vr", "vo", "mom", "roc", "disp")) {
            assertTrue(key, english[key].orEmpty().isNotEmpty() && korean[key].orEmpty().isNotEmpty())
        }
    }

    @Test
    fun everyMarkLabelHasASharedName() {
        val keys = names("values").keys
        val series = seriesOf(DoubleArray(130) { 100.0 + (it * 7) % 11 }, DoubleArray(130) { 102.0 + (it * 7) % 11 }, DoubleArray(130) { 98.0 + (it * 7) % 11 })
        val labels = allIndicators.flatMap { it.marks(series, ChartSettings()) }.lines().map { it.label }.filter { it.isNotEmpty() }.toSet()
        val unnamed = labels.filter { it !in keys && !it.startsWith("MA") && !it.startsWith("%") && !it.startsWith("+") && !it.startsWith("-") && it != "ADX" && !Regex("P|R\\d|S\\d|\\d+").matches(it) }
        assertEquals(emptyList<String>(), unnamed)
    }
}
