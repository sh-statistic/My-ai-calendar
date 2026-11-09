package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NoteEntity
import com.example.sync.NoteShareHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AddNoteDialog
import com.example.ui.components.AddToCalendarFromNoteDialog
import com.example.ui.components.EditNoteDialog
import com.example.ui.components.WhiteboardDrawingDialog
import com.example.ui.components.parseColor
import com.example.ui.rememberAppStrings
import java.io.File

@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.activeNotes.collectAsState()
    val strings = rememberAppStrings()
    var searchQuery by remember { mutableStateOf("") }
    var isFabExpanded by remember { mutableStateOf(false) }

    val fabRotation by animateFloatAsState(
        targetValue = if (isFabExpanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fab_rotation"
    )

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showWhiteboardDialog by remember { mutableStateOf(false) }
    var drawingNoteToViewOrEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToAddToCalendar by remember { mutableStateOf<NoteEntity?>(null) }

    // استخراج تمام تگ‌های یکتای موجود در یادداشت‌ها برای پیشنهاد داینامیک
    val allExistingTags = remember(notes) {
        notes.flatMap { it.tag.split(",").map { t -> t.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredNotes = if (searchQuery.isBlank()) {
        notes
    } else {
        // شناسایی جستجوی تگ: اگر با # شروع شود، فقط بر اساس تگ فیلتر شود
        val tagSearch = searchQuery.trim().let { q ->
            if (q.startsWith("#")) q.removePrefix("#").trim() else null
        }
        if (tagSearch != null) {
            // فیلتر بر اساس تگ — جستجو در فیلد tag
            notes.filter { note ->
                note.tag.split(",").any { t ->
                    t.trim().contains(tagSearch, ignoreCase = true)
                }
            }
        } else {
            // جستجوی عمومی: عنوان + محتوا + تگ‌ها
            notes.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true) ||
                it.tag.split(",").any { t ->
                    t.trim().contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(if (strings.isFa) "جستجو در یادداشت‌ها… (#برچسب)" else "Search notes… (#tag)")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = if (strings.isFa) "پاک کردن" else "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = com.example.ui.components.modernDialogTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Note,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) strings.noNotes else (if (strings.isFa) "یادداشتی با این عبارت یافت نشد." else "No notes found matching your query."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            searchQuery = searchQuery,
                            onClick = {
                                if (!note.drawingPath.isNullOrBlank()) {
                                    drawingNoteToViewOrEdit = note
                                } else {
                                    noteToEdit = note
                                }
                            },
                            onEdit = { noteToEdit = note },
                            onTagClick = { tag -> searchQuery = "#$tag" },
                            onAddToCalendar = { noteToAddToCalendar = note },
                            onSyncToGoogleCalendar = {
                                viewModel.syncSingleNoteToGoogleCalendar(note) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = { viewModel.deleteNote(note.id) }
                        )
                    }
                }
            }
        }

        // لایه تیره کم‌رنگ پس‌زمینه هنگام باز شدن منوی منبسط‌شونده
        if (isFabExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isFabExpanded = false }
            )
        }

        // منوی منبسط‌شونده دکمه شناور (Speed Dial FAB)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // گزینه‌های بازشونده بالای دکمه اصلی
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ۱. گزینه تخته وایت‌برد و نقاشی
                    Surface(
                        onClick = {
                            isFabExpanded = false
                            showWhiteboardDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("fab_whiteboard_note")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Draw,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (strings.isFa) "بوم نقاشی" else "Drawing Canvas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // ۲. گزینه یادداشت متنی
                    Surface(
                        onClick = {
                            isFabExpanded = false
                            showAddNoteDialog = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("fab_add_text_note")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Note,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (strings.isFa) "یادداشت متنی" else "Text note",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // دکمه اصلی شناور با انیمیشن چرخش به ضربدر
            FloatingActionButton(
                onClick = { isFabExpanded = !isFabExpanded },
                containerColor = if (isFabExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (isFabExpanded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier.testTag("fab_add_note")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isFabExpanded) strings.cancel else strings.addNote,
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer(rotationZ = fabRotation)
                )
            }
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, colorHex, tag ->
                viewModel.addNote(title, content, colorHex, tag = tag)
                showAddNoteDialog = false
            },
            existingTags = allExistingTags
        )
    }

    if (showWhiteboardDialog) {
        WhiteboardDrawingDialog(
            onDismiss = { showWhiteboardDialog = false },
            onSaveDrawing = { title, imagePath, colorHex ->
                viewModel.addNote(
                    title = title,
                    content = "",
                    colorHex = colorHex,
                    drawingPath = imagePath,
                    tag = ""
                )
                showWhiteboardDialog = false
                Toast.makeText(context, if (strings.isFa) "طرح وایت‌برد با موفقیت ذخیره شد ✓" else "Drawing saved ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    drawingNoteToViewOrEdit?.let { note ->
        WhiteboardDrawingDialog(
            initialTitle = note.title,
            initialDrawingPath = note.drawingPath,
            onDismiss = { drawingNoteToViewOrEdit = null },
            onSaveDrawing = { title, imagePath, colorHex ->
                viewModel.updateNote(
                    note.copy(
                        title = title,
                        drawingPath = imagePath,
                        colorHex = colorHex,
                        tag = note.tag,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                drawingNoteToViewOrEdit = null
                Toast.makeText(context, if (strings.isFa) "طرح با موفقیت به‌روزرسانی شد ✓" else "Drawing updated ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }

    noteToEdit?.let { note ->
        EditNoteDialog(
            initialNote = note,
            onDismiss = { noteToEdit = null },
            onOpenDrawingFullScreen = { noteWithDrawing ->
                noteToEdit = null
                drawingNoteToViewOrEdit = noteWithDrawing
            },
            onConfirm = { title, content, colorHex, isPinned, drawingPath, tag ->
                viewModel.updateNote(
                    note.copy(
                        title = title,
                        content = content,
                        colorHex = colorHex,
                        isPinned = isPinned,
                        drawingPath = drawingPath,
                        tag = tag,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                noteToEdit = null
            },
            existingTags = allExistingTags
        )
    }

    noteToAddToCalendar?.let { note ->
        AddToCalendarFromNoteDialog(
            note = note,
            onDismiss = { noteToAddToCalendar = null },
            onConfirm = { title, desc, pDate, time, category, colorHex ->
                viewModel.addEvent(
                    title = title,
                    description = desc,
                    persianDate = pDate,
                    time = time,
                    category = category,
                    colorHex = colorHex
                )
                noteToAddToCalendar = null
                Toast.makeText(context, "یادداشت به عنوان رویداد و یادآور در تقویم ثبت شد ✓", Toast.LENGTH_LONG).show()
            }
        )
    }
}

fun buildHighlightedString(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    return buildAnnotatedString {
        val matches = query.toRegex(RegexOption.IGNORE_CASE).findAll(text)
        var lastIndex = 0
        for (match in matches) {
            append(text.substring(lastIndex, match.range.first))
            withStyle(style = SpanStyle(background = Color.Yellow.copy(alpha = 0.5f))) {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }
}

fun getNoteAccentColor(hex: String): Color {
    return when (hex.uppercase()) {
        "#FEF3C7" -> Color(0xFFF59E0B) // Radiant Amber
        "#DCFCE7" -> Color(0xFF10B981) // Emerald Green
        "#E0F2FE" -> Color(0xFF38BDF8) // Sky Blue
        "#FCE7F3" -> Color(0xFFF43F5E) // Rose Pink
        "#F3E8FF" -> Color(0xFFA855F7) // Purple
        "#F1F5F9" -> Color(0xFF94A3B8) // Slate Gray
        else -> parseColor(hex)
    }
}

fun getAdaptiveNoteColor(hex: String, isDark: Boolean): Color {
    if (!isDark) return parseColor(hex)
    // کریستالی تیره: سطح کاملاً خنثی، لوکس و تمیز بدون رنگ‌های کدر و قهوه‌ای
    return Color(0xFF18181C)
}

fun getAdaptiveNoteBorder(hex: String, isDark: Boolean): Color {
    if (!isDark) return Color.Black.copy(alpha = 0.08f)
    // حاشیه کریستالی ظریف با ته‌رنگ درخشان و ملایم از رنگ نوت
    val accent = getNoteAccentColor(hex)
    return accent.copy(alpha = 0.35f)
}

@Composable
fun NoteCard(
    note: NoteEntity,
    searchQuery: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTagClick: (String) -> Unit,
    onAddToCalendar: () -> Unit,
    onSyncToGoogleCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val strings = rememberAppStrings()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardColor = getAdaptiveNoteColor(note.colorHex, isDark)
    val cardBorderColor = getAdaptiveNoteBorder(note.colorHex, isDark)
    val accentColor = getNoteAccentColor(note.colorHex)
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.8.dp, cardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDark) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = buildHighlightedString(note.title, searchQuery),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1E293B)
                    )
                }

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFF59E0B) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Whiteboard Drawing Thumbnail (if exists)
            if (!note.drawingPath.isNullOrBlank()) {
                val drawingBitmap = remember(note.drawingPath, note.updatedAt) {
                    val file = File(note.drawingPath)
                    if (file.exists()) BitmapFactory.decodeFile(note.drawingPath)?.asImageBitmap() else null
                }
                if (drawingBitmap != null) {
                    Image(
                        bitmap = drawingBitmap,
                        contentDescription = "طرح وایت‌برد",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(0.6.dp, if (isDark) Color(0xFF2A2A32) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (note.content.isNotBlank()) {
                Text(
                    text = buildHighlightedString(note.content, searchQuery),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFFD4D4D8) else Color(0xFF334155),
                    lineHeight = 18.sp
                )
            } else if (!note.drawingPath.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF222228) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (strings.isFa) "طرح وایت‌برد" else "Whiteboard Drawing",
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tags = note.tag.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    tags.forEach { tagItem ->
                        val isIdea = tagItem == "ایده"
                        Surface(
                            onClick = { onTagClick(tagItem) },
                            color = if (isIdea) {
                                if (isDark) Color(0xFF3B2E1E) else Color(0xFFFEF3C7)
                            } else {
                                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.6.dp,
                                if (isIdea) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isIdea) "💡 $tagItem" else tagItem,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIdea) MaterialTheme.colorScheme.primary else (if (isDark) Color(0xFFE4E4E7) else Color(0xFF334155))
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.formatDigits(note.persianDate),
                        fontSize = 10.sp,
                        color = if (isDark) Color(0xFF71717A) else Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Copy Icon (Manual save to clipboard)
                    IconButton(
                        onClick = { NoteShareHelper.copyToClipboard(context, note) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = if (strings.isFa) "کپی یادداشت در حافظه" else "Copy note",
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Edit Icon
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = strings.edit,
                            tint = if (isDark) Color(0xFFA1A1AA) else Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // More Menu for Keep, Google Calendar, Share, Calendar Add, Delete
                    Box {
                        IconButton(
                            onClick = { showDropdownMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "ارسال به Google Keep" else "Send to Google Keep", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    NoteShareHelper.sendToGoogleKeep(context, note)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "همگام‌سازی با تقویم گوگل" else "Sync with Google Calendar", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    onSyncToGoogleCalendar()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "اشتراک‌گذاری یادداشت" else "Share note", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    NoteShareHelper.shareNote(context, note)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن به تقویم و رویدادها" else "Add to Calendar", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    onAddToCalendar()
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text(strings.deleteNote, fontSize = 12.sp, color = Color(0xFFEF4444)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showDropdownMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
