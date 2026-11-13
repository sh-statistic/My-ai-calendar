package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calendar.PersianCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

// ---------- Checklist data ----------

data class ChecklistItem(val text: String, val checked: Boolean)

fun parseChecklistItems(json: String?): List<ChecklistItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        val items = mutableListOf<ChecklistItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            items.add(ChecklistItem(obj.optString("t", ""), obj.optBoolean("c", false)))
        }
        items
    } catch (_: Exception) {
        emptyList()
    }
}

fun checklistItemsToJson(items: List<ChecklistItem>): String {
    if (items.all { it.text.isBlank() }) return ""
    val arr = JSONArray()
    for (item in items) {
        if (item.text.isBlank()) continue
        val obj = JSONObject()
        obj.put("t", item.text)
        obj.put("c", item.checked)
        arr.put(obj)
    }
    return if (arr.length() == 0) "" else arr.toString()
}

// ---------- Checklist editor section (inside the note editor) ----------

@Composable
fun ChecklistEditorSection(
    items: SnapshotStateList<ChecklistItem>,
    isFa: Boolean,
    onChanged: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isFa) "چک‌لیست:" else "Checklist:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { checked ->
                        items[index] = item.copy(checked = checked)
                        onChanged()
                    }
                )
                BasicTextField(
                    value = item.text,
                    onValueChange = { newText ->
                        items[index] = item.copy(text = newText)
                        onChanged()
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    items.removeAt(index)
                    onChanged()
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        TextButton(onClick = {
            items.add(ChecklistItem("", false))
            onChanged()
        }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isFa) "افزودن مورد" else "Add item", fontSize = 12.sp)
        }
    }
}

// ---------- Checklist read-only preview (on the note card) ----------

@Composable
fun ChecklistPreview(json: String) {
    val items = remember(json) { parseChecklistItems(json) }
    if (items.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.take(4).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (item.checked) "☑" else "☐",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.text,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (items.size > 4) {
            Text(
                text = "…",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Voice note row (record / play / delete) ----------

@Composable
fun VoiceNoteRow(
    audioPath: String?,
    isFa: Boolean,
    onAudioChanged: (String?) -> Unit
) {
    val context = LocalContext.current
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordSeconds by remember { mutableStateOf(0) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    fun stopPlayer() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        player?.release()
        player = null
        isPlaying = false
    }

    fun startRecording() {
        try {
            val dir = File(context.filesDir, "notes_audio").apply { mkdirs() }
            val file = File(dir, "voice_" + UUID.randomUUID().toString() + ".m4a")
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()
            recorder = rec
            recordingFile = file
            recordSeconds = 0
            isRecording = true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                if (isFa) "شروع ضبط ممکن نشد" else "Could not start recording",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun stopRecording() {
        val rec = recorder
        val file = recordingFile
        isRecording = false
        recorder = null
        recordingFile = null
        if (rec != null) {
            try {
                rec.stop()
            } catch (_: Exception) {
                // Recording too short or already stopped: drop the file
                try {
                    file?.delete()
                } catch (_: Exception) {
                }
                rec.release()
                return
            }
            rec.release()
        }
        if (file != null && file.exists() && file.length() > 0) {
            onAudioChanged(file.absolutePath)
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            Toast.makeText(
                context,
                if (isFa) "برای ضبط صدا، اجازه میکروفون لازم است" else "Microphone permission is needed to record",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordSeconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
            } catch (_: Exception) {
            }
            recorder?.release()
            recorder = null
            try {
                player?.stop()
            } catch (_: Exception) {
            }
            player?.release()
            player = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isRecording -> {
                IconButton(onClick = { stopRecording() }) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = if (isFa) {
                        "در حال ضبط… ${recordSeconds / 60}:${(recordSeconds % 60).toString().padStart(2, '0')}"
                    } else {
                        "Recording… ${recordSeconds / 60}:${(recordSeconds % 60).toString().padStart(2, '0')}"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            !audioPath.isNullOrBlank() -> {
                IconButton(onClick = {
                    if (isPlaying) {
                        stopPlayer()
                    } else {
                        try {
                            stopPlayer()
                            val mp = MediaPlayer()
                            mp.setDataSource(audioPath)
                            mp.setOnCompletionListener {
                                isPlaying = false
                            }
                            mp.prepare()
                            mp.start()
                            player = mp
                            isPlaying = true
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                if (isFa) "پخش صدا ممکن نشد" else "Could not play the recording",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (isFa) "یادداشت صوتی" else "Voice note",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    stopPlayer()
                    try {
                        File(audioPath).delete()
                    } catch (_: Exception) {
                    }
                    onAudioChanged(null)
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            else -> {
                IconButton(onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        startRecording()
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (isFa) "ضبط صدا" else "Record voice",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ---------- Shared image copy helper ----------

suspend fun copyUriToNotesImages(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "notes_images").apply { mkdirs() }
            val file = File(dir, "img_" + UUID.randomUUID().toString() + ".jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            if (file.exists() && file.length() > 0) file.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

// ---------- Reminder helpers ----------

fun formatNoteReminderLabel(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val jalali = PersianCalendarHelper.gregorianToJalali(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val time = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    return "${jalali.formattedPersian} - ${PersianCalendarHelper.toPersianDigits(time)}"
}

fun showNoteReminderPicker(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply {
        if (currentMillis > 0) {
            timeInMillis = currentMillis
        } else {
            add(Calendar.HOUR_OF_DAY, 1)
        }
    }
    android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day, hour, minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onPicked(picked.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
