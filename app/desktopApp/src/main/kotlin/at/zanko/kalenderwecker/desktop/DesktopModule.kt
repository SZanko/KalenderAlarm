package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmScheduler
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.koin.dsl.module

/** Ring requests raised inside the running app (in-process fallback, snooze). */
class RingRequests {
    private val _date = MutableStateFlow<LocalDate?>(null)
    val date: StateFlow<LocalDate?> = _date.asStateFlow()
    fun ring(date: LocalDate) { _date.value = date }
    fun clear() { _date.value = null }
}

/** Whether alarms survive closing the app (systemd) or only ring while it runs. */
data class DesktopCapabilities(val systemd: Boolean)

val desktopModule = module {
    single<Settings> { fileSettings() }
    single<CommandRunner> { ProcessCommandRunner() }
    single { DesktopCapabilities(systemd = systemdAvailable(get())) }
    single { RingRequests() }
    single { DesktopAlarmPlayer() }
    single<AlarmScheduler> {
        if (get<DesktopCapabilities>().systemd) {
            SystemdTimerScheduler(runner = get(), exec = currentExec()) { TimeZone.currentSystemDefault() }
        } else {
            val requests = get<RingRequests>()
            InProcessScheduler(CoroutineScope(SupervisorJob() + Dispatchers.Default), get()) { requests.ring(it) }
        }
    }
}
