package at.zanko.kalenderwecker.data

import at.zanko.kalenderwecker.ics.CalendarParser
import at.zanko.kalenderwecker.model.Appointment
import at.zanko.kalenderwecker.model.toAppointments
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone

sealed class RefreshError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoUrl : RefreshError("No TimeEdit link configured")
    class InvalidUrl(val url: String) : RefreshError("Not a TimeEdit calendar link: $url")
    class Network(cause: Throwable) : RefreshError("Could not reach TimeEdit", cause)
    class Http(val status: Int) : RefreshError("TimeEdit answered with HTTP $status")
    class Parse(cause: Throwable) : RefreshError("TimeEdit sent an unreadable calendar", cause)
}

/**
 * Fetches the TimeEdit feed and exposes its appointments. The last good feed
 * is cached so the schedule (and alarms) survive being offline.
 */
class CalendarRepository(
    private val client: HttpClient,
    private val parser: CalendarParser,
    private val settings: SettingsRepository,
    private val zone: () -> TimeZone,
) {
    private val _appointments = MutableStateFlow(
        settings.cachedIcs?.let { runCatching { toAppointments(it) }.getOrNull() }.orEmpty()
    )
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    suspend fun refresh(): Result<Unit> {
        val raw = settings.settings.value.icsUrl
        if (raw.isBlank()) return Result.failure(RefreshError.NoUrl())
        val url = normalizeTimeEditUrl(raw) ?: return Result.failure(RefreshError.InvalidUrl(raw))

        val body = try {
            val response = client.get(url)
            if (response.status.value !in 200..299) {
                return Result.failure(RefreshError.Http(response.status.value))
            }
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(RefreshError.Network(e))
        }

        val parsed = try {
            toAppointments(body)
        } catch (e: Exception) {
            return Result.failure(RefreshError.Parse(e))
        }
        settings.cachedIcs = body
        _appointments.value = parsed
        return Result.success(Unit)
    }

    private fun toAppointments(ics: String): List<Appointment> =
        parser.parse(ics).toAppointments(zone())
}
