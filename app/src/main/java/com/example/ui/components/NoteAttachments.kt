package com.example.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.calendar.PersianCalendarHelper
import com.example.data.NoteBlockData
import com.example.data.noteBlocksDataToJson
import com.example.data.parseNoteBlocksData
import kotlinx.coroutines.launch
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

// ---------- Inline content blocks: text / photo / drawing in one flow ----------
// مدل سندمحور یادداشت: متن و عکس و نقاشی مثل ورد و مقالهٔ تلگرام وسط همان جریان متن می‌نشینند.

class NoteBlockState(
    val id: String = UUID.randomUUID().toString(),
    val kind: String, // "t" text, "i" image, "d" drawing, "c" checklist
    text: String = "",
    path: String? = null,
    checklistJson: String = ""
) {
    var tfv by mutableStateOf(TextFieldValue(text))
    var path by mutableStateOf(path)
    val checklistItems: SnapshotStateList<ChecklistItem> =
        mutableStateListOf<ChecklistItem>().apply { addAll(parseChecklistItems(checklistJson)) }
}

fun buildInitialNoteBlocks(
    blocksJson: String?,
    content: String,
    imagePath: String?,
    drawingPath: String?,
    checklistJson: String = "",
    audioPath: String? = null
): MutableList<NoteBlockState> {
    val parsed = parseNoteBlocksData(blocksJson)
    val blocks = mutableListOf<NoteBlockState>()
    if (parsed.isNotEmpty()) {
        for (b in parsed) {
            blocks.add(
                NoteBlockState(
                    kind = b.kind, text = b.text, path = b.path,
                    checklistJson = if (b.kind == "c") b.text else ""
                )
            )
        }
    } else {
        // یادداشت قدیمی بدون بلوک: متن، بعد پیوست‌های قدیمی به همان ترتیب نمایشی قبلی
        blocks.add(NoteBlockState(kind = "t", text = content))
        if (!drawingPath.isNullOrBlank()) blocks.add(NoteBlockState(kind = "d", path = drawingPath))
        if (!imagePath.isNullOrBlank()) blocks.add(NoteBlockState(kind = "i", path = imagePath))
        if (!audioPath.isNullOrBlank()) blocks.add(NoteBlockState(kind = "a", path = audioPath))
    }
    // صدای قدیمی (ستون جدا) اگر هنوز بلوک صدا ندارد، یک بلوک در انتها می‌شود
    if (!audioPath.isNullOrBlank() && blocks.none { it.kind == "a" && it.path == audioPath }) {
        blocks.add(NoteBlockState(kind = "a", path = audioPath))
    }
    // چک‌لیست قدیمی (ستون جدا) اگر هنوز بلوک چک‌لیست ندارد، یک بلوک در انتها می‌شود
    if (checklistJson.isNotBlank() && blocks.none { it.kind == "c" }) {
        blocks.add(NoteBlockState(kind = "c", checklistJson = checklistJson))
    }
    if (blocks.none { it.kind == "t" }) {
        blocks.add(0, NoteBlockState(kind = "t", text = content))
    }
    if (blocks.lastOrNull()?.kind != "t") {
        blocks.add(NoteBlockState(kind = "t"))
    }
    return blocks
}

// نسخهٔ ذخیره‌شدنی بلوک‌ها: بلوک رسانه‌ای بی‌فایل، چک‌لیست خالی و متن‌های خالی دو سر حذف می‌شوند؛
// اگر فقط متن ساده است، رشتهٔ خالی برمی‌گردد تا یادداشت متنی ساده بماند.
fun noteBlocksToJson(blocks: List<NoteBlockState>): String {
    val data = blocks.mapNotNull { block ->
        when (block.kind) {
            "t" -> NoteBlockData("t", text = block.tfv.text)
            "c" -> {
                val json = checklistItemsToJson(block.checklistItems.toList())
                if (json.isBlank()) null else NoteBlockData("c", text = json)
            }
            else -> if (block.path.isNullOrBlank()) null else NoteBlockData(block.kind, path = block.path)
        }
    }.toMutableList()
    while (data.isNotEmpty() && data.first().kind == "t" && data.first().text.isBlank()) data.removeAt(0)
    while (data.isNotEmpty() && data.last().kind == "t" && data.last().text.isBlank()) data.removeAt(data.lastIndex)
    if (data.isEmpty()) return ""
    if (data.all { it.kind == "t" }) return ""
    return noteBlocksDataToJson(data)
}

