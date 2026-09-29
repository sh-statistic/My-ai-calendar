package com.example.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

enum class ChatSender {
    USER,
    GEMINI
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val modelBadge: String? = null
)

object GeminiChatService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun sendMessage(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        modelName: String = ChatRole.GENERAL.modelName,
        systemInstruction: String = ChatRole.GENERAL.systemInstruction
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("کلید API تنظیم نشده است. لطفاً کلید Gemini API خود را وارد کنید.")
            )
        }

        try {
            val endpoint = "$BASE_URL/$modelName:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                // 60s timeout as mandated for complex reasoning models
                connectTimeout = 60000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            // Build request payload
            val root = JSONObject()

            // Dynamic system instruction based on role
            if (systemInstruction.isNotBlank()) {
                val sysInstructionObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().put("text", systemInstruction))
                    }
                    put("parts", partsArr)
                }
                root.put("systemInstruction", sysInstructionObj)
            }

            // Contents array with conversation history
            val contentsArr = JSONArray()
            val contextHistory = history.filter { !it.isError }.takeLast(12)
            for (msg in contextHistory) {
                val turn = JSONObject()
                turn.put("role", if (msg.sender == ChatSender.USER) "user" else "model")
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", msg.text))
                }
                turn.put("parts", parts)
                contentsArr.put(turn)
            }

            // Append current user message
            val currentTurn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", userMessage))
                }
                put("parts", parts)
            }
            contentsArr.put(currentTurn)

            root.put("contents", contentsArr)

            // Generation config
            val genConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            root.put("generationConfig", genConfig)

            // Write payload
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = BufferedReader(InputStreamReader(responseStream, StandardCharsets.UTF_8)).use {
                it.readText()
            }

            if (responseCode in 200..299) {
                val respJson = JSONObject(responseText)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return@withContext Result.success(text)
                        }
                    }
                }
                Result.success("پاسخی از مدل دریافت نشد.")
            } else {
                // Parse error details
                val errMessage = try {
                    val errJson = JSONObject(responseText).optJSONObject("error")
                    val message = errJson?.optString("message", "") ?: responseText
                    val status = errJson?.optString("status", "")
                    when {
                        responseCode == 429 || status == "RESOURCE_EXHAUSTED" || message.contains("quota", ignoreCase = true) ->
                            "سهمیه کلید API فعلی شما به اتمام رسیده است (Resource Exhausted). لطفاً کلید API جدیدی از Google AI Studio وارد کنید."
                        responseCode == 400 && message.contains("API key not valid", ignoreCase = true) ->
                            "کلید API وارد شده معتبر نمی‌باشد. لطفاً کلید API صحیح را بررسی و مجدداً وارد نمایید."
                        responseCode == 403 ->
                            "دسترسی با این کلید API مجاز نمی‌باشد. لطفاً دسترسی‌های پروژه خود را در Google Cloud یا AI Studio بررسی کنید."
                        else ->
                            "خطای سرویس هوش مصنوعی (کد $responseCode): $message"
                    }
                } catch (_: Exception) {
                    "خطا در ارتباط با سرور هوش مصنوعی (کد وضعیت: $responseCode)"
                }
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطای اتصال شبکه: ${e.localizedMessage ?: "عدم امکان برقراری ارتباط با جمینای"}"))
        }
    }
}
