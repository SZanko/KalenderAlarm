package at.zanko.kalenderwecker.di

import at.zanko.kalenderwecker.ui.home.HomeViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) { TimeZone.currentSystemDefault() } }
}

/** Starts Koin with the shared modules plus the platform's (which provides `AlarmScheduler`). */
fun initKoin(vararg platform: Module): KoinApplication =
    startKoin { modules(listOf(coreModule, appModule) + platform) }
