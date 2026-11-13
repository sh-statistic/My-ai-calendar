package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.ViewTreeObserver
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.example.calendar.EventRecurrenceConfig
import com.example.calendar.RecurrenceEndType
import com.example.calendar.RecurrenceFrequency
import com.example.calendar.PersianOccasion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import java.io.File
import com.example.ui.screens.getAdaptiveNoteColor
import com.example.ui.screens.getAdaptiveNoteBorder
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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

@Composable
fun modernDialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun DialogSectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernDialogFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
            selectedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
    )
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

/**
 * قالب‌بندی خوانا و شکیل تاریخ شمسی جهت نمایش دکمه
 */
fun formatUntilDateDisplay(pDate: String, isFa: Boolean): String {
    val parts = PersianCalendarHelper.normalizePersianDate(pDate).split("/").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return pDate
    val y = parts[0]
    val m = parts[1]
    val d = parts[2]
    val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrElse(m - 1) { "" }
    return if (isFa) {
        "${PersianCalendarHelper.toPersianDigits(d.toString())} $monthName ${PersianCalendarHelper.toPersianDigits(y.toString())}"
    } else {
        "$d $monthName $y"
    }
}

/**
 * دیالوگ انتخاب تقویم شمسی با انتخابگر سال، ماه و روز
 * کاملاً لمسی و روان بدون نیاز به تایپ دستی یا باز شدن کیبورد
 */
