package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.data.normalizeTimeEditUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeEditUrlTest {
    private val ics = "https://cloud.timeedit.net/ju/web/open/ri6Y1y6tY1vZ2QQZ51nYQZ0Qumw5wXd5jZ12Z15Q807n82jQ79m6l9A414Q0299969842AED912E265Z30F9FtAEF1055B9BABC95.ics"

    @Test
    fun gIVEN_icsUrl_WHEN_normalize_THEN_unchanged() {
        assertEquals(ics, normalizeTimeEditUrl(ics))
    }

    @Test
    fun gIVEN_htmlUrl_WHEN_normalize_THEN_icsUrl() {
        assertEquals(ics, normalizeTimeEditUrl(ics.removeSuffix(".ics") + ".html"))
    }

    @Test
    fun gIVEN_webcalUrl_WHEN_normalize_THEN_https() {
        assertEquals(ics, normalizeTimeEditUrl("webcal://" + ics.removePrefix("https://")))
    }

    @Test
    fun gIVEN_whitespace_WHEN_normalize_THEN_trimmed() {
        assertEquals(ics, normalizeTimeEditUrl("  $ics\n"))
    }

    @Test
    fun gIVEN_garbage_WHEN_normalize_THEN_null() {
        assertNull(normalizeTimeEditUrl("hello"))
        assertNull(normalizeTimeEditUrl(""))
        assertNull(normalizeTimeEditUrl("ftp://example.com/a.ics"))
        assertNull(normalizeTimeEditUrl("https://example.com/page"))
    }
}
