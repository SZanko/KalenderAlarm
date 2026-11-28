package at.zanko.kalenderwecker.android

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibratorManager
import android.util.Log
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.R
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.loadFormatTexts
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.android.ext.android.inject
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/** Plays the alarm, shows the full-screen notification and handles Dismiss/Snooze. */
class AlarmService : Service() {
    // Resolved lazily: credential storage is unreadable before the first unlock.
    private val syncer: AlarmSyncer by inject()
    private val settings: SettingsRepository by inject()
    private val scheduler: AndroidAlarmScheduler by inject()
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var date: LocalDate? = null
    private val scope = MainScope()
    private var subtitleJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> stopRinging()
            ACTION_SNOOZE -> {
                date?.let(::scheduleSnooze)
                stopRinging()
            }
            else -> startRinging(intent)
        }
        return START_NOT_STICKY
    }

    private fun startRinging(intent: Intent?) {
        val day = intent?.getStringExtra(EXTRA_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val force = intent?.getBooleanExtra(EXTRA_FORCE, false) ?: false
        startForeground(NOTIFICATION_ID, notification(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        val unlocked = isUserUnlocked(this)
        val allowed = when {
            day == null -> false
            unlocked -> force || syncer.alarmFor(day)?.enabled == true
            else -> lockedRingAllowed(scheduler.planned(), day, force)
        }
        if (!allowed) {
            stopRinging()
            return
        }
        if (unlocked) {
            syncer.alarmFor(day!!)?.let { alarm ->
                val offsetMinutes = settings.settings.value.offsetMinutes
                // The localized texts load asynchronously; the generic notification is already showing.
                subtitleJob = scope.launch {
                    val subtitle = formatAlarmSubtitle(alarm, offsetMinutes, loadFormatTexts())
                    if (player != null) {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(subtitle))
                    }
                }
            }
        }
        date = day
        // Before the first unlock the chosen file may be unreadable; play() falls back to the default alarm.
        play(if (unlocked) settings.settings.value.soundUri else null)
        vibrate()
        handler.postDelayed(::stopRinging, AUTO_STOP.inWholeMilliseconds)
    }

    private fun play(soundUri: String?) {
        player?.release()
        val candidates = listOfNotNull(
            soundUri?.let { if (it.startsWith("/")) Uri.fromFile(File(it)) else Uri.parse(it) },
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
        )
        player = candidates.firstNotNullOfOrNull { uri ->
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.onFailure { Log.w(TAG, "Cannot play $uri", it) }.getOrNull()
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(VibratorManager::class.java).defaultVibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
    }

    private fun scheduleSnooze(day: LocalDate) {
        val at = (Clock.System.now() + SNOOZE).toEpochMilliseconds()
        getSystemService(AlarmManager::class.java).setAlarmClock(
            AlarmManager.AlarmClockInfo(at, openAppPendingIntent(this)),
            ringPendingIntent(this, snoozeRequestCode(day), day, force = true),
        )
    }

    private fun stopRinging() {
        subtitleJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        player?.run { runCatching { stop() }; release() }
        player = null
        getSystemService(VibratorManager::class.java).defaultVibrator.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        scope.cancel()
        player?.release()
        super.onDestroy()
    }

    private fun notification(subtitle: String?): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.alarm_channel), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        val fullScreen = PendingIntent.getActivity(
            this, 1, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(subtitle ?: getString(R.string.alarm_notification_text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(Notification.Action.Builder(null, getString(R.string.action_snooze), serviceIntent(this, ACTION_SNOOZE)).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.action_dismiss), serviceIntent(this, ACTION_DISMISS)).build())
            .build()
    }

    companion object {
        const val ACTION_RING = "at.zanko.kalenderwecker.RING"
        const val ACTION_DISMISS = "at.zanko.kalenderwecker.DISMISS"
        const val ACTION_SNOOZE = "at.zanko.kalenderwecker.SNOOZE"
        private const val CHANNEL = "alarm"
        private const val NOTIFICATION_ID = 42
        private const val TAG = "KalenderWecker"
        private val SNOOZE = 5.minutes
        private val AUTO_STOP = 10.minutes

        fun serviceIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context, action.hashCode(), Intent(context, AlarmService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
