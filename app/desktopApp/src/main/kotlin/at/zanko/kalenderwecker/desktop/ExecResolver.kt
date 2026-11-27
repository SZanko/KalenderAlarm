package at.zanko.kalenderwecker.desktop

private val MODE_FLAGS = setOf("--ring", "--refresh", "--force")
private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")

/**
 * The command systemd units should run to start this app again.
 * `KALENDERWECKER_EXEC` wins (installed package / nix wrapper); otherwise the
 * current process command line minus any mode arguments.
 */
fun resolveExec(env: Map<String, String>, command: String?, args: List<String>): List<String> {
    env["KALENDERWECKER_EXEC"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it.split(Regex("\\s+")) }
    if (command == null) return emptyList()
    val kept = args.filterIndexed { i, arg ->
        arg !in MODE_FLAGS && !(DATE.matches(arg) && args.getOrNull(i - 1) == "--ring")
    }
    return listOf(command) + kept
}

/** [resolveExec] for the running JVM. */
fun currentExec(): List<String> {
    val info = ProcessHandle.current().info()
    return resolveExec(System.getenv(), info.command().orElse(null), info.arguments().map { it.toList() }.orElse(emptyList()))
}
