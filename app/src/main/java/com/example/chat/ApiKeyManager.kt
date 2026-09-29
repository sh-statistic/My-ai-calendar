package com.example.chat

import android.content.Context

object ApiKeyManager {
    private const val PREFS_NAME = "hamgam_gemini_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    /**
     * Returns the user-configured API key.
     * Starts strictly EMPTY by default.
     */
    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
    }

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_API_KEY, cleanKey).apply()
        }
    }

    fun clearApiKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_API_KEY).apply()
    }

    fun isKeyConfigured(context: Context): Boolean {
        return getApiKey(context).isNotBlank()
    }
}
