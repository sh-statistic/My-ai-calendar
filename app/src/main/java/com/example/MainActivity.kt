package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.ui.MainViewModel
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Ensure natural RTL layout for Persian language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    // Request notification permission on Android 13+ (TIRAMISU)
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                        onResult = { /* granted or denied handled gracefully */ }
                    )

                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    var selectedTab by remember { mutableIntStateOf(0) }
                    val isServerRunning by viewModel.isServerRunning.collectAsState()
                    val todayJalali = remember { PersianCalendarHelper.getCurrentJalaliDate() }
                    val viewYear by viewModel.currentViewYear.collectAsState()
                    val viewMonth by viewModel.currentViewMonth.collectAsState()
                    val daysInMonth = remember(viewYear, viewMonth) { PersianCalendarHelper.getDaysInJalaliMonth(viewYear, viewMonth) }
                    val firstGDate = remember(viewYear, viewMonth) { PersianCalendarHelper.jalaliToGregorian(viewYear, viewMonth, 1) }
                    val lastGDate = remember(viewYear, viewMonth) { PersianCalendarHelper.jalaliToGregorian(viewYear, viewMonth, daysInMonth) }

                    // BackHandler to navigate back to Calendar tab if on secondary tab or chat
                    BackHandler(enabled = selectedTab != 0) {
                        selectedTab = 0
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (selectedTab == 0) {
                                // COMBINED Space-Saving Top Bar for Calendar (Zero wasted vertical space, safe from status bar overlap)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .statusBarsPadding()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                                                    contentDescription = "دستیار هوشمند AI",
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
                                                        contentDescription = "ماه بعد",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "${PersianCalendarHelper.PERSIAN_MONTH_NAMES[viewMonth - 1]} ${PersianCalendarHelper.toPersianDigits(viewYear.toString())}",
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
                                                        contentDescription = "ماه قبل",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Left side: Quick jump to Today & Server status badge
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
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
                                                            contentDescription = "برو به امروز",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "امروز",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // Server Indicator
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isServerRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                                                        .testTag("server_status_badge")
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Standard Compact Header for Secondary Screens (Tasks, Notes, Sync, Settings, Chat)
                                CenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = when (selectedTab) {
                                                1 -> "وظایف و چک‌لیست"
                                                2 -> "یادداشت‌ها"
                                                3 -> "همگام‌سازی محلی و بلوتوث"
                                                4 -> "تنظیمات"
                                                5 -> "دستیار هوشمند"
                                                else -> "همگام"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    navigationIcon = {
                                        if (selectedTab != 5) {
                                            IconButton(
                                                onClick = { selectedTab = 5 },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = "دستیار هوشمند",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        Surface(
                                            color = if (isServerRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.padding(end = 10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isServerRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isServerRunning) "سرور فعال" else "آفلاین",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isServerRunning) Color(0xFF15803D) else Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            // 5 Clean Bottom Navigation Tabs
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "تقویم", modifier = Modifier.size(20.dp)) },
                                    label = { Text("تقویم", fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_calendar")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Checklist, contentDescription = "وظایف", modifier = Modifier.size(20.dp)) },
                                    label = { Text("وظایف", fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_tasks")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.NoteAlt, contentDescription = "یادداشت‌ها", modifier = Modifier.size(20.dp)) },
                                    label = { Text("یادداشت", fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_notes")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { selectedTab = 3 },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (isServerRunning) {
                                                    Badge(containerColor = Color(0xFF10B981))
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = "همگام‌سازی", modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    label = { Text("همگام", fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_sync")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 4,
                                    onClick = { selectedTab = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات", modifier = Modifier.size(20.dp)) },
                                    label = { Text("تنظیمات", fontSize = 10.sp, maxLines = 1) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_settings")
                                )
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
                                3 -> SyncScreen(viewModel = viewModel)
                                4 -> SettingsScreen(viewModel = viewModel)
                                5 -> ChatScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
