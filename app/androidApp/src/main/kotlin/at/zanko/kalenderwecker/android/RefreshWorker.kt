package at.zanko.kalenderwecker.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.RefreshError
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/** Fetches TimeEdit every 6 h and re-plans, so schedule changes reach the alarms without opening the app. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {
    private val syncer: AlarmSyncer by inject()

    override suspend fun doWork(): Result =
        when (syncer.refreshAndSync().exceptionOrNull()) {
            is RefreshError.Network -> Result.retry()
            else -> Result.success()
        }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("timeedit-refresh", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
