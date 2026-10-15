package com.example.chat

/**
 * ارائه‌دهندگان هوش مصنوعی:
 * ۱. گوگل جمینای (Google Gemini)
 * ۲. سازگار با OpenAI (شامل OpenAI، DeepSeek، Groq، مدل‌های محلی Ollama و سایر سرویس‌ها)
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val isGoogleFormat: Boolean,
    val keyHint: String,
    val helpText: String,
    val helpUrl: String
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models",
        defaultModel = "gemini-2.5-flash",
        isGoogleFormat = true,
        keyHint = "مانند AIzaSy...",
        helpText = "کلید رایگان را از aistudio.google.com تهیه نمایید.",
        helpUrl = "https://aistudio.google.com"
    ),
    OPENAI_COMPATIBLE(
        id = "openai_compatible",
        displayName = "OpenAI Compatible",
        defaultBaseUrl = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        isGoogleFormat = false,
        keyHint = "کلید API (مانند sk-... یا کلید سرویس دلخواه)",
        helpText = "سازگار با تمامی سرویس‌های استاندارد OpenAI (مانند ChatGPT، DeepSeek، Groq، مدل‌های محلی Ollama/vLLM و سایر ارائه‌دهندگان).",
        helpUrl = "https://platform.openai.com/api-keys"
    );

    companion object {
        fun fromId(id: String): AiProvider = when (id) {
            "gemini" -> GEMINI
            "openai_compatible", "openai", "deepseek", "groq", "openrouter", "custom" -> OPENAI_COMPATIBLE
            else -> GEMINI
        }
    }
}

