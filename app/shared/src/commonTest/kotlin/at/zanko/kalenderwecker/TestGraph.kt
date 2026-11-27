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
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

const val TEST_ICS = """BEGIN:VCALENDAR
VERSION:2.0
BEGIN:VEVENT
UID:mon
DTSTART:20261102T071500Z
DTEND:20261102T090000Z
SUMMARY:Data Science
LOCATION:E1423
END:VEVENT
BEGIN:VEVENT
UID:mon-pm
DTSTART:20261102T121500Z
DTEND:20261102T140000Z
SUMMARY:AI Systems
END:VEVENT
BEGIN:VEVENT
UID:far
DTSTART:20261130T071500Z
SUMMARY:Far away
END:VEVENT
END:VCALENDAR"""

class RecordingScheduler : AlarmScheduler {
    val calls = mutableListOf<List<DayAlarm>>()
    override suspend fun schedule(alarms: List<DayAlarm>) {
        calls += alarms
    }
}

class TestClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

/** Real core objects over an in-memory store and a mock HTTP engine. */
class TestGraph(url: String = "https://cloud.timeedit.net/ju/x.ics") {
    val zone = TimeZone.of("Europe/Stockholm")
    var status = HttpStatusCode.OK
    var requests = 0
    val clock = TestClock(Instant.parse("2026-11-01T20:00:00Z"))
    val scheduler = RecordingScheduler()
    val settings = SettingsRepository(MapSettings()).apply { update { it.copy(icsUrl = url) } }
    val calendar = CalendarRepository(
        HttpClient(MockEngine {
            requests++
            if (status == HttpStatusCode.OK) respond(TEST_ICS) else respondError(status)
        }),
        BasicIcsParser(),
        settings,
    ) { zone }
    val syncer = AlarmSyncer(calendar, settings, AlarmPlanner(), scheduler, clock) { zone }
}
