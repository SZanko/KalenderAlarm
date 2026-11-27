package at.zanko.kalenderwecker.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by AlarmManager; hands off to the foreground service that rings. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startForegroundService(
            Intent(context, AlarmService::class.java)
                .setAction(AlarmService.ACTION_RING)
                .putExtras(intent)
        )
    }
}
