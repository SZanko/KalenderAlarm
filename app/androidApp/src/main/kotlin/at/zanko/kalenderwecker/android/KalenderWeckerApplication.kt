package at.zanko.kalenderwecker.android

import android.app.Application
import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<AlarmScheduler> { AndroidAlarmScheduler(androidContext()) }
}

class KalenderWeckerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) { androidContext(this@KalenderWeckerApplication) }
        RefreshWorker.enqueue(this)
    }
}
