package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** An alarm more than this late (e.g. after a long suspend) stays silent. */
val MAX_LATENESS = 2.hours

/**
 * Re-checks a timer-triggered ring against the current plan: the alarm may
 * have been switched off or moved since the timer was written. [force] is
 * used by snooze.
 */
fun shouldRing(alarm: DayAlarm?, now: Instant, force: Boolean): Boolean {
    if (force) return true
    if (alarm == null || !alarm.enabled) return false
    return now - alarm.time <= MAX_LATENESS
}
