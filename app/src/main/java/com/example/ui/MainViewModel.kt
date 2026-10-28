package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.GregorianDate
import com.example.calendar.JalaliDate
import com.example.calendar.PersianCalendarHelper
import com.example.calendar.PersianOccasionsHelper
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.SyncLogEntity
import com.example.data.TaskEntity
import com.example.chat.AiProvider
import com.example.chat.ApiKeyManager
import com.example.chat.ChatMessage
import com.example.chat.ChatRole
import com.example.chat.ChatSender
import com.example.chat.GeminiChatService
import com.example.sync.LocalHttpSyncServer
import com.example.sync.GoogleCalendarSyncManager
import com.example.sync.NetworkUtils
import com.example.prayer.FajrAlarmManager
import com.example.prayer.FajrAlarmSettings
import com.example.prayer.IranianCity
import com.example.prayer.NextPrayerInfo
import com.example.prayer.PrayerTimes
import com.example.prayer.PrayerTimesCalculator
import com.example.update.AppUpdateManager
import com.example.update.UpdateInfo
import com.example.update.UpdateDownloadState
import java.io.File
import com.example.alarm.EventAlarmScheduler
import com.example.settings.AppPreferences
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    init {
        PersianOccasionsHelper.loadCustomOccasions(application)
    }

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

    // Language State
    private val _appLanguage = MutableStateFlow(AppPreferences.getAppLanguage(application))
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAppLanguage(lang: String) {
        AppPreferences.setAppLanguage(getApplication(), lang)
        _appLanguage.value = lang
    }

    // Dark Mode Theme State
    private val _isDarkMode = MutableStateFlow(AppPreferences.isDarkMode(application))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        AppPreferences.setDarkMode(getApplication(), next)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        AppPreferences.setDarkMode(getApplication(), enabled)
    }

    // Calendar State
    private val todayJalali = PersianCalendarHelper.getCurrentJalaliDate()
    private val _selectedDate = MutableStateFlow(todayJalali)
    val selectedDate: StateFlow<JalaliDate> = _selectedDate.asStateFlow()

    private val _currentViewYear = MutableStateFlow(todayJalali.year)
    val currentViewYear: StateFlow<Int> = _currentViewYear.asStateFlow()

    private val _currentViewMonth = MutableStateFlow(todayJalali.month)
    val currentViewMonth: StateFlow<Int> = _currentViewMonth.asStateFlow()

    private val _occasionsVersion = MutableStateFlow(0)
    val occasionsVersion: StateFlow<Int> = _occasionsVersion.asStateFlow()

    fun refreshOccasions() {
        _occasionsVersion.value += 1
    }

    // Sync Server State
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverIp = MutableStateFlow(NetworkUtils.getLocalIpAddress())
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _activityLogList = MutableStateFlow<List<String>>(emptyList())
    val activityLogList: StateFlow<List<String>> = _activityLogList.asStateFlow()

    // Google Calendar Sync (via Calendar Content Provider)
    val googleCalSyncManager = GoogleCalendarSyncManager(application)
    private val _isGoogleCalendarEnabled = MutableStateFlow(false)
    val isGoogleCalendarEnabled: StateFlow<Boolean> = _isGoogleCalendarEnabled.asStateFlow()

    private val _googleCalendars = MutableStateFlow<List<GoogleCalendarSyncManager.CalendarAccount>>(emptyList())
    val googleCalendars: StateFlow<List<GoogleCalendarSyncManager.CalendarAccount>> = _googleCalendars.asStateFlow()

    private val _selectedGoogleCalName = MutableStateFlow(googleCalSyncManager.getSelectedCalendarName())
    val selectedGoogleCalName: StateFlow<String> = _selectedGoogleCalName.asStateFlow()

    private val _selectedGoogleCalId = MutableStateFlow(googleCalSyncManager.getSelectedCalendarId())
    val selectedGoogleCalId: StateFlow<Long> = _selectedGoogleCalId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

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

    // In-App GitHub Update & Safety Auto-Backup
    private val _availableUpdate = MutableStateFlow<UpdateInfo?>(null)
    val availableUpdate: StateFlow<UpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow<String?>(null)
    val updateStatusMessage: StateFlow<String?> = _updateStatusMessage.asStateFlow()

    private val _updateDownloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateDownloadState: StateFlow<UpdateDownloadState> = _updateDownloadState.asStateFlow()

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
        val prev = _fajrAlarmSettings.value
        if (prev.isEnabled == isEnabled && prev.offsetMinutesBefore == offsetMinutes) {
            return
        }

        val current = prev.copy(
            isEnabled = isEnabled,
            offsetMinutesBefore = offsetMinutes,
            cityName = _selectedCity.value.nameFa
        )
        _fajrAlarmSettings.value = current

        viewModelScope.launch(Dispatchers.IO) {
            FajrAlarmManager.saveSettings(getApplication(), current)
            if (isEnabled) {
                val offsetText = when {
                    offsetMinutes > 0 -> "$offsetMinutes دقیقه قبل از اذان"
                    offsetMinutes < 0 -> {
                        val after = -offsetMinutes
                        if (after == 60) "۱ ساعت بعد از اذان" else "$after دقیقه بعد از اذان"
                    }
                    else -> "همزمان با اذان"
                }
                logMessage("هشدار هوشمند اذان صبح فعال شد ($offsetText).")
            } else {
                logMessage("هشدار هوشمند اذان صبح غیرفعال شد.")
            }
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

    fun setViewYearMonth(year: Int, month: Int) {
        if (_currentViewYear.value != year || _currentViewMonth.value != month) {
            _currentViewYear.value = year
            _currentViewMonth.value = month
        }
    }

    fun previousDay() {
        val cur = _selectedDate.value
        val gDate = PersianCalendarHelper.jalaliToGregorian(cur.year, cur.month, cur.day)
        val cal = Calendar.getInstance().apply {
            set(gDate.year, gDate.month - 1, gDate.day)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val prevJalali = PersianCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        setSelectedDate(prevJalali)
        _currentViewYear.value = prevJalali.year
        _currentViewMonth.value = prevJalali.month
    }

    fun nextDay() {
        val cur = _selectedDate.value
        val gDate = PersianCalendarHelper.jalaliToGregorian(cur.year, cur.month, cur.day)
        val cal = Calendar.getInstance().apply {
            set(gDate.year, gDate.month - 1, gDate.day)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val nextJalali = PersianCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        setSelectedDate(nextJalali)
        _currentViewYear.value = nextJalali.year
        _currentViewMonth.value = nextJalali.month
    }

    fun nextWeek() {
        val cur = _selectedDate.value
        val gDate = PersianCalendarHelper.jalaliToGregorian(cur.year, cur.month, cur.day)
        val cal = Calendar.getInstance().apply {
            set(gDate.year, gDate.month - 1, gDate.day)
            add(Calendar.DAY_OF_MONTH, 7)
        }
        val nextJalali = PersianCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        setSelectedDate(nextJalali)
        _currentViewYear.value = nextJalali.year
        _currentViewMonth.value = nextJalali.month
    }

    fun previousWeek() {
        val cur = _selectedDate.value
        val gDate = PersianCalendarHelper.jalaliToGregorian(cur.year, cur.month, cur.day)
        val cal = Calendar.getInstance().apply {
            set(gDate.year, gDate.month - 1, gDate.day)
            add(Calendar.DAY_OF_MONTH, -7)
        }
        val prevJalali = PersianCalendarHelper.gregorianToJalali(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        setSelectedDate(prevJalali)
        _currentViewYear.value = prevJalali.year
        _currentViewMonth.value = prevJalali.month
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

    private val _selectedAiProvider = MutableStateFlow(ApiKeyManager.getSelectedProvider(application))
    val selectedAiProvider: StateFlow<AiProvider> = _selectedAiProvider.asStateFlow()

    private val _userApiKey = MutableStateFlow(ApiKeyManager.getApiKey(application, _selectedAiProvider.value))
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _isApiKeyConfigured = MutableStateFlow(ApiKeyManager.isKeyConfigured(application, _selectedAiProvider.value))
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

    fun selectAiProvider(provider: AiProvider) {
        _selectedAiProvider.value = provider
        ApiKeyManager.saveSelectedProvider(getApplication(), provider)
        val key = ApiKeyManager.getApiKey(getApplication(), provider)
        _userApiKey.value = key
        _isApiKeyConfigured.value = ApiKeyManager.isKeyConfigured(getApplication(), provider)

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(
            ChatMessage(
                sender = ChatSender.GEMINI,
                text = "سرویس‌دهنده هوش مصنوعی به «${provider.displayName}» تغییر یافت.",
                modelBadge = provider.displayName.substringBefore(' ')
            )
        )
        _chatMessages.value = currentList
        logMessage("سرویس‌دهنده هوش مصنوعی به ${provider.displayName} تغییر یافت.")
    }

    fun updateApiKey(newKey: String, provider: AiProvider = _selectedAiProvider.value) {
        ApiKeyManager.saveApiKey(getApplication(), newKey, provider)
        if (provider == _selectedAiProvider.value) {
            _userApiKey.value = newKey.trim()
            _isApiKeyConfigured.value = ApiKeyManager.isKeyConfigured(getApplication(), provider)
        }
        logMessage("کلید API برای ${provider.displayName} با موفقیت به‌روزرسانی شد.")
    }

    fun updateAiConfiguration(
        provider: AiProvider,
        apiKey: String,
        customBaseUrl: String?,
        customModel: String?
    ) {
        _selectedAiProvider.value = provider
        ApiKeyManager.saveSelectedProvider(getApplication(), provider)
        ApiKeyManager.saveApiKey(getApplication(), apiKey, provider)
        if (!customBaseUrl.isNullOrBlank()) {
            ApiKeyManager.saveBaseUrl(getApplication(), customBaseUrl, provider)
        }
        if (!customModel.isNullOrBlank()) {
            ApiKeyManager.saveModel(getApplication(), customModel, provider)
        }

        _userApiKey.value = apiKey.trim()
        _isApiKeyConfigured.value = ApiKeyManager.isKeyConfigured(getApplication(), provider)

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(
            ChatMessage(
                sender = ChatSender.GEMINI,
                text = "تنظیمات هوش مصنوعی برای «${provider.displayName}» ذخیره و فعال شد.",
                modelBadge = provider.displayName.substringBefore(' ')
            )
        )
        _chatMessages.value = currentList
        logMessage("تنظیمات هوش مصنوعی ${provider.displayName} به‌روزرسانی شد.")
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isGeneratingResponse.value) return

        val userMsg = ChatMessage(sender = ChatSender.USER, text = trimmed)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        // 1. Try Offline NLP Engine First
        val nlpResult = com.example.chat.OfflineNLPProcessor.processCommand(trimmed)
        if (nlpResult is com.example.chat.OfflineNLPProcessor.NLPResult.AddNote) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertNote(nlpResult.note)
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = "یادداشت شما به صورت آفلاین ذخیره شد: ${nlpResult.note.title}",
                        modelBadge = "آفلاین"
                    )
                )
                _chatMessages.value = updatedList
            }
            return
        } else if (nlpResult is com.example.chat.OfflineNLPProcessor.NLPResult.AddTask) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertTask(nlpResult.task)
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = "وظیفه جدید شما به صورت آفلاین ثبت شد: ${nlpResult.task.title}",
                        modelBadge = "آفلاین"
                    )
                )
                _chatMessages.value = updatedList
            }
            return
        } else if (nlpResult is com.example.chat.OfflineNLPProcessor.NLPResult.AddEvent) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertEvent(nlpResult.event)
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = "رویداد جدید شما به صورت آفلاین برای تاریخ ${nlpResult.event.persianDate} ${if (nlpResult.event.startTime.isNotBlank()) "ساعت " + nlpResult.event.startTime else ""} ثبت شد: ${nlpResult.event.title}",
                        modelBadge = "آفلاین"
                    )
                )
                _chatMessages.value = updatedList
            }
            return
        }

        // 2. Fallback to Online Engine (Multi-Provider Support)
        val currentProvider = _selectedAiProvider.value
        val key = ApiKeyManager.getApiKey(getApplication(), currentProvider)
        if (currentProvider.isGoogleFormat && key.isBlank()) {
            currentList.add(
                ChatMessage(
                    sender = ChatSender.GEMINI,
                    text = "من متوجه این دستور نشدم. لطفاً برای استفاده از هوش مصنوعی آنلاین، کلید اختصاصی Google Gemini را در بخش تنظیمات هوش مصنوعی وارد نمایید.",
                    isError = true
                )
            )
            _chatMessages.value = currentList
            return
        }

        val currentRole = _selectedRole.value
        val effectiveModel = if (currentProvider.isGoogleFormat) {
            currentRole.modelName
        } else {
            ApiKeyManager.getModel(getApplication(), currentProvider)
        }
        val effectiveBaseUrl = ApiKeyManager.getBaseUrl(getApplication(), currentProvider)
        val activeBadge = "${currentProvider.displayName.substringBefore(' ')} • ${currentRole.badge}"

        viewModelScope.launch {
            _isGeneratingResponse.value = true

            // Build dynamic context
            val todayDate = PersianCalendarHelper.getCurrentJalaliDate().formattedPersian
            val tasksStr = activeTasks.value.take(10).joinToString("\n") { "- ${it.title} (انجام شده: ${it.isCompleted})" }
            val notesStr = activeNotes.value.take(10).joinToString("\n") { "- ${it.title}" }

            val dynamicContext = """
                
                اطلاعات سیستم کاربر:
                تاریخ امروز: $todayDate
                وظایف فعال فعلی:
                ${if (tasksStr.isNotBlank()) tasksStr else "هیچ وظیفه‌ای وجود ندارد"}
                
                یادداشت‌های کاربر:
                ${if (notesStr.isNotBlank()) notesStr else "هیچ یادداشتی وجود ندارد"}
                
                دستورالعمل‌های ویژه (بسیار مهم):
                اگر کاربر از شما خواست وظیفه (تسک) جدیدی اضافه کنید، باید دقیقاً و فقط این خروجی JSON را تولید کنید (هیچ متن اضافه‌ای ننویسید):
                {"action": "ADD_TASK", "title": "عنوان وظیفه"}
                
                اگر کاربر خواست یادداشت جدیدی اضافه کنید، دقیقاً این خروجی JSON را تولید کنید:
                {"action": "ADD_NOTE", "title": "عنوان", "content": "محتوای کامل یادداشت"}
            """.trimIndent()

            val fullSystemInstruction = currentRole.systemInstruction + "\n" + dynamicContext

            val result = GeminiChatService.sendMessage(
                provider = currentProvider,
                apiKey = key,
                history = currentList,
                userMessage = trimmed,
                baseUrl = effectiveBaseUrl,
                modelName = effectiveModel,
                systemInstruction = fullSystemInstruction
            )
            _isGeneratingResponse.value = false

            val updated = _chatMessages.value.toMutableList()
            result.onSuccess { reply ->
                try {
                    val cleanReply = reply.trim().removePrefix("```json").removeSuffix("```").trim()
                    if (cleanReply.startsWith("{") && cleanReply.contains("\"action\"")) {
                        val obj = org.json.JSONObject(cleanReply)
                        val action = obj.optString("action")
                        if (action == "ADD_TASK") {
                            val title = obj.optString("title", "بدون عنوان")
                            val task = TaskEntity(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                persianDueDate = PersianCalendarHelper.getCurrentJalaliDate().formatted,
                                priority = "متوسط",
                                category = "هوش مصنوعی"
                            )
                            repository.insertTask(task)
                            updated.add(ChatMessage(sender = ChatSender.GEMINI, text = "وظیفه جدید با موفقیت اضافه شد: $title", modelBadge = activeBadge))
                            _chatMessages.value = updated
                            return@launch
                        } else if (action == "ADD_NOTE") {
                            val title = obj.optString("title", "بدون عنوان")
                            val content = obj.optString("content", "")
                            val note = NoteEntity(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                content = content,
                                persianDate = PersianCalendarHelper.getCurrentJalaliDate().formattedPersian,
                                colorHex = "#FEF3C7"
                            )
                            repository.insertNote(note)
                            updated.add(ChatMessage(sender = ChatSender.GEMINI, text = "یادداشت جدید با موفقیت ثبت شد: $title", modelBadge = activeBadge))
                            _chatMessages.value = updated
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    // Not JSON, continue to normal text response
                }

                updated.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = reply,
                        modelBadge = activeBadge
                    )
                )
            }.onFailure { err ->
                updated.add(
                    ChatMessage(
                        sender = ChatSender.GEMINI,
                        text = err.message ?: "خطا در برقراری ارتباط با مدل هوش مصنوعی",
                        isError = true,
                        modelBadge = activeBadge
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

    init {
        checkAndSeedInitialData()
        refreshNetworkInfo()
        initServer()
        checkForAppUpdate(isManual = false)
        startPrayerCountdownTicker()
        refreshGoogleCalendars()
    }

    private fun startPrayerCountdownTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            var lastRecordedDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            while (isActive) {
                // هماهنگ‌سازی دقیق با شروع هر دقیقه ساعت سیستم
                val millisToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                delay(maxOf(1_000L, millisToNextMinute))
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if (currentDay != lastRecordedDay) {
                    lastRecordedDay = currentDay
                    _prayerTimes.value = calculateCurrentPrayerTimes()
                }
                _nextPrayerInfo.value = calculateNextPrayerInfo()
            }
        }
    }

    fun logMessage(msg: String, source: String = "سیستم") {
        val current = _activityLogList.value.toMutableList()
        val timestamp = PersianCalendarHelper.toPersianDigits(
            java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )
        current.add(0, "[$timestamp] $msg")
        if (current.size > 50) current.removeAt(current.size - 1)
        _activityLogList.value = current
        com.example.data.AppLogger.log(getApplication(), source, msg)
    }

    private fun initServer() {
        httpServer = LocalHttpSyncServer(
            repository = repository,
            port = _serverPort.value,
            onLog = { msg -> logMessage(msg) }
        )
        // سرور به صورت پیش‌فرض خاموش است و فقط با درخواست و روشن کردن سوییچ توسط کاربر فعال می‌شود
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

    fun toggleGoogleCalendar(enabled: Boolean) {
        _isGoogleCalendarEnabled.value = enabled
        if (enabled) {
            refreshGoogleCalendars()
            logMessage("همگام‌سازی تقویم گوگل فعال شد")
        } else {
            logMessage("همگام‌سازی تقویم گوگل غیرفعال شد")
        }
    }

    fun refreshGoogleCalendars() {
        val list = googleCalSyncManager.getAvailableCalendars()
        _googleCalendars.value = list
        googleCalSyncManager.ensureCalendarSelected()
        _selectedGoogleCalId.value = googleCalSyncManager.getSelectedCalendarId()
        _selectedGoogleCalName.value = googleCalSyncManager.getSelectedCalendarName()
    }

    fun selectGoogleCalendar(calId: Long, calName: String) {
        googleCalSyncManager.selectCalendar(calId, calName)
        _selectedGoogleCalId.value = calId
        _selectedGoogleCalName.value = calName
        logMessage("تقویم «$calName» برای همگام‌سازی انتخاب شد")
    }

    fun syncPushToGoogle() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val events = repository.allActiveEvents.first()
                val tasks = repository.allActiveTasks.first()
                val notes = repository.allActiveNotes.first()
                val result = googleCalSyncManager.syncAll(events, tasks, notes)
                logMessage(result.message)
                repository.recordLog("تقویم گوگل", result.message, result.eventsAdded + result.tasksAdded + result.notesAdded, result.errors == 0)
            } catch (e: Exception) {
                logMessage("خطا در همگام‌سازی: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncPullFromGoogle() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val imported = googleCalSyncManager.importEventsFromGoogle()
                if (imported.isEmpty()) {
                    val msg = "هیچ رویدادی در تقویم گوگل یافت نشد یا مجوز دسترسی به تقویم داده نشده است."
                    logMessage(msg)
                    repository.recordLog("تقویم گوگل", msg, 0, true)
                    return@launch
                }

                var added = 0
                val existing = repository.allActiveEvents.first().toMutableList()
                for (event in imported) {
                    val alreadyExists = existing.any {
                        it.title.trim().equals(event.title.trim(), ignoreCase = true) &&
                        (it.persianDate == event.persianDate || it.gregorianDate == event.gregorianDate)
                    }
                    if (!alreadyExists) {
                        repository.insertEvent(event)
                        existing.add(event)
                        added++
                    }
                }

                val msg = if (added > 0) {
                    "✓ $added رویداد جدید از تقویم گوگل دریافت و اضافه شد (${imported.size} رویداد بررسی شد)"
                } else {
                    "تمامی ${imported.size} رویداد تقویم گوگل از قبل در برنامه وجود دارند."
                }
                logMessage(msg)
                repository.recordLog("تقویم گوگل", msg, added, true)
            } catch (e: Exception) {
                logMessage("خطا در دریافت از تقویم گوگل: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncSingleNoteToGoogleCalendar(note: NoteEntity, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val result = googleCalSyncManager.syncNotesToGoogle(listOf(note))
                val success = result.notesAdded > 0
                val msg = if (success) {
                    "یادداشت «${note.title}» با تقویم گوگل همگام شد ✓"
                } else if (result.errors > 0) {
                    if (result.message.isNotBlank()) result.message else "خطا در همگام‌سازی یادداشت با تقویم گوگل"
                } else {
                    "این یادداشت قبلاً با تقویم گوگل همگام شده است."
                }
                logMessage(msg)
                repository.recordLog("تقویم گوگل", msg, result.notesAdded, success)
                onComplete(success, msg)
            } catch (e: Exception) {
                val err = "خطا در همگام‌سازی یادداشت با تقویم گوگل: ${e.message}"
                logMessage(err)
                onComplete(false, err)
            }
        }
    }

    fun syncAllNotesToGoogleCalendar(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (_isSyncing.value) {
            onComplete(false, "همگام‌سازی دیگری در حال انجام است...")
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val notes = repository.allActiveNotes.first()
                val result = googleCalSyncManager.syncNotesToGoogle(notes)
                logMessage(result.message)
                val success = result.notesAdded > 0 || (result.errors == 0 && result.message.contains("✓"))
                repository.recordLog("تقویم گوگل", result.message, result.notesAdded, success)
                onComplete(success, result.message)
            } catch (e: Exception) {
                val err = "خطا در همگام‌سازی یادداشت‌ها با تقویم گوگل: ${e.message}"
                logMessage(err)
                onComplete(false, err)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Event Actions
    fun addEvent(
        title: String,
        description: String,
        persianDate: String,
        time: String,
        category: String,
        colorHex: String,
        isWeeklyRecurring: Boolean = false
    ) {
        val normalizedDate = PersianCalendarHelper.normalizePersianDate(persianDate)
        val gDate = parsePersianToGregorian(normalizedDate)

        if (!isWeeklyRecurring) {
            val event = EventEntity(
                title = title,
                description = description,
                persianDate = normalizedDate,
                gregorianDate = gDate?.formatted ?: "",
                startTime = time,
                endTime = "",
                category = category,
                colorHex = colorHex,
                recurrenceId = null,
                recurrenceRule = "NONE"
            )
            viewModelScope.launch {
                repository.insertEvent(event)
                EventAlarmScheduler.scheduleEventAlarm(getApplication(), event)
                logMessage("رویداد جدید افزوده و زنگ یادآور تنظیم شد: $title")
            }
        } else {
            val recurrenceGroupId = java.util.UUID.randomUUID().toString()
            val eventsList = mutableListOf<EventEntity>()

            val parts = normalizedDate.split("/").mapNotNull { it.toIntOrNull() }
            if (parts.size == 3) {
                val jYear = parts[0]
                val jMonth = parts[1]
                val jDay = parts[2]
                val initialGregorian = PersianCalendarHelper.jalaliToGregorian(jYear, jMonth, jDay)

                for (week in 0 until 52) {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(initialGregorian.year, initialGregorian.month - 1, initialGregorian.day)
                        add(java.util.Calendar.DAY_OF_YEAR, week * 7)
                    }
                    val nextJalali = PersianCalendarHelper.gregorianToJalali(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    )
                    val gDateFormatted = "%04d-%02d-%02d".format(
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH) + 1,
                        cal.get(java.util.Calendar.DAY_OF_MONTH)
                    )

                    eventsList.add(
                        EventEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            persianDate = nextJalali.formatted,
                            gregorianDate = gDateFormatted,
                            startTime = time,
                            endTime = "",
                            category = category,
                            colorHex = colorHex,
                            recurrenceId = recurrenceGroupId,
                            recurrenceRule = "WEEKLY"
                        )
                    )
                }
            } else {
                eventsList.add(
                    EventEntity(
                        title = title,
                        description = description,
                        persianDate = normalizedDate,
                        gregorianDate = gDate?.formatted ?: "",
                        startTime = time,
                        endTime = "",
                        category = category,
                        colorHex = colorHex,
                        recurrenceId = recurrenceGroupId,
                        recurrenceRule = "WEEKLY"
                    )
                )
            }

            viewModelScope.launch {
                repository.insertEvents(eventsList)
                eventsList.firstOrNull()?.let {
                    EventAlarmScheduler.scheduleEventAlarm(getApplication(), it)
                }
                logMessage("رویداد تکرارشونده هفتگی برای ۵۲ هفته ثبت شد: $title")
            }
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
            EventAlarmScheduler.scheduleEventAlarm(getApplication(), event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            EventAlarmScheduler.cancelEventAlarm(getApplication(), id)
            repository.deleteEvent(id)
            logMessage("رویداد حذف شد")
        }
    }

    fun deleteRecurringEventsFromDate(recurrenceId: String, fromPersianDate: String) {
        viewModelScope.launch {
            repository.deleteRecurringEventsFromDate(recurrenceId, fromPersianDate)
            logMessage("رویدادهای تکرارشونده از تاریخ $fromPersianDate به بعد حذف شدند")
        }
    }

    // Task Actions
    fun addTask(title: String, dueDate: String, priority: String, category: String) {
        val normalizedDue = if (dueDate.isNotBlank()) PersianCalendarHelper.normalizePersianDate(dueDate) else ""
        val gDate = if (normalizedDue.isNotEmpty()) parsePersianToGregorian(normalizedDue) else null
        val task = TaskEntity(
            title = title,
            isCompleted = false,
            persianDueDate = normalizedDue,
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

    fun updateTask(task: TaskEntity) {
        val normalizedDue = if (task.persianDueDate.isNotBlank()) PersianCalendarHelper.normalizePersianDate(task.persianDueDate) else ""
        val gDate = if (normalizedDue.isNotEmpty()) parsePersianToGregorian(normalizedDue) else null
        val updated = task.copy(
            persianDueDate = normalizedDue,
            gregorianDueDate = gDate?.formatted ?: task.gregorianDueDate,
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateTask(updated)
            logMessage("وظیفه ویرایش شد: ${task.title}")
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // Note Actions
    fun addNote(title: String, content: String, colorHex: String, isPinned: Boolean = false, drawingPath: String? = null) {
        val note = NoteEntity(
            title = title.ifBlank { if (drawingPath != null) "طرح وایت‌برد" else "یادداشت جدید" },
            content = content,
            persianDate = PersianCalendarHelper.getCurrentJalaliDate().formattedPersian,
            colorHex = colorHex,
            isPinned = isPinned,
            drawingPath = drawingPath
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
            val existing = repository.allActiveNotes.first().find { it.id == id }
            existing?.drawingPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                    val jsonFile = File(path.replace(".png", ".json"))
                    if (jsonFile.exists()) jsonFile.delete()
                } catch (_: Exception) {}
            }
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
        val normalized = PersianCalendarHelper.normalizePersianDate(pDate)
        val parts = normalized.split("/").mapNotNull { it.trim().toIntOrNull() }
        if (parts.size == 3) {
            return PersianCalendarHelper.jalaliToGregorian(parts[0], parts[1], parts[2])
        }
        return null
    }

    private fun checkAndSeedInitialData() {
        viewModelScope.launch {
            val existingEvents = repository.allActiveEvents.first()
            if (existingEvents.isEmpty()) {
                // Try auto-restoring from safety backup first if available
                val safetyBackup = AppUpdateManager.getLatestSafetyBackup(getApplication())
                if (!safetyBackup.isNullOrBlank()) {
                    importBackupJson(safetyBackup) { success, msg ->
                        if (success) {
                            logMessage("اطلاعات شما با موفقیت از پشتیبان اضطراری بازگردانی شد ✓")
                        }
                    }
                }
            }
        }
    }

    fun checkForAppUpdate(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) _isCheckingUpdate.value = true
            try {
                val update = AppUpdateManager.checkForUpdates(getApplication(), isManual)
                if (update != null && update.isAvailable) {
                    _availableUpdate.value = update
                    // تنظیم یادآوری هوشمند ساعت ۱۸:۰۰ عصر در صورت عدم نصب
                    com.example.update.UpdateReminderReceiver.scheduleReminder(getApplication(), update)
                } else if (isManual) {
                    _updateStatusMessage.value = "برنامه شما به‌روز است (نسخه فعلی: ${com.example.BuildConfig.VERSION_NAME})"
                }
            } catch (e: Exception) {
                if (isManual) {
                    _updateStatusMessage.value = "خطا در بررسی نسخه جدید: ${e.message}"
                }
            } finally {
                if (isManual) _isCheckingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        val pending = _availableUpdate.value ?: AppUpdateManager.getPendingUpdateInfo(getApplication())
        if (pending != null && pending.isAvailable) {
            // در صورت رد کردن یا بستن دیالوگ توسط کاربر، یادآوری ساعت ۱۸ فعال می‌ماند
            com.example.update.UpdateReminderReceiver.scheduleReminder(getApplication(), pending)
        }
        _availableUpdate.value = null
        _updateDownloadState.value = UpdateDownloadState.Idle
    }

    fun clearUpdateStatusMessage() {
        _updateStatusMessage.value = null
    }

    fun startUpdateWithSafetyBackup(context: android.content.Context, updateInfo: UpdateInfo) {
        viewModelScope.launch {
            val existingApk = AppUpdateManager.getDownloadedApkIfReady(context, updateInfo.apkName)
            if (existingApk != null) {
                logMessage("فایل نصبی نسخه جدید قبلاً آماده شده است. اجرای نصاب...")
                installDownloadedUpdate(context, existingApk)
                return@launch
            }

            exportBackupJson { json ->
                val saved = AppUpdateManager.performSafetyBackup(context, json)
                if (saved) {
                    logMessage("پشتیبان اضطراری پیش از به‌روزرسانی ذخیره شد ✓")
                }
                viewModelScope.launch {
                    _updateDownloadState.value = UpdateDownloadState.Downloading(0f, 0L, 0L)
                    val result = AppUpdateManager.downloadApkWithProgress(
                        context = context,
                        downloadUrl = updateInfo.downloadUrl,
                        apkName = updateInfo.apkName,
                        onProgress = { progress, downloaded, total ->
                            _updateDownloadState.value = UpdateDownloadState.Downloading(progress, downloaded, total)
                        }
                    )

                    result.onSuccess { apkFile ->
                        logMessage("دانلود نسخه جدید با موفقیت تکمیل شد.")
                        installDownloadedUpdate(context, apkFile)
                    }.onFailure { error ->
                        val errText = error.message ?: "خطا در برقراری ارتباط با سرور دانلود"
                        logMessage("خطا در دانلود نسخه جدید: $errText")
                        _updateDownloadState.value = UpdateDownloadState.Error(errText)
                    }
                }
            }
        }
    }

    fun installDownloadedUpdate(context: android.content.Context, apkFile: File) {
        if (AppUpdateManager.canInstallPackages(context)) {
            val success = AppUpdateManager.installApk(context, apkFile)
            if (success) {
                // الزام ۴: بلافاصله تمام مودال‌ها و دیالوگ‌های اپلیکیشن dismiss می‌شوند تا زیر دیالوگ سیستمی نمانند
                _availableUpdate.value = null
                _updateDownloadState.value = UpdateDownloadState.Idle
                AppUpdateManager.clearPendingInstallApk(context)
                com.example.update.UpdateReminderReceiver.cancelReminder(context)
                logMessage("اینتنت نصب اندروید با موفقیت فراخوانی شد.")
            } else {
                _updateStatusMessage.value = "امکان باز کردن نصاب اندروید میسر نشد."
                _updateDownloadState.value = UpdateDownloadState.ReadyToInstall(apkFile)
            }
        } else {
            // الزام ۵: ذخیره فایل جهت نصب خودکار پس از دریافت مجوز در onResume
            AppUpdateManager.setPendingInstallApkPath(context, apkFile.absolutePath)
            _updateDownloadState.value = UpdateDownloadState.PermissionRequired(apkFile)
        }
    }

    fun requestInstallPermission(context: android.content.Context) {
        val curState = _updateDownloadState.value
        if (curState is UpdateDownloadState.PermissionRequired) {
            AppUpdateManager.setPendingInstallApkPath(context, curState.apkFile.absolutePath)
        }
        AppUpdateManager.openInstallPermissionSettings(context)
    }

    /**
     * الزام ۵: در بازگشت به برنامه (onResume)، داشتن مجوز خودکار بررسی شده و فایل APK دانلودشده درجا نصب می‌شود.
     */
    fun checkAndResumePendingInstall(context: android.content.Context) {
        val pendingApk = AppUpdateManager.getPendingInstallApk(context)
        if (pendingApk != null && pendingApk.exists() && pendingApk.length() > 500_000) {
            if (AppUpdateManager.canInstallPackages(context)) {
                logMessage("مجوز نصب ناشناخته فعال شد. اجرای خودکار فرآیند نصب...")
                installDownloadedUpdate(context, pendingApk)
            }
        }
    }

    /**
     * الزام ۲: مدیریت کلیک روی نوتیفیکیشن یا دکمه «مشاهده و نصب» یادآوری ۱۸:۰۰
     */
    fun handleUpdateNotificationClick(context: android.content.Context) {
        viewModelScope.launch {
            val pendingInfo = AppUpdateManager.getPendingUpdateInfo(context)
            if (pendingInfo != null && pendingInfo.isAvailable) {
                val existingApk = AppUpdateManager.getDownloadedApkIfReady(context, pendingInfo.apkName)
                if (existingApk != null && AppUpdateManager.canInstallPackages(context)) {
                    installDownloadedUpdate(context, existingApk)
                } else {
                    _availableUpdate.value = pendingInfo
                    if (existingApk != null) {
                        _updateDownloadState.value = if (AppUpdateManager.canInstallPackages(context)) {
                            UpdateDownloadState.ReadyToInstall(existingApk)
                        } else {
                            UpdateDownloadState.PermissionRequired(existingApk)
                        }
                    } else {
                        _updateDownloadState.value = UpdateDownloadState.Idle
                    }
                }
            } else {
                checkForAppUpdate(isManual = true)
            }
        }
    }

    fun cancelUpdateDownload() {
        _updateDownloadState.value = UpdateDownloadState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        httpServer?.stop()
    }
}
