package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.data.TaskEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.EditTaskDialog

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.activeTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var filterState by remember { mutableIntStateOf(0) } // 0: Selected Date, 1: This Week, 2: All, 3: Pending, 4: Completed
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    val dayOfWeekIdx = PersianCalendarHelper.getPersianDayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day)
    val dayOfWeekName = PersianCalendarHelper.WEEKDAY_NAMES_PERSIAN.getOrElse(dayOfWeekIdx) { "روز" }

    // Week range calculation (Saturday to Friday)
    val weekDates = remember(selectedDate) {
        PersianCalendarHelper.getWeekDates(selectedDate.year, selectedDate.month, selectedDate.day)
    }
    val weekFormattedDates = remember(weekDates) {
        weekDates.map { it.formatted }.toSet()
    }
    val firstWeekDay = weekDates.first()
    val lastWeekDay = weekDates.last()
    val weekRangeText = "${PersianCalendarHelper.toPersianDigits(firstWeekDay.day.toString())} ${firstWeekDay.monthName} تا ${PersianCalendarHelper.toPersianDigits(lastWeekDay.day.toString())} ${lastWeekDay.monthName}"

    val selectedDateTasks = tasks.filter { it.persianDueDate == selectedDate.formatted }
    val weekTasks = tasks.filter { it.persianDueDate in weekFormattedDates }.sortedBy { it.persianDueDate }

    val filteredTasks = when (filterState) {
        0 -> selectedDateTasks
        1 -> weekTasks
        2 -> tasks
        3 -> tasks.filter { !it.isCompleted }
        4 -> tasks.filter { it.isCompleted }
        else -> selectedDateTasks
    }

    val currentScopeTasks = when (filterState) {
        0 -> selectedDateTasks
        1 -> weekTasks
        else -> tasks
    }
    val completedCount = currentScopeTasks.count { it.isCompleted }
    val totalCount = currentScopeTasks.size
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header 1: Interactive Calendar Date Bar (Linked with Selected Calendar Date)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Right side: Week tasks button (Fixed ergonomic dimensions)
                    Surface(
                        color = if (filterState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clickable { filterState = if (filterState == 1) 0 else 1 }
                            .testTag("btn_week_tasks")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "وظایف هفته",
                                tint = if (filterState == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "وظایف هفته",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (filterState == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 2. Center: Date display with previous/next day arrows (Fills remaining space stably)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Right Arrow: Previous day or previous week in RTL
                        IconButton(
                            onClick = {
                                if (filterState == 1) {
                                    viewModel.previousWeek()
                                } else {
                                    viewModel.previousDay()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (filterState == 1) "هفته قبل" else "روز قبل",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable { filterState = 0 }
                        ) {
                            Icon(
                                if (filterState == 1) Icons.Default.DateRange else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (filterState == 1) weekRangeText else "$dayOfWeekName ${selectedDate.formattedPersian}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (filterState == 1)
                                        "${PersianCalendarHelper.toPersianDigits(weekTasks.size.toString())} وظیفه این هفته"
                                    else
                                        "${PersianCalendarHelper.toPersianDigits(selectedDateTasks.size.toString())} وظیفه امروز",
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Left Arrow: Next day or next week in RTL
                        IconButton(
                            onClick = {
                                if (filterState == 1) {
                                    viewModel.nextWeek()
                                } else {
                                    viewModel.nextDay()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (filterState == 1) "هفته بعد" else "روز بعد",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 3. Left side: Today button (Fixed ergonomic dimensions)
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clickable {
                                viewModel.setToday()
                                filterState = 0
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = "امروز",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Header 2: Progress & Filter Tabs
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Filter Chips (Including Date & Week Filter with Horizontal Scroll)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterState == 0,
                            onClick = { filterState = 0 },
                            label = { Text("این روز (${PersianCalendarHelper.toPersianDigits(selectedDateTasks.size.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterState == 1,
                            onClick = { filterState = 1 },
                            label = { Text("این هفته (${PersianCalendarHelper.toPersianDigits(weekTasks.size.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterState == 2,
                            onClick = { filterState = 2 },
                            label = { Text("همه (${PersianCalendarHelper.toPersianDigits(tasks.size.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterState == 3,
                            onClick = { filterState = 3 },
                            label = { Text("در انتظار", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterState == 4,
                            onClick = { filterState = 4 },
                            label = { Text("انجام شده", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Task Items List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (filterState) {
                                0 -> "هیچ وظیفه‌ای برای $dayOfWeekName ${selectedDate.formattedPersian} ثبت نشده است."
                                1 -> "هیچ وظیفه‌ای برای این هفته ($weekRangeText) ثبت نشده است."
                                else -> "وظیفه‌ای در این دسته‌بندی یافت نشد."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { showAddTaskDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (filterState == 1) "افزودن وظیفه برای این هفته" else "افزودن وظیفه برای $dayOfWeekName",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onToggle = { isChecked -> viewModel.toggleTask(task.id, isChecked) },
                            onEdit = { taskToEdit = task },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("fab_add_task")
        ) {
            Icon(Icons.Default.Add, contentDescription = "افزودن کار جدید")
        }
    }

    // Add Task Dialog (Defaults to current selected calendar date!)
    if (showAddTaskDialog) {
        AddTaskDialog(
            initialDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, dueDate, priority, category ->
                viewModel.addTask(title, dueDate, priority, category)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog (Full Editing Support!)
    taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, dueDate, priority, category ->
                viewModel.updateTask(
                    task.copy(
                        title = title,
                        persianDueDate = dueDate,
                        priority = priority,
                        category = category,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                taskToEdit = null
            },
            onDelete = {
                viewModel.deleteTask(task.id)
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: TaskEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "فوری", "بالا" -> Color(0xFFEF4444)
        "متوسط" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    if (task.persianDueDate.isNotBlank()) {
                        Text(
                            text = "📅 ${PersianCalendarHelper.toPersianDigits(task.persianDueDate)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    if (task.category.isNotBlank() && task.category != "عمومی") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "ویرایش وظیفه",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف وظیفه",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
