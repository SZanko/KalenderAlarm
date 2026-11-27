package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmPlanner
import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.ics.BasicIcsParser
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AlarmSyncerReliabilityTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val monday = LocalDate.parse("2026-11-02")
    private val saturday = LocalDate.parse("2026-11-07")
    private val clock = FixedClock(Instant.parse("2026-11-01T20:00:00Z"))
    private val store = MapSettings()
    private val url = "https://cloud.timeedit.net/ju/x.ics"

    private val mondayIcs = """
        BEGIN:VCALENDAR
        BEGIN:VEVENT
        UID:mon
        DTSTART:20261102T071500Z
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()

    private val withSaturdayExam = mondayIcs.replace(
        "END:VCALENDAR",
        "BEGIN:VEVENT\nUID:exam\nDTSTART:20261107T080000Z\nEND:VEVENT\nEND:VCALENDAR",
    )

    /** One "process": its own repositories over the shared [store]. */
    private fun process(scheduler: AlarmScheduler, body: String = mondayIcs): Pair<AlarmSyncer, SettingsRepository> {
        val settings = SettingsRepository(store)
        val calendar = CalendarRepository(HttpClient(MockEngine { respond(body) }), BasicIcsParser(), settings) { zone }
        return AlarmSyncer(calendar, settings, AlarmPlanner(), scheduler, clock) { zone } to settings
    }

    @Test
    fun gIVEN_slowFirstSync_WHEN_toggleAndSyncAgain_THEN_newestPlanWins() = runTest {
        store.putString("cachedIcs", mondayIcs)
        val gate = CompletableDeferred<Unit>()
        val completed = mutableListOf<List<DayAlarm>>()
        var first = true
        val scheduler = object : AlarmScheduler {
            override suspend fun schedule(alarms: List<DayAlarm>) {
                if (first) { first = false; gate.await() }
                completed += alarms
            }
        }
        val (syncer, settings) = process(scheduler)

        launch { syncer.sync() }
        advanceUntilIdle()
        settings.setEnabled(monday, false)
        launch { syncer.sync() }
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(completed.last().any { it.date == monday }, "the toggle-off must be the last schedule applied")
    }

    @Test
    fun gIVEN_otherProcessRefreshed_WHEN_sync_THEN_usesFreshScheduleAndSettings() = runTest {
        store.putString("cachedIcs", mondayIcs)
        val guiScheduler = FakeAlarmScheduler()
        val (gui, _) = process(guiScheduler)
        val (refresher, refresherSettings) = process(FakeAlarmScheduler(), body = withSaturdayExam)
        refresherSettings.update { it.copy(icsUrl = url) }

        refresher.refreshAndSync()
        gui.sync()

        val sat = guiScheduler.last.singleOrNull { it.date == saturday }
        assertNotNull(sat, "GUI must see the exam the refresh process fetched")
        assertEquals(Instant.parse("2026-11-07T06:30:00Z"), sat.time)
    }

    @Test
    fun gIVEN_schedulerFails_WHEN_sync_THEN_errorExposedNotThrown_andClearedOnSuccess() = runTest {
        var fail = true
        val scheduler = object : AlarmScheduler {
            override suspend fun schedule(alarms: List<DayAlarm>) {
                if (fail) error("systemctl enable failed")
            }
        }
        val (syncer, _) = process(scheduler)

        syncer.sync()
        assertTrue(syncer.schedulingError.value?.contains("systemctl enable failed") == true)

        fail = false
        syncer.sync()
        assertNull(syncer.schedulingError.value)
    }

    @Test
    fun gIVEN_successfulRefresh_WHEN_refreshAndSync_THEN_lastRefreshRecorded() = runTest {
        val (syncer, settings) = process(FakeAlarmScheduler())
        settings.update { it.copy(icsUrl = url) }
        assertNull(settings.lastRefresh)

        syncer.refreshAndSync()

        assertEquals(clock.now, settings.lastRefresh)
    }
}
