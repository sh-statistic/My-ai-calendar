package com.example.sync

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * Manages real sync with Google Calendar via the Android Calendar Content Provider.
 * Events, tasks and notes are written to the device calendar, and Google Calendar
 * automatically syncs them to Google servers when connected.
 *
 * Tasks are stored as all-day events with title prefix "[وظیفه] ".
 * Notes are stored as all-day events with title prefix "[یادداشت] ".
 */
class GoogleCalendarSyncManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "google_cal_sync_prefs"
        private const val KEY_SELECTED_CALENDAR_ID = "selected_calendar_id"
        private const val KEY_SELECTED_CALENDAR_NAME = "selected_calendar_name"
        private const val TASK_PREFIX = "[وظیفه] "
        private const val NOTE_PREFIX = "[یادداشت] "
        private const val APP_TAG = "[همگام] "
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ──────────────────────────────────────────────────────────────────────────
    // Calendar Account Discovery
    // ──────────────────────────────────────────────────────────────────────────

    data class CalendarAccount(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val ownerAccount: String,
        val isGoogle: Boolean
    )

    fun getAvailableCalendars(): List<CalendarAccount> {
        val calendars = mutableListOf<CalendarAccount>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val cursor: Cursor? = try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null, null
            )
        } catch (e: SecurityException) { null }

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val displayName = it.getString(1) ?: ""
                val accountName = it.getString(2) ?: ""
                val ownerAccount = it.getString(3) ?: ""
                val accountType = it.getString(4) ?: ""
                calendars.add(
                    CalendarAccount(
                        id = id,
                        displayName = displayName,
                        accountName = accountName,
                        ownerAccount = ownerAccount,
                        isGoogle = accountType == "com.google"
                    )
                )
            }
        }
        return calendars
    }

    fun getSelectedCalendarId(): Long {
        return prefs.getLong(KEY_SELECTED_CALENDAR_ID, -1L)
    }

    fun getSelectedCalendarName(): String {
        return prefs.getString(KEY_SELECTED_CALENDAR_NAME, "") ?: ""
    }

    fun selectCalendar(id: Long, name: String) {
        prefs.edit()
            .putLong(KEY_SELECTED_CALENDAR_ID, id)
            .putString(KEY_SELECTED_CALENDAR_NAME, name)
            .apply()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Event Sync
    // ──────────────────────────────────────────────────────────────────────────

    data class SyncResult(
        val eventsAdded: Int = 0,
        val tasksAdded: Int = 0,
        val notesAdded: Int = 0,
        val errors: Int = 0,
        val message: String = ""
    )

    fun syncEventsToGoogle(events: List<EventEntity>): SyncResult {
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است")

        var added = 0
        var errors = 0
        val resolver = context.contentResolver

        for (event in events) {
            if (event.isDeleted) continue
            try {
                // Check if already exists
                if (findEventByAppId(resolver, calId, event.id) != null) continue

                val startMillis = parseEventToMillis(event.gregorianDate, event.startTime)
                val endMillis = if (event.endTime.isNotBlank()) {
                    parseEventToMillis(event.gregorianDate, event.endTime)
                } else {
                    startMillis + 3600000L // +1 hour default
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, "$APP_TAG${event.title}")
                    put(CalendarContract.Events.DESCRIPTION, buildDescription(event))
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }

                resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                added++
            } catch (e: Exception) {
                errors++
            }
        }

        return SyncResult(eventsAdded = added, errors = errors,
            message = "✓ $added رویداد به تقویم گوگل اضافه شد" +
                    if (errors > 0) " ($errors خطا)" else "")
    }

    fun syncTasksToGoogle(tasks: List<TaskEntity>): SyncResult {
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است")

        var added = 0
        var errors = 0
        val resolver = context.contentResolver

        for (task in tasks) {
            if (task.isDeleted) continue
            try {
                if (findEventByAppId(resolver, calId, task.id) != null) continue

                val dateStr = task.gregorianDueDate.ifBlank {
                    val today = PersianCalendarHelper.getCurrentGregorianDate()
                    today.formatted
                }
                val startMillis = parseDateToMillis(dateStr)

                val statusText = if (task.isCompleted) "✅ انجام شده" else "⏳ در انتظار"
                val title = "$TASK_PREFIX${task.title}"
                val desc = "اولویت: ${task.priority}\nدسته: ${task.category}\nوضعیت: $statusText\n\napp_id:${task.id}"

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, desc)
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, startMillis + 86400000L)
                    put(CalendarContract.Events.ALL_DAY, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                }

                resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                added++
            } catch (e: Exception) {
                errors++
            }
        }

        return SyncResult(tasksAdded = added, errors = errors,
            message = "✓ $added وظیفه به تقویم گوگل اضافه شد" +
                    if (errors > 0) " ($errors خطا)" else "")
    }

    fun syncNotesToGoogle(notes: List<NoteEntity>): SyncResult {
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است")

        var added = 0
        var errors = 0
        val resolver = context.contentResolver

        for (note in notes) {
            if (note.isDeleted) continue
            try {
                if (findEventByAppId(resolver, calId, note.id) != null) continue

                val dateStr = if (note.persianDate.isNotBlank()) {
                    val pParts = note.persianDate.split("-", "/").mapNotNull { it.toIntOrNull() }
                    if (pParts.size == 3) {
                        PersianCalendarHelper.jalaliToGregorian(pParts[0], pParts[1], pParts[2]).formatted
                    } else {
                        PersianCalendarHelper.getCurrentGregorianDate().formatted
                    }
                } else {
                    PersianCalendarHelper.getCurrentGregorianDate().formatted
                }
                val startMillis = parseDateToMillis(dateStr)
                val title = "$NOTE_PREFIX${note.title}"
                val desc = "${note.content}\n\napp_id:${note.id}"

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, desc)
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, startMillis + 86400000L)
                    put(CalendarContract.Events.ALL_DAY, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                }

                resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                added++
            } catch (e: Exception) {
                errors++
            }
        }

        return SyncResult(notesAdded = added, errors = errors,
            message = "✓ $added یادداشت به تقویم گوگل اضافه شد" +
                    if (errors > 0) " ($errors خطا)" else "")
    }

    /**
     * Full sync: push all events, tasks, and notes to Google Calendar.
     */
    fun syncAll(
        events: List<EventEntity>,
        tasks: List<TaskEntity>,
        notes: List<NoteEntity>
    ): SyncResult {
        val evResult = syncEventsToGoogle(events)
        val tkResult = syncTasksToGoogle(tasks)
        val ntResult = syncNotesToGoogle(notes)

        val totalAdded = evResult.eventsAdded + tkResult.tasksAdded + ntResult.notesAdded
        val totalErrors = evResult.errors + tkResult.errors + ntResult.errors

        return SyncResult(
            eventsAdded = evResult.eventsAdded,
            tasksAdded = tkResult.tasksAdded,
            notesAdded = ntResult.notesAdded,
            errors = totalErrors,
            message = "همگام‌سازی کامل: $totalAdded مورد اضافه شد" +
                    " (${evResult.eventsAdded} رویداد، ${tkResult.tasksAdded} وظیفه، ${ntResult.notesAdded} یادداشت)" +
                    if (totalErrors > 0) " — $totalErrors خطا" else ""
        )
    }

    /**
     * Import events from Google Calendar into the app (pull).
     * Returns list of EventEntity objects parsed from Google Calendar.
     */
    fun importEventsFromGoogle(daysRange: Int = 90): List<EventEntity> {
        val calId = getSelectedCalendarId()
        if (calId == -1L) return emptyList()

        val imported = mutableListOf<EventEntity>()
        val now = Calendar.getInstance()
        val startMillis = now.timeInMillis
        now.add(Calendar.DAY_OF_YEAR, daysRange)
        val endMillis = now.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )

        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Events.DTSTART} >= ? AND " +
                "${CalendarContract.Events.DTSTART} <= ?"
        val selArgs = arrayOf(calId.toString(), startMillis.toString(), endMillis.toString())

        val cursor: Cursor? = try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection, selArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )
        } catch (e: SecurityException) { null }

        cursor?.use {
            while (it.moveToNext()) {
                val title = it.getString(1) ?: continue
                // Skip our own synced items
                if (title.startsWith(APP_TAG) || title.startsWith(TASK_PREFIX) || title.startsWith(NOTE_PREFIX)) continue

                val desc = it.getString(2) ?: ""
                val dtStart = it.getLong(3)

                val cal = Calendar.getInstance().apply { timeInMillis = dtStart }
                val gYear = cal.get(Calendar.YEAR)
                val gMonth = cal.get(Calendar.MONTH) + 1
                val gDay = cal.get(Calendar.DAY_OF_MONTH)
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)

                val jalali = PersianCalendarHelper.gregorianToJalali(gYear, gMonth, gDay)
                val timeStr = "%02d:%02d".format(hour, minute)
                val persianTime = PersianCalendarHelper.toPersianDigits(timeStr)

                imported.add(
                    EventEntity(
                        title = title,
                        description = desc,
                        persianDate = jalali.formatted,
                        gregorianDate = "%04d-%02d-%02d".format(gYear, gMonth, gDay),
                        startTime = persianTime,
                        category = "تقویم گوگل",
                        colorHex = "#4285F4"
                    )
                )
            }
        }
        return imported
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildDescription(event: EventEntity): String {
        val parts = mutableListOf<String>()
        if (event.description.isNotBlank()) parts.add(event.description)
        parts.add("دسته: ${event.category}")
        parts.add("app_id:${event.id}")
        return parts.joinToString("\n")
    }

    private fun findEventByAppId(resolver: ContentResolver, calId: Long, appId: String): Long? {
        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                "${CalendarContract.Events.DESCRIPTION} LIKE ?"
        val selArgs = arrayOf(calId.toString(), "%app_id:$appId%")

        val cursor: Cursor? = try {
            resolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, selArgs, null)
        } catch (e: SecurityException) { null }

        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }

    private fun parseEventToMillis(gregorianDate: String, time: String): Long {
        val cal = Calendar.getInstance()
        val dateParts = gregorianDate.split("-").mapNotNull { it.toIntOrNull() }
        if (dateParts.size == 3) {
            cal.set(Calendar.YEAR, dateParts[0])
            cal.set(Calendar.MONTH, dateParts[1] - 1)
            cal.set(Calendar.DAY_OF_MONTH, dateParts[2])
        }

        // Parse Persian digits in time
        val latinTime = persianToLatinDigits(time)
        val timeParts = latinTime.split(":").mapNotNull { it.toIntOrNull() }
        if (timeParts.size >= 2) {
            cal.set(Calendar.HOUR_OF_DAY, timeParts[0])
            cal.set(Calendar.MINUTE, timeParts[1])
        } else {
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun parseDateToMillis(gregorianDate: String): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val parts = gregorianDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size == 3) {
            cal.set(Calendar.YEAR, parts[0])
            cal.set(Calendar.MONTH, parts[1] - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2])
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun persianToLatinDigits(text: String): String {
        val persian = charArrayOf('\u06F0','\u06F1','\u06F2','\u06F3','\u06F4','\u06F5','\u06F6','\u06F7','\u06F8','\u06F9')
        val arabic  = charArrayOf('\u0660','\u0661','\u0662','\u0663','\u0664','\u0665','\u0666','\u0667','\u0668','\u0669')
        val sb = StringBuilder()
        for (ch in text) {
            val pi = persian.indexOf(ch)
            val ai = arabic.indexOf(ch)
            when {
                pi >= 0 -> sb.append(pi.toString())
                ai >= 0 -> sb.append(ai.toString())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
