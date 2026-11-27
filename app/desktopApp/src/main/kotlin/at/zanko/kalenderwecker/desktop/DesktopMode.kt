package at.zanko.kalenderwecker.desktop

import kotlinx.datetime.LocalDate

/** How the desktop executable was launched. */
sealed interface DesktopMode {
    /** Normal window. */
    data object Main : DesktopMode

    /** Started by a systemd timer (or snooze): show only the ringing window for [date]. */
    data class Ring(val date: LocalDate, val force: Boolean) : DesktopMode

    /** Started by the refresh timer: fetch, re-plan, rewrite timers, exit. */
    data object Refresh : DesktopMode
}

fun parseArgs(args: Array<String>): DesktopMode {
    if ("--refresh" in args) return DesktopMode.Refresh
    val ring = args.indexOf("--ring")
    if (ring >= 0) {
        val date = args.getOrNull(ring + 1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (date != null) return DesktopMode.Ring(date, force = "--force" in args)
    }
    return DesktopMode.Main
}
