package at.zanko.kalenderwecker.alarm

/** Platform hook that makes alarms actually ring. */
interface AlarmScheduler {
    /**
     * Replaces everything scheduled before with exactly [alarms].
     * Callers pass only enabled alarms.
     */
    suspend fun schedule(alarms: List<DayAlarm>)
}
