package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

class SchedulingException(message: String) : Exception(message)

/** Keeps `~/.config/systemd/user` in sync with the planned alarms. Throws [SchedulingException] if systemd refuses. */
class SystemdTimerScheduler(
    private val unitDir: Path = defaultUnitDir(),
    private val runner: CommandRunner,
    private val exec: List<String>,
    private val env: Map<String, String> = System.getenv(),
    private val zone: () -> TimeZone,
) : AlarmScheduler {

    override suspend fun schedule(alarms: List<DayAlarm>) = withContext(Dispatchers.IO) {
        unitDir.createDirectories()
        val wanted = alarms.map { SystemdUnits.alarmName(it.date) }.toSet()

        existingAlarmNames().filter { it !in wanted }.forEach { stale ->
            runner.run("systemctl", "--user", "disable", "--now", "$stale.timer")
            unitDir.resolve("$stale.timer").deleteIfExists()
            unitDir.resolve("$stale.service").deleteIfExists()
        }

        val tz = zone()
        alarms.forEach { alarm ->
            val name = SystemdUnits.alarmName(alarm.date)
            unitDir.resolve("$name.timer").writeText(SystemdUnits.alarmTimer(alarm, tz))
            unitDir.resolve("$name.service").writeText(SystemdUnits.alarmService(alarm.date, exec, env))
        }
        unitDir.resolve("${SystemdUnits.REFRESH}.timer").writeText(SystemdUnits.refreshTimer())
        unitDir.resolve("${SystemdUnits.REFRESH}.service").writeText(SystemdUnits.refreshService(exec, env))

        runChecked("systemctl", "--user", "daemon-reload")
        val timers = wanted.map { "$it.timer" } + "${SystemdUnits.REFRESH}.timer"
        runChecked("systemctl", "--user", "enable", "--now", *timers.toTypedArray())
    }

    private fun runChecked(vararg cmd: String) {
        val code = runner.run(*cmd)
        if (code != 0) throw SchedulingException("`${cmd.take(4).joinToString(" ")}` failed with exit code $code")
    }

    private fun existingAlarmNames(): Set<String> =
        if (!Files.isDirectory(unitDir)) emptySet()
        else unitDir.listDirectoryEntries("${SystemdUnits.PREFIX}*")
            .map { it.name.substringBeforeLast('.') }
            .toSet()

    companion object {
        fun defaultUnitDir(): Path {
            val config = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                ?: (System.getProperty("user.home") + "/.config")
            return Path(config, "systemd", "user")
        }
    }
}
