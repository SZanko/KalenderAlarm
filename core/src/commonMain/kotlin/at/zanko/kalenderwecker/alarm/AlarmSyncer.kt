package at.zanko.kalenderwecker.alarm

import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
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

    /**
     * The alarm planned for [date], even if its time has already passed —
     * needed when a ring is triggered at (or slightly after) the alarm time.
     */
    fun alarmFor(date: LocalDate): DayAlarm? {
        val tz = zone()
        val dayBefore = date.minus(DatePeriod(days = 1)).atStartOfDayIn(tz)
        return planner.plan(
            calendar.appointments.value,
            settings.settings.value.copy(horizonDays = 3),
            settings.overrides.value,
            dayBefore,
            tz,
        ).firstOrNull { it.date == date }
    }

    private val mutex = Mutex()

    private val _schedulingError = MutableStateFlow<String?>(null)

    /** Why the last attempt to hand alarms to the platform failed; null after a success. */
    val schedulingError: StateFlow<String?> = _schedulingError.asStateFlow()

    /**
     * Re-reads persisted state (another process — desktop `--refresh` / GUI —
     * may have changed it), plans and schedules. Serialized so an older plan
     * can never overwrite a newer one. Scheduler failures are exposed via
     * [schedulingError] rather than thrown.
     */
    suspend fun sync(): List<DayAlarm> = mutex.withLock {
        settings.reload()
        calendar.reloadFromCache()
        val plan = currentPlan()
        try {
            scheduler.schedule(plan.filter { it.enabled })
            _schedulingError.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _schedulingError.value = e.message ?: e.toString()
        }
        plan
    }

    /** Refreshes the calendar, then syncs — also when the refresh failed. */
    suspend fun refreshAndSync(): Result<Unit> {
        val result = calendar.refresh()
        if (result.isSuccess) settings.lastRefresh = clock.now()
        sync()
        return result
    }
}
