package at.zanko.kalenderwecker.model

import at.zanko.kalenderwecker.ics.models.CalendarEvent
import at.zanko.kalenderwecker.ics.models.IcsDateValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** A timed calendar entry the alarm logic can reason about. */
data class Appointment(
    val key: String,
    val title: String,
    val location: String?,
    val description: String?,
    val start: Instant,
    val end: Instant?,
)

/**
 * Maps a parsed event to an [Appointment]. Returns null for events that must
 * never trigger an alarm: cancelled ones and all-day (`VALUE=DATE`) ones.
 */
fun CalendarEvent.toAppointment(zone: TimeZone): Appointment? {
    if (status.equals("CANCELLED", ignoreCase = true)) return null
    val startInstant = start.toInstantIn(zone) ?: return null
    return Appointment(
        key = eventKey,
        title = summary,
        location = location,
        description = description,
        start = startInstant,
        end = end?.toInstantIn(zone),
    )
}

fun List<CalendarEvent>.toAppointments(zone: TimeZone): List<Appointment> =
    mapNotNull { it.toAppointment(zone) }.sortedBy { it.start }

private fun IcsDateValue.toInstantIn(zone: TimeZone): Instant? = when (this) {
    is IcsDateValue.Date -> null
    is IcsDateValue.Floating -> value.toInstant(zone)
    is IcsDateValue.Utc -> value
    is IcsDateValue.Zoned -> value.toInstant(
        runCatching { TimeZone.of(tzid) }.getOrDefault(zone)
    )
}
