package at.zanko.kalenderwecker.android

import android.app.Application
import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.di.initKoin
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<Settings> {
        val context = androidContext()
        SharedPreferencesSettings(deviceProtectedPrefs(context, "${context.packageName}_preferences"))
    }
    single<AlarmScheduler> { AndroidAlarmScheduler(androidContext()) }
}

class KalenderWeckerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) { androidContext(this@KalenderWeckerApplication) }
        // WorkManager keeps its database in credential storage: only touch it once unlocked
        // (BootReceiver enqueues it on BOOT_COMPLETED otherwise).
        if (isUserUnlocked(this)) RefreshWorker.enqueue(this)
    }
}
