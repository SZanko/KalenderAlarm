package at.zanko.kalenderwecker.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.resources.Res
import at.zanko.kalenderwecker.resources.ring_title
import at.zanko.kalenderwecker.ui.LocalizedContent
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.rememberFormatTexts
import org.jetbrains.compose.resources.stringResource
import at.zanko.kalenderwecker.ui.formatTime
import at.zanko.kalenderwecker.ui.ring.RingScreen
import at.zanko.kalenderwecker.ui.theme.KalenderWeckerTheme
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.android.inject
import kotlin.time.Clock

/** Shown over the lock screen by the alarm's full-screen intent. */
class AlarmActivity : ComponentActivity() {
    private val syncer: AlarmSyncer by inject()
    private val settings: SettingsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val alarm = if (isUserUnlocked(this)) syncer.alarmFor(now.toLocalDateTime(zone).date) else null
        val offsetMinutes = settings.settings.value.offsetMinutes
        setContent {
            LocalizedContent {
                KalenderWeckerTheme {
                    RingScreen(
                        time = formatTime(now, zone),
                        title = stringResource(Res.string.ring_title),
                        subtitle = alarm?.let { formatAlarmSubtitle(it, offsetMinutes, rememberFormatTexts()) },
                        onDismiss = { send(AlarmService.ACTION_DISMISS) },
                        onSnooze = { send(AlarmService.ACTION_SNOOZE) },
                    )
                }
            }
        }
    }

    private fun send(action: String) {
        startService(Intent(this, AlarmService::class.java).setAction(action))
        finish()
    }
}
