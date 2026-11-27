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

/** AlarmManager forgets alarms on reboot and clock/time-zone changes; re-plan them. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val syncer: AlarmSyncer by inject()

    override fun onReceive(context: Context, intent: Intent) {
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
