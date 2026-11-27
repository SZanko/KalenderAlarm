package at.zanko.kalenderwecker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.zanko.kalenderwecker.alarm.AlarmSettings
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.data.normalizeTimeEditUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val syncer: AlarmSyncer,
) : ViewModel() {
    val state: StateFlow<AlarmSettings> = settings.settings

    private val _urlError = MutableStateFlow(false)
    val urlError: StateFlow<Boolean> = _urlError.asStateFlow()

    /** Stores the normalized link and fetches it; a blank input clears the link. */
    fun setUrl(raw: String) {
        if (raw.isBlank()) {
            _urlError.value = false
            change { it.copy(icsUrl = "") }
            return
        }
        val url = normalizeTimeEditUrl(raw)
        _urlError.value = url == null
        if (url == null) return
        settings.update { it.copy(icsUrl = url) }
        viewModelScope.launch { syncer.refreshAndSync() }
    }

    fun setOffsetMinutes(minutes: Int) = change { it.copy(offsetMinutes = minutes.coerceIn(0, MAX_OFFSET)) }

    fun setMorningEnd(time: LocalTime) = change { it.copy(morningEnd = time) }

    fun setFallbackTime(time: LocalTime) = change { it.copy(fallbackTime = time) }

    fun toggleFallbackDay(day: DayOfWeek) = change {
        it.copy(fallbackDays = if (day in it.fallbackDays) it.fallbackDays - day else it.fallbackDays + day)
    }

    fun setSound(uri: String?) = change { it.copy(soundUri = uri) }

    private fun change(transform: (AlarmSettings) -> AlarmSettings) {
        settings.update(transform)
        viewModelScope.launch { syncer.sync() }
    }

    companion object {
        const val MAX_OFFSET = 720
    }
}
