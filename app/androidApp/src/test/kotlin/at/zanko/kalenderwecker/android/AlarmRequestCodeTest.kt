package at.zanko.kalenderwecker.android

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AlarmRequestCodeTest {
    @Test
    fun gIVEN_date_WHEN_requestCode_THEN_epochDay() {
        assertEquals(20_787, requestCode(LocalDate.parse("2026-11-30")))
    }

    @Test
    fun gIVEN_differentDates_WHEN_requestCode_THEN_distinct() {
        assertNotEquals(requestCode(LocalDate.parse("2026-11-30")), requestCode(LocalDate.parse("2026-12-01")))
    }

    @Test
    fun gIVEN_snooze_WHEN_snoozeRequestCode_THEN_doesNotCollideWithDayCodes() {
        val date = LocalDate.parse("2026-11-30")
        assertNotEquals(requestCode(date), snoozeRequestCode(date))
    }
}
