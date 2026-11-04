package at.zanko.kalenderwecker.ics

import at.zanko.kalenderwecker.ics.models.CalendarEvent


class BasicIcsParser : CalendarParser {

    private data class Property(
        val value: String,
        val params: Map<String, String>
    )

    override fun parse(ics: String): List<CalendarEvent> {
        val lines = unfold(ics)

        require("BEGIN:VCALENDAR" in lines) {
            "Invalid ICS: missing VCALENDAR header"
        }
        require("END:VCALENDAR" in lines) {
            "Incomplete ICS feed"
        }

        val events = mutableListOf<CalendarEvent>()
        var insideEvent = false
        val properties = mutableMapOf<String, Property>()

        for (line in lines) {
            when (line.uppercase()) {
                "BEGIN:VEVENT" -> {
                    insideEvent = true
                    properties.clear()
                }

                "END:VEVENT" -> {
                    if (insideEvent) {
                        events += createEvent(properties)
                    }
                    insideEvent = false
                }

                else -> {
                    if (insideEvent) {
                        val parsed = parseProperty(line)
                        if (parsed != null) {
                            properties[parsed.first] = parsed.second
                        }
                    }
                }
            }
        }

        return events
    }

    private fun createEvent(
        p: Map<String, Property>
    ): CalendarEvent {
        val uid = p["UID"]?.value
            ?: error("Event without UID")

        val start = p["DTSTART"]
            ?: error("Event without DTSTART")

        val end = p["DTEND"]

        val recurrenceId = p["RECURRENCE-ID"]?.value

        return CalendarEvent(
            eventKey = "$uid|${recurrenceId.orEmpty()}",
            uid = uid,
            recurrenceId = recurrenceId,
            summary = p["SUMMARY"]?.value?.let(::unescape)
                ?: "Untitled",
            description = p["DESCRIPTION"]?.value?.let(::unescape),
            location = p["LOCATION"]?.value?.let(::unescape),
            startValue = normalizeDate(start.value),
            startTzid = start.params["TZID"],
            startIsDate = isDate(start),
            endValue = end?.value?.let(::normalizeDate),
            endTzid = end?.params?.get("TZID"),
            endIsDate = end?.let(::isDate) ?: false,
            status = p["STATUS"]?.value,
            sequence = p["SEQUENCE"]?.value?.toIntOrNull() ?: 0,
            lastModified = p["LAST-MODIFIED"]?.value
        )
    }

    private fun isDate(property: Property): Boolean {
        return property.params["VALUE"] == "DATE" ||
                property.value.length == 8
    }

    private fun normalizeDate(value: String): String {
        require(value.matches(
            Regex("""\d{8}(T\d{4}(\d{2})?Z?)?""")
        )) {
            "Unsupported ICS date: $value"
        }

        val date = "${value.substring(0, 4)}-" +
                "${value.substring(4, 6)}-" +
                value.substring(6, 8)

        if (value.length == 8) return date

        val time = value.substring(9)
        val hour = time.substring(0, 2)
        val minute = time.substring(2, 4)

        val seconds = if (time.length >= 6) {
            time.substring(4, 6)
        } else {
            "00"
        }

        val utc = time.endsWith("Z")

        return date +
                "T$hour:$minute:$seconds" +
                if (utc) "Z" else ""
    }

    private fun unescape(value: String): String =
        value.replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")

    private fun unfold(ics: String): List<String> {
        val result = mutableListOf<String>()

        val lines = ics
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")

        for (line in lines) {
            if ((line.startsWith(" ") ||
                        line.startsWith("\t")) && result.isNotEmpty()) {
                result[result.lastIndex] += line.drop(1)
            } else {
                result += line
            }
        }

        return result.filter { it.isNotEmpty() }
    }

    private fun parseProperty(
        line: String
    ): Pair<String, Property>? {
        val separator = line.indexOf(':')
        if (separator < 0) return null

        val header = line.substring(0, separator)
            .split(';')

        val name = header.first().uppercase()

        val params = header.drop(1).mapNotNull { part ->
            val index = part.indexOf('=')
            if (index < 0) null
            else {
                part.substring(0, index).uppercase() to
                        part.substring(index + 1).trim('"')
            }
        }.toMap()

        return name to Property(
            value = line.substring(separator + 1),
            params = params
        )
    }
}