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

fun formatAlarmSubtitle(alarm: DayAlarm, offsetMinutes: Int): String = when (val source = alarm.source) {
    is AlarmSource.BeforeAppointment -> "${formatOffset(offsetMinutes)} before ${displayTitle(source.appointment)}"
    AlarmSource.Fallback -> "Fallback – no morning class"
}

fun formatTime(instant: Instant, zone: TimeZone): String = formatTime(instant.toLocalDateTime(zone).time)

fun formatTime(time: LocalTime): String =
    "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

fun formatDayHeader(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plus(DatePeriod(days = 1)) -> "Tomorrow"
    else -> "${date.dayOfWeek.short()} ${date.day} ${date.month.short()}"
}

fun DayOfWeek.short(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

private fun Month.short(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }
