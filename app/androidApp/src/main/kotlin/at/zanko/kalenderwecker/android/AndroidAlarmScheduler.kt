package at.zanko.kalenderwecker.android

import android.app.AlarmManager
import android.content.Context
import android.util.Log
import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlin.time.Clock

/**
 * Uses `setAlarmClock`, which rings in Doze and shows in the status bar like a
 * clock app's alarm. The armed alarms (date + time only) are mirrored to
 * device-protected storage so they can be re-armed after a reboot before the
 * first unlock.
 */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = deviceProtectedPrefs(context, "alarm_scheduler")

    override suspend fun schedule(alarms: List<DayAlarm>) = arm(alarms.toPlanned())

    /** Called while the phone is still locked after a reboot. */
    fun rearmFromStore() = arm(alarmsToRearm(planned(), Clock.System.now()))

    fun planned(): List<PlannedAlarm> = decodePlanned(prefs.getStringSet(KEY_PLANNED, emptySet()).orEmpty())

    private fun arm(alarms: List<PlannedAlarm>) {
        planned().forEach { alarmManager.cancel(ringPendingIntent(context, requestCode(it.date), null)) }
        val exact = alarmManager.canScheduleExactAlarms()
        if (!exact) Log.w(TAG, "Exact alarm permission missing; alarms may be late")
        alarms.forEach { alarm ->
            val pending = ringPendingIntent(context, requestCode(alarm.date), alarm.date)
            if (exact) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.epochMillis, openAppPendingIntent(context)), pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.epochMillis, pending)
            }
        }
        prefs.edit().putStringSet(KEY_PLANNED, encodePlanned(alarms)).remove(LEGACY_KEY_CODES).apply()
    }

    private companion object {
        const val TAG = "KalenderWecker"
        const val KEY_PLANNED = "planned_alarms"
        const val LEGACY_KEY_CODES = "scheduled_codes"
    }
}
