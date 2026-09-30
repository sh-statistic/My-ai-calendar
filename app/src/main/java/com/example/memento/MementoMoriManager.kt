package com.example.memento

import android.content.Context
import android.content.SharedPreferences
import com.example.calendar.PersianCalendarHelper
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
    val birthMonth: Int
)

class MementoMoriManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "hamgam_memento_mori_prefs"
        private const val KEY_BIRTH_YEAR = "birth_year"
        private const val KEY_BIRTH_MONTH = "birth_month"
        private const val KEY_MANUAL_DRILLED_COUNT = "manual_drilled_count"
        private const val KEY_NOTES_JSON = "reflection_notes_json"
        const val TOTAL_LIFETIME_MONTHS = 960 // 80 Years
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBirthYear(): Int {
        val currentYear = PersianCalendarHelper.getCurrentJalaliDate().year
        return prefs.getInt(KEY_BIRTH_YEAR, currentYear - 25) // Default 25 years old
    }

    fun getBirthMonth(): Int {
        return prefs.getInt(KEY_BIRTH_MONTH, 1) // Default Farvardin
    }

    fun setBirthDate(year: Int, month: Int) {
        prefs.edit()
            .putInt(KEY_BIRTH_YEAR, year)
            .putInt(KEY_BIRTH_MONTH, month)
            .apply()
    }

    fun getManualDrilledCount(): Int {
        return prefs.getInt(KEY_MANUAL_DRILLED_COUNT, 0)
    }

    fun addManualDrilledMonth() {
        val current = getManualDrilledCount()
        prefs.edit().putInt(KEY_MANUAL_DRILLED_COUNT, current + 1).apply()
    }

    fun calculateProgress(): LifeProgress {
        val today = PersianCalendarHelper.getCurrentJalaliDate()
        val birthY = getBirthYear()
        val birthM = getBirthMonth()

        // Calculate chronological months lived
        var rawMonths = (today.year - birthY) * 12 + (today.month - birthM)
        if (rawMonths < 0) rawMonths = 0

        // Add any manually finished months
        val totalElapsed = (rawMonths + getManualDrilledCount()).coerceIn(0, TOTAL_LIFETIME_MONTHS)
        val remaining = (TOTAL_LIFETIME_MONTHS - totalElapsed).coerceAtLeast(0)

        val elapsedYears = totalElapsed / 12
        val elapsedRemMonths = totalElapsed % 12

        val remainingYears = remaining / 12
        val remainingRemMonths = remaining % 12

        val percent = (totalElapsed.toFloat() / TOTAL_LIFETIME_MONTHS.toFloat()) * 100f
        val currentMonthIdx = totalElapsed.coerceAtMost(TOTAL_LIFETIME_MONTHS - 1)

        return LifeProgress(
            elapsedMonths = totalElapsed,
            totalMonths = TOTAL_LIFETIME_MONTHS,
            elapsedYears = elapsedYears,
            elapsedRemainingMonths = elapsedRemMonths,
            remainingYears = remainingYears,
            remainingMonths = remainingRemMonths,
            percentElapsed = percent,
            currentMonthIndex = currentMonthIdx,
            birthYear = birthY,
            birthMonth = birthM
        )
    }

    fun saveReflectionNote(monthIndex: Int, note: String) {
        val notesJson = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        try {
            val json = JSONObject(notesJson)
            json.put(monthIndex.toString(), note.trim())
            prefs.edit().putString(KEY_NOTES_JSON, json.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getReflectionNote(monthIndex: Int): String? {
        val notesJson = prefs.getString(KEY_NOTES_JSON, "{}") ?: "{}"
        return try {
            val json = JSONObject(notesJson)
            if (json.has(monthIndex.toString())) json.getString(monthIndex.toString()) else null
        } catch (_: Exception) {
            null
        }
    }
}
