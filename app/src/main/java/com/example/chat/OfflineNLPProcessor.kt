package com.example.chat

import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.EventEntity
import com.example.calendar.PersianCalendarHelper
import java.util.UUID
import java.util.Calendar

object OfflineNLPProcessor {

    sealed class NLPResult {
        data class AddNote(val note: NoteEntity) : NLPResult()
        data class AddTask(val task: TaskEntity) : NLPResult()
        data class AddEvent(val event: EventEntity) : NLPResult()
        object NotUnderstood : NLPResult()
    }

    private fun normalizeDigits(text: String): String {
        var res = text
        val persian = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
        val arabic = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        for (i in 0..9) {
            res = res.replace(persian[i], i.toString()).replace(arabic[i], i.toString())
        }
        return res
    }

    fun processCommand(text: String): NLPResult {
        val trimmed = text.trim()
        val normalized = normalizeDigits(trimmed)
        
        val currentJalali = PersianCalendarHelper.getCurrentJalaliDate()
        
        // 1. Extract Explicit Dates (e.g., 22 مهر)
        var explicitMonth = -1
        var explicitDay = -1
        for ((index, m) in PersianCalendarHelper.PERSIAN_MONTH_NAMES.withIndex()) {
            val regex = Regex("(\\d{1,2})\\s+$m")
            val match = regex.find(normalized)
            if (match != null) {
                explicitDay = match.groupValues[1].toInt()
                explicitMonth = index + 1
                break
            }
        }

        // 2. Extract Relative Dates
        var dateShift = 0
        if (normalized.contains("پس فردا") || normalized.contains("پس‌فردا")) {
            dateShift = 2
        } else if (normalized.contains("فردا")) {
            dateShift = 1
        }
        
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dateShift)
        val shiftedJalali = PersianCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )

        // Determine final date
        val finalYear = currentJalali.year
        val finalMonth = if (explicitMonth != -1) explicitMonth else shiftedJalali.month
        val finalDay = if (explicitDay != -1) explicitDay else shiftedJalali.day
        val formattedDate = "%04d/%02d/%02d".format(finalYear, finalMonth, finalDay)
        val persianFormattedDate = PersianCalendarHelper.toPersianDigits(formattedDate)

        // 3. Extract Time
        var explicitTime = ""
        val timeRegex = Regex("ساعت\\s+(\\d{1,2}(:\\d{2})?)")
        val timeMatch = timeRegex.find(normalized)
        if (timeMatch != null) {
            var timeStr = timeMatch.groupValues[1]
            if (!timeStr.contains(":")) timeStr += ":00"
            val parts = timeStr.split(":")
            explicitTime = "%02d:%02d".format(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
        }

        // 4. Determine Intent & Extract Content
        val isEvent = normalized.contains(Regex("(?i)(رویداد|قرار|جلسه)"))
        val isTask = !isEvent && normalized.contains(Regex("(?i)(وظیفه|تسک|کار|یادآوری)"))
        val isNote = !isEvent && !isTask && normalized.contains(Regex("(?i)(یادداشت|نوت)"))

        // Remove trigger words to get raw title
        var cleanContent = normalized
            .replace(Regex("(?i)(یادداشت|نوت|وظیفه|تسک|یادآوری|رویداد|قرار|جلسه)\\s*(جدید|کن|بنویس|بذار|ثبت|اضافه|تنظیم)?"), "")
            .replace(Regex("(برای\\s*)?(فردا|پس فردا|پس‌فردا)"), "")
            .replace(timeRegex, "")
        
        // Remove the explicit date words
        for (m in PersianCalendarHelper.PERSIAN_MONTH_NAMES) {
            cleanContent = cleanContent.replace(Regex("(برای\\s*)?\\d{1,2}\\s+$m"), "")
        }
        
        cleanContent = cleanContent.replace(Regex("^[\\s:،-]*"), "").trim()
        if (cleanContent.isBlank()) cleanContent = "بدون عنوان"

        // Return Action
        if (isEvent) {
            val event = EventEntity(
                id = UUID.randomUUID().toString(),
                title = cleanContent,
                persianDate = formattedDate,
                startTime = explicitTime,
                category = "آفلاین"
            )
            return NLPResult.AddEvent(event)
        } else if (isTask) {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = cleanContent,
                persianDueDate = formattedDate,
                priority = "متوسط",
                category = "آفلاین"
            )
            return NLPResult.AddTask(task)
        } else if (isNote) {
            val note = NoteEntity(
                id = UUID.randomUUID().toString(),
                title = extractTitle(cleanContent),
                content = cleanContent,
                persianDate = persianFormattedDate,
                colorHex = "#FEF3C7"
            )
            return NLPResult.AddNote(note)
        }

        // Fallback: If no explicit keyword but date/time is provided, default to Event or Task
        if (explicitMonth != -1 || timeMatch != null) {
            val event = EventEntity(
                id = UUID.randomUUID().toString(),
                title = cleanContent,
                persianDate = formattedDate,
                startTime = explicitTime,
                category = "آفلاین"
            )
            return NLPResult.AddEvent(event)
        }

        return NLPResult.NotUnderstood
    }

    private fun extractTitle(content: String): String {
        val words = content.split(" ", "،", "\n")
        return if (words.size <= 4) content else words.take(4).joinToString(" ") + "..."
    }
}
