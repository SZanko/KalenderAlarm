package at.zanko.kalenderwecker

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every language must define every English string with the same placeholders. */
class TranslationsTest {
    private val dir = File("src/commonMain/composeResources")

    @Test
    fun gIVEN_germanAndSwedish_WHEN_comparedToEnglish_THEN_sameKeysAndPlaceholders() {
        assertSameAsEnglish(dir, listOf("de", "sv"))
    }

    @Test
    fun gIVEN_dateArrays_WHEN_read_THEN_sevenWeekdaysAndTwelveMonths() {
        for (q in listOf("values", "values-de", "values-sv")) {
            val arrays = readResources(File(dir, "$q/strings.xml")).arrays
            assertEquals(7, arrays.getValue("weekdays_short").size, "$q weekdays")
            assertEquals(12, arrays.getValue("months_short").size, "$q months")
        }
    }
}

data class ResourceFile(val strings: Map<String, String>, val arrays: Map<String, List<String>>)

fun readResources(file: File): ResourceFile {
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val strings = doc.getElementsByTagName("string").let { nodes ->
        (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }
    val arrays = doc.getElementsByTagName("string-array").let { nodes ->
        (0 until nodes.length).map { nodes.item(it) }.associate { node ->
            val items = node.childNodes.let { c -> (0 until c.length).map { c.item(it) }.filter { it.nodeName == "item" } }
            node.attributes.getNamedItem("name").nodeValue to items.map { it.textContent }
        }
    }
    return ResourceFile(strings, arrays)
}

private val PLACEHOLDER = Regex("""%\d+\$[sd]""")

fun assertSameAsEnglish(resDir: File, languages: List<String>) {
    val english = readResources(File(resDir, "values/strings.xml"))
    assertTrue(english.strings.isNotEmpty(), "no English strings in $resDir")
    for (lang in languages) {
        val other = readResources(File(resDir, "values-$lang/strings.xml"))
        assertEquals(english.strings.keys, other.strings.keys, "$lang string keys")
        assertEquals(english.arrays.keys, other.arrays.keys, "$lang array keys")
        for ((key, text) in english.strings) {
            assertEquals(
                PLACEHOLDER.findAll(text).map { it.value }.toSet(),
                PLACEHOLDER.findAll(other.strings.getValue(key)).map { it.value }.toSet(),
                "$lang placeholders of $key",
            )
        }
    }
}
