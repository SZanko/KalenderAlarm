package at.zanko.kalenderwecker.ui

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun formatOffset(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

/** TimeEdit summaries look like `Kurs: TDSR22.  : T4209.  : Data Science`; keep the course name. */
fun displayTitle(appointment: Appointment): String =
    appointment.title.split(" : ").last().trim().ifEmpty { appointment.title }

/**
 * Localized words and templates the formatters need. Templates use positional
 * placeholders (`%1$s`, `%2$d`) so each language can order them freely.
 */
data class FormatTexts(
    val today: String,
    val tomorrow: String,
    /** Monday..Sunday. */
    val weekdaysShort: List<String>,
    /** January..December. */
    val monthsShort: List<String>,
    /** %1$s weekday, %2$d day of month, %3$s month. */
    val dayHeaderFormat: String,
    /** %1$s offset, %2$s class name. */
    val alarmBefore: String,
    val alarmFallback: String,
)

private val PLACEHOLDER = Regex("""%(\d+)\$[sd]|%%""")

/** Fills `%N$s` / `%N$d` placeholders with the N-th argument; `%%` becomes `%`. */
fun fillTemplate(template: String, vararg args: Any): String =
    PLACEHOLDER.replace(template) { m ->
        if (m.value == "%%") "%" else args[m.groupValues[1].toInt() - 1].toString()
    }

fun formatAlarmSubtitle(alarm: DayAlarm, offsetMinutes: Int, texts: FormatTexts): String =
    when (val source = alarm.source) {
        is AlarmSource.BeforeAppointment ->
            fillTemplate(texts.alarmBefore, formatOffset(offsetMinutes), displayTitle(source.appointment))
        AlarmSource.Fallback -> texts.alarmFallback
    }

fun formatTime(instant: Instant, zone: TimeZone): String = formatTime(instant.toLocalDateTime(zone).time)

fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun formatDayHeader(date: LocalDate, today: LocalDate, texts: FormatTexts): String = when (date) {
    today -> texts.today
    today.plus(DatePeriod(days = 1)) -> texts.tomorrow
    else -> fillTemplate(texts.dayHeaderFormat, date.dayOfWeek.short(texts), date.day, date.month.short(texts))
}

fun DayOfWeek.short(texts: FormatTexts): String = texts.weekdaysShort[ordinal]

private fun Month.short(texts: FormatTexts): String = texts.monthsShort[ordinal]
