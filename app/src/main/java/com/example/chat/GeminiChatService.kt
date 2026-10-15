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

    /**
     * Legacy entry point for backwards compatibility (defaults to Google Gemini).
     */
    suspend fun sendMessage(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        modelName: String = ChatRole.GENERAL.modelName,
        systemInstruction: String = ChatRole.GENERAL.systemInstruction
    ): Result<String> {
        return sendMessage(
            provider = AiProvider.GEMINI,
            apiKey = apiKey,
            history = history,
            userMessage = userMessage,
            baseUrl = AiProvider.GEMINI.defaultBaseUrl,
            modelName = modelName,
            systemInstruction = systemInstruction
        )
    }

    /**
     * Unified multi-provider message sender supporting Google Gemini and all OpenAI-compatible services.
     */
    suspend fun sendMessage(
        provider: AiProvider,
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        baseUrl: String = provider.defaultBaseUrl,
        modelName: String = provider.defaultModel,
        systemInstruction: String = ChatRole.GENERAL.systemInstruction
    ): Result<String> = withContext(Dispatchers.IO) {
        if (provider.isGoogleFormat && apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("کلید API برای ${provider.displayName} تنظیم نشده است. لطفاً کلید معتبر خود را وارد نمایید.")
            )
        }

        if (provider.isGoogleFormat) {
            sendGoogleGeminiMessage(apiKey, history, userMessage, baseUrl, modelName, systemInstruction)
        } else {
            sendOpenAiCompatibleMessage(provider, apiKey, history, userMessage, baseUrl, modelName, systemInstruction)
        }
    }

    /**
     * Google Gemini REST API implementation.
     */
    private fun sendGoogleGeminiMessage(
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        baseUrl: String,
        modelName: String,
        systemInstruction: String
    ): Result<String> {
        return try {
            val endpoint = "$baseUrl/$modelName:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
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
                            return Result.success(text)
                        }
                    }
                }
                Result.success("پاسخی از مدل دریافت نشد.")
            } else {
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

    /**
     * Standard OpenAI-compatible Chat Completions API implementation.
     * Supports OpenAI, DeepSeek, Groq, OpenRouter, and custom local servers (Ollama, vLLM).
     */
    private fun sendOpenAiCompatibleMessage(
        provider: AiProvider,
        apiKey: String,
        history: List<ChatMessage>,
        userMessage: String,
        baseUrl: String,
        modelName: String,
        systemInstruction: String
    ): Result<String> {
        return try {
            val url = URL(baseUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 60000
                readTimeout = 60000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                // Optional OpenRouter client headers
                setRequestProperty("HTTP-Referer", "https://github.com/sh-statistic/My-ai-calendar")
                setRequestProperty("X-Title", "My AI Calendar")
            }

            val root = JSONObject()
            root.put("model", modelName)

            val messagesArr = JSONArray()

            // System prompt
            if (systemInstruction.isNotBlank()) {
                val sysMsg = JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                }
                messagesArr.put(sysMsg)
            }

            // Conversation history
            val contextHistory = history.filter { !it.isError }.takeLast(12)
            for (msg in contextHistory) {
                val turn = JSONObject().apply {
                    put("role", if (msg.sender == ChatSender.USER) "user" else "assistant")
                    put("content", msg.text)
                }
                messagesArr.put(turn)
            }

            // Current user message
            val curMsg = JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
            messagesArr.put(curMsg)

            root.put("messages", messagesArr)
            root.put("temperature", 0.7)

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
                val choices = respJson.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val messageObj = choice.optJSONObject("message")
                    val content = messageObj?.optString("content", "") ?: choice.optString("text", "")
                    if (content.isNotBlank()) {
                        return Result.success(content)
                    }
                }
                Result.success("پاسخی از مدل دریافت نشد.")
            } else {
                val errMessage = try {
                    val errJson = JSONObject(responseText).optJSONObject("error")
                    val message = errJson?.optString("message", "") ?: responseText
                    when {
                        responseCode == 401 || responseCode == 403 ->
                            "کلید API وارد شده برای ${provider.displayName} نامعتبر است یا اعتبار کافی ندارد."
                        responseCode == 429 || message.contains("quota", ignoreCase = true) || message.contains("rate limit", ignoreCase = true) ->
                            "سهمیه درخواست یا نرخ مجاز (Rate Limit) در سرویس ${provider.displayName} به پایان رسیده است."
                        responseCode == 404 ->
                            "مدل «$modelName» در سرور ${provider.displayName} یافت نشد یا آدرس سرویس نامعتبر است."
                        else ->
                            "خطای ${provider.displayName} (کد $responseCode): $message"
                    }
                } catch (_: Exception) {
                    "خطا در ارتباط با سرور ${provider.displayName} (کد وضعیت: $responseCode)"
                }
                Result.failure(Exception(errMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطای ارتباط با ${provider.displayName}: ${e.localizedMessage ?: "عدم دسترسی به سرور"}"))
        }
    }
}

