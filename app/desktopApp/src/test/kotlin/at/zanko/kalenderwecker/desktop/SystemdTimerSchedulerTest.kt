package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class SystemdTimerSchedulerTest {
    private val dir = Files.createTempDirectory("units")
    private val commands = mutableListOf<List<String>>()
    private val scheduler = SystemdTimerScheduler(
        unitDir = dir,
        runner = { cmd -> commands += cmd.toList(); 0 },
        exec = listOf("/bin/kw"),
        zone = { TimeZone.of("Europe/Stockholm") },
    )

    private fun alarm(date: String) = DayAlarm(
        LocalDate.parse(date), Instant.parse("${date}T05:45:00Z"), AlarmSource.Fallback, true,
    )

    @Test
    fun gIVEN_twoAlarms_WHEN_schedule_THEN_unitsWrittenAndEnabled() = runTest {
        scheduler.schedule(listOf(alarm("2026-11-30"), alarm("2026-12-01")))

        assertEquals(
            setOf(
                "kalenderwecker-alarm-2026-11-30.timer", "kalenderwecker-alarm-2026-11-30.service",
                "kalenderwecker-alarm-2026-12-01.timer", "kalenderwecker-alarm-2026-12-01.service",
                "kalenderwecker-refresh.timer", "kalenderwecker-refresh.service",
            ),
            dir.listDirectoryEntries().map { it.name }.toSet(),
        )
        assertTrue(listOf("systemctl", "--user", "daemon-reload") in commands)
        val enable = commands.single { it.take(4) == listOf("systemctl", "--user", "enable", "--now") }
        assertEquals(
            setOf("kalenderwecker-alarm-2026-11-30.timer", "kalenderwecker-alarm-2026-12-01.timer", "kalenderwecker-refresh.timer"),
            enable.drop(4).toSet(),
        )
        assertTrue(commands.indexOf(listOf("systemctl", "--user", "daemon-reload")) < commands.indexOf(enable))
    }

    @Test
    fun gIVEN_alarmRemoved_WHEN_reschedule_THEN_staleUnitsDisabledAndDeleted() = runTest {
        scheduler.schedule(listOf(alarm("2026-11-30"), alarm("2026-12-01")))
        commands.clear()

        scheduler.schedule(listOf(alarm("2026-12-01")))

        assertFalse(dir.resolve("kalenderwecker-alarm-2026-11-30.timer").exists())
        assertFalse(dir.resolve("kalenderwecker-alarm-2026-11-30.service").exists())
        assertTrue(dir.resolve("kalenderwecker-alarm-2026-12-01.timer").exists())
        assertTrue(
            listOf("systemctl", "--user", "disable", "--now", "kalenderwecker-alarm-2026-11-30.timer") in commands
        )
    }

    @Test
    fun gIVEN_foreignUnit_WHEN_schedule_THEN_untouched() = runTest {
        Files.writeString(dir.resolve("other.timer"), "x")

        scheduler.schedule(emptyList())

        assertTrue(dir.resolve("other.timer").exists())
    }

    @Test
    fun gIVEN_missingDir_WHEN_schedule_THEN_created() = runTest {
        val nested = dir.resolve("a/b")
        SystemdTimerScheduler(nested, { 0 }, listOf("/bin/kw")) { TimeZone.UTC }.schedule(listOf(alarm("2026-11-30")))

        assertTrue(nested.resolve("kalenderwecker-alarm-2026-11-30.timer").exists())
    }

    @Test
    fun gIVEN_enableFails_WHEN_schedule_THEN_throwsWithCommand() = runTest {
        val failing = SystemdTimerScheduler(dir, { cmd -> if ("enable" in cmd) 1 else 0 }, listOf("/bin/kw")) {
            TimeZone.UTC
        }

        val error = kotlin.test.assertFailsWith<SchedulingException> { failing.schedule(listOf(alarm("2026-11-30"))) }
        assertTrue("enable" in error.message.orEmpty())
    }
}
