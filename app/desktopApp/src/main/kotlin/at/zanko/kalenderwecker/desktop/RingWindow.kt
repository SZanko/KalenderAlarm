package at.zanko.kalenderwecker.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import at.zanko.kalenderwecker.alarm.DayAlarm
import at.zanko.kalenderwecker.resources.Res
import at.zanko.kalenderwecker.resources.ring_title
import at.zanko.kalenderwecker.resources.ring_window_title
import at.zanko.kalenderwecker.ui.LocalizedContent
import at.zanko.kalenderwecker.ui.formatAlarmSubtitle
import at.zanko.kalenderwecker.ui.rememberFormatTexts
import org.jetbrains.compose.resources.stringResource
import at.zanko.kalenderwecker.ui.formatTime
import at.zanko.kalenderwecker.ui.ring.RingScreen
import at.zanko.kalenderwecker.ui.theme.KalenderWeckerTheme
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

val AUTO_STOP = 10.minutes

/** Always-on-top window that plays the alarm until dismissed, snoozed or [AUTO_STOP]. */
@Composable
fun RingWindow(
    alarm: DayAlarm?,
    offsetMinutes: Int,
    soundPath: String?,
    player: DesktopAlarmPlayer,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    DisposableEffect(Unit) {
        player.start(soundPath)
        onDispose { player.stop() }
    }
    LaunchedEffect(Unit) {
        delay(AUTO_STOP)
        onDismiss()
    }
    Window(
        onCloseRequest = onDismiss,
        title = stringResource(Res.string.ring_window_title),
        alwaysOnTop = true,
        icon = appIcon,
        state = rememberWindowState(width = 420.dp, height = 640.dp, position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)),
    ) {
        LocalizedContent {
            KalenderWeckerTheme {
                RingScreen(
                    time = formatTime(Clock.System.now(), TimeZone.currentSystemDefault()),
                    title = stringResource(Res.string.ring_title),
                    subtitle = alarm?.let { formatAlarmSubtitle(it, offsetMinutes, rememberFormatTexts()) },
                    onDismiss = onDismiss,
                    onSnooze = onSnooze,
                )
            }
        }
    }
}
