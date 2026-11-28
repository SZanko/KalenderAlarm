package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmPlanner
import at.zanko.kalenderwecker.alarm.AlarmSettings
import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class AlarmPlannerTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val planner = AlarmPlanner()

    /** Sunday 2026-11-01 21:00 local. */
    private val sundayEvening = Instant.parse("2026-11-01T20:00:00Z")

    private fun local(iso: String) = LocalDateTime.parse(iso).toInstant(zone)

    private fun appt(key: String, localStart: String) =
        Appointment(key, "Class $key", null, null, local(localStart), null)

    private fun plan(
        appointments: List<Appointment>,
        settings: AlarmSettings = AlarmSettings(),
        overrides: Map<LocalDate, Boolean> = emptyMap(),
        now: Instant = sundayEvening,
    ) = planner.plan(appointments, settings, overrides, now, zone)

    private fun List<DayAlarm>.on(date: String) = single { it.date == LocalDate.parse(date) }

    private fun DayAlarm.localTime() = time.toLocalDateTime(zone).toString()

    @Test
    fun gIVEN_morningClass_WHEN_plan_THEN_alarmOffsetBeforeStart() {
        val alarm = plan(listOf(appt("a", "2026-11-02T08:15"))).on("2026-11-02")

        assertEquals("2026-11-02T06:45", alarm.localTime())
        assertIs<AlarmSource.BeforeAppointment>(alarm.source)
        assertEquals("a", (alarm.source as AlarmSource.BeforeAppointment).appointment.key)
        assertTrue(alarm.enabled)
    }

    @Test
    fun gIVEN_twoMorningClasses_WHEN_plan_THEN_usesEarliest() {
        val alarm = plan(
            listOf(appt("late", "2026-11-02T10:15"), appt("early", "2026-11-02T08:15"))
        ).on("2026-11-02")

        assertEquals("2026-11-02T06:45", alarm.localTime())
        assertEquals("early", (alarm.source as AlarmSource.BeforeAppointment).appointment.key)
    }

    @Test
    fun gIVEN_onlyAfternoonClass_WHEN_plan_THEN_fallback() {
        val alarm = plan(listOf(appt("a", "2026-11-02T13:15"))).on("2026-11-02")

        assertEquals(AlarmSource.Fallback, alarm.source)
        assertEquals("2026-11-02T08:00", alarm.localTime())
        assertTrue(alarm.enabled)
    }

    @Test
    fun gIVEN_classExactlyAtMorningEnd_WHEN_plan_THEN_fallback() {
        val alarm = plan(listOf(appt("a", "2026-11-02T12:00"))).on("2026-11-02")

        assertEquals(AlarmSource.Fallback, alarm.source)
    }

    @Test
    fun gIVEN_customOffset_WHEN_plan_THEN_offsetApplied() {
        val alarm = plan(
            listOf(appt("a", "2026-11-02T08:15")),
            settings = AlarmSettings(offsetMinutes = 30),
        ).on("2026-11-02")

        assertEquals("2026-11-02T07:45", alarm.localTime())
    }

    @Test
    fun gIVEN_saturdayNoClass_WHEN_plan_THEN_fallbackDisabledByDefault() {
        val alarm = plan(emptyList()).on("2026-11-07")

        assertEquals(AlarmSource.Fallback, alarm.source)
        assertFalse(alarm.enabled)
    }

    @Test
    fun gIVEN_saturdayMorningClass_WHEN_plan_THEN_enabled() {
        val alarm = plan(listOf(appt("a", "2026-11-07T09:00"))).on("2026-11-07")

        assertTrue(alarm.enabled)
    }

    @Test
    fun gIVEN_override_WHEN_plan_THEN_overrideWins() {
        val alarms = plan(
            listOf(appt("a", "2026-11-02T08:15")),
            overrides = mapOf(
                LocalDate.parse("2026-11-02") to false,
                LocalDate.parse("2026-11-07") to true,
            ),
        )

        assertFalse(alarms.on("2026-11-02").enabled)
        assertTrue(alarms.on("2026-11-07").enabled)
    }

    @Test
    fun gIVEN_horizon14_WHEN_plan_THEN_14Days() {
        val earlySunday = Instant.parse("2026-11-01T05:00:00Z") // 06:00 local

        val alarms = plan(emptyList(), now = earlySunday)

        assertEquals(14, alarms.size)
        assertEquals(LocalDate.parse("2026-11-01"), alarms.first().date)
        assertEquals(LocalDate.parse("2026-11-14"), alarms.last().date)
    }

    @Test
    fun gIVEN_todaysAlarmPassed_WHEN_plan_THEN_todayDropped() {
        val alarms = plan(emptyList())

        assertEquals(LocalDate.parse("2026-11-02"), alarms.first().date)
        assertEquals(13, alarms.size)
    }

    @Test
    fun gIVEN_offsetCrossesMidnight_WHEN_plan_THEN_dateIsClassDate() {
        val alarm = plan(listOf(appt("a", "2026-11-03T00:30"))).on("2026-11-03")

        assertEquals("2026-11-02T23:00", alarm.localTime())
    }

    @Test
    fun gIVEN_dstSpringForward_WHEN_plan_THEN_localWallTimeKept() {
        val now = Instant.parse("2026-03-28T20:00:00Z")

        val alarm = plan(listOf(appt("a", "2026-03-30T08:15")), now = now).on("2026-03-30")

        assertEquals(Instant.parse("2026-03-30T04:45:00Z"), alarm.time)
    }

    @Test
    fun gIVEN_dstFallBack_WHEN_plan_THEN_localWallTimeKept() {
        val now = Instant.parse("2026-10-24T20:00:00Z")

        val alarm = plan(listOf(appt("a", "2026-10-26T08:15")), now = now).on("2026-10-26")

        assertEquals(Instant.parse("2026-10-26T05:45:00Z"), alarm.time)
    }

    @Test
    fun gIVEN_fallbackOnDstDay_WHEN_plan_THEN_eightLocal() {
        val now = Instant.parse("2026-03-28T20:00:00Z")

        val alarm = plan(emptyList(), now = now).on("2026-03-30")

        assertEquals("2026-03-30T08:00", alarm.localTime())
    }
}
