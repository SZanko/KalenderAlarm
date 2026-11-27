package at.zanko.kalenderwecker.android

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class PlannedAlarmsTest {
    private val monday = LocalDate.parse("2026-11-30")
    private val tuesday = LocalDate.parse("2026-12-01")
    private val mondayAt = Instant.parse("2026-11-30T05:45:00Z")
    private val tuesdayAt = Instant.parse("2026-12-01T07:00:00Z")

    @Test
    fun gIVEN_dayAlarms_WHEN_toPlanned_THEN_onlyDateAndTimeKept() {
        val appt = Appointment("k", "Secret course", "Room 1", "notes", Instant.parse("2026-11-30T07:15:00Z"), null)

        val planned = listOf(DayAlarm(monday, mondayAt, AlarmSource.BeforeAppointment(appt), true)).toPlanned()

        assertEquals(listOf(PlannedAlarm(monday, mondayAt.toEpochMilliseconds())), planned)
    }

    @Test
    fun gIVEN_planned_WHEN_encodeDecode_THEN_roundTripWithoutScheduleDetails() {
        val planned = listOf(PlannedAlarm(monday, mondayAt.toEpochMilliseconds()), PlannedAlarm(tuesday, tuesdayAt.toEpochMilliseconds()))

        val encoded = encodePlanned(planned)

        assertEquals(planned.toSet(), decodePlanned(encoded).toSet())
        assertTrue(encoded.none { "course" in it.lowercase() || "room" in it.lowercase() })
    }

    @Test
    fun gIVEN_garbageEntries_WHEN_decode_THEN_skipped() {
        val good = encodePlanned(listOf(PlannedAlarm(monday, 1L)))

        assertEquals(listOf(PlannedAlarm(monday, 1L)), decodePlanned(good + setOf("nonsense", "2026-13-40|x")))
    }

    @Test
    fun gIVEN_pastAndFutureAlarms_WHEN_toRearm_THEN_onlyFuture() {
        val planned = listOf(PlannedAlarm(monday, mondayAt.toEpochMilliseconds()), PlannedAlarm(tuesday, tuesdayAt.toEpochMilliseconds()))

        val rearm = alarmsToRearm(planned, now = Instant.parse("2026-11-30T12:00:00Z"))

        assertEquals(listOf(tuesday), rearm.map { it.date })
    }

    @Test
    fun gIVEN_lockedPhone_WHEN_lockedRingAllowed_THEN_onlyPlannedDatesOrSnooze() {
        val planned = listOf(PlannedAlarm(monday, mondayAt.toEpochMilliseconds()))

        assertTrue(lockedRingAllowed(planned, monday, force = false))
        assertFalse(lockedRingAllowed(planned, tuesday, force = false), "toggled off / not planned")
        assertTrue(lockedRingAllowed(planned, tuesday, force = true), "snooze")
    }
}
