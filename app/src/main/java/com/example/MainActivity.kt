package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
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
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val isServerRunning by viewModel.isServerRunning.collectAsState()
                    val todayJalali = remember { PersianCalendarHelper.getCurrentJalaliDate() }

                    // BackHandler to navigate back to Calendar tab if on secondary tab or chat
                    BackHandler(enabled = selectedTab != 0) {
                        selectedTab = 0
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "همگام",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = todayJalali.formattedPersian,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                navigationIcon = {
                                    // Header AI Assistant button
                                    IconButton(
                                        onClick = { selectedTab = 5 }, // Open AI Chat Screen
                                        modifier = Modifier
                                            .padding(start = 4.dp)
                                            .testTag("top_bar_ai_chat_btn")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = "دستیار هوشمند AI",
                                            tint = if (selectedTab == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    }
                                },
                                actions = {
                                    // Server status badge in top bar
                                    Surface(
                                        color = if (isServerRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .testTag("server_status_badge")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isServerRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isServerRunning) "سرور فعال" else "آفلاین",
                                                fontSize = 10.sp,
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
                        },
                        bottomBar = {
                            // 5 Clean Bottom Navigation Tabs (AI assistant is in top bar)
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "تقویم", modifier = Modifier.size(22.dp)) },
                                    label = { Text("تقویم", fontSize = 11.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_calendar")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Checklist, contentDescription = "وظایف", modifier = Modifier.size(22.dp)) },
                                    label = { Text("وظایف", fontSize = 11.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_tasks")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 },
                                    icon = { Icon(Icons.Default.NoteAlt, contentDescription = "یادداشت‌ها", modifier = Modifier.size(22.dp)) },
                                    label = { Text("یادداشت", fontSize = 11.sp) },
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
                                            Icon(Icons.Default.Sync, contentDescription = "همگام‌سازی", modifier = Modifier.size(22.dp))
                                        }
                                    },
                                    label = { Text("همگام", fontSize = 11.sp) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_sync")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 4,
                                    onClick = { selectedTab = 4 },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات", modifier = Modifier.size(22.dp)) },
                                    label = { Text("تنظیمات", fontSize = 11.sp) },
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
