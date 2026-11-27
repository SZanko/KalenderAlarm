package at.zanko.kalenderwecker.android

import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * The only alarm data kept in device-protected storage: enough to re-arm
 * and ring before the first unlock after a reboot, nothing about the
 * schedule itself (no link, course names, rooms).
 */
data class PlannedAlarm(val date: LocalDate, val epochMillis: Long)

fun List<DayAlarm>.toPlanned(): List<PlannedAlarm> = map { PlannedAlarm(it.date, it.time.toEpochMilliseconds()) }

fun encodePlanned(planned: List<PlannedAlarm>): Set<String> =
    planned.map { "${it.date}|${it.epochMillis}" }.toSet()

fun decodePlanned(raw: Set<String>): List<PlannedAlarm> = raw.mapNotNull { entry ->
    val (date, millis) = entry.split('|').takeIf { it.size == 2 } ?: return@mapNotNull null
    runCatching { PlannedAlarm(LocalDate.parse(date), millis.toLong()) }.getOrNull()
}.sortedBy { it.epochMillis }

fun alarmsToRearm(planned: List<PlannedAlarm>, now: Instant): List<PlannedAlarm> =
    planned.filter { it.epochMillis > now.toEpochMilliseconds() }

/** While locked, settings are unreadable: ring only what was planned (enabled) or a snooze. */
fun lockedRingAllowed(planned: List<PlannedAlarm>, date: LocalDate, force: Boolean): Boolean =
    force || planned.any { it.date == date }