@Composable
fun PersianDatePickerDialog(
    initialDateStr: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = rememberAppStrings()
    val parts = PersianCalendarHelper.normalizePersianDate(initialDateStr).split("/").mapNotNull { it.toIntOrNull() }
    val initialYear = if (parts.size == 3) parts[0] else 1405
    val initialMonth = if (parts.size == 3) parts[1] else 1
    val initialDay = if (parts.size == 3) parts[2] else 1

    var year by remember { mutableIntStateOf(initialYear) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var day by remember { mutableIntStateOf(initialDay) }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    val daysInMonth = PersianCalendarHelper.getDaysInJalaliMonth(year, month)
    if (day > daysInMonth) {
        day = daysInMonth
    }

    val yearsList = (initialYear - 1..initialYear + 10).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (strings.isFa) "انتخاب تاریخ پایان" else "Select End Date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // پیش‌نمایش تاریخ انتخابی
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val monthName = strings.jalaliMonths.getOrElse(month - 1) { "" }
                    val displayStr = if (strings.isFa) {
                        "${PersianCalendarHelper.toPersianDigits(day.toString())} $monthName ${PersianCalendarHelper.toPersianDigits(year.toString())}"
                    } else {
                        "$day $monthName $year"
                    }
                    Text(
                        text = displayStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }

                // ردیف سه سلکتور: روز، ماه، سال
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ۱. روز
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dayMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (strings.isFa) PersianCalendarHelper.toPersianDigits(day.toString()) else "$day",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = dayMenuExpanded,
                            onDismissRequest = { dayMenuExpanded = false }
                        ) {
                            for (d in 1..daysInMonth) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (strings.isFa) PersianCalendarHelper.toPersianDigits(d.toString()) else d.toString(),
                                            fontWeight = if (d == day) FontWeight.Bold else FontWeight.Normal,
                                            color = if (d == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        day = d
                                        dayMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // ۲. ماه
                    Box(modifier = Modifier.weight(1.3f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { monthMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.jalaliMonths.getOrElse(month - 1) { "" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            strings.jalaliMonths.forEachIndexed { index, mName ->
                                val m = index + 1
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            mName,
                                            fontWeight = if (m == month) FontWeight.Bold else FontWeight.Normal,
                                            color = if (m == month) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        month = m
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // ۳. سال
                    Box(modifier = Modifier.weight(1.1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { yearMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (strings.isFa) PersianCalendarHelper.toPersianDigits(year.toString()) else "$year",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false }
                        ) {
                            yearsList.forEach { y ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (strings.isFa) PersianCalendarHelper.toPersianDigits(y.toString()) else y.toString(),
                                            fontWeight = if (y == year) FontWeight.Bold else FontWeight.Normal,
                                            color = if (y == year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        year = y
                                        yearMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = "%04d/%02d/%02d".format(year, month, day)
                    onConfirm(formatted)
                }
            ) {
                Text(if (strings.isFa) "تأیید تاریخ" else "Confirm Date")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

/**
 * دیالوگ پیشرفته انتخاب و سفارشی‌سازی تکرار رویداد:
 * دوره‌ها (بدون تکرار، هر روز، هر هفته، هر ماه، هر سال)
 * شرایط پایان (برای همیشه، تا تاریخ، بعد از N تکرار)
 */
@Composable
fun EventRecurrenceDialog(
    initialConfig: EventRecurrenceConfig,
    eventDate: String,
    onDismiss: () -> Unit,
    onConfirm: (EventRecurrenceConfig) -> Unit
) {
    val strings = rememberAppStrings()
    var frequency by remember { mutableStateOf(initialConfig.frequency) }
    var endType by remember { mutableStateOf(initialConfig.endType) }
    var untilDate by remember {
        mutableStateOf(
            if (initialConfig.untilDate.isNotBlank()) initialConfig.untilDate
            else {
                val parts = PersianCalendarHelper.normalizePersianDate(eventDate).split("/").mapNotNull { it.toIntOrNull() }
                if (parts.size == 3) "%04d/%02d/%02d".format(parts[0] + 1, parts[1], parts[2]) else eventDate
            }
        )
    }
    var count by remember { mutableIntStateOf(if (initialConfig.count > 0) initialConfig.count else 10) }
    var showUntilDatePicker by remember { mutableStateOf(false) }

    if (showUntilDatePicker) {
        PersianDatePickerDialog(
            initialDateStr = untilDate,
            onDismiss = { showUntilDatePicker = false },
            onConfirm = { selectedFormattedDate ->
                untilDate = selectedFormattedDate
                endType = RecurrenceEndType.UNTIL_DATE
                showUntilDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (strings.isFa) "تکرار" else "Repeat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
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
                // بخش انتخاب فرکانس و دوره تکرار
                val frequencies = listOf(
                    RecurrenceFrequency.NONE,
                    RecurrenceFrequency.DAILY,
                    RecurrenceFrequency.WEEKLY,
                    RecurrenceFrequency.MONTHLY,
                    RecurrenceFrequency.YEARLY
                )

                frequencies.forEach { freq ->
                    val isSelected = frequency == freq
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { frequency = freq }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isFa) freq.titleFa else freq.titleEn,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { frequency = freq }
                        )
                    }
                }

                // بخش شرایط پایان تکرار (فقط زمانی که گزینه غیر از «بدون تکرار» است)
                if (frequency != RecurrenceFrequency.NONE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (strings.isFa) "پایان تکرار" else "End repeat",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // ۱. تکرار برای همیشه
                    val isNeverSelected = endType == RecurrenceEndType.NEVER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { endType = RecurrenceEndType.NEVER }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isFa) "تکرار برای همیشه" else "Never ends",
                            fontSize = 13.sp,
                            fontWeight = if (isNeverSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isNeverSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        RadioButton(
                            selected = isNeverSelected,
                            onClick = { endType = RecurrenceEndType.NEVER }
                        )
                    }

                    // ۲. تا تاریخ مشخص (انتخاب با تقویم لمسی بدون کیبورد)
                    val isUntilSelected = endType == RecurrenceEndType.UNTIL_DATE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                endType = RecurrenceEndType.UNTIL_DATE
                                showUntilDatePicker = true
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (strings.isFa) "تا تاریخ" else "Until date",
                                fontSize = 13.sp,
                                fontWeight = if (isUntilSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isUntilSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                onClick = {
                                    endType = RecurrenceEndType.UNTIL_DATE
                                    showUntilDatePicker = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isUntilSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isUntilSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isUntilSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = formatUntilDateDisplay(untilDate, strings.isFa),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUntilSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        RadioButton(
                            selected = isUntilSelected,
                            onClick = {
                                endType = RecurrenceEndType.UNTIL_DATE
                                showUntilDatePicker = true
                            }
                        )
                    }

                    // ۳. بعد از N تکرار (شمارنده عددی لمسی بدون کیبورد)
                    val isCountSelected = endType == RecurrenceEndType.AFTER_COUNT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { endType = RecurrenceEndType.AFTER_COUNT }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (strings.isFa) "بعد از" else "After",
                                fontSize = 13.sp,
                                fontWeight = if (isCountSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCountSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            // دکمه‌های استپر لمسی (-) و (+)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(enabled = count > 1) {
                                            if (count > 1) count--
                                            endType = RecurrenceEndType.AFTER_COUNT
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                }

                                Surface(
                                    color = if (isCountSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isCountSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = if (strings.isFa) PersianCalendarHelper.toPersianDigits(count.toString()) else count.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCountSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable(enabled = count < 365) {
                                            if (count < 365) count++
                                            endType = RecurrenceEndType.AFTER_COUNT
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }

                            Text(
                                text = if (strings.isFa) "تکرار" else "times",
                                fontSize = 13.sp,
                                color = if (isCountSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        RadioButton(
                            selected = isCountSelected,
                            onClick = { endType = RecurrenceEndType.AFTER_COUNT }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalConfig = if (frequency == RecurrenceFrequency.NONE) {
                        EventRecurrenceConfig(frequency = RecurrenceFrequency.NONE)
                    } else {
                        EventRecurrenceConfig(
                            frequency = frequency,
                            endType = endType,
                            untilDate = untilDate.trim(),
                            count = count
                        )
                    }
                    onConfirm(finalConfig)
                }
            ) {
                Text(if (strings.isFa) "ذخیره" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
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
        colorHex: String,
        isWeeklyRecurring: Boolean,
        recurrenceConfig: EventRecurrenceConfig
    ) -> Unit
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var persianDate by remember { mutableStateOf(initialDate.formatted) }
    var isAllDay by remember { mutableStateOf(false) }
    var recurrenceConfig by remember { mutableStateOf(EventRecurrenceConfig()) }
    var showRecurrenceDialog by remember { mutableStateOf(false) }
    var eventTime by remember { mutableStateOf("10:00") }
    var reminderOption by remember { mutableStateOf("همزمان با رویداد") }
    var category by remember { mutableStateOf("کاری") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }

    val categories = listOf("کاری", "شخصی", "مهم", "جلسه", "یادآوری")
    val reminderOptions = listOf("بدون زنگ", "همزمان با رویداد", "۵ دقیقه قبل", "۱۰ دقیقه قبل", "۱۵ دقیقه قبل", "۳۰ دقیقه قبل", "۱ ساعت قبل", "۱ روز قبل")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")

    if (showRecurrenceDialog) {
        EventRecurrenceDialog(
            initialConfig = recurrenceConfig,
            eventDate = persianDate,
            onDismiss = { showRecurrenceDialog = false },
            onConfirm = { newConfig ->
                recurrenceConfig = newConfig
                showRecurrenceDialog = false
            }
        )
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد *" else "Event Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "توضیحات رویداد (اختیاری)" else "Description (optional)") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ شمسی (مثال: ۱۴۰۵/۰۱/۱۵)" else "Solar Date (e.g. 1405/01/15)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // کارت زمان و تکرار رویداد
                DialogSectionCard(
                    title = if (strings.isFa) "زمان و تکرار رویداد:" else "Time & Recurrence:",
                    icon = Icons.Default.Schedule
                ) {
                    // All-day switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isFa) "رویداد تمام‌روز (بدون ساعت خاص)" else "All-day event (no specific time)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it }
                        )
                    }

                    // باکس تکرار رویداد با قابلیت سفارشی‌سازی کامل
                    Surface(
                        color = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showRecurrenceDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (strings.isFa) "تکرار رویداد" else "Repeat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = recurrenceConfig.getSummaryText(strings.isFa),
                                        fontSize = 10.sp,
                                        fontWeight = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE) FontWeight.Bold else FontWeight.Normal,
                                        color = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (strings.isFa) "تغییر" else "Change",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (recurrenceConfig.frequency != RecurrenceFrequency.NONE)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
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
                        Text(
                            text = if (strings.isFa) "⏰ زنگ و زمان یادآوری هشدار:" else "⏰ Reminder & Alarm:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            reminderOptions.forEach { option ->
                                ModernDialogFilterChip(
                                    selected = reminderOption == option,
                                    onClick = { reminderOption = option },
                                    label = reminderOptionLabel(option, strings.isFa),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // کارت دسته‌بندی
                DialogSectionCard(
                    title = if (strings.isFa) "دسته‌بندی رویداد:" else "Event Category:",
                    icon = Icons.Default.Category
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            ModernDialogFilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = eventCategoryLabel(cat, strings.isFa),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // کارت رنگ نشانگر
                DialogSectionCard(
                    title = if (strings.isFa) "رنگ نشانگر رویداد:" else "Event Marker Color:",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { hex ->
                            val col = parseColor(hex)
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { selectedColor = hex }
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
                        onConfirm(
                            title.trim(),
                            descWithReminder,
                            persianDate.trim(),
                            finalTime,
                            category,
                            selectedColor,
                            recurrenceConfig.frequency == RecurrenceFrequency.WEEKLY,
                            recurrenceConfig
                        )
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد *" else "Event Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "توضیحات رویداد" else "Description") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ شمسی" else "Solar Date") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // کارت زمان و یادآوری رویداد
                DialogSectionCard(
                    title = if (strings.isFa) "زمان و تکرار رویداد:" else "Time & Recurrence:",
                    icon = Icons.Default.Schedule
                ) {
                    // All-day switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isFa) "رویداد تمام‌روز (بدون ساعت خاص)" else "All-day event (no specific time)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                    }

                    // نشان تکرار رویداد در صورت تکراری بودن
                    if (initialEvent.recurrenceId != null) {
                        val recRuleLabel = when (initialEvent.recurrenceRule) {
                            "DAILY" -> if (strings.isFa) "تکرار روزانه" else "Daily repeat"
                            "WEEKLY" -> if (strings.isFa) "تکرار هفتگی" else "Weekly repeat"
                            "MONTHLY" -> if (strings.isFa) "تکرار ماهانه" else "Monthly repeat"
                            "YEARLY" -> if (strings.isFa) "تکرار سالانه" else "Yearly repeat"
                            else -> if (strings.isFa) "رویداد تکرارشونده" else "Recurring event"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = recRuleLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                        Text(
                            text = if (strings.isFa) "⏰ زمان زنگ و یادآوری هشدار:" else "⏰ Reminder & Alarm:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            reminderOptions.forEach { option ->
                                ModernDialogFilterChip(
                                    selected = reminderOption == option,
                                    onClick = { reminderOption = option },
                                    label = reminderOptionLabel(option, strings.isFa),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // کارت دسته‌بندی
                DialogSectionCard(
                    title = if (strings.isFa) "دسته‌بندی رویداد:" else "Event Category:",
                    icon = Icons.Default.Category
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            ModernDialogFilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = eventCategoryLabel(cat, strings.isFa),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // کارت رنگ نشانگر
                DialogSectionCard(
                    title = if (strings.isFa) "رنگ نشانگر رویداد:" else "Marker Color:",
                    icon = Icons.Default.Palette
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { hex ->
                            val col = parseColor(hex)
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { selectedColor = hex }
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
 * Dialog to delete recurring events with 3 specific options:
 * 1. This event only
 * 2. This and all future recurring events from this date forward
 * 3. All events in this recurring series
 */
@Composable
fun DeleteRecurringEventDialog(
    onDismiss: () -> Unit,
    onDeleteSingle: () -> Unit,
    onDeleteFuture: () -> Unit,
    onDeleteAll: (() -> Unit)? = null
) {
    val strings = rememberAppStrings()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isFa) "حذف رویداد تکرارشونده" else "Delete Recurring Event",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (strings.isFa)
                        "این یک رویداد تکرارشونده است. مایلید چگونه حذف شود؟"
                    else
                        "This is a recurring event. How would you like to delete it?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // گزینه ۱: فقط همین رویداد
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteSingle() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "فقط همین رویداد" else "This event only",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (strings.isFa) "رویدادهای سایر روزها و تکرارها دست‌نخورده باقی می‌مانند" else "Other recurring dates will stay intact",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // گزینه ۲: این رویداد و تکرارهای بعدی
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteFuture() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "این رویداد و تکرارهای بعدی" else "This and following events",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (strings.isFa) "از این تاریخ به بعد تکرار متوقف شده و تمام تکرارهای آینده حذف می‌شوند" else "Stops recurring and deletes all future events from this date",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // گزینه ۳: تمامی تکرارها (کل سری)
                if (onDeleteAll != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteAll() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (strings.isFa) "تمامی تکرارها (کل سری)" else "All events in series",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (strings.isFa) "تمام تکرارهای این رویداد (گذشته و آینده) کلاً حذف می‌شوند" else "Deletes all occurrences of this recurring event series",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

/**
 * Dialog to choose the scope when saving changes to a recurring event:
 * 1. This event only
 * 2. This and all future events
 * 3. All events in the series
 */
@Composable
fun EditRecurringEventScopeDialog(
    onDismiss: () -> Unit,
    onEditSingle: () -> Unit,
    onEditFuture: () -> Unit,
    onEditAll: () -> Unit
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
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (strings.isFa) "ذخیره تغییرات رویداد تکرارشونده" else "Save Recurring Event Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (strings.isFa)
                        "این یک رویداد تکرارشونده است. مایلید تغییرات چگونه اعمال شوند؟"
                    else
                        "This is a recurring event. How would you like to apply changes?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // گزینه ۱: فقط همین رویداد
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditSingle() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "فقط همین رویداد" else "This event only",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (strings.isFa) "فقط تاریخ فعلی تغییر می‌کند و سایر تکرارها دست‌نخورده می‌مانند" else "Only this date changes; other recurrences remain unchanged",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // گزینه ۲: این رویداد و تکرارهای بعدی
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditFuture() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "این رویداد و تکرارهای بعدی" else "This and following events",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (strings.isFa) "تغییرات از این تاریخ به بعد روی تمام تکرارهای آینده اعمال می‌شوند" else "Applies changes to this date and all future recurrences",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // گزینه ۳: تمامی تکرارها
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditAll() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (strings.isFa) "تمامی تکرارها (کل سری)" else "All events in series",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (strings.isFa) "تمام دفعات این رویداد (گذشته و آینده) با متن و تنظیمات جدید به‌روز می‌شوند" else "Updates all occurrences of this recurring event series",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان رویداد / یادآور" else "Event / Reminder Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (strings.isFa) "متن یادداشت" else "Note Content") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = persianDate,
                    onValueChange = { persianDate = it },
                    label = { Text(if (strings.isFa) "تاریخ اجرای رویداد (شمسی)" else "Execution Date (Solar)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                DialogSectionCard(
                    title = if (strings.isFa) "زمان و هشدار یادآوری:" else "Time & Reminder:",
                    icon = Icons.Default.Schedule
                ) {
                    PersianTimePicker(
                        initialHour = 10,
                        initialMinute = 0,
                        onTimeChange = { _, _, formatted -> eventTime = formatted },
                        label = if (strings.isFa) "ساعت رویداد و یادآور" else "Time & Reminder"
                    )

                    // Reminder alarm options
                    Text(
                        text = if (strings.isFa) "⏰ زنگ و هشدار یادآوری:" else "⏰ Reminder & Alarm:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        reminderOptions.forEach { option ->
                            ModernDialogFilterChip(
                                selected = reminderOption == option,
                                onClick = { reminderOption = option },
                                label = reminderOptionLabel(option, strings.isFa),
                                fontSize = 10.sp
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان کار یا وظیفه *" else "Task Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(if (strings.isFa) "تاریخ موعد (شمسی)" else "Due Date (Solar)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // کارت انتخاب اولویت
                DialogSectionCard(
                    title = if (strings.isFa) "اولویت انجام:" else "Task Priority:",
                    icon = Icons.Default.Flag
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        priorities.forEach { prio ->
                            ModernDialogFilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = strings.taskPriority(prio),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // کارت دسته‌بندی موضوعی
                DialogSectionCard(
                    title = if (strings.isFa) "دسته‌بندی موضوعی:" else "Task Category:",
                    icon = Icons.Default.Category
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            ModernDialogFilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = strings.taskTag(cat),
                                fontSize = 11.sp
                            )
                        }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (strings.isFa) "عنوان کار یا وظیفه *" else "Task Title *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text(if (strings.isFa) "تاریخ موعد (شمسی)" else "Due Date (Solar)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // کارت انتخاب اولویت
                DialogSectionCard(
                    title = if (strings.isFa) "اولویت انجام:" else "Task Priority:",
                    icon = Icons.Default.Flag
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        priorities.forEach { prio ->
                            ModernDialogFilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = strings.taskPriority(prio),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // کارت دسته‌بندی موضوعی
                DialogSectionCard(
                    title = if (strings.isFa) "دسته‌بندی موضوعی:" else "Task Category:",
                    icon = Icons.Default.Category
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            ModernDialogFilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = strings.taskTag(cat),
                                fontSize = 11.sp
                            )
                        }
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

/**
 * توابع کمکی هوشمند تشخیص برچسب با علامت # مشابه Google Keep
 */
fun extractActiveHashtagQuery(text: String): String? {
    // آخرین # در متنِ قبل از نشانگر: اگر بعدش فقط حروف برچسب آمده باشد، کاربر دارد برچسب می‌نویسد.
    // قبلاً # فقط اول خط یا بعد از فاصله قبول می‌شد و وسط متنِ آماده (یادداشت موجود) شناخته نمی‌شد.
    val idx = text.lastIndexOf('#')
    if (idx < 0) return null
    val tail = text.substring(idx + 1)
    if (tail.any { !(it.isLetterOrDigit() || it == '_' || it == '\u200C') }) return null
    return tail
}

fun removeActiveHashtagFromContent(text: String): String {
    val idx = text.lastIndexOf('#')
    if (idx < 0) return text
    val tail = text.substring(idx + 1)
    if (tail.any { !(it.isLetterOrDigit() || it == '_' || it == '\u200C') }) return text
    return text.substring(0, idx).trimEnd()
}

// ارتفاع واقعی صفحه‌کلید را مستقیم از قاب قابل‌مشاهدهٔ پنجره می‌خوانیم.
// روی بعضی گوشی‌ها inset صفحه‌کلید به پنجرهٔ دیالوگ نمی‌رسد و نوار ابزار از صفحه‌کلید فاصله می‌گرفت؛
// این اندازه‌گیری روی همهٔ دستگاه‌ها درست جواب می‌دهد و وقتی پنجره خودش resize شود صفر می‌شود.
@Composable
private fun rememberKeyboardHeightDp(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    var height by remember { mutableStateOf(0.dp) }
    DisposableEffect(view) {
        val root = view.rootView
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            root.getWindowVisibleDisplayFrame(rect)
            val hidden = root.height - rect.bottom
            height = if (hidden > root.height * 0.15) with(density) { hidden.toDp() } else 0.dp
        }
        root.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { root.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return height
}

@Composable
private fun rememberEditorBottomPadding(): Dp {
    val keyboardHeight = rememberKeyboardHeightDp()
    val density = LocalDensity.current
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    return if (keyboardHeight > 0.dp) keyboardHeight else navBottom
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String, tag: String, isPinned: Boolean, imagePath: String?, checklistJson: String, audioPath: String?, reminderAt: Long, blocksJson: String) -> Unit,
    existingTags: List<String> = emptyList(),
    initialTitle: String = "",
    initialContent: String = "",
    initialImagePath: String? = null
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf(initialTitle) }
    var selectedColor by remember { mutableStateOf("#FEF3C7") }
    var isPinned by remember { mutableStateOf(false) }
    // محتوای سندمحور: متن/عکس/نقاشی در یک جریان (مثل ورد و مقالهٔ تلگرام)
    val blocks = remember {
        mutableStateListOf<NoteBlockState>().apply {
            addAll(buildInitialNoteBlocks("", initialContent, initialImagePath, null))
        }
    }
    var focusedTextBlockId by remember { mutableStateOf<String?>(null) }
    var drawingBlock by remember { mutableStateOf<NoteBlockState?>(null) }
    val tagsList = remember { mutableStateListOf<String>() }

    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")
    val defaultSuggestions = listOf("ایده", "شخصی", "کاری", "مهم", "مطالعه", "پروژه", "خرید")
    // ترکیب تگ‌های پیش‌فرض با تگ‌های قبلی کاربر (داینامیک)
    val allSuggestions = remember(existingTags) {
        (defaultSuggestions + existingTags).distinct()
    }

    var reminderAt by remember { mutableStateOf(0L) }
    var showAddMenu by remember { mutableStateOf(false) }

    // درج بلوک تازه (عکس/نقاشی) دقیقاً در جای نشانگر متن فعال؛ متن از همان نقطه دو تکه می‌شود
    fun insertBlocksAtFocus(newBlocks: List<NoteBlockState>) {
        if (newBlocks.isEmpty()) return
        val focusIdx = blocks.indexOfFirst { it.id == focusedTextBlockId && it.kind == "t" }
        if (focusIdx >= 0) {
            val fb = blocks[focusIdx]
            val sel = fb.tfv.selection.start.coerceIn(0, fb.tfv.text.length)
            val before = fb.tfv.text.substring(0, sel)
            val after = fb.tfv.text.substring(sel)
            fb.tfv = androidx.compose.ui.text.input.TextFieldValue(
                before,
                androidx.compose.ui.text.TextRange(before.length)
            )
            var insertAt = focusIdx + 1
            for (nb in newBlocks) {
                blocks.add(insertAt, nb)
                insertAt++
            }
            blocks.add(insertAt, NoteBlockState(kind = "t", text = after))
        } else {
            for (nb in newBlocks) blocks.add(nb)
            blocks.add(NoteBlockState(kind = "t"))
        }
    }

    var savedOnce by remember { mutableStateOf(false) }
    val handleExit: () -> Unit = {
        val hasAnything = noteBlocksHaveContent(blocks) || title.isNotBlank() || tagsList.isNotEmpty() ||
            reminderAt > 0
        if (!savedOnce && hasAnything) {
            savedOnce = true
            onConfirm(
                title.trim(), noteBlocksToContent(blocks), selectedColor, tagsList.joinToString(","), isPinned,
                noteBlocksFirstPath(blocks, "i"), noteBlocksChecklistJson(blocks), noteBlocksFirstPath(blocks, "a"),
                reminderAt, noteBlocksToJson(blocks)
            )
        }
        onDismiss()
    }

    val editorBgAdd = getAdaptiveNoteColor(
        selectedColor,
        MaterialTheme.colorScheme.background.luminance() < 0.5f
    )

    val contextAdd = LocalContext.current

    val scopeAdd = androidx.compose.runtime.rememberCoroutineScope()
    val photoPickerAdd = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scopeAdd.launch {
                val paths = uris.mapNotNull { copyUriToNotesImages(contextAdd, it) }
                if (paths.isNotEmpty()) {
                    insertBlocksAtFocus(paths.map { NoteBlockState(kind = "i", path = it) })
                }
            }
        }
    }

    Dialog(
        onDismissRequest = handleExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = editorBgAdd) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = rememberEditorBottomPadding())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleExit) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val shareBody = buildString {
                                if (title.isNotBlank()) {
                                    appendLine(title.trim())
                                    appendLine()
                                }
                                append(noteBlocksToContent(blocks))
                            }
                            com.example.sync.NoteShareHelper.shareText(contextAdd, title.trim(), shareBody)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = if (strings.isFa) "اشتراک‌گذاری" else "Share")
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = strings.noteTitleLabel,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(2.dp)
                        .offset(y = (-2).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                )

                // پاپ‌آپ هوشمند ایجاد و انتخاب برچسب با # (دقیقاً مشابه Google Keep)
                val focusBlock = blocks.firstOrNull { it.id == focusedTextBlockId && it.kind == "t" } ?: blocks.lastOrNull { it.kind == "t" }
                val focusCursor = focusBlock?.tfv?.selection?.start?.coerceIn(0, focusBlock.tfv.text.length) ?: 0
                val focusPrefix = focusBlock?.tfv?.text?.substring(0, focusCursor) ?: ""
                val activeHashtag = extractActiveHashtagQuery(focusPrefix)
                if (activeHashtag != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (activeHashtag.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        val newTag = activeHashtag.trim()
                                        if (newTag.isNotBlank() && !tagsList.contains(newTag)) {
                                            tagsList.add(newTag)
                                        }
                                        if (focusBlock != null) {
                                            val cleanedPrefix = removeActiveHashtagFromContent(focusPrefix)
                                            val suffix = focusBlock.tfv.text.substring(focusCursor)
                                            focusBlock.tfv = androidx.compose.ui.text.input.TextFieldValue(cleanedPrefix + suffix, androidx.compose.ui.text.TextRange(cleanedPrefix.length))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (strings.isFa) "ایجاد برچسب: $activeHashtag" else "Create: $activeHashtag",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            val suggestions = allSuggestions.filter {
                                it.contains(activeHashtag, ignoreCase = true) && !tagsList.contains(it)
                            }
                            suggestions.take(5).forEach { cand ->
                                Surface(
                                    onClick = {
                                        if (!tagsList.contains(cand)) {
                                            tagsList.add(cand)
                                        }
                                        if (focusBlock != null) {
                                            val cleanedPrefix = removeActiveHashtagFromContent(focusPrefix)
                                            val suffix = focusBlock.tfv.text.substring(focusCursor)
                                            focusBlock.tfv = androidx.compose.ui.text.input.TextFieldValue(cleanedPrefix + suffix, androidx.compose.ui.text.TextRange(cleanedPrefix.length))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "#",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (cand == "ایده") "💡 $cand" else cand,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                // نمایش برچسب‌های الصاق‌شده به یادداشت
                if (tagsList.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagsList.forEach { tagItem ->
                            val isIdea = tagItem == "ایده"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isIdea) {
                                    if (isDark) Color(0xFF3B2E1E) else Color(0xFFFEF3C7)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                },
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isIdea) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isIdea) "💡 $tagItem" else tagItem,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIdea) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { tagsList.remove(tagItem) }
                                    )
                                }
                            }
                        }
                    }
                }

                NoteBlocksEditor(
                    blocks = blocks,
                    isFa = strings.isFa,
                    onChanged = {},
                    onFocusedTextBlock = { focusedTextBlockId = it },
                    onOpenDrawing = { drawingBlock = it },
                    modifier = Modifier.weight(1f)
                )

                if (reminderAt > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isFa) "یادآور: ${formatNoteReminderLabel(reminderAt)}" else "Reminder: ${formatNoteReminderLabel(reminderAt)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { reminderAt = 0L }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDarkBar = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    noteColors.forEach { hex ->
                        val col = getAdaptiveNoteColor(hex, isDarkBar)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDarkBar) Color.White else Color(0xFF374151),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showAddMenu = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = if (strings.isFa) "افزودن به یادداشت" else "Add to note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن نقاشی" else "Add drawing", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    val nb = NoteBlockState(kind = "d")
                                    insertBlocksAtFocus(listOf(nb))
                                    drawingBlock = nb
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن عکس" else "Add photo", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    photoPickerAdd.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن چک‌لیست" else "Add checklist", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    val nb = NoteBlockState(kind = "c")
                                    nb.checklistItems.add(ChecklistItem("", false))
                                    insertBlocksAtFocus(listOf(nb))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن صدا" else "Add voice", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    insertBlocksAtFocus(listOf(NoteBlockState(kind = "a")))
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        showNoteReminderPicker(contextAdd, reminderAt) { picked -> reminderAt = picked }
                    }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = if (strings.isFa) "یادآور" else "Reminder",
                            tint = if (reminderAt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (strings.isFa) "سنجاق به بالا" else "Pin to top",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // بوم نقاشی روی خود ویرایشگر باز می‌شود؛ با برگشت، کاربر به همین یادداشت برمی‌گردد
    drawingBlock?.let { block ->
        WhiteboardDrawingDialog(
            initialTitle = title,
            initialDrawingPath = block.path,
            onDismiss = { drawingBlock = null },
            onSaveDrawing = { _, path, _ ->
                block.path = path
                drawingBlock = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNoteDialog(
    initialNote: NoteEntity,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, colorHex: String, isPinned: Boolean, drawingPath: String?, tag: String, imagePath: String?, checklistJson: String, audioPath: String?, reminderAt: Long, blocksJson: String) -> Unit,
    onAutosave: ((title: String, content: String, colorHex: String, isPinned: Boolean, drawingPath: String?, tag: String, imagePath: String?, checklistJson: String, audioPath: String?, reminderAt: Long, blocksJson: String) -> Unit)? = null,
    existingTags: List<String> = emptyList()
) {
    val strings = rememberAppStrings()
    var title by remember { mutableStateOf(initialNote.title) }
    var selectedColor by remember { mutableStateOf(initialNote.colorHex) }
    val initialTags = remember(initialNote.tag) {
        initialNote.tag.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val tagsList = remember { mutableStateListOf<String>().apply { addAll(initialTags) } }
    var isPinned by remember { mutableStateOf(initialNote.isPinned) }
    // محتوای سندمحور: متن/عکس/نقاشی در یک جریان (مثل ورد و مقالهٔ تلگرام)
    val blocks = remember {
        mutableStateListOf<NoteBlockState>().apply {
            addAll(
                buildInitialNoteBlocks(
                    initialNote.blocksJson, initialNote.content, initialNote.imagePath, initialNote.drawingPath,
                    initialNote.checklistJson, initialNote.audioPath
                )
            )
        }
    }
    val initialBlocksSignature = remember { noteBlocksSignature(blocks) }
    var focusedTextBlockId by remember { mutableStateOf<String?>(null) }
    var drawingBlock by remember { mutableStateOf<NoteBlockState?>(null) }
    // صدا حالا بلوک درون‌خطی است (kind = "a")
    // چک‌لیست حالا بلوک درون‌خطی است و از buildInitialNoteBlocks می‌آید
    var reminderAt by remember { mutableStateOf(initialNote.reminderAt) }
    var showAddMenu by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val noteColors = listOf("#FEF3C7", "#DCFCE7", "#E0F2FE", "#FCE7F3", "#F3E8FF", "#F1F5F9")
    val defaultSuggestions = listOf("ایده", "شخصی", "کاری", "مهم", "مطالعه", "پروژه", "خرید")
    // ترکیب تگ‌های پیش‌فرض با تگ‌های قبلی کاربر (داینامیک)
    val allSuggestions = remember(existingTags) {
        (defaultSuggestions + existingTags).distinct()
    }

    // درج بلوک تازه (عکس/نقاشی) دقیقاً در جای نشانگر متن فعال؛ متن از همان نقطه دو تکه می‌شود
    fun insertBlocksAtFocus(newBlocks: List<NoteBlockState>) {
        if (newBlocks.isEmpty()) return
        val focusIdx = blocks.indexOfFirst { it.id == focusedTextBlockId && it.kind == "t" }
        if (focusIdx >= 0) {
            val fb = blocks[focusIdx]
            val sel = fb.tfv.selection.start.coerceIn(0, fb.tfv.text.length)
            val before = fb.tfv.text.substring(0, sel)
            val after = fb.tfv.text.substring(sel)
            fb.tfv = androidx.compose.ui.text.input.TextFieldValue(
                before,
                androidx.compose.ui.text.TextRange(before.length)
            )
            var insertAt = focusIdx + 1
            for (nb in newBlocks) {
                blocks.add(insertAt, nb)
                insertAt++
            }
            blocks.add(insertAt, NoteBlockState(kind = "t", text = after))
        } else {
            for (nb in newBlocks) blocks.add(nb)
            blocks.add(NoteBlockState(kind = "t"))
        }
    }

    val blocksSignature = noteBlocksSignature(blocks)
    val isDirty = title != initialNote.title || blocksSignature != initialBlocksSignature ||
        selectedColor != initialNote.colorHex || isPinned != initialNote.isPinned ||
        tagsList.toList() != initialTags ||
        reminderAt != initialNote.reminderAt

    LaunchedEffect(
        title, blocksSignature, selectedColor, isPinned, tagsList.toList(),
        reminderAt
    ) {
        if (isDirty && onAutosave != null) {
            delay(800)
            onAutosave(
                title, noteBlocksToContent(blocks), selectedColor, isPinned,
                noteBlocksFirstPath(blocks, "d"), tagsList.joinToString(","),
                noteBlocksFirstPath(blocks, "i"), noteBlocksChecklistJson(blocks),
                noteBlocksFirstPath(blocks, "a"), reminderAt, noteBlocksToJson(blocks)
            )
        }
    }

    var exitSaved by remember { mutableStateOf(false) }
    val handleExit: () -> Unit = {
        if (!exitSaved) {
            exitSaved = true
            if (isDirty) {
                val canSave = noteBlocksHaveContent(blocks) || title.isNotBlank()
                if (canSave) {
                    onConfirm(
                        title.trim(), noteBlocksToContent(blocks), selectedColor, isPinned,
                        noteBlocksFirstPath(blocks, "d"),
                        tagsList.joinToString(","), noteBlocksFirstPath(blocks, "i"),
                        noteBlocksChecklistJson(blocks), noteBlocksFirstPath(blocks, "a"), reminderAt,
                        noteBlocksToJson(blocks)
                    )
                }
            }
        }
        onDismiss()
    }

    val editorBgEdit = getAdaptiveNoteColor(selectedColor, isDark)

    val contextEdit = LocalContext.current

    val scopeEdit = androidx.compose.runtime.rememberCoroutineScope()
    val photoPickerEdit = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scopeEdit.launch {
                val paths = uris.mapNotNull { copyUriToNotesImages(contextEdit, it) }
                if (paths.isNotEmpty()) {
                    insertBlocksAtFocus(paths.map { NoteBlockState(kind = "i", path = it) })
                }
            }
        }
    }

    Dialog(
        onDismissRequest = handleExit,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = editorBgEdit) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(bottom = rememberEditorBottomPadding())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleExit) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val shareBody = buildString {
                                if (title.isNotBlank()) {
                                    appendLine(title.trim())
                                    appendLine()
                                }
                                append(noteBlocksToContent(blocks))
                            }
                            com.example.sync.NoteShareHelper.shareText(contextEdit, title.trim(), shareBody)
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = if (strings.isFa) "اشتراک‌گذاری" else "Share")
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    text = strings.noteTitleLabel,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(2.dp)
                        .offset(y = (-2).dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                )

                // پاپ‌آپ هوشمند ایجاد و انتخاب برچسب با # (دقیقاً مشابه Google Keep)
                val focusBlock = blocks.firstOrNull { it.id == focusedTextBlockId && it.kind == "t" } ?: blocks.lastOrNull { it.kind == "t" }
                val focusCursor = focusBlock?.tfv?.selection?.start?.coerceIn(0, focusBlock.tfv.text.length) ?: 0
                val focusPrefix = focusBlock?.tfv?.text?.substring(0, focusCursor) ?: ""
                val activeHashtag = extractActiveHashtagQuery(focusPrefix)
                if (activeHashtag != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (activeHashtag.isNotBlank()) {
                                Surface(
                                    onClick = {
                                        val newTag = activeHashtag.trim()
                                        if (newTag.isNotBlank() && !tagsList.contains(newTag)) {
                                            tagsList.add(newTag)
                                        }
                                        if (focusBlock != null) {
                                            val cleanedPrefix = removeActiveHashtagFromContent(focusPrefix)
                                            val suffix = focusBlock.tfv.text.substring(focusCursor)
                                            focusBlock.tfv = androidx.compose.ui.text.input.TextFieldValue(cleanedPrefix + suffix, androidx.compose.ui.text.TextRange(cleanedPrefix.length))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (strings.isFa) "ایجاد برچسب: $activeHashtag" else "Create: $activeHashtag",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            val suggestions = allSuggestions.filter {
                                it.contains(activeHashtag, ignoreCase = true) && !tagsList.contains(it)
                            }
                            suggestions.take(5).forEach { cand ->
                                Surface(
                                    onClick = {
                                        if (!tagsList.contains(cand)) {
                                            tagsList.add(cand)
                                        }
                                        if (focusBlock != null) {
                                            val cleanedPrefix = removeActiveHashtagFromContent(focusPrefix)
                                            val suffix = focusBlock.tfv.text.substring(focusCursor)
                                            focusBlock.tfv = androidx.compose.ui.text.input.TextFieldValue(cleanedPrefix + suffix, androidx.compose.ui.text.TextRange(cleanedPrefix.length))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "#",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (cand == "ایده") "💡 $cand" else cand,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // نمایش برچسب‌های الصاق‌شده به یادداشت
                if (tagsList.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagsList.forEach { tagItem ->
                            val isIdea = tagItem == "ایده"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isIdea) {
                                    if (isDark) Color(0xFF3B2E1E) else Color(0xFFFEF3C7)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                },
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isIdea) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = if (isIdea) "💡 $tagItem" else tagItem,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIdea) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { tagsList.remove(tagItem) }
                                    )
                                }
                            }
                        }
                    }
                }

                NoteBlocksEditor(
                    blocks = blocks,
                    isFa = strings.isFa,
                    onChanged = {},
                    onFocusedTextBlock = { focusedTextBlockId = it },
                    onOpenDrawing = { drawingBlock = it },
                    modifier = Modifier.weight(1f)
                )
                if (reminderAt > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (strings.isFa) "یادآور: ${formatNoteReminderLabel(reminderAt)}" else "Reminder: ${formatNoteReminderLabel(reminderAt)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { reminderAt = 0L }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    noteColors.forEach { hex ->
                        val col = getAdaptiveNoteColor(hex, isDark)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDark) Color.White else Color(0xFF374151),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { showAddMenu = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = if (strings.isFa) "افزودن به یادداشت" else "Add to note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن نقاشی" else "Add drawing", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    val nb = NoteBlockState(kind = "d")
                                    insertBlocksAtFocus(listOf(nb))
                                    drawingBlock = nb
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن عکس" else "Add photo", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    photoPickerEdit.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن چک‌لیست" else "Add checklist", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    val nb = NoteBlockState(kind = "c")
                                    nb.checklistItems.add(ChecklistItem("", false))
                                    insertBlocksAtFocus(listOf(nb))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (strings.isFa) "افزودن صدا" else "Add voice", fontSize = 13.sp) },
                                onClick = {
                                    showAddMenu = false
                                    insertBlocksAtFocus(listOf(NoteBlockState(kind = "a")))
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        showNoteReminderPicker(contextEdit, reminderAt) { picked -> reminderAt = picked }
                    }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = if (strings.isFa) "یادآور" else "Reminder",
                            tint = if (reminderAt > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (strings.isFa) "سنجاق به بالا" else "Pin to top",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // بوم نقاشی روی خود ویرایشگر باز می‌شود؛ با برگشت، کاربر به همین یادداشت برمی‌گردد
    drawingBlock?.let { block ->
        WhiteboardDrawingDialog(
            initialTitle = title,
            initialDrawingPath = block.path,
            onDismiss = { drawingBlock = null },
            onSaveDrawing = { _, path, _ ->
                block.path = path
                drawingBlock = null
            }
        )
    }
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
                Text(
                    text = if (strings.isFa) "راهنمای اتصال به افزونه مرورگر کروم" else "Connect to Chrome Extension Guide",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Address Card
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
                        Column {
                            Text(
                                text = if (strings.isFa) "آدرس سرور گوشی در شبکه محلی:" else "Local Server URL:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = url,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(url)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = if (strings.isFa) "کپی آدرس" else "Copy URL")
                        }
                    }
                }

                // Method 1: Cable USB
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (strings.isFa) "🔌 روش ۱: با کابل USB (سریع‌ترین و ۱۰۰٪ پایدار)" else "🔌 Method 1: USB Cable (Fast & Stable)",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (strings.isFa)
                                "۱. کابل را بین گوشی و کامپیوتر متصل کنید.\n۲. در تنظیمات گوشی (Settings > Connections) گزینه «اتصال با کابل (USB Tethering)» را روشن کنید.\n۳. در افزونه کروم دکمه «اتصال با کابل (USB)» را بزنید."
                            else
                                "1. Connect USB cable to PC.\n2. In Phone Settings > Connections, turn on 'USB Tethering'.\n3. In Chrome extension click 'Connect via Cable (USB)'.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Method 2: Wi-Fi
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (strings.isFa) "📶 روش ۲: بی‌سیم با Wi-Fi یا هات‌اسپات" else "📶 Method 2: Wireless via Wi-Fi / Hotspot",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (strings.isFa)
                                "۱. گوشی و لپ‌تاپ به یک مودم Wi-Fi متصل باشند (یا هات‌اسپات گوشی را روشن کنید).\n۲. در افزونه کروم دکمه «جستجوی خودکار گوشی» را بزنید یا آدرس بالا را درج کنید.\n۳. دکمه همگام‌سازی فوری را لمس کنید."
                            else
                                "1. Connect phone and PC to same Wi-Fi (or turn on phone Hotspot).\n2. In Chrome extension click 'Auto Scan' or enter the URL above.\n3. Click Start Sync.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }

                // VPN Notice
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (strings.isFa)
                                "⚠️ نکته فیلترشکن لپ‌تاپ: اگر VPN روی رایانه روشن است، اتصال محلی مسدود می‌شود. موقع همگام‌سازی چند ثانیه فیلترشکن را قطع کنید یا در تنظیمات آن گزینه Allow LAN Traffic را فعال کنید."
                            else
                                "⚠️ PC VPN Tip: If VPN is ON on PC, disconnect it for 3 seconds or enable 'Allow LAN Traffic' in VPN settings.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var importText by remember { mutableStateOf("") }
    var activeTab by remember(jsonString) { mutableStateOf(if (jsonString.isBlank()) 1 else 0) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var copiedMessage by remember { mutableStateOf(false) }
    val detectedAutoBackup = remember {
        com.example.update.AppUpdateManager.getLatestSafetyBackup(context)
    }

    val openBackupFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    importText = content
                    onImport(content)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        if (strings.isFa) "فایل انتخاب‌شده خالی است" else "Selected file is empty",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    if (strings.isFa) "خطا در خواندن فایل: ${e.localizedMessage}" else "Error reading file: ${e.localizedMessage}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val saveBackupFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && jsonString.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(
                    context,
                    if (strings.isFa) "فایل پشتیبان با موفقیت ذخیره شد ✓" else "Backup file saved successfully ✓",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    if (strings.isFa) "خطا در ذخیره فایل: ${e.localizedMessage}" else "Error saving file: ${e.localizedMessage}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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
                    Text(if (strings.isFa) "کد JSON کلیه رویدادها، وظایف، یادداشت‌ها و نقطه‌های عمر شما:" else "JSON code of all events, tasks, notes, and life grid:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = jsonString,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            com.example.update.AppUpdateManager.performSafetyBackup(context, jsonString)
                            saveBackupFileLauncher.launch("MyCalendar_SafetyBackup.json")
                        },
                        enabled = jsonString.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (strings.isFa) "💾 ذخیره فایل پشتیبان در گوشی (Downloads)" else "💾 Save Backup File to Phone")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonString))
                            copiedMessage = true
                        },
                        enabled = jsonString.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedMessage) (if (strings.isFa) "کپی شد!" else "Copied!") else (if (strings.isFa) "کپی کد پشتیبان در حافظه" else "Copy backup code"))
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (strings.isFa) {
                                    "💡 راهنمای بازیابی پس از نصب مجدد یا آپدیت:\nبرنامه پیش از هر آپدیت، یک فایل پشتیبان خودکار با نام MyCalendar_SafetyBackup.json در پوشه Downloads گوشی ذخیره می‌کند. با دکمه زیر می‌توانید این فایل را انتخاب و تمام اطلاعات را بازیابی کنید:"
                                } else {
                                    "💡 Restore Tip:\nBefore every update, an automatic backup named MyCalendar_SafetyBackup.json is saved in your Downloads folder. Tap below to select and restore it:"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    openBackupFileLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (strings.isFa) "📂 انتخاب فایل پشتیبان از پوشه Downloads" else "📂 Select Backup File from Downloads",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            if (!detectedAutoBackup.isNullOrBlank()) {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        onImport(detectedAutoBackup)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (strings.isFa) "⚡ بازیابی فوری از آخرین پشتیبان خودکار" else "⚡ Instant Restore from Auto-Backup",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(if (strings.isFa) "یا کد پشتیبان JSON را در کادر زیر جای‌گذاری (Paste) کنید:" else "Or paste backup JSON code below:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("{\"deviceId\":\"...\", ...}") },
                        minLines = 4,
                        maxLines = 6,
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
                        Text(if (strings.isFa) "بازیابی و ادغام اطلاعات متنی" else "Restore & Merge Text Data")
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
                    shape = RoundedCornerShape(12.dp),
                    colors = modernDialogTextFieldColors(),
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
