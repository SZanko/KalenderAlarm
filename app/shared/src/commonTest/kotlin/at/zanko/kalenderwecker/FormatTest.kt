package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import at.zanko.kalenderwecker.ui.FormatTexts
import at.zanko.kalenderwecker.ui.displayTitle
import at.zanko.kalenderwecker.ui.fillTemplate
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.formatDayHeader
import at.zanko.kalenderwecker.ui.formatOffset
import at.zanko.kalenderwecker.ui.formatTime
import at.zanko.kalenderwecker.ui.short
import kotlinx.datetime.DayOfWeek
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

    /** German-shaped fixture: the formatters must take word order and punctuation from the texts. */
    private val german = FormatTexts(
        today = "Heute",
        tomorrow = "Morgen",
        weekdaysShort = listOf("Mo.", "Di.", "Mi.", "Do.", "Fr.", "Sa.", "So."),
        monthsShort = listOf("Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez."),
        dayHeaderFormat = "%1\$s, %2\$d. %3\$s",
        alarmBefore = "%1\$s vor %2\$s",
        alarmFallback = "Ersatzwecker – kein Kurs am Morgen",
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
    fun gIVEN_alarms_WHEN_formatAlarmSubtitle_THEN_usesLocalizedTemplate() {
        val before = DayAlarm(LocalDate.parse("2026-11-02"), appt.start, AlarmSource.BeforeAppointment(appt), true)
        val fallback = before.copy(source = AlarmSource.Fallback)

        assertEquals("1 h 30 min vor Data Science", formatAlarmSubtitle(before, 90, german))
        assertEquals("Ersatzwecker – kein Kurs am Morgen", formatAlarmSubtitle(fallback, 90, german))
    }

    @Test
    fun gIVEN_instant_WHEN_formatTime_THEN_localHourMinute() {
        assertEquals("08:15", formatTime(appt.start, zone))
    }

    @Test
    fun gIVEN_dates_WHEN_formatDayHeader_THEN_relativeOrLocalizedDate() {
        val today = LocalDate.parse("2026-11-02")
        assertEquals("Heute", formatDayHeader(today, today, german))
        assertEquals("Morgen", formatDayHeader(LocalDate.parse("2026-11-03"), today, german))
        assertEquals("Fr., 6. Nov.", formatDayHeader(LocalDate.parse("2026-11-06"), today, german))
        assertEquals("So., 1. März", formatDayHeader(LocalDate.parse("2026-03-01"), today, german))
    }

    @Test
    fun gIVEN_dayOfWeek_WHEN_short_THEN_localizedAbbreviation() {
        assertEquals("Mo.", DayOfWeek.MONDAY.short(german))
        assertEquals("So.", DayOfWeek.SUNDAY.short(german))
    }

    @Test
    fun gIVEN_positionalTemplate_WHEN_fill_THEN_argsInTemplateOrder() {
        assertEquals("b a 3", fillTemplate("%2\$s %1\$s %3\$d", "a", "b", 3))
        assertEquals("100 %", fillTemplate("%1\$d %%", 100))
    }
}
