package at.zanko.kalenderwecker.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * AlarmManager forgets alarms on reboot and clock/time-zone changes; re-plan them.
 * On LOCKED_BOOT_COMPLETED (before the first unlock) only the stored alarm
 * times are re-armed; BOOT_COMPLETED after unlock does the full re-plan.
 */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val syncer: AlarmSyncer by inject()
    private val scheduler: AndroidAlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (!isUserUnlocked(context)) {
            // Settings and schedule are unreadable until unlock: re-arm what was planned.
            scheduler.rearmFromStore()
            return
        }
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) RefreshWorker.enqueue(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncer.sync()
            } finally {
                pending.finish()
            }
        }
    }
}
