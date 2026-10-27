package com.example.ui.screens

import android.R
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import com.example.calendar.PersianCalendarHelper
import com.example.update.UpdateDownloadState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
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
    val updateDownloadState by viewModel.updateDownloadState.collectAsState()

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
                                .background(if (isServerRunning) Color(0xFF10B981) else Color(0xFF94A3B8))
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
                val selectedGCalId by viewModel.selectedGoogleCalId.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()

                val hasCalendarPerms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                     ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

                LaunchedEffect(hasCalendarPerms) {
                    if (hasCalendarPerms) {
                        viewModel.refreshGoogleCalendars()
                    }
                }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.selectCalendar, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                            IconButton(
                                onClick = { viewModel.refreshGoogleCalendars() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            googleCalendars.forEach { cal ->
                                val isSelected = selectedGCalId == cal.id || (selectedGCalId == -1L && selectedGCalName == cal.displayName)
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectGoogleCalendar(cal.id, cal.displayName) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = cal.displayName.ifBlank { cal.accountName },
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (cal.isGoogle) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Google",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (cal.accountName.isNotBlank() && cal.accountName != cal.displayName) {
                                                Text(
                                                    text = cal.accountName,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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
                    Spacer(modifier = Modifier.height(18.dp))

                    var previewOffsetMinutes by remember(fajrSettings.offsetMinutesBefore) {
                        mutableIntStateOf(fajrSettings.offsetMinutesBefore)
                    }

                    // تابع تبدیل آفست به موقعیت فرکشن اسلایدر (0.0f تا 1.0f)
                    // بازه متقارن: 0.0f = 15 دقیقه قبل، 0.5f = همزمان، 1.0f = 60 دقیقه بعد
                    fun offsetToFraction(offset: Int): Float {
                        return when {
                            offset > 0 -> {
                                val progress = (15 - offset).coerceIn(0, 15) / 15f
                                0.5f * progress
                            }
                            offset < 0 -> {
                                val progress = (-offset).coerceIn(0, 60) / 60f
                                0.5f + 0.5f * progress
                            }
                            else -> 0.5f
                        }.coerceIn(0f, 1f)
                    }

                    fun fractionToOffset(fraction: Float): Int {
                        val f = fraction.coerceIn(0f, 1f)
                        return if (f <= 0.5f) {
                            val progress = f / 0.5f
                            (15f * (1f - progress)).roundToInt().coerceIn(0, 15)
                        } else {
                            val progress = (f - 0.5f) / 0.5f
                            -((60f * progress).roundToInt().coerceIn(0, 60))
                        }
                    }

                    var sliderFraction by remember(fajrSettings.offsetMinutesBefore) {
                        mutableFloatStateOf(offsetToFraction(fajrSettings.offsetMinutesBefore))
                    }

                    // عنوان متنی بالای اسلایدر با نگارش تمیز فارسی بدون دو نقطه اضافی
                    val labelText = when {
                        previewOffsetMinutes > 0 -> {
                            val digits = if (strings.isFa) PersianCalendarHelper.toPersianDigits(previewOffsetMinutes.toString()) else previewOffsetMinutes.toString()
                            if (strings.isFa) "$digits دقیقه قبل از اذان" else "$digits min before Fajr"
                        }
                        previewOffsetMinutes < 0 -> {
                            val after = -previewOffsetMinutes
                            val digits = if (strings.isFa) PersianCalendarHelper.toPersianDigits(after.toString()) else after.toString()
                            if (strings.isFa) "$digits دقیقه بعد از اذان" else "$digits min after Fajr"
                        }
                        else -> {
                            if (strings.isFa) "همزمان با اذان" else "At Fajr"
                        }
                    }

                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    // کل بخش کنترل زمان (دکمه‌های منفی/مثبت، اسلایدر و دکمه‌های میانبر) در جهت استاندارد LTR
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        // رنگ‌های هماهنگ با تم کهربایی و گرم اپلیکیشن
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackInactiveColor = MaterialTheme.colorScheme.surfaceVariant
                        val trackActiveColor = primaryColor

                        // ردیف اسلایدر و دکمه‌های (-) و (+)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // دکمه (-) در سمت چپ: حرکت به سمت زودتر (قبل از اذان / چپ)
                            val canDecrease = previewOffsetMinutes < 15
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(
                                        width = 1.2.dp,
                                        color = if (canDecrease) primaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable(enabled = canDecrease) {
                                        val newOffset = (previewOffsetMinutes + 1).coerceAtMost(15)
                                        previewOffsetMinutes = newOffset
                                        sliderFraction = offsetToFraction(newOffset)
                                        viewModel.updateFajrAlarm(true, newOffset)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "کاهش زمان",
                                    tint = if (canDecrease) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            // اسلایدر سفارشی با پالت هماهنگ تم گرم اپلیکیشن
                            val density = LocalDensity.current
                            val thumbRadiusDp = 9.dp

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .padding(horizontal = 10.dp)
                                    .pointerInput(Unit) {
                                        val thumbRadiusPx = with(density) { thumbRadiusDp.toPx() }
                                        detectTapGestures { offset ->
                                            val availableWidth = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
                                            val touchX = (offset.x - thumbRadiusPx).coerceIn(0f, availableWidth)
                                            val fraction = touchX / availableWidth
                                            sliderFraction = fraction
                                            val newOffset = fractionToOffset(fraction)
                                            previewOffsetMinutes = newOffset
                                            viewModel.updateFajrAlarm(true, newOffset)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        val thumbRadiusPx = with(density) { thumbRadiusDp.toPx() }
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                val newOffset = fractionToOffset(sliderFraction)
                                                previewOffsetMinutes = newOffset
                                                viewModel.updateFajrAlarm(true, newOffset)
                                            },
                                            onDragCancel = {
                                                val newOffset = fractionToOffset(sliderFraction)
                                                previewOffsetMinutes = newOffset
                                                viewModel.updateFajrAlarm(true, newOffset)
                                            }
                                        ) { change, _ ->
                                            change.consume()
                                            val availableWidth = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
                                            val touchX = (change.position.x - thumbRadiusPx).coerceIn(0f, availableWidth)
                                            val fraction = touchX / availableWidth
                                            sliderFraction = fraction
                                            previewOffsetMinutes = fractionToOffset(fraction)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    val trackH = 6.dp.toPx()
                                    val trackY = (size.height - trackH) / 2f
                                    val radius = trackH / 2f
                                    val thumbRadiusPx = thumbRadiusDp.toPx()
                                    val availableWidth = size.width - thumbRadiusPx * 2
                                    val thumbCenterXPx = thumbRadiusPx + (availableWidth * sliderFraction.coerceIn(0f, 1f))

                                    // ترک غیرفعال (رنگ ملایم سرفیس گرم)
                                    drawRoundRect(
                                        color = trackInactiveColor,
                                        topLeft = Offset(0f, trackY),
                                        size = Size(size.width, trackH),
                                        cornerRadius = CornerRadius(radius, radius)
                                    )

                                    // ترک فعال (رنگ تم اصلی کهربایی)
                                    if (thumbCenterXPx > 0f) {
                                        drawRoundRect(
                                            color = trackActiveColor,
                                            topLeft = Offset(0f, trackY),
                                            size = Size(thumbCenterXPx.coerceAtMost(size.width), trackH),
                                            cornerRadius = CornerRadius(radius, radius)
                                        )
                                    }

                                    // دایره نشانگر کهربایی با مرکز سفید شیک
                                    drawCircle(
                                        color = primaryColor,
                                        radius = thumbRadiusPx,
                                        center = Offset(thumbCenterXPx, size.height / 2f)
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = thumbRadiusPx * 0.38f,
                                        center = Offset(thumbCenterXPx, size.height / 2f)
                                    )
                                }
                            }

                            // دکمه (+) در سمت راست: حرکت به سمت دیرتر (بعد از اذان / راست)
                            val canIncrease = previewOffsetMinutes > -60
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(
                                        width = 1.2.dp,
                                        color = if (canIncrease) primaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape)
                                    .clickable(enabled = canIncrease) {
                                        val newOffset = (previewOffsetMinutes - 1).coerceAtLeast(-60)
                                        previewOffsetMinutes = newOffset
                                        sliderFraction = offsetToFraction(newOffset)
                                        viewModel.updateFajrAlarm(true, newOffset)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "افزایش زمان",
                                    tint = if (canIncrease) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ۳ دکمه میانبر زمانی بدون عناوین زائد ابتدا/انتها
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // ۱. دکمه ۱۵ دقیقه قبل (سمت چپ)
                            val isStartSelected = previewOffsetMinutes == 15
                            Surface(
                                onClick = {
                                    previewOffsetMinutes = 15
                                    sliderFraction = 0f
                                    viewModel.updateFajrAlarm(true, 15)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isStartSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isStartSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides if (strings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                                        Text(
                                            text = if (strings.isFa) "۱۵ دقیقه قبل" else "15m before",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                textDirection = if (strings.isFa) TextDirection.Rtl else TextDirection.Ltr
                                            ),
                                            fontWeight = if (isStartSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isStartSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // ۲. دکمه همزمان با اذان (وسط)
                            val isSimultaneousSelected = previewOffsetMinutes == 0
                            Surface(
                                onClick = {
                                    previewOffsetMinutes = 0
                                    sliderFraction = 0.5f
                                    viewModel.updateFajrAlarm(true, 0)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSimultaneousSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSimultaneousSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides if (strings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                                        Text(
                                            text = if (strings.isFa) "همزمان با اذان" else "At Fajr",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                textDirection = if (strings.isFa) TextDirection.Rtl else TextDirection.Ltr
                                            ),
                                            fontWeight = if (isSimultaneousSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSimultaneousSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // ۳. دکمه ۱ ساعت بعد (سمت راست)
                            val isEndSelected = previewOffsetMinutes == -60
                            Surface(
                                onClick = {
                                    previewOffsetMinutes = -60
                                    sliderFraction = 1f
                                    viewModel.updateFajrAlarm(true, -60)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEndSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isEndSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides if (strings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                                        Text(
                                            text = if (strings.isFa) "۱ ساعت بعد" else "1h after",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                textDirection = if (strings.isFa) TextDirection.Rtl else TextDirection.Ltr
                                            ),
                                            fontWeight = if (isEndSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isEndSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
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

                if (dbLogs.isEmpty() && logs.isEmpty()) {
                    Text(
                        text = strings.noSyncLogsYet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (dbLogs.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dbLogs.take(25).forEach { log ->
                            Surface(
                                color = if (log.isSuccess) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (log.isSuccess) Color(0xFFE0F2FE) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = log.source,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.isSuccess) Color(0xFF0369A1) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.details,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formatLogTime(log.timestamp),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logs.take(10).forEach { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = log,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (strings.isFa) "ظاهر و زبان برنامه" else "Appearance & Language", fontWeight = FontWeight.Bold)
                }

                // Dark / Night Mode Toggle
                val isDarkMode by viewModel.isDarkMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFFFBBF24) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "حالت شب (تم تاریک)" else "Night / Dark Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (strings.isFa) "کاهش خستگی چشم و صرفه‌جویی در باتری" else "Reduces eye strain in low-light environments",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                        enabled = !isCheckingUpdate && updateDownloadState !is UpdateDownloadState.Downloading
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

                // نوار وضعیت دانلود یا دکمه نصب نسخه آماده‌شده
                if (updateDownloadState is UpdateDownloadState.Downloading) {
                    val progress = (updateDownloadState as UpdateDownloadState.Downloading).progress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("در حال دریافت نسخه جدید...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (progress >= 0f) {
                                Text(PersianCalendarHelper.toPersianDigits("${(progress * 100).toInt()}%"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (updateDownloadState is UpdateDownloadState.ReadyToInstall) {
                    val apk = (updateDownloadState as UpdateDownloadState.ReadyToInstall).apkFile
                    Button(
                        onClick = { viewModel.installDownloadedUpdate(context, apk) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نصب نسخه جدید آماده شده", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

private fun formatLogTime(timestamp: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    val jDate = PersianCalendarHelper.gregorianToJalali(
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH)
    )
    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = cal.get(java.util.Calendar.MINUTE)
    val second = cal.get(java.util.Calendar.SECOND)
    val timeFormatted = PersianCalendarHelper.toPersianDigits(
        String.format(java.util.Locale.US, "%02d:%02d:%02d", hour, minute, second)
    )
    return "${jDate.formattedPersian} $timeFormatted"
}

