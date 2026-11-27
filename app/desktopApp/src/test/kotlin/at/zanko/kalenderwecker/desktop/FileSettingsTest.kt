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

    @Test
    fun gIVEN_twoProcessesOnSameFile_WHEN_eachWritesAKey_THEN_neitherWriteIsLost() {
        val gui = fileSettings(file)
        val refresher = fileSettings(file)

        refresher.putString("cachedIcs", "NEW")
        gui.putString("overrides", "{}")

        val reopened = fileSettings(file)
        assertEquals("NEW", reopened.getStringOrNull("cachedIcs"))
        assertEquals("{}", reopened.getStringOrNull("overrides"))
    }

    @Test
    fun gIVEN_otherProcessWrote_WHEN_read_THEN_seesLatestValue() {
        val gui = fileSettings(file)
        gui.putString("settings", "old")

        fileSettings(file).putString("settings", "new")

        assertEquals("new", gui.getStringOrNull("settings"))
    }

    @Test
    fun gIVEN_key_WHEN_removed_THEN_goneForOthers() {
        fileSettings(file).putString("lastRefresh", "x")

        fileSettings(file).remove("lastRefresh")

        assertEquals(null, fileSettings(file).getStringOrNull("lastRefresh"))
    }
}
