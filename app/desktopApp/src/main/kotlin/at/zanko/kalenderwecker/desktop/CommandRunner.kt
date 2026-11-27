package at.zanko.kalenderwecker.desktop

/** Runs an external command and returns its exit code. */
fun interface CommandRunner {
    fun run(vararg cmd: String): Int
}

class ProcessCommandRunner : CommandRunner {
    override fun run(vararg cmd: String): Int = runCatching {
        ProcessBuilder(*cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start().waitFor()
    }.getOrDefault(-1)
}

/** True when a systemd user manager is reachable (Linux desktops with systemd). */
fun systemdAvailable(runner: CommandRunner): Boolean =
    runner.run("systemctl", "--user", "show-environment") == 0
