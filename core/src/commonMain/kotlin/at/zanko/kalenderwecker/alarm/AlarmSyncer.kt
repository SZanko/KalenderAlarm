package at.zanko.kalenderwecker.alarm

import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/** Glues schedule, settings and toggles to the platform [AlarmScheduler]. */
class AlarmSyncer(
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val planner: AlarmPlanner,
    private val scheduler: AlarmScheduler,
    private val clock: Clock,
    private val zone: () -> TimeZone,
) {
    val plan: Flow<List<DayAlarm>> = combine(
        calendar.appointments,
        settings.settings,
        settings.overrides,
    ) { appointments, current, overrides ->
        planner.plan(appointments, current, overrides, clock.now(), zone())
    }

    fun currentPlan(): List<DayAlarm> = planner.plan(
        calendar.appointments.value,
        settings.settings.value,
        settings.overrides.value,
        clock.now(),
        zone(),
    )

    fun alarmFor(date: LocalDate): DayAlarm? = currentPlan().firstOrNull { it.date == date }

    suspend fun sync(): List<DayAlarm> {
        val plan = currentPlan()
        scheduler.schedule(plan.filter { it.enabled })
        return plan
    }

    /** Refreshes the calendar, then syncs — also when the refresh failed. */
    suspend fun refreshAndSync(): Result<Unit> {
        val result = calendar.refresh()
        sync()
        return result
    }
}
