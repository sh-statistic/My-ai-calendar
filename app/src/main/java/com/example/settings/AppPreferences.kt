package com.example.settings

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val PREFS_NAME = "hamgam_app_preferences"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_APP_LANGUAGE = "app_language"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getAppLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_APP_LANGUAGE, "fa") ?: "fa"
    }

    fun setAppLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_APP_LANGUAGE, language).apply()
    }
}
