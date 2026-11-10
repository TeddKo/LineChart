package tddy.ko.linechart.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourceNamesTest {

    @Test
    fun everyResourceNameCarriesThePrefix() {
        val names = File("src/main/res").walk().filter { it.extension == "xml" }
            .flatMap { Regex("""<string name="([^"]+)"""").findAll(it.readText()).map { m -> m.groupValues[1] } }.toList()
        assertEquals(emptyList<String>(), names.filterNot { it.startsWith("linechart_") })
    }
}
