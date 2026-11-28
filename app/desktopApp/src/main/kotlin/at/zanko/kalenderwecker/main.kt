package at.zanko.kalenderwecker

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import at.zanko.kalenderwecker.desktop.appIcon
import androidx.compose.ui.window.application
import at.zanko.kalenderwecker.alarm.AlarmSyncer
import at.zanko.kalenderwecker.data.SettingsRepository
import at.zanko.kalenderwecker.desktop.CommandRunner
import at.zanko.kalenderwecker.desktop.DesktopAlarmPlayer
import at.zanko.kalenderwecker.desktop.DesktopCapabilities
import at.zanko.kalenderwecker.desktop.DesktopMode
import at.zanko.kalenderwecker.desktop.RingRequests
import at.zanko.kalenderwecker.desktop.RingWindow
import at.zanko.kalenderwecker.desktop.SystemdUnits
import at.zanko.kalenderwecker.desktop.currentExec
import at.zanko.kalenderwecker.desktop.desktopModule
import at.zanko.kalenderwecker.desktop.parseArgs
import at.zanko.kalenderwecker.desktop.shouldRing
import at.zanko.kalenderwecker.di.initKoin
import at.zanko.kalenderwecker.ui.LocalAppLocale
import at.zanko.kalenderwecker.resources.Res
import at.zanko.kalenderwecker.resources.desktop_in_process
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.koin.core.Koin
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private val SNOOZE = 5.minutes

fun main(args: Array<String>) {
    val koin = initKoin(desktopModule).koin
    LocalAppLocale.applyAtStartup(koin.get<SettingsRepository>().settings.value.language)
    when (val mode = parseArgs(args)) {
        DesktopMode.Refresh -> {
            val syncer = koin.get<AlarmSyncer>()
            val result = runBlocking { syncer.refreshAndSync() }
            result.exceptionOrNull()?.let { System.err.println("KalenderWecker refresh failed: ${it.message}") }
            syncer.schedulingError.value?.let { System.err.println("KalenderWecker could not set alarms: $it") }
            exitProcess(if (result.isSuccess && syncer.schedulingError.value == null) 0 else 1)
        }
        is DesktopMode.Ring -> ringOnly(koin, mode)
        DesktopMode.Main -> mainWindow(koin)
    }
}

/** Launched by a systemd timer: re-check the plan, then show only the ring window. */
private fun ringOnly(koin: Koin, mode: DesktopMode.Ring) {
    val syncer = koin.get<AlarmSyncer>()
    val alarm = syncer.alarmFor(mode.date)
    if (!shouldRing(alarm, Clock.System.now(), mode.force)) exitProcess(0)
    val settings = koin.get<SettingsRepository>().settings.value
    application {
        RingWindow(
            alarm = alarm,
            offsetMinutes = settings.offsetMinutes,
            soundPath = settings.soundUri,
            player = koin.get(),
            onDismiss = ::exitApplication,
            onSnooze = {
                scheduleSnooze(koin.get(), mode.date)
                exitApplication()
            },
        )
    }
}

/** A one-shot transient systemd timer that re-runs `--ring <date> --force` in 5 minutes. */
private fun scheduleSnooze(runner: CommandRunner, date: LocalDate) {
    val env = SystemdUnits.PASSED_ENV.mapNotNull { k -> System.getenv(k)?.let { "--setenv=$k=$it" } }
    val cmd = listOf(
        "systemd-run", "--user", "--on-active=${SNOOZE.inWholeSeconds}s",
        "--unit=kalenderwecker-snooze-${Clock.System.now().epochSeconds}",
    ) + env + currentExec() + listOf("--ring", date.toString(), "--force")
    runner.run(*cmd.toTypedArray())
}

private fun mainWindow(koin: Koin) {
    val syncer = koin.get<AlarmSyncer>()
    val requests = koin.get<RingRequests>()
    val systemd = koin.get<DesktopCapabilities>().systemd
    val player = koin.get<DesktopAlarmPlayer>()
    val settingsRepo = koin.get<SettingsRepository>()
    application {
        val scope = rememberCoroutineScope()
        val ringing by requests.date.collectAsState()
        LaunchedEffect(Unit) { syncer.sync() }
        Window(onCloseRequest = ::exitApplication, title = "KalenderWecker", icon = appIcon) {
            App(banner = { if (!systemd) InProcessBanner() })
        }
        ringing?.let { date ->
            val settings = settingsRepo.settings.value
            RingWindow(
                alarm = syncer.alarmFor(date),
                offsetMinutes = settings.offsetMinutes,
                soundPath = settings.soundUri,
                player = player,
                onDismiss = requests::clear,
                onSnooze = {
                    requests.clear()
                    if (systemd) {
                        scheduleSnooze(koin.get(), date)
                    } else {
                        scope.launch {
                            delay(SNOOZE)
                            requests.ring(date)
                        }
                    }
                },
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun InProcessBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(
            stringResource(Res.string.desktop_in_process),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
