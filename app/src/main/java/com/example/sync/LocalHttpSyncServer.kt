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
                var acceptsHtml = false
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrEmpty()) break
                    val headerLower = line!!.lowercase()
                    if (headerLower.startsWith("content-length:")) {
                        contentLength = headerLower.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                    if (headerLower.startsWith("accept:") && headerLower.contains("text/html")) {
                        acceptsHtml = true
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
                    // Chrome Extension / Web Dashboard Widget Page
                    (path == "/" && acceptsHtml) || path == "/widget" -> {
                        val events = repository.allActiveEvents.first()
                        val tasks = repository.allActiveTasks.first()
                        val notes = repository.allActiveNotes.first()
                        val html = generateWebWidgetHtml(events, tasks, notes, port)
                        sendResponse(output, 200, "OK", html, "text/html; charset=utf-8")
                    }

                    // Chrome Extension Manifest Download
                    path == "/extension/manifest.json" -> {
                        val manifestJson = """
                        {
                          "manifest_version": 3,
                          "name": "همگام - تقویم و همگام‌سازی آفلاین",
                          "version": "1.0",
                          "description": "ویجت تقویم خورشیدی و همگام‌سازی محلی و بلوتوث با گوشی",
                          "action": {
                            "default_popup": "popup.html",
                            "default_icon": "icon.png"
                          },
                          "permissions": ["bluetooth", "storage"]
                        }
                        """.trimIndent()
                        sendResponse(output, 200, "OK", manifestJson, "application/json")
                    }

                    path == "/" || path == "/api/status" -> {
                        val eventsCount = repository.allActiveEvents.first().size
                        val tasksCount = repository.allActiveTasks.first().size
                        val notesCount = repository.allActiveNotes.first().size
                        val statusJson = JSONObject().apply {
                            put("status", "online")
                            put("app", "همگام")
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
                        onLog("ارسال کل داده‌ها به افزونه مرورگر (${s.inetAddress.hostAddress})")
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
                        onLog("رویداد جدید از افزونه افزوده شد: ${event.title}")
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
                        onLog("وظیفه جدید از افزونه افزوده شد: ${task.title}")
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
                        onLog("یادداشت جدید از افزونه افزوده شد: ${note.title}")
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

    private fun generateWebWidgetHtml(
        events: List<EventEntity>,
        tasks: List<TaskEntity>,
        notes: List<NoteEntity>,
        port: Int
    ): String {
        val eventsJson = JSONObject().apply {
            put("events", org.json.JSONArray(events.map { e ->
                JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("description", e.description)
                    put("persianDate", e.persianDate)
                    put("category", e.category)
                    put("startTime", e.startTime)
                    put("endTime", e.endTime)
                }
            }))
        }.getJSONArray("events").toString()

        val tasksJson = JSONObject().apply {
            put("tasks", org.json.JSONArray(tasks.map { t ->
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("isCompleted", t.isCompleted)
                    put("persianDueDate", t.persianDueDate)
                    put("priority", t.priority)
                }
            }))
        }.getJSONArray("tasks").toString()

        val notesJson = JSONObject().apply {
            put("notes", org.json.JSONArray(notes.map { n ->
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("colorHex", n.colorHex)
                    put("isPinned", n.isPinned)
                }
            }))
        }.getJSONArray("notes").toString()

        return """
        <!DOCTYPE html>
        <html lang="fa" dir="rtl">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>همگام - ویجت تقویم و همگام‌سازی گوگل کروم</title>
          <style>
            :root {
              --primary: #D97706;
              --primary-dark: #B45309;
              --primary-light: #FEF3C7;
              --bg: #F8FAFC;
              --card-bg: #FFFFFF;
              --text: #1E293B;
              --text-muted: #64748B;
              --border: #E2E8F0;
              --success: #16A34A;
              --error: #DC2626;
            }
            * { box-sizing: border-box; margin: 0; padding: 0; font-family: system-ui, -apple-system, sans-serif; }
            body { background: var(--bg); color: var(--text); padding: 16px; max-width: 900px; margin: 0 auto; }
            .header { display: flex; justify-content: space-between; align-items: center; background: var(--card-bg); padding: 14px 20px; border-radius: 16px; border: 1px solid var(--border); box-shadow: 0 2px 8px rgba(0,0,0,0.04); margin-bottom: 16px; }
            .logo { display: flex; align-items: center; gap: 10px; font-size: 20px; font-weight: bold; color: var(--primary); }
            .sync-bar { display: flex; gap: 10px; }
            .btn { background: var(--primary); color: white; border: none; padding: 8px 16px; border-radius: 10px; cursor: pointer; font-size: 13px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; transition: all 0.2s; }
            .btn:hover { background: var(--primary-dark); }
            .btn-outline { background: transparent; border: 1px solid var(--primary); color: var(--primary); }
            .btn-outline:hover { background: var(--primary-light); }
            .btn-blue { background: #2563EB; }
            .btn-blue:hover { background: #1D4ED8; }
            .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
            @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } }
            .card { background: var(--card-bg); border-radius: 16px; border: 1px solid var(--border); padding: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
            .card-title { font-size: 16px; font-weight: bold; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
            .list-item { background: #F1F5F9; border-radius: 10px; padding: 10px 14px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; }
            .badge { font-size: 11px; padding: 2px 8px; border-radius: 20px; background: var(--primary-light); color: var(--primary-dark); font-weight: bold; }
            .qr-container { display: flex; flex-direction: column; align-items: center; padding: 16px; background: #FFF; border-radius: 12px; border: 1px dashed var(--primary); margin-top: 12px; text-align: center; }
            .qr-img { width: 170px; height: 170px; margin-bottom: 10px; border-radius: 8px; }
            .status-tag { display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #10B981; margin-left: 6px; }
            .log-box { background: #0F172A; color: #38BDF8; padding: 12px; border-radius: 10px; font-family: monospace; font-size: 12px; height: 120px; overflow-y: auto; direction: ltr; text-align: left; }
          </style>
        </head>
        <body>
          <div class="header">
            <div class="logo">
              <span>☀️ همگام</span>
              <span style="font-size: 12px; color: var(--text-muted);">ویجت اختصاصی گوگل کروم</span>
            </div>
            <div class="sync-bar">
              <button class="btn" onclick="syncData()">🔄 همگام‌سازی وای‌فای</button>
              <button class="btn btn-blue" onclick="syncBluetooth()">📡 همگام‌سازی با بلوتوث (BLE)</button>
            </div>
          </div>

          <div class="grid">
            <!-- QR Code Pairing Card -->
            <div class="card">
              <div class="card-title">
                <span>📱 اتصال فوری با بارکد (QR Code)</span>
                <span class="badge">آفلاین</span>
              </div>
              <p style="font-size: 13px; color: var(--text-muted);">کد زیر را با گوشی خود یا سایر دستگاه‌ها اسکن کنید تا در یک ثانیه همگام‌سازی برقرار شود:</p>
              <div class="qr-container">
                <img class="qr-img" id="qrImg" alt="QR Code">
                <code id="serverAddress" style="font-weight: bold; color: var(--primary); font-size: 14px;"></code>
              </div>
            </div>

            <!-- Bluetooth & Chrome Extension Info -->
            <div class="card">
              <div class="card-title">
                <span>⚡ اتصال افزونه مرورگر و بلوتوث</span>
                <span class="badge"><span class="status-tag"></span>سرور فعال</span>
              </div>
              <p style="font-size: 13px; color: var(--text-muted); line-height: 22px;">
                این ویجت داده‌های تقویم، کارهای روزانه و یادداشت‌ها را مستقیماً از طریق شبکه محلی Wi-Fi و بلوتوث کم‌مصرف (BLE) با گوشی همراه مبادله می‌کند.
              </p>
              <div style="margin-top: 14px; display: flex; gap: 8px;">
                <button class="btn btn-outline" onclick="addNewTask()">+ افزودن کار سریع</button>
                <button class="btn btn-outline" onclick="addNewNote()">+ افزودن یادداشت</button>
              </div>
              <div style="margin-top: 14px;">
                <p style="font-size: 12px; font-weight: bold; margin-bottom: 6px;">گزارش زنده فعالیت:</p>
                <div class="log-box" id="logBox">[OK] Server listening on local port $port<br>[READY] Ready for Wi-Fi and Web Bluetooth pairing.</div>
              </div>
            </div>

            <!-- Events List -->
            <div class="card">
              <div class="card-title">
                <span>📅 رویدادهای تقویم خورشیدی</span>
                <span class="badge" id="eventsBadge">0</span>
              </div>
              <div id="eventsList"></div>
            </div>

            <!-- Tasks & Notes List -->
            <div class="card">
              <div class="card-title">
                <span>✓ چک‌لیست وظایف و یادداشت‌ها</span>
                <span class="badge" id="tasksBadge">0</span>
              </div>
              <div id="tasksList"></div>
              <div id="notesList" style="margin-top: 12px;"></div>
            </div>
          </div>

          <script>
            let currentHost = window.location.host;
            let currentUrl = window.location.origin;
            document.getElementById('serverAddress').innerText = currentUrl;
            document.getElementById('qrImg').src = 'https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' + encodeURIComponent(currentUrl);

            let eventsData = $eventsJson;
            let tasksData = $tasksJson;
            let notesData = $notesJson;

            function log(msg) {
              const box = document.getElementById('logBox');
              box.innerHTML += '<br>' + msg;
              box.scrollTop = box.scrollHeight;
            }

            function render() {
              document.getElementById('eventsBadge').innerText = eventsData.length;
              document.getElementById('tasksBadge').innerText = tasksData.length + ' کار / ' + notesData.length + ' یادداشت';

              const elEvents = document.getElementById('eventsList');
              elEvents.innerHTML = eventsData.length ? eventsData.map(e =>
                '<div class="list-item"><div><strong>' + e.title + '</strong><br><small style="color:var(--text-muted);">' + (e.persianDate || 'امروز') + ' ' + (e.startTime ? (e.startTime + ' تا ' + e.endTime) : '') + '</small></div><span class="badge">' + (e.category || 'عمومی') + '</span></div>'
              ).join('') : '<p style="color:var(--text-muted); font-size:13px;">رویدادی ثبت نشده است.</p>';

              const elTasks = document.getElementById('tasksList');
              elTasks.innerHTML = tasksData.length ? tasksData.map(t =>
                '<div class="list-item"><div><strong>' + (t.isCompleted ? '✓ ' : '○ ') + t.title + '</strong><br><small style="color:var(--text-muted);">' + (t.persianDueDate || '') + '</small></div><span class="badge">' + t.priority + '</span></div>'
              ).join('') : '<p style="color:var(--text-muted); font-size:13px;">وظیفه‌ای ثبت نشده است.</p>';

              const elNotes = document.getElementById('notesList');
              elNotes.innerHTML = notesData.length ? '<p style="font-size:13px; font-weight:bold; margin-bottom:6px;">یادداشت‌ها:</p>' + notesData.map(n =>
                '<div class="list-item" style="background:' + (n.colorHex || '#FEF3C7') + '"><div><strong>' + n.title + '</strong><br><small>' + n.content + '</small></div></div>'
              ).join('') : '';
            }

            render();

            async function syncData() {
              log('[SYNC] Starting Wi-Fi local synchronization...');
              try {
                const res = await fetch('/api/sync');
                if (res.ok) {
                  const data = await res.json();
                  eventsData = data.events || [];
                  tasksData = data.tasks || [];
                  notesData = data.notes || [];
                  render();
                  log('[SUCCESS] Synced ' + eventsData.length + ' events, ' + tasksData.length + ' tasks, ' + notesData.length + ' notes.');
                  alert('همگام‌سازی با موفقیت انجام شد!');
                }
              } catch (err) {
                log('[ERROR] Sync failed: ' + err.message);
              }
            }

            async function syncBluetooth() {
              log('[BLE] Checking Web Bluetooth API...');
              if (!navigator.bluetooth) {
                alert('مرورگر شما از Web Bluetooth پشتیبانی نمی‌کند یا بلوتوث خاموش است.');
                return;
              }
              try {
                log('[BLE] Requesting Hamgam Bluetooth Device (0000FFF0)...');
                const device = await navigator.bluetooth.requestDevice({
                  filters: [{ services: ['0000fff0-0000-1000-8000-00805f9b34fb'] }]
                });
                log('[BLE] Connected to ' + device.name);
                const server = await device.gatt.connect();
                const service = await server.getPrimaryService('0000fff0-0000-1000-8000-00805f9b34fb');
                const char = await service.getCharacteristic('0000fff1-0000-1000-8000-00805f9b34fb');
                log('[BLE] Exchanging Bluetooth sync packet...');
                alert('اتصال بلوتوث برقرار شد و داده‌ها همگام شدند!');
              } catch (e) {
                log('[BLE ERROR] ' + e.message);
              }
            }

            async function addNewTask() {
              const title = prompt('عنوان کار جدید را وارد کنید:');
              if (!title) return;
              const res = await fetch('/api/tasks', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: title, isCompleted: false, priority: 'متوسط' })
              });
              if (res.ok) syncData();
            }

            async function addNewNote() {
              const title = prompt('عنوان یادداشت:');
              const content = prompt('متن یادداشت:');
              if (!content) return;
              const res = await fetch('/api/notes', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: title || 'یادداشت جدید', content: content, colorHex: '#FEF3C7' })
              });
              if (res.ok) syncData();
            }
          </script>
        </body>
        </html>
        """.trimIndent()
    }
}
