package at.zanko.kalenderwecker.alarm

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class AlarmSettings(
    val icsUrl: String = "",
    /** Minutes between the alarm and the first morning appointment. */
    val offsetMinutes: Int = 90,
    /** Appointments starting at or after this time do not count as morning. */
    val morningEnd: LocalTime = LocalTime(12, 0),
    val fallbackTime: LocalTime = LocalTime(8, 0),
    /** Days whose fallback alarm is on unless toggled. */
    val fallbackDays: Set<DayOfWeek> = WEEKDAYS,
    /** Platform-specific location of the chosen sound; null = default sound. */
    val soundUri: String? = null,
    val horizonDays: Int = 14,
) {
    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
    }
}
