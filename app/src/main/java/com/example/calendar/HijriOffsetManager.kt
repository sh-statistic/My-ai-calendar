package com.example.calendar

import android.content.Context
import android.content.SharedPreferences

object HijriOffsetManager {
    private const val PREFS_NAME = "hijri_offset_prefs"
    private const val KEY_OFFSET = "hijri_offset_days"
    
    private var prefs: SharedPreferences? = null
    var currentOffset: Int = 0
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentOffset = prefs?.getInt(KEY_OFFSET, 0) ?: 0
    }

    fun setOffset(offset: Int) {
        currentOffset = offset
        prefs?.edit()?.putInt(KEY_OFFSET, offset)?.apply()
        // Clear occasions cache because the dates of lunar events will shift
        HijriCalendarUtils.clearCache()
    }
}
