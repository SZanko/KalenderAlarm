package at.zanko.kalenderwecker.android

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Android-only strings (notification, permission banner) exist in every supported language. */
class AndroidTranslationsTest {
    private val res = File("src/main/res")

    private fun keys(qualifier: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$qualifier/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .map { it.attributes.getNamedItem("name").nodeValue }
            .toSet()
    }

    @Test
    fun gIVEN_germanAndSwedish_WHEN_comparedToEnglish_THEN_sameKeys() {
        val english = keys("values")
        assertTrue("alarm_notification_title" in english)
        assertEquals(english, keys("values-de"), "German")
        assertEquals(english, keys("values-sv"), "Swedish")
    }

    @Test
    fun gIVEN_localeConfig_WHEN_read_THEN_englishIsTheDefault() {
        assertEquals("unqualifiedResLocale=en", File(res, "resources.properties").readText().trim())
    }
}
