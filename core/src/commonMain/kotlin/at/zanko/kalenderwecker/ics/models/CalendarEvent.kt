package at.zanko.kalenderwecker.ics.models

data class CalendarEvent(
    val eventKey: String,
    val uid: String,
    val recurrenceId: String?,
    val summary: String,
    val description: String?,
    val location: String?,
    val startValue: String,
    val startTzid: String?,
    val startIsDate: Boolean,
    val endValue: String?,
    val endTzid: String?,
    val endIsDate: Boolean,
    val status: String?,
    val sequence: Int,
    val lastModified: String?
)