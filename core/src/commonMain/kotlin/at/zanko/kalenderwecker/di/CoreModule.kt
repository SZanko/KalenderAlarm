package at.zanko.kalenderwecker.di

import at.zanko.kalenderwecker.alarm.AlarmPlanner
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.CalendarRepository
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.ics.BasicIcsParser
import at.zanko.kalenderwecker.ics.CalendarParser
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import kotlinx.datetime.TimeZone
import org.koin.dsl.module
import kotlin.time.Clock

/** Shared bindings. A platform module must provide `AlarmScheduler`. */
val coreModule = module {
    single<Settings> { Settings() }
    single { HttpClient() }
    single<CalendarParser> { BasicIcsParser() }
    single<Clock> { Clock.System }
    single { SettingsRepository(get()) }
    single { CalendarRepository(get(), get(), get()) { TimeZone.currentSystemDefault() } }
    single { AlarmPlanner() }
    single {
        AlarmSyncer(get(), get(), get(), get(), get()) { TimeZone.currentSystemDefault() }
    }
}
