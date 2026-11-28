package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class InProcessSchedulerTest {
    private val start = Instant.parse("2026-11-30T05:00:00Z")

    private fun TestScope.virtualClock() = object : Clock {
        override fun now() = start.plus(kotlin.time.Duration.parse("${currentTime}ms"))
    }

    @Test
    fun gIVEN_alarmIn45Min_WHEN_timePasses_THEN_ringsOnceAtAlarmTime() = runTest {
        val rung = mutableListOf<Pair<LocalDate, Long>>()
        val scheduler = InProcessScheduler(backgroundScope, virtualClock()) { rung += it to currentTime }
        val date = LocalDate.parse("2026-11-30")

        scheduler.schedule(listOf(DayAlarm(date, start + 45.minutes, AlarmSource.Fallback, true)))
        advanceTimeBy(44.minutes)
        runCurrent()
        assertEquals(emptyList(), rung)

        advanceTimeBy(2.minutes)
        runCurrent()
        assertEquals(listOf(date to 45.minutes.inWholeMilliseconds), rung)
    }

    @Test
    fun gIVEN_rescheduledWithoutAlarm_WHEN_timePasses_THEN_noRing() = runTest {
        val rung = mutableListOf<LocalDate>()
        val scheduler = InProcessScheduler(backgroundScope, virtualClock()) { rung += it }

        scheduler.schedule(listOf(DayAlarm(LocalDate.parse("2026-11-30"), start + 10.minutes, AlarmSource.Fallback, true)))
        scheduler.schedule(emptyList())
        advanceTimeBy(20.minutes)
        runCurrent()

        assertEquals(emptyList(), rung)
    }
}
