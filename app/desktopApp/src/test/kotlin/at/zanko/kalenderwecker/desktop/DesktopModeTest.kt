package at.zanko.kalenderwecker.desktop

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopModeTest {
    @Test
    fun gIVEN_noArgs_WHEN_parse_THEN_main() {
        assertEquals(DesktopMode.Main, parseArgs(emptyArray()))
    }

    @Test
    fun gIVEN_ring_WHEN_parse_THEN_ringWithDate() {
        assertEquals(DesktopMode.Ring(LocalDate.parse("2026-11-30"), force = false), parseArgs(arrayOf("--ring", "2026-11-30")))
        assertEquals(DesktopMode.Ring(LocalDate.parse("2026-11-30"), force = true), parseArgs(arrayOf("--ring", "2026-11-30", "--force")))
    }

    @Test
    fun gIVEN_refresh_WHEN_parse_THEN_refresh() {
        assertEquals(DesktopMode.Refresh, parseArgs(arrayOf("--refresh")))
    }

    @Test
    fun gIVEN_ringWithoutValidDate_WHEN_parse_THEN_main() {
        assertEquals(DesktopMode.Main, parseArgs(arrayOf("--ring")))
        assertEquals(DesktopMode.Main, parseArgs(arrayOf("--ring", "tomorrow")))
    }
}
