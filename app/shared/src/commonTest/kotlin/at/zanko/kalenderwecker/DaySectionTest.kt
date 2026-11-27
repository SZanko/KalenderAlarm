package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import at.zanko.kalenderwecker.ui.home.buildDaySections
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DaySectionTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val today = LocalDate.parse("2026-11-02")

    private fun appt(key: String, start: String) =
        Appointment(key, key, null, null, Instant.parse(start), null)

    private fun alarm(date: String, time: String) =
        DayAlarm(LocalDate.parse(date), Instant.parse(time), AlarmSource.Fallback, true)

    @Test
    fun gIVEN_alarmsAndAppointments_WHEN_build_THEN_groupedByLocalDate() {
        val sections = buildDaySections(
            alarms = listOf(alarm("2026-11-02", "2026-11-02T07:00:00Z"), alarm("2026-11-03", "2026-11-03T07:00:00Z")),
            appointments = listOf(
                appt("b", "2026-11-02T12:00:00Z"),
                appt("a", "2026-11-02T07:15:00Z"),
                appt("late-night", "2026-11-02T23:30:00Z"), // 00:30 local on the 3rd
            ),
            zone = zone,
            today = today,
        )

        assertEquals(listOf(LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-03")), sections.map { it.date })
        assertEquals(listOf("a", "b"), sections[0].appointments.map { it.key })
        assertEquals(listOf("late-night"), sections[1].appointments.map { it.key })
        assertEquals(LocalDate.parse("2026-11-03"), sections[1].alarm?.date)
    }

    @Test
    fun gIVEN_appointmentBeyondHorizon_WHEN_build_THEN_listedWithoutAlarm() {
        val sections = buildDaySections(emptyList(), listOf(appt("far", "2026-12-20T08:00:00Z")), zone, today)

        assertEquals(LocalDate.parse("2026-12-20"), sections.single().date)
        assertEquals(null, sections.single().alarm)
    }

    @Test
    fun gIVEN_pastAppointment_WHEN_build_THEN_hidden() {
        val sections = buildDaySections(emptyList(), listOf(appt("old", "2026-11-01T08:00:00Z")), zone, today)

        assertEquals(emptyList(), sections)
    }
}
