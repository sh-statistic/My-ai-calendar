package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
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

/**
 * Persian Interactive Time Picker.
 * Provides intuitive stepper buttons and presets so users NEVER have to type or mess with colons.
 */
@Composable
fun PersianTimePicker(
    initialHour: Int = 10,
    initialMinute: Int = 0,
    onTimeChange: (hour: Int, minute: Int, formatted: String) -> Unit,
    label: String = "ساعت رویداد"
) {
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }

    val formattedTime = String.format("%02d:%02d", hour, minute)
    val persianDisplay = PersianCalendarHelper.toPersianDigits(formattedTime)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = persianDisplay,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Hour & Minute Stepper Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour Stepper
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        hour = if (hour > 0) hour - 1 else 23
                        onTimeChange(hour, minute, String.format("%02d:%02d", hour, minute))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "کاهش ساعت", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${PersianCalendarHelper.toPersianDigits(String.format("%02d", hour))} ساعت",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = {
                        hour = (hour + 1) % 24
                        onTimeChange(hour, minute, String.format("%02d:%02d", hour, minute))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزایش ساعت", modifier = Modifier.size(16.dp))
                }
            }

            Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            // Minute Stepper (5-minute increments)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        minute = if (minute >= 5) minute - 5 else 55
                        onTimeChange(hour, minute, String.format("%02d:%02d", hour, minute))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "کاهش دقیقه", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${PersianCalendarHelper.toPersianDigits(String.format("%02d", minute))} دقیقه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = {
                        minute = (minute + 5) % 60
                        onTimeChange(hour, minute, String.format("%02d:%02d", hour, minute))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "افزایش دقیقه", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("۰۹:۰۰" to (9 to 0), "۱۲:۰۰" to (12 to 0), "۱۶:۰۰" to (16 to 0), "۱۹:۰۰" to (19 to 0), "۲۱:۰۰" to (21 to 0)).forEach { (labelStr, pair) ->
                Surface(
                    color = if (hour == pair.first && minute == pair.second) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clickable {
                            hour = pair.first
                            minute = pair.second
                            onTimeChange(hour, minute, String.format("%02d:%02d", hour, minute))
                        }
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = labelStr,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEventDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        startTime: String,
        endTime: String,
        category: String,
        colorHex: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var persianDate by remember { mutableStateOf(initialDate.formatted) }
    var isAllDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("11:00") }
    var showEndTime by remember { mutableStateOf(false) }
    var reminderOption by remember { mutableStateOf("همزمان با شروع") }
    var category by remember { mutableStateOf("کاری") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }

    val categories = listOf("کاری", "شخصی", "مهم", "جلسه", "یادآوری")
    val reminderOptions = listOf("بدون زنگ", "همزمان با شروع", "۱۰ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت رویداد جدید", fontWeight = FontWeight.Bold)
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
                    label = { Text("عنوان رویداد *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات رویداد (اختیاری)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text("تاریخ شمسی (مثال: ۱۴۰۵/۰۱/۱۵)") },
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
                    Text("رویداد تمام‌روز (بدون ساعت مشخص)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it }
                    )
                }

                if (!isAllDay) {
                    // Easy Persian Time Picker for Start Time
                    PersianTimePicker(
                        initialHour = 10,
                        initialMinute = 0,
                        onTimeChange = { _, _, formatted -> startTime = formatted },
                        label = "ساعت شروع رویداد"
                    )

                    // Option for End Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مشخص کردن ساعت پایان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(
                            checked = showEndTime,
                            onCheckedChange = { showEndTime = it }
                        )
                    }

                    if (showEndTime) {
                        PersianTimePicker(
                            initialHour = 11,
                            initialMinute = 0,
                            onTimeChange = { _, _, formatted -> endTime = formatted },
                            label = "ساعت پایان رویداد"
                        )
                    }
                }

                // Alarm / Reminder Section
                Text("⏰ زنگ و هشدار یادآوری:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminderOptions.take(3).forEach { option ->
                        FilterChip(
                            selected = reminderOption == option,
                            onClick = { reminderOption = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reminderOptions.drop(3).forEach { option ->
                        FilterChip(
                            selected = reminderOption == option,
                            onClick = { reminderOption = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }

                Text("دسته‌بندی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Text("رنگ نشانگر رویداد:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                        val finalStart = if (isAllDay) "" else startTime
                        val finalEnd = if (isAllDay || !showEndTime) "" else endTime
                        onConfirm(title.trim(), description.trim(), persianDate.trim(), finalStart, finalEnd, category, selectedColor)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_event")
            ) {
                Text("ثبت رویداد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun EditEventDialog(
    initialEvent: EventEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        startTime: String,
        endTime: String,
        category: String,
        colorHex: String
    ) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(initialEvent.title) }
    var description by remember { mutableStateOf(initialEvent.description) }
    var persianDate by remember { mutableStateOf(initialEvent.persianDate) }
    var isAllDay by remember { mutableStateOf(initialEvent.startTime.isBlank()) }
    var startTime by remember { mutableStateOf(if (initialEvent.startTime.isNotBlank()) initialEvent.startTime else "10:00") }
    var endTime by remember { mutableStateOf(if (initialEvent.endTime.isNotBlank()) initialEvent.endTime else "11:00") }
    var showEndTime by remember { mutableStateOf(initialEvent.endTime.isNotBlank()) }
    var reminderOption by remember { mutableStateOf("همزمان با شروع") }
    var category by remember { mutableStateOf(initialEvent.category) }
    var selectedColor by remember { mutableStateOf(initialEvent.colorHex) }

    val categories = listOf("کاری", "شخصی", "مهم", "جلسه", "یادآوری")
    val reminderOptions = listOf("بدون زنگ", "همزمان با شروع", "۱۰ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ویرایش رویداد", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف رویداد", tint = MaterialTheme.colorScheme.error)
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
                    label = { Text("عنوان رویداد *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات رویداد") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text("تاریخ شمسی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // All-day toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("رویداد تمام‌روز", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }

                if (!isAllDay) {
                    val (h, m) = try {
                        val parts = startTime.split(":")
                        (parts[0].toIntOrNull() ?: 10) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    } catch (_: Exception) { 10 to 0 }

                    PersianTimePicker(
                        initialHour = h,
                        initialMinute = m,
                        onTimeChange = { _, _, formatted -> startTime = formatted },
                        label = "ساعت شروع رویداد"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مشخص کردن ساعت پایان", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Switch(checked = showEndTime, onCheckedChange = { showEndTime = it })
                    }

                    if (showEndTime) {
                        val (eh, em) = try {
                            val parts = endTime.split(":")
                            (parts[0].toIntOrNull() ?: 11) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                        } catch (_: Exception) { 11 to 0 }

                        PersianTimePicker(
                            initialHour = eh,
                            initialMinute = em,
                            onTimeChange = { _, _, formatted -> endTime = formatted },
                            label = "ساعت پایان رویداد"
                        )
                    }
                }

                // Alarm options
                Text("⏰ زنگ و یادآوری:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reminderOptions.take(3).forEach { option ->
                        FilterChip(
                            selected = reminderOption == option,
                            onClick = { reminderOption = option },
                            label = { Text(option, fontSize = 10.sp) }
                        )
                    }
                }

                Text("دسته‌بندی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, fontSize = 11.sp) })
                    }
                }

                Text("رنگ نشانگر:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                        val finalStart = if (isAllDay) "" else startTime
                        val finalEnd = if (isAllDay || !showEndTime) "" else endTime
                        onConfirm(title.trim(), description.trim(), persianDate.trim(), finalStart, finalEnd, category, selectedColor)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("ذخیره تغییرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

/**
 * Dialog to directly schedule/convert a Note into a Calendar Event / Reminder
 */
@Composable
fun AddToCalendarFromNoteDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        persianDate: String,
        startTime: String,
        category: String,
        colorHex: String
    ) -> Unit
) {
    val today = PersianCalendarHelper.getCurrentJalaliDate()
    var title by remember { mutableStateOf(note.title) }
    var description by remember { mutableStateOf(note.content) }
    var persianDate by remember { mutableStateOf(today.formatted) }
    var startTime by remember { mutableStateOf("10:00") }
    var category by remember { mutableStateOf("یادآوری") }
    var selectedColor by remember { mutableStateOf(note.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("افزودن یادداشت به تقویم و یادآور", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    label = { Text("عنوان رویداد / یادآور") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("متن یادداشت") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text("تاریخ اجرای رویداد (شمسی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PersianTimePicker(
                    initialHour = 10,
                    initialMinute = 0,
                    onTimeChange = { _, _, formatted -> startTime = formatted },
                    label = "زمان هشدار و زنگ یادآوری"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title.trim(), description.trim(), persianDate.trim(), startTime.trim(), category, selectedColor)
                    }
                }
            ) {
                Text("ثبت در تقویم")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddTaskDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: String, priority: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(initialDate.formatted) }
    var priority by remember { mutableStateOf("متوسط") }
    var category by remember { mutableStateOf("عمومی") }

    val priorities = listOf("پایین", "متوسط", "بالا", "فوری")
    val categories = listOf("عمومی", "کاری", "شخصی", "خرید", "مطالعه")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن وظیفه جدید", fontWeight = FontWeight.Bold) },
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
                    label = { Text("عنوان کار یا وظیفه *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("تاریخ موعد (شمسی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اولویت:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio, fontSize = 12.sp) }
                        )
                    }
                }

                Text("دسته‌بندی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
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
                Text("افزودن وظیفه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dueDate: String, priority: String, category: String) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var dueDate by remember { mutableStateOf(task.persianDueDate) }
    var priority by remember { mutableStateOf(task.priority) }
    var category by remember { mutableStateOf(task.category) }

    val priorities = listOf("پایین", "متوسط", "بالا", "فوری")
    val categories = listOf("عمومی", "کاری", "شخصی", "خرید", "مطالعه")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ویرایش وظیفه", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف وظیفه", tint = MaterialTheme.colorScheme.error)
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
                    label = { Text("عنوان کار یا وظیفه *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("تاریخ موعد (شمسی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("اولویت:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio, fontSize = 12.sp) }
                        )
                    }
                }

                Text("دسته‌بندی:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
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
                Text("ذخیره تغییرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FEF3C7") }

    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("یادداشت جدید", fontWeight = FontWeight.Bold) },
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
                    label = { Text("عنوان یادداشت") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("متن یادداشت *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                Text("رنگ یادداشت:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                Text("ذخیره یادداشت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun EditNoteDialog(
    initialNote: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initialNote.title) }
    var content by remember { mutableStateOf(initialNote.content) }
    var selectedColor by remember { mutableStateOf(initialNote.colorHex) }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }

    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویرایش یادداشت", fontWeight = FontWeight.Bold) },
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
                    label = { Text("عنوان یادداشت") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("متن یادداشت *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("سنجاق به بالا", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }

                Text("رنگ یادداشت:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
                Text("ذخیره تغییرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

@Composable
fun PairingInfoDialog(
    ip: String,
    port: Int,
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val url = "http://$ip:$port"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اتصال به افزونه مرورگر کروم", fontWeight = FontWeight.Bold)
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
                    "برای همگام‌سازی مستقیم و بدون نیاز به اینترنت:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "۱. گوشی و رایانه را به یک شبکه Wi-Fi متصل کنید، یا هات‌اسپات (نقطه اتصال) یکی از دستگاه‌ها را روشن کنید.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "۲. در افزونه کروم روی لپ‌تاپ، آدرس زیر را باز کنید:",
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
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی آدرس")
                        }
                    }
                }

                // Live Scannable QR Code
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeView(
                        data = url,
                        size = 150.dp
                    )
                }

                Text(
                    "۳. با دوربین گوشی، کد QR روی صفحه مانیتور لپ‌تاپ را اسکن کنید تا تقویم و اطلاعات فوراً همگام‌سازی شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("متوجه شدم") }
        }
    )
}

@Composable
fun JsonBackupDialog(
    jsonString: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var importText by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پشتیبان‌گیری و بازیابی آفلاین JSON", fontWeight = FontWeight.Bold) },
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
                        label = { Text("خروجی پشتیبان (Export)") }
                    )
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("ورود اطلاعات (Import)") }
                    )
                }

                if (activeTab == 0) {
                    Text("کد JSON کلیه رویدادها، وظایف و یادداشت‌های شما:", style = MaterialTheme.typography.bodySmall)
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
                        Text(if (copiedMessage) "کپی شد!" else "کپی کد پشتیبان در حافظه")
                    }
                } else {
                    Text("کد پشتیبان JSON را در کادر زیر جای‌گذاری (Paste) کنید:", style = MaterialTheme.typography.bodySmall)
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
                        Text("بازیابی و ادغام اطلاعات")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
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
