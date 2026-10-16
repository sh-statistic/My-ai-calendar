package com.example.sync

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * Manages real sync with Google Calendar via the Android Calendar Content Provider.
 * Events, tasks and notes are written to the selected device calendar, and Google Calendar
 * automatically syncs them to Google cloud servers.
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
        val accountType: String,
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
        } catch (_: SecurityException) { null }

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
                        accountType = accountType,
                        isGoogle = accountType.equals("com.google", ignoreCase = true)
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

    fun ensureCalendarSelected(): Boolean {
        val currentId = getSelectedCalendarId()
        val calendars = getAvailableCalendars()
        if (currentId != -1L && calendars.any { it.id == currentId }) {
            return true
        }
        val target = calendars.firstOrNull { it.isGoogle } ?: calendars.firstOrNull()
        return if (target != null) {
            selectCalendar(target.id, target.displayName)
            true
        } else {
            false
        }
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
        ensureCalendarSelected()
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است یا مجوز تقویم داده نشده است")

        var added = 0
        var updated = 0
        var deleted = 0
        var errors = 0
        val resolver = context.contentResolver

        for (event in events) {
            // رویدادهای دریافتی از تقویم گوگل نباید دوباره به عنوان رویداد محلی پوش شوند
            if (event.category == "تقویم گوگل") continue

            try {
                val existingEventId = findEventByAppId(resolver, calId, event.id)

                if (event.isDeleted) {
                    if (existingEventId != null) {
                        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                        resolver.delete(deleteUri, null, null)
                        deleted++
                    }
                    continue
                }

                val isAllDay = event.startTime.isBlank()
                val startMillis: Long
                val endMillis: Long

                if (isAllDay) {
                    val dateStr = if (event.gregorianDate.isNotBlank()) event.gregorianDate else {
                        val pParts = event.persianDate.split("-", "/").mapNotNull { it.toIntOrNull() }
                        if (pParts.size == 3) {
                            PersianCalendarHelper.jalaliToGregorian(pParts[0], pParts[1], pParts[2]).formatted
                        } else {
                            PersianCalendarHelper.getCurrentGregorianDate().formatted
                        }
                    }
                    startMillis = parseDateToMillis(dateStr)
                    endMillis = startMillis + 86400000L
                } else {
                    startMillis = parseEventToMillis(event.persianDate, event.gregorianDate, event.startTime)
                    endMillis = if (event.endTime.isNotBlank()) {
                        parseEventToMillis(event.persianDate, event.gregorianDate, event.endTime)
                    } else {
                        startMillis + 3600000L
                    }
                }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, "$APP_TAG${event.title}")
                    put(CalendarContract.Events.DESCRIPTION, buildDescription(event))
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    if (isAllDay) {
                        put(CalendarContract.Events.ALL_DAY, 1)
                        put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    } else {
                        put(CalendarContract.Events.ALL_DAY, 0)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    }
                    put(CalendarContract.Events.HAS_ALARM, 1)
                }

                if (existingEventId != null) {
                    val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                    resolver.update(updateUri, values, null, null)
                    updated++
                } else {
                    resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    added++
                }
            } catch (_: Exception) {
                errors++
            }
        }

        val totalChanges = added + updated + deleted
        val msg = buildString {
            append("✓ $totalChanges رویداد پردازش شد ($added جدید، $updated به‌روزرسانی")
            if (deleted > 0) append("، $deleted حذف")
            append(")")
            if (errors > 0) append(" ($errors خطا)")
        }

        return SyncResult(eventsAdded = added + updated, errors = errors, message = msg)
    }

    fun syncTasksToGoogle(tasks: List<TaskEntity>): SyncResult {
        ensureCalendarSelected()
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است یا مجوز تقویم داده نشده است")

        var added = 0
        var updated = 0
        var deleted = 0
        var errors = 0
        val resolver = context.contentResolver

        for (task in tasks) {
            try {
                val existingEventId = findEventByAppId(resolver, calId, task.id)

                if (task.isDeleted) {
                    if (existingEventId != null) {
                        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                        resolver.delete(deleteUri, null, null)
                        deleted++
                    }
                    continue
                }

                val dateStr = if (task.gregorianDueDate.isNotBlank()) {
                    task.gregorianDueDate
                } else if (task.persianDueDate.isNotBlank()) {
                    val pParts = task.persianDueDate.split("-", "/").mapNotNull { it.toIntOrNull() }
                    if (pParts.size == 3) {
                        PersianCalendarHelper.jalaliToGregorian(pParts[0], pParts[1], pParts[2]).formatted
                    } else {
                        PersianCalendarHelper.getCurrentGregorianDate().formatted
                    }
                } else {
                    PersianCalendarHelper.getCurrentGregorianDate().formatted
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

                if (existingEventId != null) {
                    val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                    resolver.update(updateUri, values, null, null)
                    updated++
                } else {
                    resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    added++
                }
            } catch (_: Exception) {
                errors++
            }
        }

        val totalChanges = added + updated + deleted
        val msg = buildString {
            append("✓ $totalChanges وظیفه پردازش شد ($added جدید، $updated به‌روزرسانی")
            if (deleted > 0) append("، $deleted حذف")
            append(")")
            if (errors > 0) append(" ($errors خطا)")
        }

        return SyncResult(tasksAdded = added + updated, errors = errors, message = msg)
    }

    fun syncNotesToGoogle(notes: List<NoteEntity>): SyncResult {
        ensureCalendarSelected()
        val calId = getSelectedCalendarId()
        if (calId == -1L) return SyncResult(message = "تقویمی انتخاب نشده است یا مجوز تقویم داده نشده است")

        var added = 0
        var updated = 0
        var deleted = 0
        var errors = 0
        val resolver = context.contentResolver

        for (note in notes) {
            try {
                val existingEventId = findEventByAppId(resolver, calId, note.id)

                if (note.isDeleted) {
                    if (existingEventId != null) {
                        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                        resolver.delete(deleteUri, null, null)
                        deleted++
                    }
                    continue
                }

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

                if (existingEventId != null) {
                    val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingEventId)
                    resolver.update(updateUri, values, null, null)
                    updated++
                } else {
                    resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    added++
                }
            } catch (_: Exception) {
                errors++
            }
        }

        val totalChanges = added + updated + deleted
        val msg = buildString {
            append("✓ $totalChanges یادداشت پردازش شد ($added جدید، $updated به‌روزرسانی")
            if (deleted > 0) append("، $deleted حذف")
            append(")")
            if (errors > 0) append(" ($errors خطا)")
        }

        return SyncResult(notesAdded = added + updated, errors = errors, message = msg)
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
            message = "همگام‌سازی کامل انجام شد: $totalAdded مورد پردازش شد" +
                    " (${evResult.eventsAdded} رویداد، ${tkResult.tasksAdded} وظیفه، ${ntResult.notesAdded} یادداشت)" +
                    if (totalErrors > 0) " — $totalErrors خطا" else ""
        )
    }

    /**
     * Import events from Google Calendar into the app (pull).
     * Retrieves events ONLY from the selected calendar to respect user choice.
     */
    fun importEventsFromGoogle(pastDays: Int = 1095, futureDays: Int = 1095): List<EventEntity> {
        ensureCalendarSelected()
        val selectedId = getSelectedCalendarId()
        if (selectedId == -1L) return emptyList()

        val imported = mutableListOf<EventEntity>()
        val seenKeys = mutableSetOf<String>()

        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -pastDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, futureDays)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = startCal.timeInMillis
        val endMillis = endCal.timeInMillis

        // 1. Try querying CalendarContract.Instances first (handles recurring events and instances)
        try {
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)
            val instancesUri = builder.build()

            val instanceProjection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID
            )

            val selection = "${CalendarContract.Instances.CALENDAR_ID} = ?"
            val selArgs = arrayOf(selectedId.toString())

            val cursor = context.contentResolver.query(
                instancesUri,
                instanceProjection,
                selection,
                selArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val title = it.getString(1) ?: continue
                    if (title.startsWith(APP_TAG) || title.startsWith(TASK_PREFIX) || title.startsWith(NOTE_PREFIX)) continue

                    val desc = it.getString(2) ?: ""
                    if (desc.contains("app_id:")) continue

                    val dtStart = it.getLong(3)
                    val dtEnd = it.getLong(4)
                    val isAllDay = it.getInt(5) == 1

                    val cal = if (isAllDay) {
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dtStart }
                    } else {
                        Calendar.getInstance().apply { timeInMillis = dtStart }
                    }

                    val gYear = cal.get(Calendar.YEAR)
                    val gMonth = cal.get(Calendar.MONTH) + 1
                    val gDay = cal.get(Calendar.DAY_OF_MONTH)

                    val jalali = PersianCalendarHelper.gregorianToJalali(gYear, gMonth, gDay)
                    val gregorianDate = "%04d-%02d-%02d".format(gYear, gMonth, gDay)

                    val startTime = if (isAllDay) {
                        ""
                    } else {
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        val minute = cal.get(Calendar.MINUTE)
                        "%02d:%02d".format(hour, minute)
                    }

                    val endTime = if (isAllDay || dtEnd <= dtStart) {
                        ""
                    } else {
                        val endCalLocal = Calendar.getInstance().apply { timeInMillis = dtEnd }
                        val hour = endCalLocal.get(Calendar.HOUR_OF_DAY)
                        val minute = endCalLocal.get(Calendar.MINUTE)
                        "%02d:%02d".format(hour, minute)
                    }

                    val dedupKey = "$title|$gregorianDate|$startTime"
                    if (seenKeys.add(dedupKey)) {
                        imported.add(
                            EventEntity(
                                title = title,
                                description = desc,
                                persianDate = jalali.formatted,
                                gregorianDate = gregorianDate,
                                startTime = startTime,
                                endTime = endTime,
                                category = "تقویم گوگل",
                                colorHex = "#4285F4"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback to CalendarContract.Events if Instances returned nothing
        if (imported.isEmpty()) {
            try {
                val eventsProjection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DESCRIPTION,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.CALENDAR_ID
                )

                val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND " +
                        "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selArgs = arrayOf(selectedId.toString(), startMillis.toString(), endMillis.toString())

                val cursor = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    eventsProjection,
                    selection,
                    selArgs,
                    "${CalendarContract.Events.DTSTART} ASC"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val title = it.getString(1) ?: continue
                        if (title.startsWith(APP_TAG) || title.startsWith(TASK_PREFIX) || title.startsWith(NOTE_PREFIX)) continue

                        val desc = it.getString(2) ?: ""
                        if (desc.contains("app_id:")) continue

                        val dtStart = it.getLong(3)
                        val dtEnd = it.getLong(4)
                        val isAllDay = it.getInt(5) == 1

                        val cal = if (isAllDay) {
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dtStart }
                        } else {
                            Calendar.getInstance().apply { timeInMillis = dtStart }
                        }

                        val gYear = cal.get(Calendar.YEAR)
                        val gMonth = cal.get(Calendar.MONTH) + 1
                        val gDay = cal.get(Calendar.DAY_OF_MONTH)

                        val jalali = PersianCalendarHelper.gregorianToJalali(gYear, gMonth, gDay)
                        val gregorianDate = "%04d-%02d-%02d".format(gYear, gMonth, gDay)

                        val startTime = if (isAllDay) {
                            ""
                        } else {
                            val hour = cal.get(Calendar.HOUR_OF_DAY)
                            val minute = cal.get(Calendar.MINUTE)
                            "%02d:%02d".format(hour, minute)
                        }

                        val endTime = if (isAllDay || dtEnd <= dtStart) {
                            ""
                        } else {
                            val endCalLocal = Calendar.getInstance().apply { timeInMillis = dtEnd }
                            val hour = endCalLocal.get(Calendar.HOUR_OF_DAY)
                            val minute = endCalLocal.get(Calendar.MINUTE)
                            "%02d:%02d".format(hour, minute)
                        }

                        val dedupKey = "$title|$gregorianDate|$startTime"
                        if (seenKeys.add(dedupKey)) {
                            imported.add(
                                EventEntity(
                                    title = title,
                                    description = desc,
                                    persianDate = jalali.formatted,
                                    gregorianDate = gregorianDate,
                                    startTime = startTime,
                                    endTime = endTime,
                                    category = "تقویم گوگل",
                                    colorHex = "#4285F4"
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
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
        } catch (_: SecurityException) { null }

        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return null
    }

    private fun parseEventToMillis(persianDate: String, gregorianDate: String, time: String): Long {
        val cal = Calendar.getInstance()
        var dateParts = gregorianDate.split("-").mapNotNull { it.toIntOrNull() }
        if (dateParts.size != 3 && persianDate.isNotBlank()) {
            val pParts = persianDate.split("-", "/").mapNotNull { it.toIntOrNull() }
            if (pParts.size == 3) {
                val g = PersianCalendarHelper.jalaliToGregorian(pParts[0], pParts[1], pParts[2])
                dateParts = listOf(g.year, g.month, g.day)
            }
        }

        if (dateParts.size == 3) {
            cal.set(Calendar.YEAR, dateParts[0])
            cal.set(Calendar.MONTH, dateParts[1] - 1)
            cal.set(Calendar.DAY_OF_MONTH, dateParts[2])
        }

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
        val persian = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
        val arabic  = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
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
