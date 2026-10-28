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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.ui.LocalAppLanguage
import com.example.ui.theme.MyApplicationTheme
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
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        }
    ) {
        UpdateDialogCard(
            updateInfo = updateInfo,
            downloadState = downloadState,
            onDismiss = onDismiss,
            onConfirmUpdate = onConfirmUpdate,
            onInstallApk = onInstallApk,
            onRequestPermission = onRequestPermission,
            onOpenBrowser = onOpenBrowser,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
fun UpdateDialogCard(
    updateInfo: UpdateInfo,
    downloadState: UpdateDownloadState = UpdateDownloadState.Idle,
    onDismiss: () -> Unit,
    onConfirmUpdate: () -> Unit,
    onInstallApk: (File) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.example.ui.rememberAppStrings()
    val layoutDirection = if (strings.isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Title & Icon
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
                            is UpdateDownloadState.Downloading -> strings.updateDialogTitleDownloading
                            is UpdateDownloadState.ReadyToInstall -> strings.updateDialogTitleReady
                            is UpdateDownloadState.PermissionRequired -> strings.updateDialogTitlePermission
                            is UpdateDownloadState.Error -> strings.updateDialogTitleError
                            else -> strings.updateDialogTitleAvailable
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Body Content
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
                                strings.formatDigits("${(progress * 100).toInt()}%")
                            } else {
                                strings.downloadProgressCalculating
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
                                            text = strings.downloadProgressLabel,
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
                                        val downloadedStr = formatFileSize(downloadState.downloadedBytes, strings)
                                        val totalStr = formatFileSize(downloadState.totalBytes, strings)
                                        Text(
                                            text = if (strings.isFa) "$downloadedStr از $totalStr" else "$downloadedStr / $totalStr",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = strings.downloadingVersionNote(updateInfo.latestVersion),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface
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
                                            text = strings.apkReadyTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                    Text(
                                        text = strings.apkReadyMessage,
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
                                        text = strings.permissionRequiredMessage,
                                        fontSize = 11.sp,
                                        lineHeight = 18.sp,
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = strings.permissionRequiredHint,
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
                                        text = strings.updateErrorMessage(downloadState.message),
                                        fontSize = 11.sp,
                                        lineHeight = 17.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = strings.updateErrorHint,
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
                                        text = "${strings.currentVersionBadge} ${strings.formatDigits(updateInfo.currentVersion)}",
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
                                        text = "${strings.newVersionBadge} ${strings.formatDigits(updateInfo.latestVersion)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Release notes
                            if (updateInfo.releaseNotes.isNotBlank()) {
                                val localizedNotes = extractLocalizedNotes(updateInfo.releaseNotes, strings.isFa)
                                Text(
                                    text = strings.whatsNewInUpdate,
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
                                        text = localizedNotes,
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
                                        text = strings.safetyBackupNotice,
                                        fontSize = 10.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFF047857),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Google Play Protect Notice
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.playProtectNotice,
                                        fontSize = 10.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Section: Prominent, Modern & Elegant
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Primary Action Button
                    when (downloadState) {
                        is UpdateDownloadState.Downloading -> {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(strings.cancelAction, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        is UpdateDownloadState.ReadyToInstall -> {
                            Button(
                                onClick = { onInstallApk(downloadState.apkFile) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.installNow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        is UpdateDownloadState.PermissionRequired -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.enablePermission, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onInstallApk(downloadState.apkFile) },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text(strings.reinstall, fontSize = 12.sp)
                                }
                            }
                        }
                        is UpdateDownloadState.Error -> {
                            Button(
                                onClick = onConfirmUpdate,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.tryAgain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onConfirmUpdate,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.autoUpdate, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. Secondary Sub-Actions (Later & GitHub Browser Link)
                    if (downloadState !is UpdateDownloadState.Downloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = strings.updateLater,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onOpenBrowser) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.downloadFromGithub,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long, strings: com.example.ui.AppStrings): String {
    if (bytes <= 0) return strings.fileSizeMegaBytes("0.0")
    val mb = bytes.toDouble() / (1024 * 1024)
    val formatted = String.format(java.util.Locale.US, "%.1f", mb)
    return strings.fileSizeMegaBytes(formatted)
}

/**
 * Parses release notes and automatically filters content based on user's active language.
 * Supports tags like [fa]...[/fa] and [en]...[/en], or headers like "فارسی:" and "English:".
 * If no language partition is found, returns the original text.
 */
fun extractLocalizedNotes(rawNotes: String, isFa: Boolean): String {
    if (rawNotes.isBlank()) return rawNotes

    // 1. Bracket tags: [fa]...[/fa] and [en]...[/en] (or without closing tag until next)
    val faBracketRegex = Regex("(?i)\\[fa\\]([\\s\\S]*?)(?:\\[/fa\\]|(?=\\[en\\])|$)")
    val enBracketRegex = Regex("(?i)\\[en\\]([\\s\\S]*?)(?:\\[/en\\]|(?=\\[fa\\])|$)")

    val faBracketContent = faBracketRegex.find(rawNotes)?.groupValues?.get(1)?.trim()
    val enBracketContent = enBracketRegex.find(rawNotes)?.groupValues?.get(1)?.trim()

    if (!faBracketContent.isNullOrBlank() || !enBracketContent.isNullOrBlank()) {
        val selected = if (isFa) faBracketContent else enBracketContent
        if (!selected.isNullOrBlank()) return selected
    }

    // 2. HTML comment tags: <!-- fa --> ... <!-- /fa -->
    val faHtmlRegex = Regex("(?i)<!--\\s*fa\\s*-->([\\s\\S]*?)(?:<!--\\s*/fa\\s*-->|(?=<!--\\s*en\\s*-->)|$)")
    val enHtmlRegex = Regex("(?i)<!--\\s*en\\s*-->([\\s\\S]*?)(?:<!--\\s*/en\\s*-->|(?=<!--\\s*fa\\s*-->)|$)")

    val faHtmlContent = faHtmlRegex.find(rawNotes)?.groupValues?.get(1)?.trim()
    val enHtmlContent = enHtmlRegex.find(rawNotes)?.groupValues?.get(1)?.trim()

    if (!faHtmlContent.isNullOrBlank() || !enHtmlContent.isNullOrBlank()) {
        val selected = if (isFa) faHtmlContent else enHtmlContent
        if (!selected.isNullOrBlank()) return selected
    }

    // 3. Common readable headers: "فارسی:" vs "English:"
    val faHeaderRegex = Regex("(?im)^[#*\\s✨🌟]*(?:فارسی|persian)\\s*[:：]?\\s*$")
    val enHeaderRegex = Regex("(?im)^[#*\\s✨🌟]*(?:english|انگلیسی)\\s*[:：]?\\s*$")

    val faMatch = faHeaderRegex.find(rawNotes)
    val enMatch = enHeaderRegex.find(rawNotes)

    if (faMatch != null && enMatch != null) {
        val faIndex = faMatch.range.first
        val enIndex = enMatch.range.first

        val (firstLang, secondLang, firstStart, secondStart) = if (faIndex < enIndex) {
            listOf("fa", "en", faIndex, enIndex)
        } else {
            listOf("en", "fa", enIndex, faIndex)
        }

        val firstContent = rawNotes.substring(firstStart as Int, secondStart as Int)
            .replace(if (firstLang == "fa") faHeaderRegex else enHeaderRegex, "")
            .trim()
        val secondContent = rawNotes.substring(secondStart as Int)
            .replace(if (secondLang == "fa") faHeaderRegex else enHeaderRegex, "")
            .trim()

        val selected = if (isFa) {
            if (firstLang == "fa") firstContent else secondContent
        } else {
            if (firstLang == "en") firstContent else secondContent
        }

        if (selected.isNotBlank()) return selected
    }

    // Fallback: return full text if no language tags exist
    return rawNotes
}
