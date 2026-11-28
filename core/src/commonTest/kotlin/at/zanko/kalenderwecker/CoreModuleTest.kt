package at.zanko.kalenderwecker

import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.di.coreModule
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

class CoreModuleTest {
    @Test
    fun gIVEN_coreModuleWithPlatformScheduler_WHEN_resolve_THEN_syncerAvailable() {
        val koin = koinApplication {
            allowOverride(true)
            modules(
                coreModule,
                module {
                    single<AlarmScheduler> { FakeAlarmScheduler() }
                    single<Settings> { MapSettings() }
                },
            )
        }.koin

        assertNotNull(koin.get<AlarmSyncer>())
    }
}
