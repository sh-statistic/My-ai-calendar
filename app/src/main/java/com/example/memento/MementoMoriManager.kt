package com.example.memento

import android.content.Context
import android.content.SharedPreferences
import com.example.calendar.PersianCalendarHelper
import com.example.data.NoteEntity
import org.json.JSONObject

data class LifeProgress(
    val elapsedMonths: Int,
    val totalMonths: Int = 960, // 80 years * 12 months
    val elapsedYears: Int,
    val elapsedRemainingMonths: Int,
    val remainingYears: Int,
    val remainingMonths: Int,
    val percentElapsed: Float,
    val currentMonthIndex: Int,
    val birthYear: Int,
    val birthMonth: Int,
    val isConfigured: Boolean = true
)

class MementoMoriManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "hamgam_memento_mori_prefs"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_BIRTH_MONTH = "birth_month"
        private const val KEY_EXPECTED_LIFESPAN = "expected_lifespan"
        private const val KEY_MANUAL_DRILLED_COUNT = "manual_drilled_count"
        private const val KEY_NOTES_JSON = "reflection_notes_json"
        private const val KEY_LAST_SEEN_MONTH = "last_seen_month"
        private const val KEY_LAST_COMPILED_YEAR_IDX = "last_compiled_year_idx"

        private val EN_MONTH_NAMES = listOf(
            "Farvardin", "Ordibehesht", "Khordad",
            "Tir", "Mordad", "Shahrivar",
            "Mehr", "Aban", "Azar",
            "Dey", "Bahman", "Esfand"
        )
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getExpectedLifespan(): Int {
        return prefs.getInt(KEY_EXPECTED_LIFESPAN, 80) // Default 80 years
    }

    fun setExpectedLifespan(years: Int) {
        prefs.edit().putInt(KEY_EXPECTED_LIFESPAN, years).apply()
    }

    fun getTotalLifetimeMonths(): Int {
        return getExpectedLifespan() * 12
    }

    fun isBirthDateConfigured(): Boolean {
        return prefs.contains(KEY_BIRTH_YEAR)
    }

    fun clearBirthDate() {
        prefs.edit()
            .remove(KEY_BIRTH_YEAR)
            .remove(KEY_BIRTH_MONTH)
            .remove(KEY_LAST_SEEN_MONTH)
            .remove(KEY_LAST_COMPILED_YEAR_IDX)
            .apply()
    }

    fun getBirthYear(): Int {
        val currentYear = PersianCalendarHelper.getCurrentJalaliDate().year
        return prefs.getInt(KEY_BIRTH_YEAR, currentYear)
    }

    fun getBirthMonth(): Int {
        return prefs.getInt(KEY_BIRTH_MONTH, 1) // Default Farvardin
    }

    fun setBirthDate(year: Int, month: Int) {
        val prevYear = prefs.getInt(KEY_BIRTH_YEAR, -1)
        val prevMonth = prefs.getInt(KEY_BIRTH_MONTH, -1)
        prefs.edit()
            .putInt(KEY_BIRTH_YEAR, year)
            .putInt(KEY_BIRTH_MONTH, month)
            .apply()

        val progress = calculateProgress()
        if (progress.currentMonthIndex >= 0 && (!prefs.contains(KEY_LAST_SEEN_MONTH) || prevYear != year || prevMonth != month)) {
            prefs.edit()
                .putInt(KEY_LAST_SEEN_MONTH, progress.currentMonthIndex)
                .putInt(KEY_LAST_COMPILED_YEAR_IDX, (progress.currentMonthIndex / 12).coerceAtLeast(0))
                .apply()
        }
    }

    fun calculateProgress(): LifeProgress {
        val today = PersianCalendarHelper.getCurrentJalaliDate()
        val totalLifetimeMonths = getTotalLifetimeMonths()

        if (!isBirthDateConfigured()) {
            return LifeProgress(
                elapsedMonths = 0,
                totalMonths = totalLifetimeMonths,
                elapsedYears = 0,
                elapsedRemainingMonths = 0,
                remainingYears = getExpectedLifespan(),
                remainingMonths = 0,
                percentElapsed = 0f,
                currentMonthIndex = -1,
                birthYear = today.year,
                birthMonth = 1,
                isConfigured = false
            )
        }

        val birthY = getBirthYear()
        val birthM = getBirthMonth()

        // Calculate actual lived months
        var rawMonths = (today.year - birthY) * 12 + (today.month - birthM)
        if (rawMonths < 0) rawMonths = 0

        // Calculate grid index (calendar months since Farvardin of birth year)
        var gridIndex = (today.year - birthY) * 12 + (today.month - 1)
        if (gridIndex < 0) gridIndex = 0

        val totalElapsed = rawMonths.coerceIn(0, totalLifetimeMonths)
        val remaining = (totalLifetimeMonths - totalElapsed).coerceAtLeast(0)

        val elapsedYears = totalElapsed / 12
        val elapsedRemMonths = totalElapsed % 12

        val remainingYears = remaining / 12
        val remainingRemMonths = remaining % 12

        val percent = (totalElapsed.toFloat() / totalLifetimeMonths.toFloat()) * 100f

        // The grid has `totalLifetimeMonths + birthM - 1` total slots because of the grayed out pre-birth months
        val totalGridSlots = totalLifetimeMonths + (birthM - 1)
        val currentMonthIdx = gridIndex.coerceAtMost(totalGridSlots - 1)

        return LifeProgress(
            elapsedMonths = totalElapsed,
            totalMonths = totalLifetimeMonths,
            elapsedYears = elapsedYears,
            elapsedRemainingMonths = elapsedRemMonths,
            remainingYears = remainingYears,
            remainingMonths = remainingRemMonths,
            percentElapsed = percent,
            currentMonthIndex = currentMonthIdx,
            birthYear = birthY,
            birthMonth = birthM,
            isConfigured = true
        )
    }

    /**
     * بررسی می‌کند آیا ماه جدیدی شروع شده و ماه قبلی به پایان رسیده است یا خیر.
     * در صورت تمام شدن ماه، اندیس ماهِ تمام‌شده (currentMonthIdx - 1) را برمی‌گرداند.
     */
    fun getPendingCompletedMonthIndex(currentMonthIdx: Int): Int? {
        if (!isBirthDateConfigured() || currentMonthIdx <= 0) return null
        if (!prefs.contains(KEY_LAST_SEEN_MONTH)) {
            val prevMonthIdx = currentMonthIdx - 1
            val hasNoteForPrevMonth = !getReflectionNote(prevMonthIdx).isNullOrBlank()
            return if (!hasNoteForPrevMonth) {
                prefs.edit().putInt(KEY_LAST_SEEN_MONTH, prevMonthIdx).apply()
                prevMonthIdx
            } else {
                prefs.edit().putInt(KEY_LAST_SEEN_MONTH, currentMonthIdx).apply()
                null
            }
        }
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_MONTH, currentMonthIdx)
        return if (currentMonthIdx > lastSeen) {
            currentMonthIdx - 1
        } else {
            null
        }
    }

    fun acknowledgeMonthCompleted(currentMonthIdx: Int) {
        if (currentMonthIdx < 0) return
        prefs.edit().putInt(KEY_LAST_SEEN_MONTH, currentMonthIdx).apply()
    }

    fun checkNewMonthCompleted(currentMonthIdx: Int): Boolean {
        val pending = getPendingCompletedMonthIndex(currentMonthIdx)
        if (pending != null) {
            acknowledgeMonthCompleted(currentMonthIdx)
            return true
        }
        return false
    }

    fun saveReflectionNote(monthIndex: Int, note: String) {
        val notesJson = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        try {
            val json = JSONObject(notesJson)
            val trimmed = note.trim()
            if (trimmed.isEmpty()) {
                json.remove(monthIndex.toString())
            } else {
                json.put(monthIndex.toString(), trimmed)
            }
            prefs.edit().putString(KEY_NOTES_JSON, json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getReflectionNote(monthIndex: Int): String? {
        val notesJson = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        return try {
            val json = JSONObject(notesJson)
            if (json.has(monthIndex.toString())) {
                val text = json.getString(monthIndex.toString()).trim()
                if (text.isNotEmpty()) text else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getMonthYearLabel(monthIndex: Int, isFa: Boolean = true): String {
        if (monthIndex < 0) return ""
        val yearIdx = monthIndex / 12
        val monthInYear = monthIndex % 12
        val persianYear = getBirthYear() + yearIdx
        return if (isFa) {
            val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(monthInYear) { "" }
            "$monthName ${PersianCalendarHelper.toPersianDigits(persianYear.toString())}"
        } else {
            val monthName = EN_MONTH_NAMES.getOrElse(monthInYear) { "" }
            "$monthName $persianYear"
        }
    }

    fun getNotesForYear(yearIdx: Int): Map<Int, String> {
        val result = LinkedHashMap<Int, String>()
        if (yearIdx < 0) return result
        val startMonthIdx = yearIdx * 12
        for (monthInYear in 0..11) {
            val absMonthIdx = startMonthIdx + monthInYear
            val note = getReflectionNote(absMonthIdx)
            if (!note.isNullOrBlank()) {
                result[monthInYear] = note
            }
        }
        return result
    }

    fun buildYearlyReflectionNote(yearIdx: Int, isFa: Boolean = true): NoteEntity? {
        if (!isBirthDateConfigured() || yearIdx < 0) return null
        val monthNotes = getNotesForYear(yearIdx)
        if (monthNotes.isEmpty()) return null

        val persianYear = getBirthYear() + yearIdx
        val noteId = "yearly_reflection_$persianYear"
        val startAge = yearIdx
        val endAge = yearIdx + 1

        val title = if (isFa) {
            val yearFa = PersianCalendarHelper.toPersianDigits(persianYear.toString())
            val startAgeFa = PersianCalendarHelper.toPersianDigits(startAge.toString())
            val endAgeFa = PersianCalendarHelper.toPersianDigits(endAge.toString())
            "کارنامه عملکرد سال $yearFa ($startAgeFa تا $endAgeFa سالگی)"
        } else {
            "Year $persianYear Performance Review (Age $startAge to $endAge)"
        }

        val sb = StringBuilder()
        if (isFa) {
            val yearFa = PersianCalendarHelper.toPersianDigits(persianYear.toString())
            sb.appendLine("📋 مرور ماه‌به‌ماه نقطه‌های عمر در سال $yearFa:")
            sb.appendLine()
            for ((monthInYear, noteText) in monthNotes) {
                val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(monthInYear) { "ماه ${monthInYear + 1}" }
                sb.appendLine("🔹 $monthName:")
                sb.appendLine(noteText)
                sb.appendLine()
            }
            sb.appendLine("────────────────────")
            val countFa = PersianCalendarHelper.toPersianDigits(monthNotes.size.toString())
            sb.append("📊 مجموع ماه‌های ثبت‌شده: $countFa از ۱۲ ماه")
        } else {
            sb.appendLine("📋 Month-by-month Life Grid reflections in $persianYear:")
            sb.appendLine()
            for ((monthInYear, noteText) in monthNotes) {
                val monthName = EN_MONTH_NAMES.getOrElse(monthInYear) { "Month ${monthInYear + 1}" }
                sb.appendLine("🔹 $monthName:")
                sb.appendLine(noteText)
                sb.appendLine()
            }
            sb.appendLine("────────────────────")
            sb.append("📊 Recorded months: ${monthNotes.size} of 12")
        }

        return NoteEntity(
            id = noteId,
            title = title,
            content = sb.toString().trim(),
            persianDate = PersianCalendarHelper.getCurrentJalaliDate().formattedPersian,
            colorHex = "#FEF3C7",
            isPinned = true,
            tag = if (isFa) "عمر من, عملکرد سالانه" else "Life Grid, Yearly Review"
        )
    }

    /**
     * خروجی JSON از تنظیمات تاریخ تولد و تمام خاطرات ماهانه برای ذخیره در فایل پشتیبان
     */
    fun exportToJson(): JSONObject {
        val obj = JSONObject()
        obj.put("isConfigured", isBirthDateConfigured())
        if (isBirthDateConfigured()) {
            obj.put("birthYear", getBirthYear())
            obj.put("birthMonth", getBirthMonth())
        }
        obj.put("expectedLifespan", getExpectedLifespan())
        if (prefs.contains(KEY_LAST_SEEN_MONTH)) {
            obj.put("lastSeenMonth", prefs.getInt(KEY_LAST_SEEN_MONTH, -1))
        }
        if (prefs.contains(KEY_LAST_COMPILED_YEAR_IDX)) {
            obj.put("lastCompiledYearIdx", prefs.getInt(KEY_LAST_COMPILED_YEAR_IDX, 0))
        }
        val notesRaw = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        val notesObj = try { JSONObject(notesRaw) } catch (_: Exception) { JSONObject() }
        obj.put("reflectionNotes", notesObj)
        return obj
    }

    /**
     * بازیابی و ادغام غیرمخرب اطلاعات «نقطه‌های عمر من» از فایل پشتیبان.
     * خاطرات ماهانه‌ای که در دستگاه خالی هستند از بکاپ بازیابی می‌شوند.
     */
    fun importFromJson(obj: JSONObject?): Int {
        if (obj == null) return 0
        var restoredCount = 0
        val editor = prefs.edit()

        if (!isBirthDateConfigured() && obj.optBoolean("isConfigured", false)) {
            val by = obj.optInt("birthYear", -1)
            val bm = obj.optInt("birthMonth", -1)
            if (by > 1200 && bm in 1..12) {
                editor.putInt(KEY_BIRTH_YEAR, by)
                editor.putInt(KEY_BIRTH_MONTH, bm)
                restoredCount++
            }
        }

        val lifespan = obj.optInt("expectedLifespan", -1)
        if (lifespan in 40..120 && !prefs.contains(KEY_EXPECTED_LIFESPAN)) {
            editor.putInt(KEY_EXPECTED_LIFESPAN, lifespan)
        }

        if (!prefs.contains(KEY_LAST_SEEN_MONTH) && obj.has("lastSeenMonth")) {
            val lsm = obj.optInt("lastSeenMonth", -1)
            if (lsm >= 0) editor.putInt(KEY_LAST_SEEN_MONTH, lsm)
        }

        if (!prefs.contains(KEY_LAST_COMPILED_YEAR_IDX) && obj.has("lastCompiledYearIdx")) {
            val lcy = obj.optInt("lastCompiledYearIdx", -1)
            if (lcy >= 0) editor.putInt(KEY_LAST_COMPILED_YEAR_IDX, lcy)
        }

        val incomingNotes = obj.optJSONObject("reflectionNotes")
        if (incomingNotes != null) {
            val currentNotesRaw = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
            val currentNotes = try { JSONObject(currentNotesRaw) } catch (_: Exception) { JSONObject() }
            val keys = incomingNotes.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val incomingText = incomingNotes.optString(key, "").trim()
                val existingText = currentNotes.optString(key, "").trim()
                if (incomingText.isNotEmpty() && existingText.isEmpty()) {
                    currentNotes.put(key, incomingText)
                    restoredCount++
                }
            }
            editor.putString(KEY_NOTES_JSON, currentNotes.toString())
        }

        editor.apply()
        return restoredCount
    }
}
