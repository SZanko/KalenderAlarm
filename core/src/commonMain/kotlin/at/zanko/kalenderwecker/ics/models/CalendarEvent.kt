package at.zanko.kalenderwecker.ics.models

import kotlin.time.Instant

data class CalendarEvent(
    val eventKey: String,
    val uid: String,
    val recurrenceId: IcsDateValue?,
    val summary: String,
    val description: String?,
    val location: String?,
    val start: IcsDateValue,
    val end: IcsDateValue?,
    val status: String?,
    val sequence: Int,
    val lastModified: Instant?
)