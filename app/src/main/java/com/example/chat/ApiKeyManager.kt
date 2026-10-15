package com.example.chat

import android.content.Context

object ApiKeyManager {
    private const val PREFS_NAME = "hamgam_gemini_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key" // Legacy key for Gemini
    private const val KEY_SELECTED_PROVIDER = "selected_ai_provider"
    private const val PREFIX_API_KEY = "api_key_"
    private const val PREFIX_BASE_URL = "base_url_"
    private const val PREFIX_MODEL = "model_"

    /**
     * Returns the currently active AI provider (defaults to GEMINI).
     */
    fun getSelectedProvider(context: Context): AiProvider {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val providerId = prefs.getString(KEY_SELECTED_PROVIDER, AiProvider.GEMINI.id) ?: AiProvider.GEMINI.id
        return AiProvider.fromId(providerId)
    }

    /**
     * Saves the selected AI provider.
     */
    fun saveSelectedProvider(context: Context, provider: AiProvider) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_PROVIDER, provider.id).apply()
    }

    /**
     * Returns the API key for the given provider (or current provider by default).
     * Starts strictly EMPTY by default.
     * Preserves backwards compatibility with legacy custom_gemini_api_key.
     */
    fun getApiKey(context: Context, provider: AiProvider = getSelectedProvider(context)): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val providerKey = prefs.getString(PREFIX_API_KEY + provider.id, null)?.trim()
        if (!providerKey.isNullOrBlank()) {
            return providerKey
        }
        // Fallback for Gemini to the legacy pref key
        if (provider == AiProvider.GEMINI) {
            return prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
        }
        return ""
    }

    /**
     * Saves the API key for the given provider (or current provider by default).
     */
    fun saveApiKey(context: Context, apiKey: String, provider: AiProvider = getSelectedProvider(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanKey = apiKey.trim()
        val editor = prefs.edit()
        val keyName = PREFIX_API_KEY + provider.id
        if (cleanKey.isBlank()) {
            editor.remove(keyName)
            if (provider == AiProvider.GEMINI) {
                editor.remove(KEY_CUSTOM_API_KEY)
            }
        } else {
            editor.putString(keyName, cleanKey)
            if (provider == AiProvider.GEMINI) {
                editor.putString(KEY_CUSTOM_API_KEY, cleanKey)
            }
        }
        editor.apply()
    }

    /**
     * Clears the API key for the given provider.
     */
    fun clearApiKey(context: Context, provider: AiProvider = getSelectedProvider(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().remove(PREFIX_API_KEY + provider.id)
        if (provider == AiProvider.GEMINI) {
            editor.remove(KEY_CUSTOM_API_KEY)
        }
        editor.apply()
    }

    /**
     * Checks if the API key is configured.
     * Note: CUSTOM provider might work with empty key if local Ollama server doesn't need auth.
     */
    fun isKeyConfigured(context: Context, provider: AiProvider = getSelectedProvider(context)): Boolean {
        if (provider == AiProvider.OPENAI_COMPATIBLE) {
            return getApiKey(context, provider).isNotBlank() || getBaseUrl(context, provider).isNotBlank()
        }
        return getApiKey(context, provider).isNotBlank()
    }

    /**
     * Gets the base URL for the given provider (user-configured or default).
     */
    fun getBaseUrl(context: Context, provider: AiProvider = getSelectedProvider(context)): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(PREFIX_BASE_URL + provider.id, null)?.trim()
        return if (!custom.isNullOrBlank()) custom else provider.defaultBaseUrl
    }

    fun saveBaseUrl(context: Context, url: String, provider: AiProvider = getSelectedProvider(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanUrl = url.trim()
        val editor = prefs.edit()
        val keyName = PREFIX_BASE_URL + provider.id
        if (cleanUrl.isBlank() || cleanUrl == provider.defaultBaseUrl) {
            editor.remove(keyName)
        } else {
            editor.putString(keyName, cleanUrl)
        }
        editor.apply()
    }

    /**
     * Gets the model name for the given provider (user-configured or default).
     */
    fun getModel(context: Context, provider: AiProvider = getSelectedProvider(context)): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val custom = prefs.getString(PREFIX_MODEL + provider.id, null)?.trim()
        return if (!custom.isNullOrBlank()) custom else provider.defaultModel
    }

    fun saveModel(context: Context, model: String, provider: AiProvider = getSelectedProvider(context)) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cleanModel = model.trim()
        val editor = prefs.edit()
        val keyName = PREFIX_MODEL + provider.id
        if (cleanModel.isBlank() || cleanModel == provider.defaultModel) {
            editor.remove(keyName)
        } else {
            editor.putString(keyName, cleanModel)
        }
        editor.apply()
    }
}

