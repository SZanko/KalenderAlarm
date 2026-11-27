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

const val MAIN_CLASS = "at.zanko.kalenderwecker.MainKt"

/** Command that starts this app on a plain JVM (`./gradlew run`, IDE). */
fun jvmExec(javaHome: String, classPath: String, mainClass: String = MAIN_CLASS): List<String> =
    listOf("$javaHome/bin/java", "-cp", classPath, mainClass)

/**
 * Exec for the running app: `KALENDERWECKER_EXEC`, else the jpackage launcher
 * (installed .deb/AppImage), else java + the current classpath. The process
 * command line is not used: under Gradle its arguments are not visible.
 */
fun currentExec(): List<String> {
    resolveExec(System.getenv(), null, emptyList()).takeIf { it.isNotEmpty() }?.let { return it }
    System.getProperty("jpackage.app-path")?.takeIf { it.isNotBlank() }?.let { return listOf(it) }
    return jvmExec(System.getProperty("java.home"), System.getProperty("java.class.path"))
}
