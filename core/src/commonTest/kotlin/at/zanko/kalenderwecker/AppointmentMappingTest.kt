package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.ics.BasicIcsParser
import at.zanko.kalenderwecker.model.toAppointment
import at.zanko.kalenderwecker.model.toAppointments
import com.goncalossilva.resources.Resource
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class AppointmentMappingTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val parser = BasicIcsParser()

    private fun calendar(vararg events: String) = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        events.forEach { appendLine(it.trimIndent()) }
        appendLine("END:VCALENDAR")
    }

    @Test
    fun gIVEN_utcEvent_WHEN_toAppointment_THEN_startIsInstant() {
        val events = parser.parse(Resource("TimeEdit_UltraShort.ics").readText())

        val first = events.toAppointments(zone).first()

        assertEquals(Instant.parse("2026-11-02T07:15:00Z"), first.start)
        assertEquals(Instant.parse("2026-11-02T09:00:00Z"), first.end)
        assertEquals("Kurs: TDSR22.  : T4209.  : Data Science", first.title)
        assertEquals("E1423 (Fagerhultsaulan)", first.location)
    }

    @Test
    fun gIVEN_cancelledEvent_WHEN_toAppointment_THEN_null() {
        val event = parser.parse(
            calendar(
                """
                BEGIN:VEVENT
                UID:1
                DTSTART:20261102T071500Z
                STATUS:CANCELLED
                END:VEVENT
                """
            )
        ).single()

        assertNull(event.toAppointment(zone))
    }

    @Test
    fun gIVEN_allDayEvent_WHEN_toAppointment_THEN_null() {
        val event = parser.parse(
            calendar(
                """
                BEGIN:VEVENT
                UID:1
                DTSTART;VALUE=DATE:20261102
                END:VEVENT
                """
            )
        ).single()

        assertNull(event.toAppointment(zone))
    }

    @Test
    fun gIVEN_floatingEvent_WHEN_toAppointment_THEN_interpretedInZone() {
        val event = parser.parse(
            calendar(
                """
                BEGIN:VEVENT
                UID:1
                DTSTART:20261102T081500
                END:VEVENT
                """
            )
        ).single()

        assertEquals(Instant.parse("2026-11-02T07:15:00Z"), event.toAppointment(zone)?.start)
    }

    @Test
    fun gIVEN_unknownTzid_WHEN_toAppointment_THEN_fallsBackToZone() {
        val event = parser.parse(
            calendar(
                """
                BEGIN:VEVENT
                UID:1
                DTSTART;TZID=W. Europe Standard Time:20261102T081500
                END:VEVENT
                """
            )
        ).single()

        assertEquals(Instant.parse("2026-11-02T07:15:00Z"), event.toAppointment(zone)?.start)
    }

    @Test
    fun gIVEN_unsortedEvents_WHEN_toAppointments_THEN_sortedByStart() {
        val events = parser.parse(
            calendar(
                """
                BEGIN:VEVENT
                UID:late
                DTSTART:20261103T071500Z
                END:VEVENT
                """,
                """
                BEGIN:VEVENT
                UID:early
                DTSTART:20261102T071500Z
                END:VEVENT
                """
            )
        )

        assertEquals(listOf("early|", "late|"), events.toAppointments(zone).map { it.key })
    }

    @Test
    fun gIVEN_parserClass_WHEN_parse_THEN_sameAsFunction() {
        val events = parser.parse(Resource("TimeEdit_UltraShort.ics").readText())

        assertEquals(3, events.size)
    }
}
