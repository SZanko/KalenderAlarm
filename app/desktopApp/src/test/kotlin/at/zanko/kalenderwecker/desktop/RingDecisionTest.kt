package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmSource
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RingDecisionTest {
    private val time = Instant.parse("2026-11-30T05:45:00Z")
    private val alarm = DayAlarm(LocalDate.parse("2026-11-30"), time, AlarmSource.Fallback, true)

    @Test
    fun gIVEN_onTime_WHEN_shouldRing_THEN_true() {
        assertTrue(shouldRing(alarm, time + 1.minutes, force = false))
        assertTrue(shouldRing(alarm, time - 30.minutes, force = false), "systemd may fire slightly early on clock jumps")
    }

    @Test
    fun gIVEN_threeHoursLate_WHEN_shouldRing_THEN_false() {
        assertFalse(shouldRing(alarm, time + 3.hours, force = false))
    }

    @Test
    fun gIVEN_disabledOrMissing_WHEN_shouldRing_THEN_false() {
        assertFalse(shouldRing(alarm.copy(enabled = false), time, force = false))
        assertFalse(shouldRing(null, time, force = false))
    }

    @Test
    fun gIVEN_force_WHEN_shouldRing_THEN_trueEvenIfLateOrMissing() {
        assertTrue(shouldRing(alarm, time + 3.hours, force = true))
        assertTrue(shouldRing(null, time, force = true))
    }
}
