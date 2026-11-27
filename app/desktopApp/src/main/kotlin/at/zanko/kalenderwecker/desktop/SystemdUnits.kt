package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Renders the systemd user units that make desktop alarms ring without a resident process. */
object SystemdUnits {
    const val PREFIX = "kalenderwecker-alarm-"
    const val REFRESH = "kalenderwecker-refresh"

    fun alarmName(date: LocalDate): String = "$PREFIX$date"

    fun alarmTimer(alarm: DayAlarm, zone: TimeZone): String {
        val local = alarm.time.toLocalDateTime(zone)
        val stamp = "${local.date} ${two(local.hour)}:${two(local.minute)}:${two(local.second)}"
        return """
            [Unit]
            Description=KalenderWecker wake-up alarm for ${alarm.date}

            [Timer]
            OnCalendar=$stamp ${zone.id}
            Persistent=true
            AccuracySec=1s
            Unit=${alarmName(alarm.date)}.service

            [Install]
            WantedBy=timers.target

        """.trimIndent()
    }

    /** Variables a GUI launched by the systemd user manager needs and may not inherit. */
    val PASSED_ENV = listOf(
        "DISPLAY", "WAYLAND_DISPLAY", "XAUTHORITY", "XDG_RUNTIME_DIR", "XDG_CONFIG_HOME",
        "LD_LIBRARY_PATH", "JAVA_HOME", "PULSE_SERVER", "KALENDERWECKER_EXEC",
    )

    fun alarmService(date: LocalDate, exec: List<String>, env: Map<String, String> = emptyMap()): String = service(
        "KalenderWecker ring window for $date",
        exec + listOf("--ring", date.toString()),
        env,
    )

    fun refreshTimer(): String = """
        [Unit]
        Description=Refresh KalenderWecker schedule from TimeEdit

        [Timer]
        OnBootSec=2min
        OnUnitActiveSec=6h
        Persistent=true
        Unit=$REFRESH.service

        [Install]
        WantedBy=timers.target

    """.trimIndent()

    fun refreshService(exec: List<String>, env: Map<String, String> = emptyMap()): String =
        service("Refresh KalenderWecker schedule", exec + "--refresh", env)

    private fun service(description: String, command: List<String>, env: Map<String, String>): String {
        val environment = PASSED_ENV.mapNotNull { key ->
            env[key]?.takeIf { it.isNotEmpty() }?.let { "Environment=${quote("$key=$it")}\n" }
        }.joinToString("")
        return "[Unit]\nDescription=$description\n\n[Service]\nType=oneshot\n" +
            environment +
            "ExecStart=${command.joinToString(" ") { quote(it) }}\n"
    }

    /** systemd ExecStart quoting: double quotes, C-style escapes, `%` specifiers and `$` doubled. */
    private fun quote(arg: String): String = "\"" + arg
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("%", "%%")
        .replace("$", "$$") + "\""

    private fun two(n: Int) = n.toString().padStart(2, '0')
}
