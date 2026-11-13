package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.settings.AppPreferences
import com.example.ui.MainViewModel
import com.example.ui.CalendarViewMode
import com.example.ui.LocalAppLanguage
import com.example.ui.rememberAppStrings
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.LifeGridScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        handleIntentForUpdate(intent)
        handleShareIntent(intent)

        // راه‌اندازی استعلام خودکار و دوره‌ای پس‌زمینه با WorkManager حتی در صورت بسته بودن برنامه
        com.example.update.UpdateWorkScheduler.schedulePeriodicCheck(this)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            LaunchedEffect(isDarkMode) {
                if (isDarkMode) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    )
                }
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                var isOnboardingCompleted by remember {
                    mutableStateOf(AppPreferences.isOnboardingCompleted(context))
                }
                val currentLanguage by viewModel.appLanguage.collectAsState()
                val isRtl = currentLanguage == "fa"
                val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalAppLanguage provides currentLanguage
                ) {
                    val strings = rememberAppStrings()

                    if (!isOnboardingCompleted) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onFinished = {
                                isOnboardingCompleted = true
                                viewModel.setAppLanguage(AppPreferences.getAppLanguage(context))
                                viewModel.checkMementoMonthAndYearStatus()
                            }
                        )
                    } else {
                        // Request notification permission on Android 13+ (TIRAMISU)
                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            onResult = { /* granted or denied handled gracefully */ }
                        )

                        // Request storage permission for safety backup on Android 6-9 (API 23-28)
                        var pendingUpdateForStorageBackup by remember { mutableStateOf<com.example.update.UpdateInfo?>(null) }
                        val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            onResult = { _ ->
                                pendingUpdateForStorageBackup?.let { updateInfo ->
                                    viewModel.proceedWithUpdateAndBackup(context, updateInfo)
                                }
                                pendingUpdateForStorageBackup = null
                            }
                        )

                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            com.example.calendar.PersianOccasionsHelper.loadOfficialDataForYear(applicationContext, 1405)
                            com.example.calendar.HijriOffsetManager.init(applicationContext)
                        }

                        var selectedTab by remember { mutableIntStateOf(0) }
                        val sharedNoteSeed by viewModel.sharedNoteSeed.collectAsState()
                        LaunchedEffect(sharedNoteSeed) {
                            if (sharedNoteSeed != null) {
                                selectedTab = 2
                            }
                        }
                        val todayJalali = remember { PersianCalendarHelper.getCurrentJalaliDate() }
                        val selectedDate by viewModel.selectedDate.collectAsState()
                        val viewYear by viewModel.currentViewYear.collectAsState()
                        val viewMonth by viewModel.currentViewMonth.collectAsState()
                        val calendarViewMode by viewModel.calendarViewMode.collectAsState()
                        val daysInMonth = remember(viewYear, viewMonth) { PersianCalendarHelper.getDaysInJalaliMonth(viewYear, viewMonth) }
                        val firstGDate = remember(viewYear, viewMonth) { PersianCalendarHelper.jalaliToGregorian(viewYear, viewMonth, 1) }
                        val lastGDate = remember(viewYear, viewMonth) { PersianCalendarHelper.jalaliToGregorian(viewYear, viewMonth, daysInMonth) }

                        val isTodaySelected = remember(selectedDate, todayJalali, viewYear, viewMonth) {
                            (selectedDate.formatted == todayJalali.formatted) &&
                            (viewYear == todayJalali.year) &&
                            (viewMonth == todayJalali.month)
                        }

                        val todayBtnContainerColor by animateColorAsState(
                            targetValue = if (isTodaySelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            },
                            label = "today_btn_container_color"
                        )

                        val todayBtnContentColor by animateColorAsState(
                            targetValue = if (isTodaySelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            },
                            label = "today_btn_content_color"
                        )

                        // BackHandler to navigate back to Calendar tab if on secondary tab or chat
                        BackHandler(enabled = selectedTab != 0) {
                            selectedTab = 0
                        }

                        Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (selectedTab == 0) {
                                // Space-Saving Top Bar for Calendar (Surface extends behind status bar, content safely padded)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Right side: AI Sparkle & Dark Mode Toggle Buttons
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { selectedTab = 5 },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("top_bar_ai_chat_btn")
                                                ) {
                                                    Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = strings.navAiChat,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { viewModel.toggleDarkMode() },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("top_bar_dark_mode_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                        contentDescription = if (isDarkMode) "تغییر به حالت روز" else "تغییر به حالت شب",
                                                        tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Center: Clean Month Title & Gregorian Subtitle (Swiped via touch gesture)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${strings.jalaliMonths[viewMonth - 1]} ${strings.formatDigits(viewYear.toString())}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${firstGDate.monthName} ${firstGDate.day} - ${lastGDate.monthName} ${lastGDate.day}, ${firstGDate.year}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Left side: View Mode Toggle & Quick jump to Today
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.toggleCalendarViewMode() },
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .testTag("top_bar_view_mode_toggle_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = if (calendarViewMode == CalendarViewMode.MONTH) Icons.Default.Menu else Icons.Default.CalendarMonth,
                                                        contentDescription = if (calendarViewMode == CalendarViewMode.MONTH) "نمای هفتگی" else "نمای ماهانه",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Surface(
                                                    color = todayBtnContainerColor,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .clickable { viewModel.setToday() }
                                                        .testTag("btn_quick_today")
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.CalendarToday,
                                                            contentDescription = strings.today,
                                                            tint = todayBtnContentColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = strings.today,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isTodaySelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = todayBtnContentColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Standard Compact Header for Secondary Screens (Tasks, Notes, Sync, Settings, Chat)
                                CenterAlignedTopAppBar(
                                    title = {
                                        if (selectedTab == 3) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = strings.lifeGridTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE08A00)
                                                )
                                                Text(
                                                    text = if (isRtl) "هر روز، یک فرصت دیگر" else "Each day, another opportunity",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = when (selectedTab) {
                                                    1 -> strings.tasksTitle
                                                    2 -> strings.notesTitle
                                                    4 -> strings.settingsTitle
                                                    5 -> strings.navAiChat
                                                    else -> strings.appName
                                                },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    navigationIcon = {
                                        if (selectedTab != 5) {
                                            IconButton(
                                                onClick = { selectedTab = 5 },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = strings.navAiChat,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    actions = {},
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            // 5 Clean Compact Bottom Navigation Tabs (56dp height + precise navigation bars padding)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bottom_nav_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .height(56.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = strings.navCalendar, modifier = Modifier.size(20.dp)) },
                                        label = { Text(strings.navCalendar, fontSize = 10.sp, maxLines = 1) },
                                        alwaysShowLabel = false,
                                        modifier = Modifier.testTag("nav_calendar")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = { Icon(Icons.Default.Checklist, contentDescription = strings.navTasks, modifier = Modifier.size(20.dp)) },
                                        label = { Text(strings.navTasks, fontSize = 10.sp, maxLines = 1) },
                                        alwaysShowLabel = false,
                                        modifier = Modifier.testTag("nav_tasks")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = { Icon(Icons.Default.NoteAlt, contentDescription = strings.navNotes, modifier = Modifier.size(20.dp)) },
                                        label = { Text(strings.navNotes, fontSize = 10.sp, maxLines = 1) },
                                        alwaysShowLabel = false,
                                        modifier = Modifier.testTag("nav_notes")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 3,
                                        onClick = { selectedTab = 3 },
                                        icon = {
                                            Icon(
                                                Icons.Default.HourglassBottom,
                                                contentDescription = strings.navLifeGrid,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (selectedTab == 3) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        label = { Text(strings.navLifeGrid, fontSize = 10.sp, maxLines = 1, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                                        alwaysShowLabel = false,
                                        modifier = Modifier.testTag("nav_memento_mori")
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 4,
                                        onClick = { selectedTab = 4 },
                                        icon = {
                                            Icon(Icons.Default.Settings, contentDescription = strings.navSettings, modifier = Modifier.size(20.dp))
                                        },
                                        label = { Text(strings.navSettings, fontSize = 10.sp, maxLines = 1) },
                                        alwaysShowLabel = false,
                                        modifier = Modifier.testTag("nav_settings")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Crossfade(
                            targetState = selectedTab,
                            modifier = Modifier.padding(innerPadding),
                            label = "tab_crossfade"
                        ) { tab ->
                            when (tab) {
                                0 -> CalendarScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                                1 -> TasksScreen(viewModel = viewModel)
                                2 -> NotesScreen(viewModel = viewModel)
                                3 -> LifeGridScreen(viewModel = viewModel)
                                4 -> SettingsScreen(viewModel = viewModel)
                                5 -> ChatScreen(viewModel = viewModel)
                            }
                        }
                    }

                    // In-App Update Dialog with Automatic Safety Backup
                    val availableUpdate by viewModel.availableUpdate.collectAsState()
                    val updateDownloadState by viewModel.updateDownloadState.collectAsState()
                    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()
                    val showExternalBackupPrompt by viewModel.showExternalBackupPrompt.collectAsState()
                    val pendingReflectionMonthIdx by viewModel.pendingReflectionMonthIdx.collectAsState()
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                    androidx.compose.runtime.LaunchedEffect(updateStatusMessage) {
                        if (selectedTab != 4) {
                            updateStatusMessage?.let { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                viewModel.clearUpdateStatusMessage()
                            }
                        }
                    }

                    val externalBackupFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            try {
                                val jsonText = context.contentResolver.openInputStream(uri)
                                    ?.bufferedReader()
                                    ?.use { it.readText() }
                                if (!jsonText.isNullOrBlank()) {
                                    viewModel.importBackupJson(jsonText) { ok, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (ok) {
                                            viewModel.dismissExternalBackupPrompt()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        if (isRtl) "فایل انتخاب‌شده خالی است" else "Selected file is empty",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    if (isRtl) "خطا در خواندن فایل: ${e.localizedMessage}" else "Error reading file: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    // Global Prompt when MyCalendar_SafetyBackup.json is detected in Downloads after Uninstall & Reinstall
                    if (showExternalBackupPrompt && availableUpdate == null) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissExternalBackupPrompt() },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isRtl) "📦 فایل پشتیبان شما در گوشی پیدا شد!" else "📦 Backup File Found on Phone!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            },
                            text = {
                                Text(
                                    text = if (isRtl) {
                                        "به نظر می‌رسد برنامه حذف و مجدداً نصب شده است.\n\nیک فایل پشتیبان خودکار (MyCalendar_SafetyBackup.json) در پوشه Downloads گوشی شما شناسایی شد؛ اما به دلیل محدودیت امنیتی اندروید پس از نصب مجدد، نیاز است فایل را یک بار انتخاب کنید تا تمام رویدادها، کارها و یادداشت‌هایتان بازیابی شوند."
                                    } else {
                                        "It looks like the app was reinstalled.\n\nAn automatic backup file (MyCalendar_SafetyBackup.json) was detected in your Downloads folder. Due to Android security restrictions after reinstallation, please select the file once from Downloads to restore all your events, tasks, and notes."
                                    },
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        externalBackupFileLauncher.launch(arrayOf("application/json", "*/*"))
                                    }
                                ) {
                                    Text(
                                        text = if (isRtl) "📂 انتخاب فایل از Downloads و بازیابی" else "📂 Select from Downloads & Restore",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissExternalBackupPrompt() }) {
                                    Text(strings.later)
                                }
                            }
                        )
                    }

                    // Global Monthly Reflection Prompt ("این ماه چطور گذشت؟") shown across the entire app
                    if (availableUpdate == null && pendingReflectionMonthIdx != null) {
                        val monthIdx = pendingReflectionMonthIdx!!
                        val monthYearLabel = remember(monthIdx, isRtl) {
                            viewModel.mementoManager.getMonthYearLabel(monthIdx, isRtl)
                        }
                        var reflectionText by remember(monthIdx) {
                            mutableStateOf(viewModel.mementoManager.getReflectionNote(monthIdx) ?: "")
                        }

                        AlertDialog(
                            onDismissRequest = { viewModel.dismissMonthlyReflectionPrompt() },
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = strings.howWasThisMonthTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        if (monthYearLabel.isNotBlank()) {
                                            Text(
                                                text = monthYearLabel,
                                                fontSize = 12.sp,
                                                color = Color(0xFFF59E0B),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = if (isRtl) {
                                            "یک ماه دیگر از عمر شما ($monthYearLabel) به پایان رسید و در نقطه‌های عمر ثبت شد! مهم‌ترین دستاورد، خاطره یا تجربه‌تان در این ماه چه بود؟\n(در پایان سال، تمام نوشته‌های ماهانه شما به صورت یک کارنامه سالانه در بخش یادداشت‌ها جمع‌آوری می‌شود)"
                                        } else {
                                            "Another month of your life ($monthYearLabel) has completed! Write your key achievement or memory for this month (at year-end, all monthly notes are compiled into your Notes):"
                                        },
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )
                                    OutlinedTextField(
                                        value = reflectionText,
                                        onValueChange = { reflectionText = it },
                                        placeholder = { Text(strings.notePlaceholder) },
                                        minLines = 3,
                                        maxLines = 6,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    enabled = reflectionText.isNotBlank(),
                                    onClick = {
                                        viewModel.saveMonthlyReflection(
                                            monthIdx = monthIdx,
                                            noteText = reflectionText.trim(),
                                            markPromptHandled = true
                                        )
                                        val toastMsg = if (isRtl) {
                                            "یادداشت $monthYearLabel در نقطه‌های عمر ثبت شد ✓"
                                        } else {
                                            "Reflection for $monthYearLabel saved ✓"
                                        }
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                                ) {
                                    Text(strings.saveNote)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissMonthlyReflectionPrompt() }) {
                                    Text(strings.later)
                                }
                            }
                        )
                    }

                    availableUpdate?.let { updateInfo ->
                        com.example.ui.components.UpdateDialog(
                            updateInfo = updateInfo,
                            downloadState = updateDownloadState,
                            onDismiss = { viewModel.dismissUpdateDialog() },
                            onConfirmUpdate = {
                                viewModel.startUpdateWithSafetyBackup(
                                    context = context,
                                    updateInfo = updateInfo,
                                    onPermissionNeeded = {
                                        pendingUpdateForStorageBackup = updateInfo
                                        storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                )
                            },
                            onInstallApk = { apkFile ->
                                viewModel.installDownloadedUpdate(context, apkFile)
                            },
                            onRequestPermission = {
                                viewModel.requestInstallPermission(context)
                            },
                            onOpenBrowser = {
                                viewModel.dismissUpdateDialog()
                                uriHandler.openUri(updateInfo.downloadUrl)
                            }
                        )
                    }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        // الزام ۵: بررسی خودکار داشتن مجوز نصب در onResume و اجرای درجا و خودکار نصب بدون نیاز به کلیک مجدد
        viewModel.checkAndResumePendingInstall(this)
        viewModel.checkMementoMonthAndYearStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentForUpdate(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val type = intent.type ?: return
        when {
            action == Intent.ACTION_SEND && type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                val sharedTitle = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
                viewModel.onSharedContentReceived(
                    MainViewModel.SharedNoteSeed(title = sharedTitle, content = sharedText)
                )
            }
            action == Intent.ACTION_SEND && type.startsWith("image/") -> {
                val uri = androidx.core.content.IntentCompat.getParcelableExtra(
                    intent, Intent.EXTRA_STREAM, Uri::class.java
                ) ?: return
                copySharedImageToAppStorage(uri, intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
            }
            action == Intent.ACTION_SEND_MULTIPLE && type.startsWith("image/") -> {
                val uris = androidx.core.content.IntentCompat.getParcelableArrayListExtra(
                    intent, Intent.EXTRA_STREAM, Uri::class.java
                )
                val first = uris?.firstOrNull() ?: return
                copySharedImageToAppStorage(first, intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
            }
        }
    }

    private fun copySharedImageToAppStorage(uri: Uri, sharedTitle: String) {
        lifecycleScope.launch {
            val savedPath = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val dir = java.io.File(filesDir, "notes_images").apply { mkdirs() }
                    val file = java.io.File(dir, "shared_" + java.util.UUID.randomUUID().toString() + ".jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (file.exists() && file.length() > 0) file.absolutePath else null
                } catch (_: Exception) {
                    null
                }
            }
            if (savedPath != null) {
                viewModel.onSharedContentReceived(
                    MainViewModel.SharedNoteSeed(title = sharedTitle, imagePath = savedPath)
                )
            }
        }
    }

    private fun handleIntentForUpdate(intent: Intent?) {
        if (intent?.getBooleanExtra(com.example.update.UpdateReminderReceiver.EXTRA_FROM_UPDATE_REMINDER, false) == true) {
            viewModel.handleUpdateNotificationClick(this)
        }
    }
}
