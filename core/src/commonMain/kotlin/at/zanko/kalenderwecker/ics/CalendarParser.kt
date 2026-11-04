package at.zanko.kalenderwecker.ics

import at.zanko.kalenderwecker.ics.models.CalendarEvent

fun interface CalendarParser {
    fun parse(ics: String): List<CalendarEvent>
}