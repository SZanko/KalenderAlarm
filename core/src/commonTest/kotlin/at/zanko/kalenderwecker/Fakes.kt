package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlin.time.Clock
import kotlin.time.Instant

class FakeAlarmScheduler : AlarmScheduler {
    val calls = mutableListOf<List<DayAlarm>>()
    val last: List<DayAlarm> get() = calls.last()

    override suspend fun schedule(alarms: List<DayAlarm>) {
        calls += alarms
    }
}

class FixedClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}
