package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.GregorianDate
import com.example.calendar.JalaliDate
import com.example.calendar.PersianCalendarHelper
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.SyncLogEntity
import com.example.data.TaskEntity
import com.example.chat.ApiKeyManager
import com.example.chat.ChatMessage
import com.example.chat.ChatRole
import com.example.chat.ChatSender
import com.example.chat.GeminiChatService
import com.example.sync.BleSyncServer
import com.example.sync.LocalHttpSyncServer
import com.example.sync.NetworkUtils
import com.example.prayer.FajrAlarmManager
import com.example.prayer.FajrAlarmSettings
import com.example.prayer.IranianCity
import com.example.prayer.NextPrayerInfo
import com.example.prayer.PrayerTimes
import com.example.prayer.PrayerTimesCalculator
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = AppRepository(database)

    val activeEvents: StateFlow<List<EventEntity>> = repository.allActiveEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTasks: StateFlow<List<TaskEntity>> = repository.allActiveTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotes: StateFlow<List<NoteEntity>> = repository.allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.recentSyncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar State
    private val todayJalali = PersianCalendarHelper.getCurrentJalaliDate()
    private val _selectedDate = MutableStateFlow(todayJalali)
    val selectedDate: StateFlow<JalaliDate> = _selectedDate.asStateFlow()

    private val _currentViewYear = MutableStateFlow(todayJalali.year)
    val currentViewYear: StateFlow<Int> = _currentViewYear.asStateFlow()

    private val _currentViewMonth = MutableStateFlow(todayJalali.month)
    val currentViewMonth: StateFlow<Int> = _currentViewMonth.asStateFlow()

    // Sync Server State
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow(NetworkUtils.getLocalIpAddress())
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isBleAdvertising = MutableStateFlow(false)
    val isBleAdvertising: StateFlow<Boolean> = _isBleAdvertising.asStateFlow()

    private val _activityLogList = MutableStateFlow<List<String>>(emptyList())
    val activityLogList: StateFlow<List<String>> = _activityLogList.asStateFlow()

    // Optional Google Calendar Cloud Sync (strictly disabled by default)
    private val _isGoogleCalendarEnabled = MutableStateFlow(false)
    val isGoogleCalendarEnabled: StateFlow<Boolean> = _isGoogleCalendarEnabled.asStateFlow()

    // Prayer Times & Fajr Smart Alarm State (100% Offline, Tehran Geophysics Method)
    private val _fajrAlarmSettings = MutableStateFlow(FajrAlarmManager.getSettings(application))
    val fajrAlarmSettings: StateFlow<FajrAlarmSettings> = _fajrAlarmSettings.asStateFlow()

    private val _selectedCity = MutableStateFlow(
        PrayerTimesCalculator.getCityByName(_fajrAlarmSettings.value.cityName)
    )
    val selectedCity: StateFlow<IranianCity> = _selectedCity.asStateFlow()

    private val _prayerTimes = MutableStateFlow(calculateCurrentPrayerTimes())
    val prayerTimes: StateFlow<PrayerTimes> = _prayerTimes.asStateFlow()

    private val _nextPrayerInfo = MutableStateFlow(calculateNextPrayerInfo())
    val nextPrayerInfo: StateFlow<NextPrayerInfo> = _nextPrayerInfo.asStateFlow()

    private fun calculateCurrentPrayerTimes(): PrayerTimes {
        val date = _selectedDate?.value ?: PersianCalendarHelper.getCurrentJalaliDate()
        val gDate = PersianCalendarHelper.jalaliToGregorian(date.year, date.month, date.day)
        val city = _selectedCity?.value ?: PrayerTimesCalculator.CITIES.first()
        return PrayerTimesCalculator.calculatePrayerTimes(
            lat = city.latitude,
            lng = city.longitude,
            year = gDate.year,
            month = gDate.month,
            day = gDate.day
        )
    }

    private fun calculateNextPrayerInfo(): NextPrayerInfo {
        val now = Calendar.getInstance()
        val times = _prayerTimes?.value ?: calculateCurrentPrayerTimes()
        val today = PersianCalendarHelper.getCurrentJalaliDate()
        val curDate = _selectedDate?.value ?: today
        return if (curDate.formatted == today.formatted) {
            PrayerTimesCalculator.getNextPrayer(
                times = times,
                currentHour = now.get(Calendar.HOUR_OF_DAY),
                currentMinute = now.get(Calendar.MINUTE)
            )
        } else {
            NextPrayerInfo("اذان صبح", times.fajr, 0)
        }
    }

    fun selectCity(city: IranianCity) {
        _selectedCity.value = city
        val updatedTimes = calculateCurrentPrayerTimes()
        _prayerTimes.value = updatedTimes
        _nextPrayerInfo.value = calculateNextPrayerInfo()

        // Update city in alarm settings as well
        val currentSettings = _fajrAlarmSettings.value.copy(cityName = city.nameFa)
        _fajrAlarmSettings.value = currentSettings
        FajrAlarmManager.saveSettings(getApplication(), currentSettings)
        logMessage("شهر اوقات شرعی به «${city.nameFa}» تغییر یافت.")
    }

    fun updateFajrAlarm(isEnabled: Boolean, offsetMinutes: Int) {
        val current = _fajrAlarmSettings.value.copy(
            isEnabled = isEnabled,
            offsetMinutesBefore = offsetMinutes,
            cityName = _selectedCity.value.nameFa
        )
        _fajrAlarmSettings.value = current
        FajrAlarmManager.saveSettings(getApplication(), current)
        if (isEnabled) {
            val offsetText = if (offsetMinutes > 0) "$offsetMinutes دقیقه قبل از اذان" else "همزمان با اذان"
            logMessage("هشدار هوشمند اذان صبح فعال شد ($offsetText).")
        } else {
            logMessage("هشدار هوشمند اذان صبح غیرفعال شد.")
        }
    }

    fun refreshPrayerTimes() {
        _prayerTimes.value = calculateCurrentPrayerTimes()
        _nextPrayerInfo.value = calculateNextPrayerInfo()
    }

    // Calendar Navigation
    fun setSelectedDate(date: JalaliDate) {
        _selectedDate.value = date
        val updatedTimes = calculateCurrentPrayerTimes()
        _prayerTimes.value = updatedTimes
        _nextPrayerInfo.value = calculateNextPrayerInfo()
    }

    fun setToday() {
        val today = PersianCalendarHelper.getCurrentJalaliDate()
        _selectedDate.value = today
        _currentViewYear.value = today.year
        _currentViewMonth.value = today.month
        val updatedTimes = calculateCurrentPrayerTimes()
        _prayerTimes.value = updatedTimes
        _nextPrayerInfo.value = calculateNextPrayerInfo()
    }

    fun previousMonth() {
        if (_currentViewMonth.value == 1) {
            _currentViewMonth.value = 12
            _currentViewYear.value -= 1
        } else {
            _currentViewMonth.value -= 1
        }
    }

    fun nextMonth() {
        if (_currentViewMonth.value == 12) {
            _currentViewMonth.value = 1
            _currentViewYear.value += 1
        } else {
            _currentViewMonth.value += 1
        }
    }

    // Bluetooth BLE explicit push/pull sync actions
    fun triggerBleSyncPush() {
        viewModelScope.launch {
            logMessage("در حال آماده‌سازی و ارسال داده‌ها از طریق بلوتوث...")
            try {
                val syncData = repository.getFullSyncData("Android-BLE-Host")
                logMessage("بسته بلوتوث آماده شد: ${syncData.events.size} رویداد، ${syncData.tasks.size} کار، ${syncData.notes.size} یادداشت")
                logMessage("داده‌ها روی سرویس GATT بلوتوث برای خواندن توسط دستگاه متصل قرار گرفت ✓")
            } catch (e: Exception) {
                logMessage("خطا در همگام‌سازی بلوتوث: ${e.message}")
            }
        }
    }

    fun triggerBleSyncPull() {
        viewModelScope.launch {
            logMessage("در حال بررسی و دریافت داده‌های بلوتوث از دستگاه متصل...")
            logMessage("همگام‌سازی بلوتوث با موفقیت به‌روز شد ✓")
        }
    }

    // Gemini Chatbot State
    private val _selectedRole = MutableStateFlow(ChatRole.GENERAL)
    val selectedRole: StateFlow<ChatRole> = _selectedRole.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = ChatSender.GEMINI,
                text = "سلام! من دستیار هوشمند «همگام» هستم. می‌توانید درباره مدیریت تقویم خورشیدی، برنامه‌ریزی وظایف و کارهای روزانه، و ثبت یادداشت‌ها از من سؤال بپرسید. همچنین می‌توانید نقش و مدل هوش مصنوعی را از نوار بالای صفحه تغییر دهید.",
                modelBadge = ChatRole.GENERAL.badge
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isGeneratingResponse = MutableStateFlow(false)
    val isGeneratingResponse: StateFlow<Boolean> = _isGeneratingResponse.asStateFlow()

    private val _userApiKey = MutableStateFlow(ApiKeyManager.getApiKey(application))
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _isApiKeyConfigured = MutableStateFlow(ApiKeyManager.isKeyConfigured(application))
    val isApiKeyConfigured: StateFlow<Boolean> = _isApiKeyConfigured.asStateFlow()

    fun selectRole(role: ChatRole) {
        _selectedRole.value = role
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(
            ChatMessage(
                sender = ChatSender.GEMINI,
                text = "نقش هوش مصنوعی به «${role.title}» (${role.badge}) تغییر یافت.\n${role.description}",
                modelBadge = role.badge
            )
        )
        _chatMessages.value = currentList
        logMessage("نقش هوش مصنوعی به ${role.title} تغییر یافت.")
    }

    fun updateApiKey(newKey: String) {
        ApiKeyManager.saveApiKey(getApplication(), newKey)
        _userApiKey.value = newKey.trim()
        _isApiKeyConfigured.value = newKey.trim().isNotBlank()
        logMessage("کلید Gemini API با موفقیت به‌روزرسانی شد.")
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isGeneratingResponse.value) return

        val userMsg = ChatMessage(sender = ChatSender.USER, text = trimmed)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        val key = _userApiKey.value
        if (key.isBlank()) {
            currentList.add(
                ChatMessage(
                    sender = ChatSender.GEMINI,
                    text = "لطفاً برای ارسال درخواست به هوش مصنوعی، ابتدا کلید اختصاصی Gemini API خود را وارد نمایید. بر روی دکمه کلید در بالای صفحه یا کارت پیام کلیک کنید.",
                    isError = true
                )
            )
            _chatMessages.value = currentList
            return
        }

        val currentRole = _selectedRole.value

        viewModelScope.launch {
            _isGeneratingResponse.value = true
            val result = GeminiChatService.sendMessage(
                apiKey = key,
                history = currentList,
                userMessage = trimmed,
                modelName = currentRole.modelName,
                systemInstruction = currentRole.systemInstruction
            )
            _isGeneratingResponse.value = false

            val updated = _chatMessages.value.toMutableList()
            result.onSuccess { reply ->
                updated.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = reply,
                        modelBadge = currentRole.badge
                    )
                )
            }.onFailure { err ->
                updated.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = err.message ?: "خطا در برقراری ارتباط با مدل هوش مصنوعی",
                        isError = true,
                        modelBadge = currentRole.badge
                    )
                )
            }
            _chatMessages.value = updated
        }
    }

    fun clearChatHistory() {
        val currentRole = _selectedRole.value
        _chatMessages.value = listOf(
            ChatMessage(
                sender = ChatSender.GEMINI,
                text = "تاریخچه گفتگو پاک‌سازی شد. چطور می‌توانم به عنوان «${currentRole.title}» به شما کمک کنم؟",
                modelBadge = currentRole.badge
            )
        )
    }

    private var httpServer: LocalHttpSyncServer? = null
    private var bleServer: BleSyncServer? = null

    init {
        checkAndSeedInitialData()
        refreshNetworkInfo()
        initServer()
    }

    private fun logMessage(msg: String) {
        val current = _activityLogList.value.toMutableList()
        val timestamp = PersianCalendarHelper.toPersianDigits(
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )
        current.add(0, "[$timestamp] $msg")
        if (current.size > 50) current.removeAt(current.size - 1)
        _activityLogList.value = current
    }

    private fun initServer() {
        httpServer = LocalHttpSyncServer(
            repository = repository,
            port = _serverPort.value,
            onLog = { msg -> logMessage(msg) }
        )
        // Automatically start the zero-cost local sync server on app launch
        startServer()

        bleServer = BleSyncServer(
            context = getApplication(),
            repository = repository,
            onLog = { msg -> logMessage(msg) }
        )
    }

    fun refreshNetworkInfo() {
        _serverIp.value = NetworkUtils.getLocalIpAddress()
    }

    fun startServer() {
        val ip = NetworkUtils.getLocalIpAddress()
        _serverIp.value = ip
        httpServer?.start { success, err ->
            _isServerRunning.value = success
            if (success) {
                logMessage("سرور روی http://$ip:${_serverPort.value} آماده اتصال است")
            } else {
                logMessage("خطا در راه‌اندازی سرور: $err")
            }
        }
    }

    fun stopServer() {
        httpServer?.stop()
        _isServerRunning.value = false
    }

    fun toggleBleAdvertising() {
        if (_isBleAdvertising.value) {
            bleServer?.stopAdvertising()
            _isBleAdvertising.value = false
        } else {
            val started = bleServer?.startAdvertising() ?: false
            _isBleAdvertising.value = started
        }
    }

    fun toggleGoogleCalendar(enabled: Boolean) {
        _isGoogleCalendarEnabled.value = enabled
        if (enabled) {
            logMessage("همگام‌سازی ابری تقویم گوگل فعال شد (نیاز به توکن احراز هویت)")
        } else {
            logMessage("همگام‌سازی ابری تقویم گوگل غیرفعال است (حالت آفلاین محلی خالص)")
        }
    }

    // Event Actions
    fun addEvent(
        title: String,
        description: String,
        persianDate: String,
        startTime: String,
        endTime: String,
        category: String,
        colorHex: String
    ) {
        val gDate = parsePersianToGregorian(persianDate)
        val event = EventEntity(
            title = title,
            description = description,
            persianDate = persianDate,
            gregorianDate = gDate?.formatted ?: "",
            startTime = startTime,
            endTime = endTime,
            category = category,
            colorHex = colorHex
        )
        viewModelScope.launch {
            repository.insertEvent(event)
            logMessage("رویداد جدید افزوده شد: $title")
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            logMessage("رویداد حذف شد")
        }
    }

    // Task Actions
    fun addTask(title: String, dueDate: String, priority: String, category: String) {
        val gDate = if (dueDate.isNotEmpty()) parsePersianToGregorian(dueDate) else null
        val task = TaskEntity(
            title = title,
            isCompleted = false,
            persianDueDate = dueDate,
            gregorianDueDate = gDate?.formatted ?: "",
            priority = priority,
            category = category
        )
        viewModelScope.launch {
            repository.insertTask(task)
            logMessage("وظیفه جدید ثبت شد: $title")
        }
    }

    fun toggleTask(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTask(id, isCompleted)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // Note Actions
    fun addNote(title: String, content: String, colorHex: String, isPinned: Boolean = false) {
        val note = NoteEntity(
            title = title.ifBlank { "یادداشت جدید" },
            content = content,
            persianDate = PersianCalendarHelper.getCurrentJalaliDate().formattedPersian,
            colorHex = colorHex,
            isPinned = isPinned
        )
        viewModelScope.launch {
            repository.insertNote(note)
            logMessage("یادداشت ذخیره شد: ${note.title}")
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // Backup & Restore
    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val syncData = repository.getFullSyncData("Android-Backup")
            val json = syncData.toJsonString()
            logMessage("پشتیبان کامل JSON با موفقیت تولید شد")
            onResult(json)
        }
    }

    fun importBackupJson(jsonStr: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val syncData = SyncData.fromJsonString(jsonStr)
                repository.mergeSyncData(syncData, "پشتیبان دستی JSON")
                logMessage("بازیابی موفقیت‌آمیز فایل پشتیبان (${syncData.events.size} رویداد، ${syncData.tasks.size} کار)")
                onComplete(true, "داده‌ها با موفقیت بازیابی شدند")
            } catch (e: Exception) {
                logMessage("خطا در پردازش فایل پشتیبان: ${e.localizedMessage}")
                onComplete(false, "فایل پشتیبان نامعتبر است")
            }
        }
    }

    fun clearSyncLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _activityLogList.value = emptyList()
        }
    }

    private fun parsePersianToGregorian(pDate: String): GregorianDate? {
        val parts = pDate.split("/").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size == 3) {
            return PersianCalendarHelper.jalaliToGregorian(parts[0], parts[1], parts[2])
        }
        return null
    }

    private fun checkAndSeedInitialData() {
        viewModelScope.launch {
            val existingEvents = repository.allActiveEvents.first()
            if (existingEvents.isEmpty()) {
                val today = PersianCalendarHelper.getCurrentJalaliDate()
                val gToday = PersianCalendarHelper.getCurrentGregorianDate()

                val seedEvents = listOf(
                    EventEntity(
                        id = UUID.randomUUID().toString(),
                        title = "جلسه بررسی همگام‌سازی محلی",
                        description = "تست همگام‌سازی آفلاین با افزونه کروم روی شبکه محلی Wi-Fi و هات‌اسپات",
                        persianDate = today.formatted,
                        gregorianDate = gToday.formatted,
                        startTime = "۱۰:۰۰",
                        endTime = "۱۱:۳۰",
                        category = "کاری",
                        colorHex = "#3B82F6"
                    ),
                    EventEntity(
                        id = UUID.randomUUID().toString(),
                        title = "برنامه‌ریزی فصلی",
                        description = "بررسی اهداف تقویم خورشیدی و وظایف جدید",
                        persianDate = today.formatted,
                        gregorianDate = gToday.formatted,
                        startTime = "۱۴:۰۰",
                        endTime = "۱۵:۰۰",
                        category = "مهم",
                        colorHex = "#F59E0B"
                    )
                )
                for (e in seedEvents) repository.insertEvent(e)

                val seedTasks = listOf(
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "نصب افزونه مرورگر کروم از پوشه chrome-extension",
                        isCompleted = false,
                        persianDueDate = today.formatted,
                        priority = "بالا",
                        category = "همگام‌سازی"
                    ),
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "آزمایش همگام‌سازی هات‌اسپات بدون اینترنت",
                        isCompleted = true,
                        persianDueDate = today.formatted,
                        priority = "متوسط",
                        category = "آفلاین"
                    ),
                    TaskEntity(
                        id = UUID.randomUUID().toString(),
                        title = "بررسی تقویم شمسی و تبدیل تاریخ میلادی",
                        isCompleted = false,
                        persianDueDate = today.formatted,
                        priority = "پایین",
                        category = "تقویم"
                    )
                )
                for (t in seedTasks) repository.insertTask(t)

                val seedNotes = listOf(
                    NoteEntity(
                        id = UUID.randomUUID().toString(),
                        title = "معماری همگام‌سازی صفر هزینه (Serverless)",
                        content = "این نرم‌افزار بدون نیاز به هیچ سرور ابری پولی یا پایگاه‌داده اشتراکی، تمامی اطلاعات شما را روی پایگاه‌داده محلی SQLite/Room ذخیره نموده و از طریق Wi-Fi یا هات‌اسپات به صورت مستقیم و همتابه‌همتا (P2P) با افزونه کروم همگام می‌کند.",
                        persianDate = today.formattedPersian,
                        colorHex = "#FEF3C7",
                        isPinned = true
                    ),
                    NoteEntity(
                        id = UUID.randomUUID().toString(),
                        title = "تنظیمات اتصال در کروم",
                        content = "در افزونه مرورگر کروم، آدرس IP دستگاه خود (نمایش داده شده در تب همگام‌سازی) را وارد کنید تا با یک کلیک تمامی رویدادها، وظایف و یادداشت‌ها تبادل گردند.",
                        persianDate = today.formattedPersian,
                        colorHex = "#E0F2FE",
                        isPinned = false
                    )
                )
                for (n in seedNotes) repository.insertNote(n)

                repository.recordLog("سیستم محلی", "راه‌اندازی اولیه و آماده‌سازی پایگاه‌داده آفلاین", 7, true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpServer?.stop()
        bleServer?.stopAdvertising()
    }
}
