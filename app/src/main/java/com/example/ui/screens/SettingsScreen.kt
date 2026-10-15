package com.example.ui.screens

import android.R
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.PrayerTimesCalculator
import com.example.sync.NetworkUtils
import com.example.ui.MainViewModel
import com.example.ui.components.EditOccasionDialog
import com.example.ui.components.JsonBackupDialog
import com.example.ui.components.PairingInfoDialog
import com.example.ui.components.ShareAppDialog
import com.example.ui.rememberAppStrings

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = rememberAppStrings()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
        if (readGranted && writeGranted) {
            viewModel.toggleGoogleCalendar(true)
        } else {
            Toast.makeText(context, "مجوز دسترسی به تقویم داده نشد", Toast.LENGTH_SHORT).show()
            viewModel.toggleGoogleCalendar(false)
        }
    }

    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val logs by viewModel.activityLogList.collectAsState()
    val dbLogs by viewModel.syncLogs.collectAsState()

    val isGoogleCalendarEnabled by viewModel.isGoogleCalendarEnabled.collectAsState()

    val fajrSettings by viewModel.fajrAlarmSettings.collectAsState()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsState()
    val updateStatusMessage by viewModel.updateStatusMessage.collectAsState()

    LaunchedEffect(updateStatusMessage) {
        updateStatusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateStatusMessage()
        }
    }

    var showPairingDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showShareAppDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showFajrCityDialog by remember { mutableStateOf(false) }
    var occasionToEditInSettings by remember { mutableStateOf<com.example.calendar.PersianOccasion?>(null) }
    var customOccasionsVersion by remember { mutableStateOf(0) }

    val networkInfo = NetworkUtils.getNetworkInfo(context)
    val serverUrl = "http://$serverIp:$serverPort"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Unified Sync Settings Card
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.syncTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 1.1 Local Wi-Fi ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isServerRunning) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(strings.localWifiServer, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isServerRunning) "${strings.activeStatus} ($serverUrl)" else strings.inactiveStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isServerRunning) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(checked = isServerRunning, onCheckedChange = { if (it) viewModel.startServer() else viewModel.stopServer() })
                }

                if (isServerRunning) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(serverUrl))
                            Toast.makeText(context, strings.addressCopied, Toast.LENGTH_SHORT).show()
                        }) { Text(strings.copyAddress, fontSize = 11.sp) }
                        TextButton(onClick = { showPairingDialog = true }) { Text(strings.guide, fontSize = 11.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.height(12.dp))

                // --- 1.2 Google Calendar ---
                val googleCalendars by viewModel.googleCalendars.collectAsState()
                val selectedGCalName by viewModel.selectedGoogleCalName.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()

                val hasCalendarPerms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                     ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasCalendarPerms) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (hasCalendarPerms) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(strings.googleCalendarCloud, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (hasCalendarPerms) {
                                    if (selectedGCalName.isNotBlank()) "${strings.accessGranted} — $selectedGCalName" else "${strings.accessGranted} — ${strings.noCalendarSelected}"
                                } else strings.requiresCalendarPerm,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasCalendarPerms) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                    if (!hasCalendarPerms) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(strings.grantPermission, fontSize = 11.sp)
                        }
                    }
                }

                if (hasCalendarPerms) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar account picker
                    if (googleCalendars.isNotEmpty()) {
                        Text(strings.selectCalendar, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            googleCalendars.filter { it.isGoogle }.forEach { cal ->
                                val isSelected = selectedGCalName == cal.displayName
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectGoogleCalendar(cal.id, cal.displayName) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(cal.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            Text(cal.accountName, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        // Show non-Google calendars if no Google ones found
                        if (googleCalendars.none { it.isGoogle }) {
                            googleCalendars.forEach { cal ->
                                val isSelected = selectedGCalName == cal.displayName
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectGoogleCalendar(cal.id, cal.displayName) }
                                ) {
                                    Text(
                                        "${cal.displayName} (${cal.accountName})",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            strings.noCalendarFound,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sync action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncPushToGoogle() },
                            enabled = selectedGCalName.isNotBlank() && !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSyncing) strings.uploading else strings.uploadToGoogle, fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.syncPullFromGoogle() },
                            enabled = selectedGCalName.isNotBlank() && !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSyncing) strings.downloading else strings.downloadFromGoogle, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.googleSyncNote,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ==========================================
        // 2. Calendar Settings
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.calendarSettingsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.hijriDayAdjustment, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.hijriAdjustmentDesc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentOffset = com.example.calendar.HijriOffsetManager.currentOffset
                    val offsetOptions = listOf(-2, -1, 0, 1, 2)

                    offsetOptions.forEach { offset ->
                        FilterChip(
                            selected = currentOffset == offset,
                            onClick = { com.example.calendar.HijriOffsetManager.setOffset(offset) },
                            label = { Text(strings.daysOffsetLabel(offset), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 3. Offline JSON Backup Card
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Section Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.offlineBackupTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.offlineJsonFile, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = strings.saveFileOnPhone,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(onClick = { backupJsonContent = ""; showBackupDialog = true }) { Text(strings.importBackup, fontSize = 11.sp) }
                        TextButton(onClick = { viewModel.exportBackupJson { json -> backupJsonContent = json; showBackupDialog = true } }) { Text(strings.exportBackup, fontSize = 11.sp) }
                    }
                }
            }
        }

        // ==========================================
        // 3. Fajr Smart Alarm Settings Card
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = strings.smartFajrAlarm,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (fajrSettings.isEnabled) "${strings.activeStatus} (${fajrSettings.cityName})" else strings.inactiveStatus,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (fajrSettings.isEnabled) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = fajrSettings.isEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateFajrAlarm(enabled, fajrSettings.offsetMinutesBefore)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.calendarPrayerHorizonCity} ${fajrSettings.cityName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { showFajrCityDialog = true }) {
                        Text(strings.changeCity, fontSize = 11.sp)
                    }
                }

                if (fajrSettings.isEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(strings.alarmTimingBeforeFajr, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            0 to strings.simultaneous,
                            5 to if (strings.isFa) "۵ دقیقه قبل" else "5 min before",
                            10 to if (strings.isFa) "۱۰ دقیقه قبل" else "10 min before",
                            15 to if (strings.isFa) "۱۵ دقیقه قبل" else "15 min before"
                        ).forEach { (min, label) ->
                            FilterChip(
                                selected = fajrSettings.offsetMinutesBefore == min,
                                onClick = { viewModel.updateFajrAlarm(true, min) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Custom Occasions Management
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.userCustomOccasions, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val customOccasions = remember(customOccasionsVersion) { com.example.calendar.PersianOccasionsHelper.getCustomOccasions() }
                if (customOccasions.isEmpty()) {
                    Text(
                        strings.noCustomOccasionsYet,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                } else {
                    customOccasions.forEach { occ ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(occ.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                val monthName = strings.jalaliMonths.getOrElse(occ.customMonth - 1) { "" }
                                Text(
                                    "${strings.formatDigits(occ.customDay.toString())} $monthName" + if (occ.isHoliday) "  ${strings.holidayBadge}" else "",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        occasionToEditInSettings = occ
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = strings.edit,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        com.example.calendar.PersianOccasionsHelper.removeCustomOccasion(context, occ)
                                        viewModel.refreshOccasions()
                                        customOccasionsVersion++
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Occasion Dialog in Settings
        occasionToEditInSettings?.let { occ ->
            val occJalaliDate = com.example.calendar.JalaliDate(
                year = 1403,
                month = if (occ.customMonth != -1) occ.customMonth else 1,
                day = if (occ.customDay != -1) occ.customDay else 1
            )
            EditOccasionDialog(
                occasion = occ,
                currentDate = occJalaliDate,
                onDismiss = { occasionToEditInSettings = null },
                onConfirm = { newTitle, newMonth, newDay, newIsHoliday ->
                    com.example.calendar.PersianOccasionsHelper.updateCustomOccasion(
                        context, occ, newTitle, newMonth, newDay, newIsHoliday
                    )
                    viewModel.refreshOccasions()
                    customOccasionsVersion++
                    occasionToEditInSettings = null
                },
                onDelete = {
                    com.example.calendar.PersianOccasionsHelper.removeCustomOccasion(context, occ)
                    viewModel.refreshOccasions()
                    customOccasionsVersion++
                    occasionToEditInSettings = null
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 4. Live Activity & Sync Logs
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.appLogs, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { viewModel.clearSyncLogs() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = strings.clearLogs, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (logs.isEmpty() && dbLogs.isEmpty()) {
                    Text(
                        text = strings.noSyncLogsYet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        logs.take(5).forEach { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = log,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Language & Onboarding Setup Card
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.languageAndWizard, fontWeight = FontWeight.Bold)
                }

                val currentAppLang by viewModel.appLanguage.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.languageSettings + ":", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = currentAppLang == "fa",
                            onClick = {
                                viewModel.setAppLanguage("fa")
                                Toast.makeText(context, "زبان روی فارسی تنظیم شد", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("فارسی", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = currentAppLang == "en",
                            onClick = {
                                viewModel.setAppLanguage("en")
                                Toast.makeText(context, "Language set to English", Toast.LENGTH_SHORT).show()
                            },
                            label = { Text("English", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        com.example.settings.AppPreferences.setOnboardingCompleted(context, false)
                        Toast.makeText(context, if (strings.isFa) "راهنمای راه‌اندازی اولیه در ورود بعدی نمایش داده خواهد شد" else "Welcome wizard will appear on next launch", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.recheckOnboarding, fontSize = 11.sp)
                }
            }
        }

        // Technical Specs & Update Checker
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${strings.appVersion} ${strings.formatDigits(com.example.BuildConfig.VERSION_NAME)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    OutlinedButton(
                        onClick = { viewModel.checkForAppUpdate(isManual = true) },
                        enabled = !isCheckingUpdate
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.checkingForUpdate, fontSize = 10.sp)
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.checkNewVersion, fontSize = 10.sp)
                        }
                    }
                }

                // دکمه ارسال نرم‌افزار به دیگران
                Button(
                    onClick = { showShareAppDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.shareAppWithOthers,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val uriHandler = LocalUriHandler.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uriHandler.openUri("https://github.com/sh-statistic") }
                            .padding(4.dp)
                    ) {
                        Text("created by: sh-statistic", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_github),
                            contentDescription = "GitHub",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showPairingDialog) {
        PairingInfoDialog(
            ip = serverIp,
            port = serverPort,
            onDismiss = { showPairingDialog = false }
        )
    }

    if (showBackupDialog) {
        JsonBackupDialog(
            jsonString = backupJsonContent,
            onDismiss = { showBackupDialog = false },
            onImport = { json ->
                viewModel.importBackupJson(json) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                showBackupDialog = false
            }
        )
    }

    if (showFajrCityDialog) {
        val cities = PrayerTimesCalculator.CITIES
        AlertDialog(
            onDismissRequest = { showFajrCityDialog = false },
            title = { Text(strings.selectCityForPrayerTimes, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cities.forEach { city ->
                        val cityName = strings.cityName(city)
                        Surface(
                            color = if (fajrSettings.cityName == city.nameFa || fajrSettings.cityName == city.nameEn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCity(city)
                                    showFajrCityDialog = false
                                }
                        ) {
                            Text(
                                text = cityName,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = if (fajrSettings.cityName == city.nameFa || fajrSettings.cityName == city.nameEn) FontWeight.Bold else FontWeight.Normal,
                                color = if (fajrSettings.cityName == city.nameFa || fajrSettings.cityName == city.nameEn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFajrCityDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showShareAppDialog) {
        ShareAppDialog(
            onDismiss = { showShareAppDialog = false }
        )
    }
}
