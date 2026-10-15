package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.example.calendar.PersianOccasion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.JalaliDate
import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.ui.rememberAppStrings

fun eventCategoryLabel(cat: String, isFa: Boolean): String = if (isFa) cat else when (cat) {
    "کاری" -> "Work"
    "شخصی" -> "Personal"
    "مهم" -> "Important"
    "جلسه" -> "Meeting"
    "یادآوری" -> "Reminder"
    else -> cat
}

fun reminderOptionLabel(opt: String, isFa: Boolean): String = if (isFa) opt else when (opt) {
    "بدون زنگ" -> "No Alarm"
    "همزمان با رویداد" -> "At event time"
    "۵ دقیقه قبل" -> "5 min before"
    "۱۰ دقیقه قبل" -> "10 min before"
    "۱۵ دقیقه قبل" -> "15 min before"
    "۳۰ دقیقه قبل" -> "30 min before"
    "۱ ساعت قبل" -> "1 hour before"
    "۱ روز قبل" -> "1 day before"
    else -> opt
}

/**
 * Super Simple & Clean Persian Time Selector.
 * Touching the card opens the standard Material 3 Clock Dial TimePicker Dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianTimePicker(
    initialHour: Int = 10,
    initialMinute: Int = 0,
    onTimeChange: (hour: Int, minute: Int, formatted: String) -> Unit,
    label: String = ""
) {
    val strings = rememberAppStrings()
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    val formattedTime = String.format("%02d:%02d", hour, minute)
    val displayDigits = strings.formatDigits(formattedTime)
    val displayLabel = if (label.isNotBlank()) label else (if (strings.isFa) "ساعت برگزاری رویداد" else "Event Time")

    // Main Clickable Time Card
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showTimePickerDialog = true }
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(displayLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (strings.isFa) "ساعت $displayDigits" else "Time: $displayDigits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (strings.isFa) "تنظیم ساعت" else "Set Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    // Material 3 Clock TimePicker Dialog
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(if (strings.isFa) "انتخاب ساعت رویداد" else "Select Event Time", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                        val formatted = String.format("%02d:%02d", hour, minute)
                        onTimeChange(hour, minute, formatted)
                        showTimePickerDialog = false
                    }
                ) {
                    Text(if (strings.isFa) "تأیید ساعت" else "Confirm Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEventDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        time: String,
        category: String,
        colorHex: String
    ) -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var persianDate by remember { mutableStateOf(initialDate.formatted) }
    var isAllDay by remember { mutableStateOf(false) }
    var eventTime by remember { mutableStateOf("10:00") }
    var reminderOption by remember { mutableStateOf("همزمان با رویداد") }
    var category by remember { mutableStateOf("کاری") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }

    val categories = listOf("کاری", "شخصی", "مهم", "جلسه", "یادآوری")
    val reminderOptions = listOf("بدون زنگ", "همزمان با رویداد", "۵ دقیقه قبل", "۱۰ دقیقه قبل", "۱۵ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.newEvent, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد *" else "Event Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "توضیحات رویداد (اختیاری)" else "Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ شمسی (مثال: ۱۴۰۵/۰۱/۱۵)" else "Solar Date (e.g. 1405/01/15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // All-day switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (strings.isFa) "رویداد تمام‌روز (بدون ساعت خاص)" else "All-day event (no specific time)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                if (!isAllDay) {
                    // Simple Clean Persian Time Picker
                    PersianTimePicker(
                        initialHour = 10,
                        initialMinute = 0,
                        onTimeChange = { _, _, formatted -> eventTime = formatted },
                        label = if (strings.isFa) "ساعت برگزاری رویداد" else "Event Time"
                    )

                    // Reminder Alarm Selection
                    Text(if (strings.isFa) "⏰ زنگ و زمان یادآوری هشدار:" else "⏰ Reminder & Alarm:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reminderOptions.forEach { option ->
                            FilterChip(
                                selected = reminderOption == option,
                                onClick = { reminderOption = option },
                                label = { Text(reminderOptionLabel(option, strings.isFa), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Text(if (strings.isFa) "دسته‌بندی:" else "Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(eventCategoryLabel(cat, strings.isFa), fontSize = 11.sp) }
                        )
                    }
                }

                Text(if (strings.isFa) "رنگ نشانگر رویداد:" else "Event Marker Color:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colors.forEach { hex ->
                        val col = parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalTime = if (isAllDay) "" else eventTime
                        val descWithReminder = if (isAllDay || reminderOption == "بدون زنگ") {
                            description.trim()
                        } else {
                            if (description.isNotBlank()) "${description.trim()}\n[یادآوری: $reminderOption]" else "[یادآوری: $reminderOption]"
                        }
                        onConfirm(title.trim(), descWithReminder, persianDate.trim(), finalTime, category, selectedColor)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_event")
            ) {
                Text(if (strings.isFa) "ثبت رویداد" else "Add Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditEventDialog(
    initialEvent: EventEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        time: String,
        category: String,
        colorHex: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf(initialEvent.title) }
    var description by remember { mutableStateOf(initialEvent.description.replace(Regex("\\[یادآوری:[^\\]]+\\]"), "").trim()) }
    var persianDate by remember { mutableStateOf(initialEvent.persianDate) }
    var isAllDay by remember { mutableStateOf(initialEvent.startTime.isBlank()) }
    var eventTime by remember { mutableStateOf(if (initialEvent.startTime.isNotBlank()) initialEvent.startTime else "10:00") }
    var reminderOption by remember {
        val match = Regex("\\[یادآوری:([^\\]]+)\\]").find(initialEvent.description)
        mutableStateOf(match?.groupValues?.getOrNull(1)?.trim() ?: "همزمان با رویداد")
    }
    var category by remember { mutableStateOf(initialEvent.category) }
    var selectedColor by remember { mutableStateOf(initialEvent.colorHex) }

    val categories = listOf("کاری", "شخصی", "مهم", "جلسه", "یادآوری")
    val reminderOptions = listOf("بدون زنگ", "همزمان با رویداد", "۵ دقیقه قبل", "۱۰ دقیقه قبل", "۱۵ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (strings.isFa) "ویرایش رویداد" else "Edit Event", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = if (strings.isFa) "حذف رویداد" else "Delete Event", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد *" else "Event Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "توضیحات رویداد" else "Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ شمسی" else "Solar Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // All-day switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (strings.isFa) "رویداد تمام‌روز (بدون ساعت خاص)" else "All-day event (no specific time)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }

                if (!isAllDay) {
                    val (h, m) = try {
                        val parts = eventTime.split(":")
                        (parts[0].toIntOrNull() ?: 10) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    } catch (_: Exception) { 10 to 0 }

                    PersianTimePicker(
                        initialHour = h,
                        initialMinute = m,
                        onTimeChange = { _, _, formatted -> eventTime = formatted },
                        label = if (strings.isFa) "ساعت برگزاری رویداد" else "Event Time"
                    )

                    // Reminder Alarm Selection
                    Text(if (strings.isFa) "⏰ زمان زنگ و یادآوری هشدار:" else "⏰ Reminder & Alarm:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reminderOptions.forEach { option ->
                            FilterChip(
                                selected = reminderOption == option,
                                onClick = { reminderOption = option },
                                label = { Text(reminderOptionLabel(option, strings.isFa), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Text(if (strings.isFa) "دسته‌بندی:" else "Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(eventCategoryLabel(cat, strings.isFa), fontSize = 11.sp) })
                    }
                }

                Text(if (strings.isFa) "رنگ نشانگر:" else "Marker Color:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colors.forEach { hex ->
                        val col = parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .then(if (selectedColor == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalTime = if (isAllDay) "" else eventTime
                        val descWithReminder = if (isAllDay || reminderOption == "بدون زنگ") {
                            description.trim()
                        } else {
                            if (description.isNotBlank()) "${description.trim()}\n[یادآوری: $reminderOption]" else "[یادآوری: $reminderOption]"
                        }
                        onConfirm(title.trim(), descWithReminder, persianDate.trim(), finalTime, category, selectedColor)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/**
 * Dialog to directly schedule/convert a Note into a Calendar Event / Reminder
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddToCalendarFromNoteDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        time: String,
        category: String,
        colorHex: String
    ) -> Unit
) {
    val strings = rememberAppStrings()
    val today = PersianCalendarHelper.getCurrentJalaliDate()
    var title by remember { mutableStateOf(note.title) }
    var description by remember { mutableStateOf(note.content) }
    var persianDate by remember { mutableStateOf(today.formatted) }
    var eventTime by remember { mutableStateOf("10:00") }
    var reminderOption by remember { mutableStateOf("همزمان با رویداد") }
    var category by remember { mutableStateOf("یادآوری") }
    var selectedColor by remember { mutableStateOf(note.colorHex) }

    val reminderOptions = listOf("بدون زنگ", "همزمان با رویداد", "۵ دقیقه قبل", "۱۰ دقیقه قبل", "۱۵ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (strings.isFa) "افزودن یادداشت به تقویم و یادآور" else "Add Note to Calendar & Reminder", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد / یادآور" else "Event / Reminder Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "متن یادداشت" else "Note Content") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ اجرای رویداد (شمسی)" else "Execution Date (Solar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PersianTimePicker(
                    initialHour = 10,
                    initialMinute = 0,
                    onTimeChange = { _, _, formatted -> eventTime = formatted },
                    label = if (strings.isFa) "ساعت رویداد و یادآور" else "Time & Reminder"
                )

                // Reminder alarm options
                Text(if (strings.isFa) "⏰ زنگ و هشدار یادآوری:" else "⏰ Reminder & Alarm:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminderOptions.forEach { option ->
                        FilterChip(
                            selected = reminderOption == option,
                            onClick = { reminderOption = option },
                            label = { Text(reminderOptionLabel(option, strings.isFa), fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val descWithReminder = if (reminderOption == "بدون زنگ") {
                            description.trim()
                        } else {
                            if (description.isNotBlank()) "${description.trim()}\n[یادآوری: $reminderOption]" else "[یادآوری: $reminderOption]"
                        }
                        onConfirm(title.trim(), descWithReminder, persianDate.trim(), eventTime.trim(), category, selectedColor)
                    }
                }
            ) {
                Text(if (strings.isFa) "ثبت در تقویم" else "Add to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: String, priority: String, category: String) -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(initialDate.formatted) }
    var priority by remember { mutableStateOf("متوسط") }
    var category by remember { mutableStateOf("عمومی") }

    val priorities = listOf("پایین", "متوسط", "بالا", "فوری")
    val categories = listOf("عمومی", "کسب و کار", "عبادت", "تفریح", "معاشرت", "خرید", "شخصی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newTask, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان کار یا وظیفه *" else "Task Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(if (strings.isFa) "تاریخ موعد (شمسی)" else "Due Date (Solar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (strings.isFa) "اولویت:" else "Priority:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(strings.taskPriority(prio), fontSize = 12.sp) }
                        )
                    }
                }

                Text(if (strings.isFa) "دسته‌بندی:" else "Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.taskTag(cat), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), dueDate.trim(), priority, category)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_task")
            ) {
                Text(strings.newTask)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: String, priority: String, category: String) -> Unit,
    onDelete: () -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf(task.title) }
    var dueDate by remember { mutableStateOf(task.persianDueDate) }
    var priority by remember { mutableStateOf(task.priority) }
    var category by remember { mutableStateOf(task.category) }

    val priorities = listOf("پایین", "متوسط", "بالا", "فوری")
    val categories = listOf("عمومی", "کسب و کار", "عبادت", "تفریح", "معاشرت", "خرید", "شخصی")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.editTask, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = strings.deleteTask, tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان کار یا وظیفه *" else "Task Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(if (strings.isFa) "تاریخ موعد (شمسی)" else "Due Date (Solar)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (strings.isFa) "اولویت:" else "Priority:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(strings.taskPriority(prio), fontSize = 12.sp) }
                        )
                    }
                }

                Text(if (strings.isFa) "دسته‌بندی:" else "Category:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(strings.taskTag(cat), fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), dueDate.trim(), priority, category)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String) -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FEF3C7") }

    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addNote, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (strings.isFa) "متن یادداشت *" else "Note Content *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                Text(if (strings.isFa) "رنگ یادداشت:" else "Note Color:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    noteColors.forEach { hex ->
                        val col = parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onConfirm(title.trim(), content.trim(), selectedColor)
                    }
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_note")
            ) {
                Text(if (strings.isFa) "ذخیره یادداشت" else "Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun EditNoteDialog(
    initialNote: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String, isPinned: Boolean) -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.content) }
    var selectedColor by remember { mutableStateOf(initialNote.colorHex) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }

    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editNote, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.noteTitleLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (strings.isFa) "متن یادداشت *" else "Note Content *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (strings.isFa) "سنجاق به بالا" else "Pin to top", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }

                Text(if (strings.isFa) "رنگ یادداشت:" else "Note Color:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    noteColors.forEach { hex ->
                        val col = parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (selectedColor == hex) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onConfirm(title.trim(), content.trim(), selectedColor, isPinned)
                    }
                },
                enabled = content.isNotBlank()
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
fun PairingInfoDialog(
    ip: String,
    port: Int,
    onDismiss: () -> Unit
) {
    val strings = rememberAppStrings()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val url = "http://$ip:$port"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (strings.isFa) "اتصال به افزونه مرورگر کروم" else "Connect to Chrome Extension", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (strings.isFa) "برای همگام‌سازی مستقیم و بدون نیاز به اینترنت:" else "For direct offline local sync:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    if (strings.isFa) "۱. گوشی و رایانه را به یک شبکه Wi-Fi متصل کنید، یا هات‌اسپات (نقطه اتصال) یکی از دستگاه‌ها را روشن کنید." else "1. Connect phone and PC to the same Wi-Fi network, or enable hotspot on one device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (strings.isFa) "۲. آدرس زیر را در مرورگر لپ‌تاپ باز کنید یا در افزونه کروم وارد نمایید:" else "2. Open the URL below in your PC browser or enter it in the Chrome extension:",
                    style = MaterialTheme.typography.bodySmall
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(url)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = if (strings.isFa) "کپی آدرس" else "Copy URL")
                        }
                    }
                }

                Text(
                    if (strings.isFa) "۳. با فشردن دکمه همگام‌سازی در افزونه کروم، اطلاعات رویدادها، وظایف و یادداشت‌ها به صورت خودکار در شبکه محلی تبادل می‌شوند." else "3. By clicking sync in the Chrome extension, events, tasks, and notes are exchanged automatically over the local network.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text(strings.understood) }
        }
    )
}

@Composable
fun JsonBackupDialog(
    jsonString: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val strings = rememberAppStrings()
    var importText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (strings.isFa) "پشتیبان‌گیری و بازیابی آفلاین JSON" else "Offline JSON Backup & Restore", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text(if (strings.isFa) "خروجی پشتیبان (Export)" else "Export Backup") }
                    )
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text(if (strings.isFa) "ورود اطلاعات (Import)" else "Import Data") }
                    )
                }

                if (activeTab == 0) {
                    Text(if (strings.isFa) "کد JSON کلیه رویدادها، وظایف و یادداشت‌های شما:" else "JSON code of all events, tasks, and notes:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = jsonString,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonString))
                            copiedMessage = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedMessage) (if (strings.isFa) "کپی شد!" else "Copied!") else (if (strings.isFa) "کپی کد پشتیبان در حافظه" else "Copy backup code"))
                    }
                } else {
                    Text(if (strings.isFa) "کد پشتیبان JSON را در کادر زیر جای‌گذاری (Paste) کنید:" else "Paste backup JSON code below:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("{\"deviceId\":\"...\", ...}") },
                        minLines = 5,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (importText.isNotBlank()) {
                                onImport(importText)
                            }
                        },
                        enabled = importText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (strings.isFa) "بازیابی و ادغام اطلاعات" else "Restore & Merge Data")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

/**
 * Dialog to edit an Occasion (custom or official calendar occasion).
 * Allows changing the title, holiday status, month, and day.
 */
