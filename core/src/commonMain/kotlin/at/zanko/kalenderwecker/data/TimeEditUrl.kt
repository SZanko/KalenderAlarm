package at.zanko.kalenderwecker.data

/**
 * Accepts what a student is likely to paste from TimeEdit — the `.ics`
 * subscription link, the `.html` schedule page, or a `webcal://` link — and
 * returns the `https://….ics` URL, or null when it can't be a calendar feed.
 */
fun normalizeTimeEditUrl(input: String): String? {
    var url = input.trim()
    if (url.startsWith("webcal://", ignoreCase = true)) {
        url = "https://" + url.substring("webcal://".length)
    }
    if (url.endsWith(".html", ignoreCase = true)) {
        url = url.dropLast(".html".length) + ".ics"
    }
    val scheme = url.substringBefore("://", missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return null
    if (url.substringAfter("://").substringBefore('/').isEmpty()) return null
    if (!url.substringBefore('?').endsWith(".ics", ignoreCase = true)) return null
    return url
}
