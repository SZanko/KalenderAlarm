package at.zanko.kalenderwecker.ui.home

import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One day in the home list: its wake-up alarm (if planned) and its appointments. */
data class DaySection(
    val date: LocalDate,
    val alarm: DayAlarm?,
    val appointments: List<Appointment>,
)

fun buildDaySections(
    alarms: List<DayAlarm>,
    appointments: List<Appointment>,
    zone: TimeZone,
    today: LocalDate,
): List<DaySection> {
    val byDate = appointments
        .sortedBy { it.start }
        .groupBy { it.start.toLocalDateTime(zone).date }
    val alarmByDate = alarms.associateBy { it.date }
    return (byDate.keys + alarmByDate.keys)
        .filter { it >= today }
        .sorted()
        .map { date -> DaySection(date, alarmByDate[date], byDate[date].orEmpty()) }
}