@Composable
fun EditOccasionDialog(
    occasion: PersianOccasion,
    currentDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, month: Int, day: Int, isHoliday: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = rememberAppStrings()
    val initialMonth = if (occasion.customMonth != -1) occasion.customMonth else currentDate.month
    val initialDay = if (occasion.customDay != -1) occasion.customDay else currentDate.day

    var title by remember { mutableStateOf(occasion.title) }
    var isHoliday by remember { mutableStateOf(occasion.isHoliday) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    var monthMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    val daysInSelectedMonth = when (selectedMonth) {
        in 1..6 -> 31
        in 7..11 -> 30
        else -> 29
    }
    if (selectedDay > daysInSelectedMonth) {
        selectedDay = daysInSelectedMonth
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isFa) "ویرایش مناسبت" else "Edit Occasion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Occasion Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان مناسبت" else "Occasion Title") },
                    placeholder = { Text(if (strings.isFa) "مثلاً: سالگرد، تولد، رویداد..." else "e.g. Anniversary, Birthday, Event...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Date Picker (Month and Day selectors)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (strings.isFa) "تاریخ مناسبت (تکرار سالانه):" else "Occasion Date (Yearly):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month selector
                            Box(modifier = Modifier.weight(1.2f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { monthMenuExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.jalaliMonths.getOrElse(selectedMonth - 1) { "" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = monthMenuExpanded,
                                    onDismissRequest = { monthMenuExpanded = false }
                                ) {
                                    strings.jalaliMonths.forEachIndexed { index, mName ->
                                        DropdownMenuItem(
                                            text = { Text(mName, fontSize = 12.sp) },
                                            onClick = {
                                                selectedMonth = index + 1
                                                monthMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Day selector
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dayMenuExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (strings.isFa) "${strings.formatDigits(selectedDay.toString())} ام" else "Day ${strings.formatDigits(selectedDay.toString())}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = dayMenuExpanded,
                                    onDismissRequest = { dayMenuExpanded = false }
                                ) {
                                    for (d in 1..daysInSelectedMonth) {
                                        DropdownMenuItem(
                                            text = { Text(if (strings.isFa) "${strings.formatDigits(d.toString())} ام" else "Day ${strings.formatDigits(d.toString())}", fontSize = 12.sp) },
                                            onClick = {
                                                selectedDay = d
                                                dayMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Holiday Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isHoliday = !isHoliday }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isHoliday,
                        onCheckedChange = { isHoliday = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(if (strings.isFa) "روز تعطیل رسمی" else "Official Holiday", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (strings.isFa) "در صورت انتخاب، این روز در تقویم با رنگ قرمز تعطیل نشان داده خواهد شد" else "If selected, this day will be marked as a red holiday on the calendar",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }

                // If editing an official calendar occasion
                if (occasion.customMonth == -1) {
                    Text(
                        text = if (strings.isFa) "💡 این مناسبت از تقویم رسمی است. با ویرایش، تغییرات شما جایگزین خواهد شد و هر زمان بخواهید می‌توانید آن را به حالت اولیه برگردانید." else "💡 This is an official calendar occasion. Editing will override it, and you can reset it anytime.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), selectedMonth, selectedDay, isHoliday)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (occasion.customMonth == -1) (if (strings.isFa) "بازگردانی به پیش‌فرض" else "Reset to default") else (if (strings.isFa) "حذف مناسبت" else "Delete Occasion"))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        }
    )
}

/**
 * Dialog to pick which occasion to edit when a day has multiple occasions.
 */
@Composable
fun SelectOccasionToEditDialog(
    occasions: List<PersianOccasion>,
    onSelect: (PersianOccasion) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = rememberAppStrings()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isFa) "انتخاب مناسبت برای ویرایش" else "Select Occasion to Edit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (strings.isFa) "مناسبت مورد نظر برای تغییر یا اصلاح را انتخاب کنید:" else "Select the occasion to edit or modify:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                occasions.forEach { occ ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(occ) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = occ.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (occ.isHoliday) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface
                                )
                                if (occ.isHoliday) {
                                    Text(if (strings.isFa) "تعطیل رسمی" else "Official Holiday", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFFF59E0B)
    }
}
