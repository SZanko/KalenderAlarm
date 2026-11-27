package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmPlanner
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.RefreshError
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.ics.BasicIcsParser
import com.goncalossilva.resources.Resource
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class AlarmSyncerTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val ultraShort = Resource("TimeEdit_UltraShort.ics").readText()
    private val settings = SettingsRepository(MapSettings()).apply {
        update { it.copy(icsUrl = "https://cloud.timeedit.net/ju/x.ics") }
    }
    private val scheduler = FakeAlarmScheduler()
    private val clock = FixedClock(Instant.parse("2026-11-01T20:00:00Z"))
    private var status = HttpStatusCode.OK

    private val syncer by lazy { AlarmSyncer(
        calendar = CalendarRepository(
            HttpClient(MockEngine {
                if (status == HttpStatusCode.OK) respond(ultraShort) else respondError(status)
            }),
            BasicIcsParser(),
            settings,
        ) { zone },
        settings = settings,
        planner = AlarmPlanner(),
        scheduler = scheduler,
        clock = clock,
        zone = { zone },
    ) }

    private val monday = LocalDate.parse("2026-11-02")

    @Test
    fun gIVEN_plan_WHEN_sync_THEN_onlyEnabledAlarmsScheduled() = runTest {
        val plan = syncer.sync()

        assertTrue(plan.any { !it.enabled }, "weekend fallbacks are disabled by default")
        assertEquals(plan.filter { it.enabled }, scheduler.last)
    }

    @Test
    fun gIVEN_dateToggledOff_WHEN_sync_THEN_dateNotScheduled() = runTest {
        settings.setEnabled(monday, false)

        syncer.sync()

        assertFalse(scheduler.last.any { it.date == monday })
    }

    @Test
    fun gIVEN_refreshOk_WHEN_refreshAndSync_THEN_mondayAlarmBeforeClass() = runTest {
        assertTrue(syncer.refreshAndSync().isSuccess)

        val monday = scheduler.last.single { it.date == monday }
        assertEquals(Instant.parse("2026-11-02T05:45:00Z"), monday.time)
    }

    @Test
    fun gIVEN_refreshFails_WHEN_refreshAndSync_THEN_cachedPlanStillScheduled() = runTest {
        settings.cachedIcs = ultraShort
        status = HttpStatusCode.BadGateway

        val result = syncer.refreshAndSync()

        assertIs<RefreshError.Http>(result.exceptionOrNull())
        assertEquals(Instant.parse("2026-11-02T05:45:00Z"), scheduler.last.single { it.date == monday }.time)
    }

    @Test
    fun gIVEN_date_WHEN_alarmFor_THEN_thatDaysAlarm() {
        assertEquals(monday, syncer.alarmFor(monday)?.date)
        assertEquals(null, syncer.alarmFor(LocalDate.parse("2027-01-01")))
    }

    @Test
    fun gIVEN_overrideChange_WHEN_collectPlan_THEN_reflected() = runTest {
        settings.setEnabled(monday, false)

        assertFalse(syncer.plan.first().single { it.date == monday }.enabled)
    }
}
