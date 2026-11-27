package at.zanko.kalenderwecker.android

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.DayAlarm

/** Uses `setAlarmClock`, which rings in Doze and shows in the status bar like a clock app's alarm. */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = deviceProtectedPrefs(context, "alarm_scheduler")

    override suspend fun schedule(alarms: List<DayAlarm>) {
        prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { code ->
            alarmManager.cancel(ringPendingIntent(context, code.toInt(), null))
        }
        val exact = alarmManager.canScheduleExactAlarms()
        if (!exact) Log.w(TAG, "Exact alarm permission missing; alarms may be late")
        alarms.forEach { alarm ->
            val pending = ringPendingIntent(context, requestCode(alarm.date), alarm.date)
            val at = alarm.time.toEpochMilliseconds()
            if (exact) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, openAppPendingIntent(context)), pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            }
        }
        prefs.edit().putStringSet(KEY_CODES, alarms.map { requestCode(it.date).toString() }.toSet()).apply()
    }

    private companion object {
        const val TAG = "KalenderWecker"
        const val KEY_CODES = "scheduled_codes"
    }
}
