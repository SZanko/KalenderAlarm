package at.zanko.kalenderwecker.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class FileSettingsTest {
    private val file = Files.createTempDirectory("kw").resolve("sub/settings.properties")

    @Test
    fun gIVEN_largeValue_WHEN_storedAndReopened_THEN_roundTrips() {
        val big = "BEGIN:VCALENDAR\n" + "x".repeat(200_000)

        fileSettings(file).putString("cachedIcs", big)

        assertEquals(big, fileSettings(file).getStringOrNull("cachedIcs"))
    }

    @Test
    fun gIVEN_missingFile_WHEN_open_THEN_empty() {
        assertEquals(null, fileSettings(file).getStringOrNull("settings"))
    }
}
