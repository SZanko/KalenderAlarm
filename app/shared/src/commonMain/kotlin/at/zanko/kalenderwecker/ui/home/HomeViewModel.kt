package at.zanko.kalenderwecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.RefreshError
import at.zanko.kalenderwecker.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class HomeUiState(
    val sections: List<DaySection> = emptyList(),
    val nextAlarm: DayAlarm? = null,
    val isRefreshing: Boolean = false,
    val error: RefreshError? = null,
    val hasUrl: Boolean = false,
)

class HomeViewModel(
    private val syncer: AlarmSyncer,
    calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val zone: () -> TimeZone,
) : ViewModel() {
    private data class Transient(val refreshing: Boolean = false, val error: RefreshError? = null)

    private val transient = MutableStateFlow(Transient())

    val state: StateFlow<HomeUiState> = combine(
        syncer.plan,
        calendar.appointments,
        settings.settings,
        transient,
    ) { plan, appointments, current, t ->
        val tz = zone()
        HomeUiState(
            sections = buildDaySections(plan, appointments, tz, clock.now().toLocalDateTime(tz).date),
            nextAlarm = plan.firstOrNull { it.enabled },
            isRefreshing = t.refreshing,
            error = t.error,
            hasUrl = current.icsUrl.isNotBlank(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    init {
        if (settings.settings.value.icsUrl.isNotBlank()) {
            refresh()
        } else {
            viewModelScope.launch { syncer.sync() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            transient.update { it.copy(refreshing = true) }
            val error = syncer.refreshAndSync().exceptionOrNull() as? RefreshError
            transient.update { Transient(refreshing = false, error = error) }
        }
    }

    fun setAlarmEnabled(date: LocalDate, enabled: Boolean) {
        settings.setEnabled(date, enabled)
        viewModelScope.launch { syncer.sync() }
    }

    fun dismissError() {
        transient.update { it.copy(error = null) }
    }
}
