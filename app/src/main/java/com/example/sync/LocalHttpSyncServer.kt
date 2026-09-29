package com.example.sync

import com.example.data.AppRepository
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class LocalHttpSyncServer(
    private val repository: AppRepository,
    private val port: Int = 8080,
    private val onLog: (String) -> Unit = {}
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRunning: Boolean = false
        private set

    fun start(onStarted: (Boolean, String?) -> Unit) {
        if (isRunning) {
            onStarted(true, null)
            return
        }

        try {
            serverSocket = ServerSocket(port)
            isRunning = true
            onLog("سرور محلی روی پورت $port راه‌اندازی شد")
            onStarted(true, null)

            serverJob = scope.launch {
                while (isActive && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            }
        } catch (e: Exception) {
            isRunning = false
            onLog("خطا در اجرای سرور: ${e.localizedMessage}")
            onStarted(false, e.localizedMessage)
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
        onLog("سرور محلی متوقف شد")
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { s ->
            try {
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))
                val output = s.getOutputStream()

                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val path = parts[1].split("?")[0]

                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) break
                    val headerLower = line!!.lowercase()
                    if (headerLower.startsWith("content-length:")) {
                        contentLength = headerLower.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                if (method == "OPTIONS") {
                    sendResponse(output, 204, "No Content", "", "text/plain")
                    return
                }

                val body = if (contentLength > 0) {
                    val charBuffer = CharArray(contentLength)
                    var readTotal = 0
                    while (readTotal < contentLength) {
                        val read = reader.read(charBuffer, readTotal, contentLength - readTotal)
                        if (read == -1) break
                        readTotal += read
                    }
                    String(charBuffer, 0, readTotal)
                } else ""

                when {
                    path == "/" || path == "/api/status" -> {
                        val eventsCount = repository.allActiveEvents.first().size
                        val tasksCount = repository.allActiveTasks.first().size
                        val notesCount = repository.allActiveNotes.first().size
                        val statusJson = JSONObject().apply {
                            put("status", "online")
                            put("app", "Hamgam Local Sync")
                            put("version", "1.0")
                            put("eventsCount", eventsCount)
                            put("tasksCount", tasksCount)
                            put("notesCount", notesCount)
                            put("timestamp", System.currentTimeMillis())
                        }
                        sendResponse(output, 200, "OK", statusJson.toString(), "application/json")
                    }

                    method == "GET" && path == "/api/sync" -> {
                        val fullData = repository.getFullSyncData("Android-Server")
                        onLog("ارسال کل داده‌ها به کلاینت (${s.inetAddress.hostAddress})")
                        sendResponse(output, 200, "OK", fullData.toJsonString(), "application/json")
                    }

                    method == "POST" && path == "/api/sync" -> {
                        val remoteSyncData = SyncData.fromJsonString(body)
                        val merged = repository.mergeSyncData(remoteSyncData, "افزونه کروم (${s.inetAddress.hostAddress})")
                        onLog("همگام‌سازی موفق با ${remoteSyncData.deviceId}")
                        sendResponse(output, 200, "OK", merged.toJsonString(), "application/json")
                    }

                    method == "POST" && path == "/api/events" -> {
                        val obj = JSONObject(body)
                        val event = EventEntity(
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            persianDate = obj.optString("persianDate", ""),
                            gregorianDate = obj.optString("gregorianDate", ""),
                            startTime = obj.optString("startTime", ""),
                            endTime = obj.optString("endTime", ""),
                            category = obj.optString("category", "کاری"),
                            colorHex = obj.optString("colorHex", "#F59E0B")
                        )
                        repository.insertEvent(event)
                        onLog("رویداد جدید افزوده شد: ${event.title}")
                        sendResponse(output, 201, "Created", JSONObject().put("success", true).toString(), "application/json")
                    }

                    method == "POST" && path == "/api/tasks" -> {
                        val obj = JSONObject(body)
                        val task = TaskEntity(
                            title = obj.getString("title"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            persianDueDate = obj.optString("persianDueDate", ""),
                            priority = obj.optString("priority", "متوسط")
                        )
                        repository.insertTask(task)
                        onLog("وظیفه جدید افزوده شد: ${task.title}")
                        sendResponse(output, 201, "Created", JSONObject().put("success", true).toString(), "application/json")
                    }

                    method == "POST" && path == "/api/notes" -> {
                        val obj = JSONObject(body)
                        val note = NoteEntity(
                            title = obj.optString("title", "یادداشت جدید"),
                            content = obj.getString("content"),
                            persianDate = obj.optString("persianDate", ""),
                            colorHex = obj.optString("colorHex", "#FEF3C7")
                        )
                        repository.insertNote(note)
                        onLog("یادداشت جدید افزوده شد: ${note.title}")
                        sendResponse(output, 201, "Created", JSONObject().put("success", true).toString(), "application/json")
                    }

                    else -> {
                        sendResponse(output, 404, "Not Found", "{\"error\":\"Route not found\"}", "application/json")
                    }
                }
            } catch (e: Exception) {
                try {
                    sendResponse(s.getOutputStream(), 500, "Internal Server Error", "{\"error\":\"${e.message}\"}", "application/json")
                } catch (_: Exception) {}
            }
        }
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        body: String,
        contentType: String
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val header = StringBuilder()
            .append("HTTP/1.1 $statusCode $statusText\r\n")
            .append("Content-Type: $contentType; charset=utf-8\r\n")
            .append("Content-Length: ${bytes.size}\r\n")
            .append("Access-Control-Allow-Origin: *\r\n")
            .append("Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE\r\n")
            .append("Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With\r\n")
            .append("Connection: close\r\n")
            .append("\r\n")

        output.write(header.toString().toByteArray(StandardCharsets.UTF_8))
        if (bytes.isNotEmpty()) {
            output.write(bytes)
        }
        output.flush()
    }
}
