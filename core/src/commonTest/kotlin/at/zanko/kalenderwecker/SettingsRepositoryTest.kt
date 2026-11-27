package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmSettings
import at.zanko.kalenderwecker.data.SettingsRepository
import com.russhwolf.settings.MapSettings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {
    private val store = MapSettings()

    @Test
    fun gIVEN_emptyStore_WHEN_read_THEN_defaults() {
        val repo = SettingsRepository(store)

        assertEquals(AlarmSettings(), repo.settings.value)
        assertEquals(emptyMap(), repo.overrides.value)
        assertNull(repo.cachedIcs)
    }

    @Test
    fun gIVEN_update_WHEN_newRepositoryOnSameStore_THEN_persisted() {
        SettingsRepository(store).update {
            it.copy(
                offsetMinutes = 45,
                fallbackTime = LocalTime(7, 30),
                fallbackDays = setOf(DayOfWeek.SATURDAY),
                soundUri = "/music/wake.mp3",
            )
        }

        val reloaded = SettingsRepository(store).settings.value

        assertEquals(45, reloaded.offsetMinutes)
        assertEquals(LocalTime(7, 30), reloaded.fallbackTime)
        assertEquals(setOf(DayOfWeek.SATURDAY), reloaded.fallbackDays)
        assertEquals("/music/wake.mp3", reloaded.soundUri)
    }

    @Test
    fun gIVEN_setEnabled_WHEN_read_THEN_emittedAndPersisted() {
        val repo = SettingsRepository(store)
        val date = LocalDate.parse("2026-11-02")

        repo.setEnabled(date, false)

        assertEquals(mapOf(date to false), repo.overrides.value)
        assertEquals(mapOf(date to false), SettingsRepository(store).overrides.value)
    }

    @Test
    fun gIVEN_cachedIcs_WHEN_set_THEN_persisted() {
        SettingsRepository(store).cachedIcs = "BEGIN:VCALENDAR"

        assertEquals("BEGIN:VCALENDAR", SettingsRepository(store).cachedIcs)
    }

    @Test
    fun gIVEN_corruptJson_WHEN_read_THEN_defaultsWithoutThrowing() {
        store.putString("settings", "{not json")
        store.putString("overrides", "[1,2")

        val repo = SettingsRepository(store)

        assertEquals(AlarmSettings(), repo.settings.value)
        assertEquals(emptyMap(), repo.overrides.value)
    }

    @Test
    fun gIVEN_otherWriterChangedStore_WHEN_reload_THEN_flowsUpdated() {
        val repo = SettingsRepository(store)
        SettingsRepository(store).update { it.copy(offsetMinutes = 10) }

        repo.reload()

        assertEquals(10, repo.settings.value.offsetMinutes)
    }
}
