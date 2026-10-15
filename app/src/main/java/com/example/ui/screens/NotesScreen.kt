package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.parseColor
import com.example.ui.rememberAppStrings

@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.activeNotes.collectAsState()
    val strings = rememberAppStrings()
    var searchQuery by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteEntity?>(null) }
    var noteToAddToCalendar by remember { mutableStateOf<NoteEntity?>(null) }

    val filteredNotes = if (searchQuery.isBlank()) {
        notes
    } else {
        notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true)
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
                placeholder = { Text(strings.searchNotes) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
                            onClick = { noteToEdit = note },
                            onEdit = { noteToEdit = note },
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

        FloatingActionButton(
            onClick = { showAddNoteDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_note")
        ) {
            Icon(Icons.Default.Add, contentDescription = strings.addNote)
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { title, content, colorHex ->
                viewModel.addNote(title, content, colorHex)
                showAddNoteDialog = false
            }
        )
    }

    noteToEdit?.let { note ->
        EditNoteDialog(
            initialNote = note,
            onDismiss = { noteToEdit = null },
            onConfirm = { title, content, colorHex, isPinned ->
                viewModel.updateNote(
                    note.copy(
                        title = title,
                        content = content,
                        colorHex = colorHex,
                        isPinned = isPinned,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                noteToEdit = null
            }
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

@Composable
fun NoteCard(
    note: NoteEntity,
    searchQuery: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAddToCalendar: () -> Unit,
    onSyncToGoogleCalendar: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val strings = rememberAppStrings()
    val cardColor = parseColor(note.colorHex)
    var showDropdownMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildHighlightedString(note.title, searchQuery),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f)
                )

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = buildHighlightedString(note.content, searchQuery),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF334155),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.formatDigits(note.persianDate),
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Copy Icon (Manual save to clipboard)
                    IconButton(
                        onClick = { NoteShareHelper.copyToClipboard(context, note) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = if (strings.isFa) "کپی یادداشت در حافظه" else "Copy note",
                            tint = Color(0xFF475569),
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
                            tint = Color(0xFF475569),
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
