package com.example

import com.example.alarm.EventAlarmScheduler
import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the pure alarm trigger calculation (spec
 * 001-recurring-event-reminder: FR-002/FR-004 — every occurrence rings,
 * only strictly-future triggers are scheduled).
 *
 * "Now" is fixed at 1405/08/23 08:00 local time.
 */
class EventAlarmTriggerTest {

    private fun millisOf(jYear: Int, jMonth: Int, jDay: Int, hour: Int, minute: Int): Long {
        val g = PersianCalendarHelper.jalaliToGregorian(jYear, jMonth, jDay)
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, g.year)
            set(Calendar.MONTH, g.month - 1)
            set(Calendar.DAY_OF_MONTH, g.day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private val now = millisOf(1405, 8, 23, 8, 0)

    private fun event(
        date: String = "1405/08/24",
        time: String = "09:00",
        description: String = "",
        isDeleted: Boolean = false
    ) = EventEntity(
        title = "تست",
        description = description,
        persianDate = date,
        startTime = time,
        isDeleted = isDeleted
    )

    @Test
    fun futureEvent_noReminderTag_triggersAtEventTime() {
        assertEquals(millisOf(1405, 8, 24, 9, 0), EventAlarmScheduler.triggerTimeMillis(event(), now))
    }

    @Test
    fun reminderOffset_isSubtracted() {
        val e = event(date = "1405/08/23", time = "10:00", description = "x [یادآوری:۵ دقیقه قبل]")
        assertEquals(millisOf(1405, 8, 23, 9, 55), EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun oneDayBeforeOffset_landsOnPreviousDay() {
        val e = event(date = "1405/08/25", time = "00:30", description = "[یادآوری:۱ روز قبل]")
        assertEquals(millisOf(1405, 8, 24, 0, 30), EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun oneHourBeforeOffset_crossesMidnight() {
        val e = event(date = "1405/08/24", time = "00:30", description = "[یادآوری:۱ ساعت قبل]")
        assertEquals(millisOf(1405, 8, 23, 23, 30), EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun pastEvent_doesNotTrigger() {
        val e = event(date = "1405/08/23", time = "07:00")
        assertNull(EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun offsetPushingTriggerIntoPast_doesNotTrigger() {
        val e = event(date = "1405/08/23", time = "08:30", description = "[یادآوری:۱ ساعت قبل]")
        assertNull(EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun noRingReminder_doesNotTrigger() {
        val e = event(description = "[یادآوری:بدون زنگ]")
        assertNull(EventAlarmScheduler.triggerTimeMillis(e, now))
    }

    @Test
    fun blankTimeOrDeleted_doesNotTrigger() {
        assertNull(EventAlarmScheduler.triggerTimeMillis(event(time = ""), now))
        assertNull(EventAlarmScheduler.triggerTimeMillis(event(isDeleted = true), now))
    }
}
