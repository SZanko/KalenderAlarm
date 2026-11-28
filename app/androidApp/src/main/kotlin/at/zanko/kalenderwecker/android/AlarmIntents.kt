package at.zanko.kalenderwecker.android

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import at.zanko.kalenderwecker.MainActivity
import kotlinx.datetime.LocalDate

const val EXTRA_DATE = "date"
const val EXTRA_FORCE = "force"
private const val SNOOZE_OFFSET = 1_000_000

/** One PendingIntent slot per day, so rescheduling replaces rather than duplicates. */
fun requestCode(date: LocalDate): Int = date.toEpochDays().toInt()

fun snoozeRequestCode(date: LocalDate): Int = requestCode(date) + SNOOZE_OFFSET

fun ringPendingIntent(context: Context, code: Int, date: LocalDate?, force: Boolean = false): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        code,
        Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_DATE, date?.toString())
            .putExtra(EXTRA_FORCE, force),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

fun openAppPendingIntent(context: Context): PendingIntent =
    PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
