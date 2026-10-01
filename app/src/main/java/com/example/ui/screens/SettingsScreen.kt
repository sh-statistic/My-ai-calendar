package com.example.ui.screens

import android.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
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
import com.example.ui.components.JsonBackupDialog
import com.example.ui.components.PairingInfoDialog

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val isBleAdvertising by viewModel.isBleAdvertising.collectAsState()
    val logs by viewModel.activityLogList.collectAsState()
    val dbLogs by viewModel.syncLogs.collectAsState()

    val isGoogleCalendarEnabled by viewModel.isGoogleCalendarEnabled.collectAsState()

    val fajrSettings by viewModel.fajrAlarmSettings.collectAsState()

    var showPairingDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonContent by remember { mutableStateOf("") }
    var showFajrCityDialog by remember { mutableStateOf(false) }

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
                        text = "همگام‌سازی",
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
                            Text("سرور محلی Wi-Fi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isServerRunning) "فعال ($serverUrl)" else "غیرفعال",
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
                            Toast.makeText(context, "آدرس کپی شد", Toast.LENGTH_SHORT).show()
                        }) { Text("کپی آدرس", fontSize = 11.sp) }
                        TextButton(onClick = { showPairingDialog = true }) { Text("راهنما", fontSize = 11.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.height(12.dp))

                // --- 1.2 Bluetooth BLE ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isBleAdvertising) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (isBleAdvertising) Color(0xFF3B82F6) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("بلوتوث (BLE)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isBleAdvertising) "آماده انتقال" else "خاموش",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isBleAdvertising) Color(0xFF2563EB) else Color.Gray
                            )
                        }
                    }
                    Switch(checked = isBleAdvertising, onCheckedChange = { viewModel.toggleBleAdvertising() })
                }

                if (isBleAdvertising) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.triggerBleSyncPush()
                                Toast.makeText(context, "بسته داده‌ها برای بلوتوث ارسال شد", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.weight(1f)
                        ) { Text("ارسال بلوتوث", fontSize = 10.sp) }

                        OutlinedButton(
                            onClick = {
                                viewModel.triggerBleSyncPull()
                                Toast.makeText(context, "داده‌های بلوتوث بازخوانی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("دریافت", fontSize = 10.sp) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(modifier = Modifier.height(12.dp))

                // --- 1.3 Google Calendar ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isGoogleCalendarEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isGoogleCalendarEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("تقویم گوگل (Cloud)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isGoogleCalendarEnabled) "فعال" else "غیرفعال",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGoogleCalendarEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                    Switch(checked = isGoogleCalendarEnabled, onCheckedChange = { viewModel.toggleGoogleCalendar(it) })
                }
                
                Text(
                    text = "هشدار: برای استفاده از این قابلیت، گوشی باید به اینترنت متصل بوده و حساب گوگل سینک شده باشد.",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 28.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ==========================================
        // 2. Offline JSON Backup Card
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
                        text = "پشتیبان‌گیری (آفلاین)",
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
                        Text("فایل آفلاین (JSON)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "ذخیره فایل در گوشی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(onClick = { backupJsonContent = ""; showBackupDialog = true }) { Text("ورود", fontSize = 11.sp) }
                        TextButton(onClick = { viewModel.exportBackupJson { json -> backupJsonContent = json; showBackupDialog = true } }) { Text("خروجی", fontSize = 11.sp) }
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
                                text = "بیدارباش هوشمند اذان صبح",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (fajrSettings.isEnabled) "فعال (افق ${fajrSettings.cityName})" else "غیرفعال",
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

                if (fajrSettings.isEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("شهر افق شرعی: ${fajrSettings.cityName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = { showFajrCityDialog = true }) {
                            Text("تغییر شهر", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("زمان زنگ هشدار قبل از اذان:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0 to "همزمان", 5 to "۵ دقیقه قبل", 10 to "۱۰ دقیقه قبل", 15 to "۱۵ دقیقه قبل").forEach { (min, label) ->
                            FilterChip(
                                selected = fajrSettings.offsetMinutesBefore == min,
                                onClick = { viewModel.updateFajrAlarm(true, min) },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Live Activity & Sync Logs
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
                        Text("گزارش‌های زنده همگام‌سازی", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { viewModel.clearSyncLogs() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "پاک کردن لاگ‌ها", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (logs.isEmpty() && dbLogs.isEmpty()) {
                    Text(
                        text = "هنوز رویداد همگام‌سازی ثبت نشده است.",
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

        // Technical Specs
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("نسخه برنامه: ۰.۳.۰", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    
                    val uriHandler = LocalUriHandler.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uriHandler.openUri("https://github.com/sh-statistic") }
                            .padding(4.dp)
                    ) {
                        Text("Design & Development: sh-statistic", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp)
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
            title = { Text("انتخاب شهر برای اوقات شرعی", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    cities.forEach { city ->
                        Surface(
                            color = if (fajrSettings.cityName == city.nameFa) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCity(city)
                                    showFajrCityDialog = false
                                }
                        ) {
                            Text(
                                text = city.nameFa,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = if (fajrSettings.cityName == city.nameFa) FontWeight.Bold else FontWeight.Normal,
                                color = if (fajrSettings.cityName == city.nameFa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFajrCityDialog = false }) { Text("انصراف") }
            }
        )
    }
}