fun noteBlocksToContent(blocks: List<NoteBlockState>): String =
    blocks.filter { it.kind == "t" }.joinToString("\n\n") { it.tfv.text }.trim()

fun noteBlocksFirstPath(blocks: List<NoteBlockState>, kind: String): String? =
    blocks.firstOrNull { it.kind == kind && !it.path.isNullOrBlank() }?.path

fun noteBlocksSignature(blocks: List<NoteBlockState>): String =
    blocks.joinToString("|") { block ->
        block.kind + ":" + when (block.kind) {
            "t" -> block.tfv.text
            "c" -> checklistItemsToJson(block.checklistItems.toList())
            else -> block.path ?: ""
        }
    }

fun noteBlocksHaveContent(blocks: List<NoteBlockState>): Boolean =
    blocks.any {
        (it.kind == "t" && it.tfv.text.isNotBlank()) ||
            (it.kind == "c" && it.checklistItems.any { item -> item.text.isNotBlank() }) ||
            ((it.kind == "i" || it.kind == "d" || it.kind == "a") && !it.path.isNullOrBlank())
    }

// چک‌لیست یکپارچهٔ همهٔ بلوک‌های چک‌لیست، برای ستون قدیمی checklistJson (کارت یادداشت و بکاپ)
fun noteBlocksChecklistJson(blocks: List<NoteBlockState>): String {
    val all = blocks.filter { it.kind == "c" }.flatMap { it.checklistItems.toList() }
    return checklistItemsToJson(all)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteTextBlock(
    block: NoteBlockState,
    isFa: Boolean,
    showPlaceholder: Boolean,
    onChanged: () -> Unit,
    onFocused: (String) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(block.tfv.selection, focused) {
        if (focused) {
            val cursor = layoutResult?.getCursorRect(block.tfv.selection.start)
            if (cursor != null) {
                bringIntoViewRequester.bringIntoView(cursor)
            }
        }
    }

    BasicTextField(
        value = block.tfv,
        onValueChange = { newValue ->
            block.tfv = newValue
            onChanged()
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) {
                    onFocused(block.id)
                    scope.launch {
                        val cursor = layoutResult?.getCursorRect(block.tfv.selection.start)
                        if (cursor != null) bringIntoViewRequester.bringIntoView(cursor)
                    }
                }
            },
        decorationBox = { innerTextField ->
            if (showPlaceholder && block.tfv.text.isEmpty()) {
                Text(
                    text = if (isFa) "متن یادداشت (برای برچسب # بنویسید)" else "Note content (use # for label)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun NoteMediaBlock(
    block: NoteBlockState,
    isFa: Boolean,
    isDrawing: Boolean,
    onOpenDrawing: (NoteBlockState) -> Unit,
    onRemove: (NoteBlockState) -> Unit
) {
    val bitmap = remember(block.path) {
        val p = block.path
        if (p.isNullOrBlank()) null else try {
            val f = File(p)
            if (f.exists()) BitmapFactory.decodeFile(p)?.asImageBitmap() else null
        } catch (_: Exception) {
            null
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (bitmap != null) {
            val ratio = (bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1)).coerceIn(0.55f, 2.4f)
            Image(
                bitmap = bitmap,
                contentDescription = if (isDrawing) {
                    if (isFa) "طرح نقاشی" else "Drawing"
                } else {
                    if (isFa) "عکس" else "Photo"
                },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .heightIn(max = 430.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (isDrawing) {
                            Modifier
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { onOpenDrawing(block) }
                        } else {
                            Modifier
                        }
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .clickable(enabled = isDrawing) { onOpenDrawing(block) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "برای کشیدن طرح لمس کنید" else "Tap to draw",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        IconButton(
            onClick = { onRemove(block) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = if (isFa) "حذف" else "Remove",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        if (isDrawing && bitmap != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isFa) "طرح — برای ویرایش لمس کنید" else "Drawing — tap to edit",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NoteBlocksEditor(
    blocks: SnapshotStateList<NoteBlockState>,
    isFa: Boolean,
    onChanged: () -> Unit,
    onFocusedTextBlock: (String) -> Unit,
    onOpenDrawing: (NoteBlockState) -> Unit,
    modifier: Modifier = Modifier
) {
    val textBlockCount = blocks.count { it.kind == "t" }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            key(block.id) {
                when (block.kind) {
                    "t" -> NoteTextBlock(
                        block = block,
                        isFa = isFa,
                        showPlaceholder = textBlockCount == 1,
                        onChanged = onChanged,
                        onFocused = onFocusedTextBlock
                    )
                    "c" -> Column(modifier = Modifier.fillMaxWidth()) {
                        ChecklistEditorSection(
                            items = block.checklistItems,
                            isFa = isFa,
                            onChanged = onChanged
                        )
                        TextButton(onClick = { blocks.remove(block); onChanged() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isFa) "حذف چک‌لیست" else "Remove checklist",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    "a" -> Column(modifier = Modifier.fillMaxWidth()) {
                        InlineVoiceBlock(
                            block = block,
                            isFa = isFa,
                            onChanged = onChanged
                        )
                        TextButton(onClick = { blocks.remove(block); onChanged() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isFa) "حذف صدا" else "Remove voice",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    "i" -> NoteMediaBlock(
                        block = block,
                        isFa = isFa,
                        isDrawing = false,
                        onOpenDrawing = onOpenDrawing,
                        onRemove = { blocks.remove(it); onChanged() }
                    )
                    "d" -> NoteMediaBlock(
                        block = block,
                        isFa = isFa,
                        isDrawing = true,
                        onOpenDrawing = onOpenDrawing,
                        onRemove = { blocks.remove(it); onChanged() }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
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

// بلوک صدای درون‌خطی: ضبط و پخش، دقیقاً همان‌جایی که بلوک در متن نشسته است
@Composable
fun InlineVoiceBlock(
    block: NoteBlockState,
    isFa: Boolean,
    onChanged: () -> Unit
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
        } catch (_: Exception) {
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
            block.path = file.absolutePath
            onChanged()
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
                recorder?.release()
            } catch (_: Exception) {
            }
            try {
                player?.release()
            } catch (_: Exception) {
            }
        }
    }

    val path = block.path
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (path.isNullOrBlank()) {
                if (isRecording) {
                    IconButton(onClick = { stopRecording() }) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = if (isFa) "در حال ضبط… $recordSeconds ثانیه" else "Recording… ${recordSeconds}s",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    IconButton(onClick = {
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            startRecording()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = if (isFa) "ضبط صدا" else "Record voice",
                        fontSize = 13.sp
                    )
                }
            } else {
                val durationLabel = remember(path) {
                    try {
                        val retriever = android.media.MediaMetadataRetriever()
                        retriever.setDataSource(path)
                        val ms = retriever.extractMetadata(
                            android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                        )?.toLongOrNull() ?: 0L
                        retriever.release()
                        val totalSec = (ms / 1000).toInt()
                        "%d:%02d".format(totalSec / 60, totalSec % 60)
                    } catch (_: Exception) {
                        ""
                    }
                }
                IconButton(onClick = {
                    if (isPlaying) {
                        stopPlayer()
                    } else {
                        stopPlayer()
                        try {
                            val mp = MediaPlayer()
                            mp.setDataSource(path)
                            mp.setOnCompletionListener {
                                isPlaying = false
                            }
                            mp.prepare()
                            mp.start()
                            player = mp
                            isPlaying = true
                        } catch (_: Exception) {
                            Toast.makeText(
                                context,
                                if (isFa) "پخش ممکن نشد" else "Could not play",
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
                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (isFa) "یادداشت صوتی" else "Voice note",
                    fontSize = 13.sp
                )
                if (durationLabel.isNotBlank()) {
                    Text(text = durationLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
