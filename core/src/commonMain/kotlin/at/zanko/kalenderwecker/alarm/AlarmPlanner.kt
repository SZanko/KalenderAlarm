package at.zanko.kalenderwecker.alarm

import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Pure rules that turn a schedule into one wake-up alarm per day. */
class AlarmPlanner {
    fun plan(
        appointments: List<Appointment>,
        settings: AlarmSettings,
        overrides: Map<LocalDate, Boolean>,
        now: Instant,
        zone: TimeZone,
    ): List<DayAlarm> {
        val today = now.toLocalDateTime(zone).date
        val firstMorning: Map<LocalDate, Appointment> = appointments
            .map { it to it.start.toLocalDateTime(zone) }
            .filter { (_, start) -> start.time < settings.morningEnd }
            .groupBy({ (_, start) -> start.date }, { (appointment, _) -> appointment })
            .mapValues { (_, list) -> list.minBy { it.start } }

        return (0 until settings.horizonDays)
            .map { today.plus(DatePeriod(days = it)) }
            .map { date -> alarmFor(date, firstMorning[date], settings, overrides, zone) }
            .filter { it.time > now }
    }

    private fun alarmFor(
        date: LocalDate,
        morningAppointment: Appointment?,
        settings: AlarmSettings,
        overrides: Map<LocalDate, Boolean>,
        zone: TimeZone,
    ): DayAlarm {
        val (time, source, defaultEnabled) = if (morningAppointment != null) {
            Triple(
                morningAppointment.start - settings.offsetMinutes.minutes,
                AlarmSource.BeforeAppointment(morningAppointment),
                true,
            )
        } else {
            Triple(
                date.atTime(settings.fallbackTime).toInstant(zone),
                AlarmSource.Fallback,
                date.dayOfWeek in settings.fallbackDays,
            )
        }
        return DayAlarm(date, time, source, overrides[date] ?: defaultEnabled)
    }
}
