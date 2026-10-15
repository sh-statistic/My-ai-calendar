package com.example

import android.os.Bundle
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
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

        setContent {
            MyApplicationTheme {
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
                            }
                        )
                    } else {
                        // Request notification permission on Android 13+ (TIRAMISU)
                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            onResult = { /* granted or denied handled gracefully */ }
                        )

                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                            com.example.calendar.PersianOccasionsHelper.loadOfficialDataForYear(applicationContext, 1405)
                            com.example.calendar.HijriOffsetManager.init(applicationContext)
                        }

                        var selectedTab by remember { mutableIntStateOf(0) }
                        val isServerRunning by viewModel.isServerRunning.collectAsState()
                        val todayJalali = remember { PersianCalendarHelper.getCurrentJalaliDate() }
                        val selectedDate by viewModel.selectedDate.collectAsState()
                        val viewYear by viewModel.currentViewYear.collectAsState()
                        val viewMonth by viewModel.currentViewMonth.collectAsState()
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
                                            // Right side: AI Sparkle Button
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

                                            // Center: Month Navigator with Arrow Buttons
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.nextMonth() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = strings.nextMonth,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                                                IconButton(
                                                    onClick = { viewModel.previousMonth() },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = strings.prevMonth,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Left side: Quick jump to Today
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
                                            BadgedBox(
                                                badge = {
                                                    if (isServerRunning) {
                                                        Badge(containerColor = Color(0xFF10B981))
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = strings.navSettings, modifier = Modifier.size(20.dp))
                                            }
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
                                0 -> CalendarScreen(viewModel = viewModel)
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
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                    availableUpdate?.let { updateInfo ->
                        com.example.ui.components.UpdateDialog(
                            updateInfo = updateInfo,
                            onDismiss = { viewModel.dismissUpdateDialog() },
                            onConfirmUpdate = {
                                viewModel.startUpdateWithSafetyBackup(context, updateInfo)
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
}
