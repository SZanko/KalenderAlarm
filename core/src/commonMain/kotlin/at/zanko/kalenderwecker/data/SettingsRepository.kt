package at.zanko.kalenderwecker.data

import at.zanko.kalenderwecker.alarm.AlarmSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** Persists user settings, per-day alarm toggles and the last fetched calendar. */
class SettingsRepository(private val store: Settings) {
    private val json = Json { ignoreUnknownKeys = true }
    private val overridesSerializer: KSerializer<Map<LocalDate, Boolean>> =
        MapSerializer(LocalDate.serializer(), Boolean.serializer())

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<AlarmSettings> = _settings.asStateFlow()

    private val _overrides = MutableStateFlow(readOverrides())
    val overrides: StateFlow<Map<LocalDate, Boolean>> = _overrides.asStateFlow()

    var cachedIcs: String?
        get() = store.getStringOrNull(KEY_ICS)
        set(value) {
            if (value == null) store.remove(KEY_ICS) else store.putString(KEY_ICS, value)
        }

    /** When the calendar was last fetched successfully; null = never. */
    var lastRefresh: Instant?
        get() = store.getStringOrNull(KEY_LAST_REFRESH)?.let { runCatching { Instant.parse(it) }.getOrNull() }
        set(value) {
            if (value == null) store.remove(KEY_LAST_REFRESH) else store.putString(KEY_LAST_REFRESH, value.toString())
        }

    fun update(transform: (AlarmSettings) -> AlarmSettings) {
        val updated = _settings.updateAndGet(transform)
        store.putString(KEY_SETTINGS, json.encodeToString(AlarmSettings.serializer(), updated))
    }

    fun setEnabled(date: LocalDate, enabled: Boolean) {
        val updated = _overrides.updateAndGet { it + (date to enabled) }
        store.putString(KEY_OVERRIDES, json.encodeToString(overridesSerializer, updated))
    }

    /** Re-reads the store, e.g. after another process changed it. */
    fun reload() {
        _settings.value = readSettings()
        _overrides.value = readOverrides()
    }

    private fun readSettings(): AlarmSettings =
        decodeOrNull(KEY_SETTINGS, AlarmSettings.serializer()) ?: AlarmSettings()

    private fun readOverrides(): Map<LocalDate, Boolean> =
        decodeOrNull(KEY_OVERRIDES, overridesSerializer) ?: emptyMap()

    private fun <T> decodeOrNull(key: String, serializer: KSerializer<T>): T? =
        store.getStringOrNull(key)?.let { raw ->
            runCatching { json.decodeFromString(serializer, raw) }.getOrNull()
        }

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_OVERRIDES = "overrides"
        const val KEY_ICS = "cachedIcs"
        const val KEY_LAST_REFRESH = "lastRefresh"
    }
}
