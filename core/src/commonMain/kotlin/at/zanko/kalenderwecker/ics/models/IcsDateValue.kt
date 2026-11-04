package at.zanko.kalenderwecker.ics.models
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

sealed interface IcsDateValue {

    // VALUE=DATE
    data class Date(
        val value: LocalDate
    ) : IcsDateValue

    // DATE-TIME without TZID or Z
    data class Floating(
        val value: LocalDateTime
    ) : IcsDateValue

    // DATE-TIME with TZID
    data class Zoned(
        val value: LocalDateTime,
        val tzid: String
    ) : IcsDateValue

    // DATE-TIME ending in Z
    data class Utc(
        val value: Instant
    ) : IcsDateValue
}

fun IcsDateValue.toInstant(
    floatingTimeZone: TimeZone? = null
): Instant? {
    return when (this) {
        is IcsDateValue.Date -> null

        is IcsDateValue.Floating ->
            floatingTimeZone?.let {
                value.toInstant(it)
            }

        is IcsDateValue.Zoned ->
            value.toInstant(timeZone = TimeZone.of(tzid))

        is IcsDateValue.Utc ->
            value
    }
}