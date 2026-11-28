package at.zanko.kalenderwecker.desktop

import at.zanko.kalenderwecker.alarm.AlarmScheduler
import at.zanko.kalenderwecker.alarm.DayAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Fallback without systemd: rings only while this process is running. */
class InProcessScheduler(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val onRing: (LocalDate) -> Unit,
) : AlarmScheduler {
    private var job: Job? = null

    override suspend fun schedule(alarms: List<DayAlarm>) {
        job?.cancel()
        job = scope.launch {
            alarms.sortedBy { it.time }.forEach { alarm ->
                launch {
                    delay(alarm.time - clock.now())
                    onRing(alarm.date)
                }
            }
        }
    }
}
