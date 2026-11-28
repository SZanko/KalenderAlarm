package at.zanko.kalenderwecker.ui

import androidx.compose.runtime.Composable
import at.zanko.kalenderwecker.data.RefreshError
import at.zanko.kalenderwecker.resources.Res
import at.zanko.kalenderwecker.resources.alarm_before
import at.zanko.kalenderwecker.resources.alarm_fallback
import at.zanko.kalenderwecker.resources.day_header_format
import at.zanko.kalenderwecker.resources.error_http
import at.zanko.kalenderwecker.resources.error_invalid_url
import at.zanko.kalenderwecker.resources.error_network
import at.zanko.kalenderwecker.resources.error_no_url
import at.zanko.kalenderwecker.resources.error_parse
import at.zanko.kalenderwecker.resources.months_short
import at.zanko.kalenderwecker.resources.today
import at.zanko.kalenderwecker.resources.tomorrow
import at.zanko.kalenderwecker.resources.weekdays_short
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** Format texts in the current UI language. */
@Composable
fun rememberFormatTexts(): FormatTexts = FormatTexts(
    today = stringResource(Res.string.today),
    tomorrow = stringResource(Res.string.tomorrow),
    weekdaysShort = stringArrayResource(Res.array.weekdays_short),
    monthsShort = stringArrayResource(Res.array.months_short),
    dayHeaderFormat = stringResource(Res.string.day_header_format),
    alarmBefore = stringResource(Res.string.alarm_before),
    alarmFallback = stringResource(Res.string.alarm_fallback),
)

/** Format texts in the system language, for code outside composition (e.g. notifications). */
suspend fun loadFormatTexts(): FormatTexts = FormatTexts(
    today = getString(Res.string.today),
    tomorrow = getString(Res.string.tomorrow),
    weekdaysShort = getStringArray(Res.array.weekdays_short),
    monthsShort = getStringArray(Res.array.months_short),
    dayHeaderFormat = getString(Res.string.day_header_format),
    alarmBefore = getString(Res.string.alarm_before),
    alarmFallback = getString(Res.string.alarm_fallback),
)

/** User-facing message for a refresh failure (the exception messages are English diagnostics). */
suspend fun RefreshError.localizedMessage(): String = when (this) {
    is RefreshError.NoUrl -> getString(Res.string.error_no_url)
    is RefreshError.InvalidUrl -> getString(Res.string.error_invalid_url)
    is RefreshError.Network -> getString(Res.string.error_network)
    is RefreshError.Http -> getString(Res.string.error_http, status)
    is RefreshError.Parse -> getString(Res.string.error_parse)
}
