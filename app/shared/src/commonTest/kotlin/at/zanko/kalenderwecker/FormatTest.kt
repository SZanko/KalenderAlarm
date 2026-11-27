package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import at.zanko.kalenderwecker.ui.displayTitle
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.formatDayHeader
import at.zanko.kalenderwecker.ui.formatOffset
import at.zanko.kalenderwecker.ui.formatTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class FormatTest {
    private val zone = TimeZone.of("Europe/Stockholm")
    private val appt = Appointment(
        "k", "Kurs: TDSR22.  : T4209.  : Data Science", null, null,
        Instant.parse("2026-11-02T07:15:00Z"), null,
    )

    @Test
    fun gIVEN_minutes_WHEN_formatOffset_THEN_humanReadable() {
        assertEquals("1 h 30 min", formatOffset(90))
        assertEquals("45 min", formatOffset(45))
        assertEquals("2 h", formatOffset(120))
        assertEquals("0 min", formatOffset(0))
    }

    @Test
    fun gIVEN_timeEditSummary_WHEN_displayTitle_THEN_courseName() {
        assertEquals("Data Science", displayTitle(appt))
        assertEquals("Plain title", displayTitle(appt.copy(title = "Plain title")))
    }

    @Test
    fun gIVEN_alarms_WHEN_formatAlarmSubtitle_THEN_describesSource() {
        val before = DayAlarm(LocalDate.parse("2026-11-02"), appt.start, AlarmSource.BeforeAppointment(appt), true)
        val fallback = before.copy(source = AlarmSource.Fallback)

        assertEquals("1 h 30 min before Data Science", formatAlarmSubtitle(before, 90))
        assertEquals("Fallback – no morning class", formatAlarmSubtitle(fallback, 90))
    }

    @Test
    fun gIVEN_instant_WHEN_formatTime_THEN_localHourMinute() {
        assertEquals("08:15", formatTime(appt.start, zone))
    }

    @Test
    fun gIVEN_dates_WHEN_formatDayHeader_THEN_relativeOrWeekday() {
        val today = LocalDate.parse("2026-11-02")
        assertEquals("Today", formatDayHeader(today, today))
        assertEquals("Tomorrow", formatDayHeader(LocalDate.parse("2026-11-03"), today))
        assertEquals("Fri 6 Nov", formatDayHeader(LocalDate.parse("2026-11-06"), today))
    }
}
