package at.zanko.kalenderwecker

// Loads the IANA time zone database so TimeZone.of("Europe/Stockholm") works in the browser.
@JsModule("@js-joda/timezone")
@JsNonModule
internal external object JsJodaTimeZoneModule

@Suppress("unused")
private val jsJodaTz = JsJodaTimeZoneModule
