package at.zanko.kalenderwecker.di

import at.zanko.kalenderwecker.ui.home.HomeViewModel
import at.zanko.kalenderwecker.ui.settings.SettingsViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) { TimeZone.currentSystemDefault() } }
    viewModel { SettingsViewModel(get(), get()) }
}

/** Starts Koin with the shared modules plus the platform's (which provides `AlarmScheduler`). */
fun initKoin(vararg platform: Module, config: KoinApplication.() -> Unit = {}): KoinApplication =
    startKoin {
        config()
        modules(listOf(coreModule, appModule) + platform)
    }
