package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.update.UpdateDownloadState
import com.example.update.UpdateInfo
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onDismiss: () -> Unit,
    onConfirmUpdate: () -> Unit,
    onInstallApk: (File) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (downloadState) {
                    is UpdateDownloadState.ReadyToInstall -> Icons.Default.CheckCircle
                    is UpdateDownloadState.PermissionRequired -> Icons.Default.Security
                    is UpdateDownloadState.Error -> Icons.Default.ErrorOutline
                    is UpdateDownloadState.Downloading -> Icons.Default.Download
                    else -> Icons.Default.SystemUpdate
                }
                val iconTint = when (downloadState) {
                    is UpdateDownloadState.ReadyToInstall -> Color(0xFF10B981)
                    is UpdateDownloadState.PermissionRequired -> Color(0xFFF59E0B)
                    is UpdateDownloadState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (downloadState) {
                        is UpdateDownloadState.Downloading -> "در حال دریافت نسخه جدید"
                        is UpdateDownloadState.ReadyToInstall -> "نسخه جدید آماده نصب است"
                        is UpdateDownloadState.PermissionRequired -> "مجوز نصب برنامه‌ها مورد نیاز است"
                        is UpdateDownloadState.Error -> "خطا در فرآیند به‌روزرسانی"
                        else -> "نسخه جدید برنامه موجود است"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        val progress = downloadState.progress
                        val progressPercent = if (progress >= 0f) {
                            PersianCalendarHelper.toPersianDigits("${(progress * 100).toInt()}%")
                        } else {
                            "در حال محاسبه..."
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "پیشرفت دانلود:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = progressPercent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (progress >= 0f) {
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }

                                if (downloadState.totalBytes > 0) {
                                    val downloadedStr = formatFileSize(downloadState.downloadedBytes)
                                    val totalStr = formatFileSize(downloadState.totalBytes)
                                    Text(
                                        text = "$downloadedStr از $totalStr",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "در حال دانلود فایل نسخه ${PersianCalendarHelper.toPersianDigits(updateInfo.latestVersion)}... لطفاً برنامه را نبندید.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.ReadyToInstall -> {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "فایل با موفقیت دریافت شد",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF047857)
                                    )
                                }
                                Text(
                                    text = "نصاب اندروید باز شد. در صورتی که پنجره نصب ظاهر نشد، دکمه «نصب اکنون» زیر را فشار دهید.",
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.PermissionRequired -> {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "جهت نصب مستقیم نسخه جدید، لطفاً اجازه «نصب برنامه‌های ناشناخته» را در تنظیمات فعال کنید.",
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "پس از روشن کردن کلید دسترسی، دکمه بازگشت را بزنید و سپس روی «نصب نسخه جدید» کلیک کنید.",
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    is UpdateDownloadState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "پیام خطا: ${downloadState.message}",
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "می‌توانید مجدداً تلاش کنید یا فایل را مستقیماً از طریق مرورگر از گیت‌هاب دانلود نمایید.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    else -> {
                        // Version Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "نسخه فعلی: ${PersianCalendarHelper.toPersianDigits(updateInfo.currentVersion)}",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "نسخه جدید: ${PersianCalendarHelper.toPersianDigits(updateInfo.latestVersion)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Release notes
                        if (updateInfo.releaseNotes.isNotBlank()) {
                            Text(
                                text = "تغییرات نسخه جدید:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                            ) {
                                Text(
                                    text = updateInfo.releaseNotes,
                                    fontSize = 11.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }

                        // Safety Guarantee Alert
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "محض احتیاط، پیش از نصب یک نسخه پشتیبان خودکار از کلیه اطلاعات و رویدادهای شما تهیه می‌شود.",
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is UpdateDownloadState.Downloading -> {
                    // هیچ دکمه تاییدی در حین دانلود نیاز نیست
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = { onInstallApk(downloadState.apkFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نصب اکنون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateDownloadState.PermissionRequired -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("فعال‌سازی مجوز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onInstallApk(downloadState.apkFile) }
                        ) {
                            Text("نصب مجدد", fontSize = 11.sp)
                        }
                    }
                }
                is UpdateDownloadState.Error -> {
                    Button(
                        onClick = onConfirmUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تلاش دوباره", fontSize = 11.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onConfirmUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("به‌روزرسانی خودکار", fontSize = 11.sp)
                    }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (downloadState !is UpdateDownloadState.Downloading) {
                    TextButton(onClick = onOpenBrowser) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دانلود از گیت‌هاب", fontSize = 11.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        if (downloadState is UpdateDownloadState.Downloading) "انصراف" else "بعداً",
                        fontSize = 11.sp
                    )
                }
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return PersianCalendarHelper.toPersianDigits("0.0 مگابایت")
    val mb = bytes.toDouble() / (1024 * 1024)
    val formatted = String.format(java.util.Locale.US, "%.1f", mb)
    return PersianCalendarHelper.toPersianDigits("$formatted مگابایت")
}
