package at.zanko.kalenderwecker.alarm

import at.zanko.kalenderwecker.model.Appointment
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

sealed interface AlarmSource {
    data class BeforeAppointment(val appointment: Appointment) : AlarmSource
    data object Fallback : AlarmSource
}

/** The single wake-up alarm of [date]. [time] may fall on the previous evening. */
data class DayAlarm(
    val date: LocalDate,
    val time: Instant,
    val source: AlarmSource,
    val enabled: Boolean,
)
