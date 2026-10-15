package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import com.example.ui.rememberAppStrings

@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.activeTasks.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val strings = rememberAppStrings()

    var isWeekMode by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Pending, 2: Completed, 3: All Time
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    val today = remember { PersianCalendarHelper.getCurrentJalaliDate() }
    val dayOfWeekIdx = PersianCalendarHelper.getPersianDayOfWeek(selectedDate.year, selectedDate.month, selectedDate.day)
    val dayOfWeekName = strings.weekdayNames.getOrElse(dayOfWeekIdx) { "" }

    // Week range calculation (Saturday to Friday)
    val weekDates = remember(selectedDate) {
        PersianCalendarHelper.getWeekDates(selectedDate.year, selectedDate.month, selectedDate.day)
    }
    val weekFormattedDates = remember(weekDates) {
        weekDates.map { it.formatted }.toSet()
    }
    val firstWeekDay = weekDates.first()
    val lastWeekDay = weekDates.last()
    val weekRangeText = if (strings.isFa) {
        "${strings.formatDigits(firstWeekDay.day.toString())} ${firstWeekDay.monthName} تا ${strings.formatDigits(lastWeekDay.day.toString())} ${lastWeekDay.monthName}"
    } else {
        "${firstWeekDay.day} ${strings.jalaliMonths[firstWeekDay.month - 1]} to ${lastWeekDay.day} ${strings.jalaliMonths[lastWeekDay.month - 1]}"
    }

    val todayWeekDates = remember(today) {
        PersianCalendarHelper.getWeekDates(today.year, today.month, today.day)
    }
    val isCurrentWeek = weekDates.first().formatted == todayWeekDates.first().formatted
    val isToday = selectedDate.formatted == today.formatted

    val selectedDateNormalized = remember(selectedDate) {
        PersianCalendarHelper.normalizePersianDate(selectedDate.formatted)
    }
    val selectedDateTasks = tasks.filter {
        PersianCalendarHelper.normalizePersianDate(it.persianDueDate) == selectedDateNormalized
    }
    val weekTasks = tasks.filter {
        val normDue = PersianCalendarHelper.normalizePersianDate(it.persianDueDate)
        normDue in weekFormattedDates
    }.sortedWith(
        compareBy({ it.isCompleted }, { PersianCalendarHelper.normalizePersianDate(it.persianDueDate) })
    )

    // وظایف بازه انتخابی (روز یا هفته)
    val currentScopeTasks = if (isWeekMode) weekTasks else selectedDateTasks

    // فیلتر وضعیت وظایف
    val filteredTasks = when (statusFilter) {
        0 -> currentScopeTasks
        1 -> currentScopeTasks.filter { !it.isCompleted }
        2 -> currentScopeTasks.filter { it.isCompleted }
        3 -> tasks
        else -> currentScopeTasks
    }

    val completedCount = if (statusFilter == 3) tasks.count { it.isCompleted } else currentScopeTasks.count { it.isCompleted }
    val totalCount = if (statusFilter == 3) tasks.size else currentScopeTasks.size
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount).coerceIn(0f, 1f) else 0f

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header 1: Time scope & Interactive Calendar Date Bar
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
                    // 1. Right side: Week mode toggle button
                    Surface(
                        color = if (isWeekMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clickable { isWeekMode = !isWeekMode }
                            .testTag("btn_week_tasks")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = strings.weekTasks,
                                tint = if (isWeekMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.weekTasks,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isWeekMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // 2. Center: Date display with previous/next arrows
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Previous day or previous week
                        IconButton(
                            onClick = {
                                if (isWeekMode) {
                                    viewModel.previousWeek()
                                } else {
                                    viewModel.previousDay()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isWeekMode) strings.prevWeek else strings.prevDay,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f, fill = false)
                        ) {
                            Icon(
                                if (isWeekMode) Icons.Default.DateRange else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isWeekMode) weekRangeText else "$dayOfWeekName ${strings.formatDigits(selectedDate.day.toString())} ${strings.jalaliMonths[selectedDate.month - 1]}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isWeekMode) {
                                        if (strings.isFa) {
                                            if (isCurrentWeek)
                                                "${strings.formatDigits(weekTasks.size.toString())} وظیفه این هفته"
                                            else
                                                "${strings.formatDigits(weekTasks.size.toString())} وظیفه در این بازه"
                                        } else {
                                            if (isCurrentWeek)
                                                "${weekTasks.size} tasks this week"
                                            else
                                                "${weekTasks.size} tasks in range"
                                        }
                                    } else {
                                        if (strings.isFa) {
                                            if (isToday)
                                                "${strings.formatDigits(selectedDateTasks.size.toString())} وظیفه امروز"
                                            else
                                                "${strings.formatDigits(selectedDateTasks.size.toString())} وظیفه در این روز"
                                        } else {
                                            if (isToday)
                                                "${selectedDateTasks.size} tasks today"
                                            else
                                                "${selectedDateTasks.size} tasks on this day"
                                        }
                                    },
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Next day or next week
                        IconButton(
                            onClick = {
                                if (isWeekMode) {
                                    viewModel.nextWeek()
                                } else {
                                    viewModel.nextDay()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (isWeekMode) strings.nextWeek else strings.nextDay,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // 3. Left side: Today button (Jumps to today)
                    val isTodayActive = if (isWeekMode) isCurrentWeek else isToday
                    val tasksTodayBtnContainerColor by animateColorAsState(
                        targetValue = if (isTodayActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        },
                        label = "tasks_today_btn_container_color"
                    )

                    val tasksTodayBtnContentColor by animateColorAsState(
                        targetValue = if (isTodayActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        },
                        label = "tasks_today_btn_content_color"
                    )

                    Surface(
                        color = tasksTodayBtnContainerColor,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .clickable {
                                viewModel.setToday()
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = strings.today,
                                fontSize = 11.sp,
                                fontWeight = if (isTodayActive) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                color = tasksTodayBtnContentColor
                            )
                        }
                    }
                }
            }

            // Header 2: Task Status Filters (Distinct from time scope)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Status Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = statusFilter == 0,
                            onClick = { statusFilter = 0 },
                            label = { Text("${strings.allTasks} (${strings.formatDigits(currentScopeTasks.size.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == 1,
                            onClick = { statusFilter = 1 },
                            label = { Text("${strings.pendingTasks} (${strings.formatDigits(currentScopeTasks.count { !it.isCompleted }.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == 2,
                            onClick = { statusFilter = 2 },
                            label = { Text("${strings.completedTasks} (${strings.formatDigits(currentScopeTasks.count { it.isCompleted }.toString())})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == 3,
                            onClick = { statusFilter = 3 },
                            label = { Text("${strings.allTimeTasks} (${strings.formatDigits(tasks.size.toString())})", fontSize = 11.sp) }
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
                            text = when (statusFilter) {
                                0 -> if (strings.isFa) {
                                    if (isWeekMode) "هیچ وظیفه‌ای برای بازه ($weekRangeText) ثبت نشده است." else "هیچ وظیفه‌ای برای $dayOfWeekName ${selectedDate.formattedPersian} ثبت نشده است."
                                } else {
                                    if (isWeekMode) "No tasks scheduled for ($weekRangeText)." else "No tasks scheduled for $dayOfWeekName."
                                }
                                1 -> if (strings.isFa) "هیچ وظیفه در انتظاری در این بازه وجود ندارد." else "No pending tasks in this range."
                                2 -> if (strings.isFa) "هنوز وظیفه‌ای در این بازه تکمیل نشده است." else "No completed tasks in this range yet."
                                3 -> if (strings.isFa) "هیچ وظیفه‌ای در کل برنامه ثبت نشده است." else "No tasks found in the app."
                                else -> strings.noTasks
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(onClick = { showAddTaskDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.addTask,
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
            Icon(Icons.Default.Add, contentDescription = strings.addTask)
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
    val strings = rememberAppStrings()
    val priorityColor = when (task.priority) {
        "فوری", "بالا" -> Color(0xFFEF4444)
        "متوسط" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val todayFormatted = remember { PersianCalendarHelper.getCurrentJalaliDate().formatted }
    val isOverdue = !task.isCompleted && task.persianDueDate.isNotBlank() &&
        PersianCalendarHelper.normalizePersianDate(task.persianDueDate) < todayFormatted

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
                            text = "📅 ${strings.formatDigits(task.persianDueDate)}",
                            fontSize = 11.sp,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isOverdue) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = strings.overdue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = strings.taskPriority(task.priority),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    if (task.category.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = strings.taskTag(task.category),
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
