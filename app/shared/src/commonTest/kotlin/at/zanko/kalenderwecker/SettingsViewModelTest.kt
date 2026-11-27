package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val graph = TestGraph(url = "", httpDispatcher = dispatcher)
    private val vm by lazy { SettingsViewModel(graph.settings, graph.syncer) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun gIVEN_htmlUrl_WHEN_setUrl_THEN_storedAsIcsAndRefreshed() = runTest(dispatcher) {
        vm.setUrl(" https://cloud.timeedit.net/ju/web/open/abc.html ")
        advanceUntilIdle()

        assertEquals("https://cloud.timeedit.net/ju/web/open/abc.ics", graph.settings.settings.value.icsUrl)
        assertFalse(vm.urlError.value)
        assertEquals(1, graph.requests)
        assertTrue(graph.scheduler.calls.isNotEmpty())
    }

    @Test
    fun gIVEN_garbageUrl_WHEN_setUrl_THEN_errorAndNotStored() = runTest(dispatcher) {
        vm.setUrl("hello")
        advanceUntilIdle()

        assertTrue(vm.urlError.value)
        assertEquals("", graph.settings.settings.value.icsUrl)
        assertEquals(0, graph.requests)
    }

    @Test
    fun gIVEN_emptyUrl_WHEN_setUrl_THEN_clearedWithoutError() = runTest(dispatcher) {
        vm.setUrl("https://cloud.timeedit.net/ju/a.ics")
        vm.setUrl("")
        advanceUntilIdle()

        assertFalse(vm.urlError.value)
        assertEquals("", graph.settings.settings.value.icsUrl)
    }

    @Test
    fun gIVEN_hugeOffset_WHEN_setOffset_THEN_coercedTo720() = runTest(dispatcher) {
        vm.setOffsetMinutes(1000)
        assertEquals(720, vm.state.value.offsetMinutes)
        vm.setOffsetMinutes(-5)
        assertEquals(0, vm.state.value.offsetMinutes)
    }

    @Test
    fun gIVEN_saturday_WHEN_toggleFallbackDay_THEN_addedThenRemoved() = runTest(dispatcher) {
        vm.toggleFallbackDay(DayOfWeek.SATURDAY)
        assertTrue(DayOfWeek.SATURDAY in vm.state.value.fallbackDays)
        vm.toggleFallbackDay(DayOfWeek.SATURDAY)
        assertFalse(DayOfWeek.SATURDAY in vm.state.value.fallbackDays)
    }

    @Test
    fun gIVEN_anyChange_WHEN_applied_THEN_alarmsResynced() = runTest(dispatcher) {
        vm.setMorningEnd(LocalTime(11, 0))
        vm.setFallbackTime(LocalTime(7, 0))
        vm.setSound("/home/me/wake.mp3")
        advanceUntilIdle()

        assertEquals(3, graph.scheduler.calls.size)
        assertEquals(LocalTime(11, 0), graph.settings.settings.value.morningEnd)
        assertEquals(LocalTime(7, 0), graph.settings.settings.value.fallbackTime)
        assertEquals("/home/me/wake.mp3", graph.settings.settings.value.soundUri)
    }
}
