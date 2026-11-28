package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.ui.formatDayHeader
import at.zanko.kalenderwecker.ui.loadFormatTexts
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The real resources resolve for the system language. */
class LocalizedTextsTest {
    private val original = Locale.getDefault()
    private val today = LocalDate.parse("2026-11-02")
    private val friday = LocalDate.parse("2026-11-06")

    @AfterTest
    fun restore() = Locale.setDefault(original)

    private fun header(locale: Locale, date: LocalDate) = runTest {
        Locale.setDefault(locale)
        result = formatDayHeader(date, today, loadFormatTexts())
    }.let { result }

    private var result = ""

    @Test
    fun gIVEN_english_WHEN_dayHeader_THEN_english() {
        assertEquals("Fri 6 Nov", header(Locale.ENGLISH, friday))
        assertEquals("Today", header(Locale.ENGLISH, today))
    }

    @Test
    fun gIVEN_german_WHEN_dayHeader_THEN_german() {
        assertEquals("Fr., 6. Nov.", header(Locale.GERMANY, friday))
        assertEquals("Heute", header(Locale.GERMANY, today))
    }

    @Test
    fun gIVEN_swedish_WHEN_dayHeader_THEN_swedish() {
        assertEquals("fre 6 nov", header(Locale.forLanguageTag("sv-SE"), friday))
        assertEquals("Idag", header(Locale.forLanguageTag("sv-SE"), today))
    }

    @Test
    fun gIVEN_unsupportedLanguage_WHEN_dayHeader_THEN_englishFallback() {
        assertEquals("Fri 6 Nov", header(Locale.FRANCE, friday))
    }
}
