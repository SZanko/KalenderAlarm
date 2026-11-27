package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SystemdUnitsTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val date = LocalDate.parse("2026-11-30")
    private val alarm = DayAlarm(date, Instant.parse("2026-11-30T05:45:00Z"), AlarmSource.Fallback, true)

    @Test
    fun gIVEN_date_WHEN_alarmName_THEN_stableName() {
        assertEquals("kalenderwecker-alarm-2026-11-30", SystemdUnits.alarmName(date))
    }

    @Test
    fun gIVEN_alarm_WHEN_alarmTimer_THEN_localCalendarSpec() {
        assertEquals(
            """
            [Unit]
            Description=KalenderWecker wake-up alarm for 2026-11-30

            [Timer]
            OnCalendar=2026-11-30 06:45:00 Europe/Stockholm
            Persistent=true
            AccuracySec=1s
            Unit=kalenderwecker-alarm-2026-11-30.service

            [Install]
            WantedBy=timers.target

            """.trimIndent(),
            SystemdUnits.alarmTimer(alarm, zone),
        )
    }

    @Test
    fun gIVEN_execWithSpaces_WHEN_alarmService_THEN_argsQuoted() {
        val service = SystemdUnits.alarmService(date, listOf("/opt/My App/bin/kw", "-Dx=1"))

        assertTrue("Type=oneshot" in service)
        assertTrue("""ExecStart="/opt/My App/bin/kw" "-Dx=1" "--ring" "2026-11-30"""" in service, service)
    }

    @Test
    fun gIVEN_execWithPercentAndQuote_WHEN_alarmService_THEN_escaped() {
        val service = SystemdUnits.alarmService(date, listOf("/bin/a%b\"c"))

        assertTrue("""ExecStart="/bin/a%%b\"c"""" in service, service)
    }

    @Test
    fun gIVEN_refresh_WHEN_units_THEN_every6hAndAfterBoot() {
        val timer = SystemdUnits.refreshTimer()
        assertTrue("OnBootSec=2min" in timer)
        assertTrue("OnUnitActiveSec=6h" in timer)
        assertTrue(""""--refresh"""" in SystemdUnits.refreshService(listOf("/bin/kw")))
    }

    @Test
    fun gIVEN_guiEnv_WHEN_alarmService_THEN_environmentLinesForWhitelistedVars() {
        val service = SystemdUnits.alarmService(
            date,
            listOf("/bin/kw"),
            mapOf("DISPLAY" to ":0", "WAYLAND_DISPLAY" to "wayland-1", "LD_LIBRARY_PATH" to "/nix/a:/nix/b", "SECRET_TOKEN" to "x"),
        )

        assertTrue("""Environment="DISPLAY=:0"""" in service, service)
        assertTrue("""Environment="WAYLAND_DISPLAY=wayland-1"""" in service, service)
        assertTrue("""Environment="LD_LIBRARY_PATH=/nix/a:/nix/b"""" in service, service)
        assertTrue("SECRET_TOKEN" !in service)
    }
}
