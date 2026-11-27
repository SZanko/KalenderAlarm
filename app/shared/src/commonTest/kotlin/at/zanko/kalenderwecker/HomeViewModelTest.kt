package at.zanko.kalenderwecker

import app.cash.turbine.test
import at.zanko.kalenderwecker.data.RefreshError
import at.zanko.kalenderwecker.ui.home.HomeViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val monday = LocalDate.parse("2026-11-02")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(graph: TestGraph) =
        HomeViewModel(graph.syncer, graph.calendar, graph.settings, graph.clock) { graph.zone }

    @Test
    fun gIVEN_url_WHEN_created_THEN_refreshesAndShowsSections() = runTest(dispatcher) {
        val graph = TestGraph()
        val vm = vm(graph)

        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, graph.requests)
        assertTrue(state.hasUrl)
        assertFalse(state.isRefreshing)
        assertEquals(monday, state.sections.first().date)
        assertEquals(listOf("Data Science", "AI Systems"), state.sections.first().appointments.map { it.title })
        assertEquals(LocalDate.parse("2026-11-30"), state.sections.last().date)
        assertEquals(monday, state.nextAlarm?.date)
    }

    @Test
    fun gIVEN_noUrl_WHEN_created_THEN_noRequestAndHasUrlFalse() = runTest(dispatcher) {
        val graph = TestGraph(url = "")
        val vm = vm(graph)

        advanceUntilIdle()

        assertEquals(0, graph.requests)
        assertFalse(vm.state.value.hasUrl)
        assertNull(vm.state.value.error)
    }

    @Test
    fun gIVEN_mondayToggledOff_WHEN_setAlarmEnabled_THEN_persistedUnscheduledAndNextAlarmMoves() = runTest(dispatcher) {
        val graph = TestGraph()
        val vm = vm(graph)
        advanceUntilIdle()

        vm.setAlarmEnabled(monday, false)
        advanceUntilIdle()

        assertEquals(false, graph.settings.overrides.value[monday])
        assertFalse(graph.scheduler.calls.last().any { it.date == monday })
        assertEquals(LocalDate.parse("2026-11-03"), vm.state.value.nextAlarm?.date)
        assertFalse(vm.state.value.sections.first { it.date == monday }.alarm!!.enabled)
    }

    @Test
    fun gIVEN_networkError_WHEN_refresh_THEN_errorShownAndNotRefreshing() = runTest(dispatcher) {
        val graph = TestGraph().apply { status = HttpStatusCode.ServiceUnavailable }
        val vm = vm(graph)

        vm.state.test {
            advanceUntilIdle()
            val last = expectMostRecentItem()
            assertIs<RefreshError.Http>(last.error)
            assertFalse(last.isRefreshing)
        }
        assertTrue(graph.scheduler.calls.isNotEmpty(), "sync still runs after a failed refresh")

        vm.dismissError()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
    }
}
