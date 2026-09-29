package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.JalaliDate
import com.example.calendar.PersianCalendarHelper
import com.example.calendar.PersianOccasionsHelper
import com.example.data.EventEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddEventDialog
import com.example.ui.components.EditEventDialog
import com.example.ui.components.PrayerTimesCard
import com.example.ui.components.parseColor

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val viewYear by viewModel.currentViewYear.collectAsState()
    val viewMonth by viewModel.currentViewMonth.collectAsState()
    val events by viewModel.activeEvents.collectAsState()
    val tasks by viewModel.activeTasks.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EventEntity?>(null) }

    val today = PersianCalendarHelper.getCurrentJalaliDate()
    val daysInMonth = PersianCalendarHelper.getDaysInJalaliMonth(viewYear, viewMonth)
    val firstDayOfWeek = PersianCalendarHelper.getPersianDayOfWeek(viewYear, viewMonth, 1)

    val selectedDayEvents = events.filter { it.persianDate == selectedDate.formatted }
    val selectedDayTasks = tasks.filter { it.persianDueDate == selectedDate.formatted }
    val selectedDayOccasions = PersianOccasionsHelper.getOccasionsForDate(selectedDate.month, selectedDate.day)
    val isSelectedDayHoliday = PersianOccasionsHelper.isHoliday(selectedDate.month, selectedDate.day)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEventDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_event")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن رویداد جدید")
            }
        }
    ) { innerPadding ->
        // Single unified scrollable container so all details are visible on smaller Android 6 screens
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("calendar_scrollable_container"),
            contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Calendar Grid Card (with national & religious holidays marked)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        // Weekday Titles (ش, ی, د, س, چ, پ, ج)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            PersianCalendarHelper.WEEKDAY_ABBR.forEachIndexed { idx, name ->
                                Text(
                                    text = name,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == 6) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Days Grid
                        val totalCells = firstDayOfWeek + daysInMonth
                        val rowsCount = Math.ceil(totalCells / 7.0).toInt()

                        for (row in 0 until rowsCount) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - firstDayOfWeek + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val cellJalali = JalaliDate(viewYear, viewMonth, dayNum)
                                        val cellGregorian = PersianCalendarHelper.jalaliToGregorian(viewYear, viewMonth, dayNum)
                                        val isSelected = cellJalali.formatted == selectedDate.formatted
                                        val isToday = cellJalali.formatted == today.formatted
                                        val isFriday = col == 6
                                        val isHoliday = isFriday || PersianOccasionsHelper.isHoliday(viewMonth, dayNum)
                                        val cellOccasions = PersianOccasionsHelper.getOccasionsForDate(viewMonth, dayNum)

                                        val hasEvents = events.any { it.persianDate == cellJalali.formatted }
                                        val hasTasks = tasks.any { it.persianDueDate == cellJalali.formatted }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.15f)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    when {
                                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .then(
                                                    if (isSelected) Modifier.border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    else Modifier
                                                )
                                                .clickable { viewModel.setSelectedDate(cellJalali) }
                                                .padding(1.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Primary: Persian Day (Red if holiday or Friday)
                                                Text(
                                                    text = PersianCalendarHelper.toPersianDigits(dayNum.toString()),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected || isToday || isHoliday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isHoliday -> MaterialTheme.colorScheme.error
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                                // Secondary: Gregorian Day
                                                Text(
                                                    text = cellGregorian.day.toString(),
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )

                                                // Dot indicators (Events / Tasks / Occasions)
                                                if (hasEvents || hasTasks || cellOccasions.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    ) {
                                                        if (cellOccasions.isNotEmpty()) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isHoliday) MaterialTheme.colorScheme.error else Color(0xFF0D9488))
                                                            )
                                                        }
                                                        if (hasEvents) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
                                                        if (hasTasks) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(3.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.tertiary)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Ultra-Lightweight Offline Prayer Times & Fajr Alarm Card (Dynamically updates with selectedDate!)
            item {
                PrayerTimesCard(
                    viewModel = viewModel,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            // 4. National & Religious Occasions Card for Selected Date
            if (selectedDayOccasions.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelectedDayHoliday) Color(0xFFFEE2E2) else Color(0xFFF0FDF4)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelectedDayHoliday) Color(0xFFFCA5A5) else Color(0xFFBBF7D0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .testTag("card_occasions")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSelectedDayHoliday) Color(0xFFDC2626) else Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "مناسبت‌های این روز:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedDayHoliday) Color(0xFF991B1B) else Color(0xFF166534)
                                    )
                                }

                                if (isSelectedDayHoliday) {
                                    Surface(
                                        color = Color(0xFFDC2626),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "تعطیل رسمی",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            selectedDayOccasions.forEach { occ ->
                                Text(
                                    text = "• ${occ.title}",
                                    fontSize = 11.sp,
                                    fontWeight = if (occ.isHoliday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (occ.isHoliday) Color(0xFFB91C1C) else Color(0xFF1E293B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Selected Day Details Header
            item {
                val selGDate = PersianCalendarHelper.jalaliToGregorian(selectedDate.year, selectedDate.month, selectedDate.day)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "برنامه‌های ${selectedDate.formattedPersian}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "معادل میلادی: ${selGDate.formatted} (${selGDate.day} ${selGDate.monthName} ${selGDate.year})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${PersianCalendarHelper.toPersianDigits((selectedDayEvents.size + selectedDayTasks.size).toString())} مورد",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 6. Events List for Selected Day
            if (selectedDayEvents.isEmpty() && selectedDayTasks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "هیچ رویدادی برای این تاریخ ثبت نشده است.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(selectedDayEvents, key = { it.id }) { event ->
                    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                        EventItemCard(
                            event = event,
                            onClick = { eventToEdit = event },
                            onEdit = { eventToEdit = event },
                            onDelete = { viewModel.deleteEvent(event.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            initialDate = selectedDate,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, desc, pDate, start, end, cat, col ->
                viewModel.addEvent(title, desc, pDate, start, end, cat, col)
                showAddEventDialog = false
            }
        )
    }

    eventToEdit?.let { event ->
        EditEventDialog(
            initialEvent = event,
            onDismiss = { eventToEdit = null },
            onConfirm = { title, desc, pDate, start, end, cat, col ->
                viewModel.updateEvent(
                    event.copy(
                        title = title,
                        description = desc,
                        persianDate = pDate,
                        startTime = start,
                        endTime = end,
                        category = cat,
                        colorHex = col,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                eventToEdit = null
            },
            onDelete = {
                viewModel.deleteEvent(event.id)
                eventToEdit = null
            }
        )
    }
}

@Composable
fun EventItemCard(
    event: EventEntity,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category/Color Accent Bar
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(parseColor(event.colorHex))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (event.startTime.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val timeStr = if (event.endTime.isNotBlank()) {
                                "${PersianCalendarHelper.toPersianDigits(event.startTime)} تا ${PersianCalendarHelper.toPersianDigits(event.endTime)}"
                            } else {
                                PersianCalendarHelper.toPersianDigits(event.startTime)
                            }
                            Text(
                                text = timeStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        color = parseColor(event.colorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = event.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = parseColor(event.colorHex),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش رویداد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف رویداد",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
