package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.RefreshError
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.ics.BasicIcsParser
import com.goncalossilva.resources.Resource
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CalendarRepositoryTest {
    private val url = "https://cloud.timeedit.net/ju/web/open/x.ics"
    private val ultraShort = Resource("TimeEdit_UltraShort.ics").readText()
    private val store = MapSettings()
    private val settings = SettingsRepository(store).apply { update { it.copy(icsUrl = url) } }
    private var requests = 0

    private fun repo(handler: MockRequestHandler) = CalendarRepository(
        client = HttpClient(MockEngine { requests++; handler(it) }),
        parser = BasicIcsParser(),
        settings = settings,
        zone = { TimeZone.of("Europe/Stockholm") },
    )

    @Test
    fun gIVEN_validFeed_WHEN_refresh_THEN_appointmentsAndCacheUpdated() = runTest {
        val repo = repo { request ->
            assertEquals(url, request.url.toString())
            respond(ultraShort)
        }

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        assertEquals(3, repo.appointments.value.size)
        assertEquals(ultraShort, settings.cachedIcs)
    }

    @Test
    fun gIVEN_http500_WHEN_refresh_THEN_httpErrorAndCacheKept() = runTest {
        settings.cachedIcs = ultraShort
        val repo = repo { respondError(HttpStatusCode.InternalServerError) }

        val error = repo.refresh().exceptionOrNull()

        assertIs<RefreshError.Http>(error)
        assertEquals(500, error.status)
        assertEquals(3, repo.appointments.value.size)
    }

    @Test
    fun gIVEN_networkDown_WHEN_refresh_THEN_networkErrorAndCacheKept() = runTest {
        settings.cachedIcs = ultraShort
        val repo = repo { throw IOException("offline") }

        assertIs<RefreshError.Network>(repo.refresh().exceptionOrNull())
        assertEquals(3, repo.appointments.value.size)
        assertEquals(ultraShort, settings.cachedIcs)
    }

    @Test
    fun gIVEN_invalidBody_WHEN_refresh_THEN_parseErrorAndCacheKept() = runTest {
        settings.cachedIcs = ultraShort
        val repo = repo { respond("<html>login</html>") }

        assertIs<RefreshError.Parse>(repo.refresh().exceptionOrNull())
        assertEquals(3, repo.appointments.value.size)
        assertEquals(ultraShort, settings.cachedIcs)
    }

    @Test
    fun gIVEN_noUrl_WHEN_refresh_THEN_noUrlWithoutRequest() = runTest {
        settings.update { it.copy(icsUrl = "") }
        val repo = repo { respond(ultraShort) }

        assertIs<RefreshError.NoUrl>(repo.refresh().exceptionOrNull())
        assertEquals(0, requests)
    }

    @Test
    fun gIVEN_invalidUrl_WHEN_refresh_THEN_invalidUrlWithoutRequest() = runTest {
        settings.update { it.copy(icsUrl = "nonsense") }
        val repo = repo { respond(ultraShort) }

        assertIs<RefreshError.InvalidUrl>(repo.refresh().exceptionOrNull())
        assertEquals(0, requests)
    }

    @Test
    fun gIVEN_cachedIcs_WHEN_created_THEN_appointmentsAvailableBeforeRefresh() {
        settings.cachedIcs = ultraShort

        val repo = repo { error("must not be called") }

        assertEquals(3, repo.appointments.value.size)
    }

    @Test
    fun gIVEN_corruptCache_WHEN_created_THEN_emptyWithoutThrowing() {
        settings.cachedIcs = "garbage"

        assertEquals(emptyList(), repo { error("unused") }.appointments.value)
    }
}
