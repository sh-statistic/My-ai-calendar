package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.memento.MementoMoriManager
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Memento Mori - Life in Months Screen
 * 960 Circles representing 80 Years of Life (12 Months per Year)
 * - Past Months: Hollow dark drilled holes
 * - Current Month: Pulsing Orange Circle with 3-Second Physical Drill Long-Press & Continuous Haptic Feedback
 * - Future Months: Light Gray Solid Circles
 */
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.min

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeGridScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember { MementoMoriManager(context) }
    var progressData by remember { mutableStateOf(manager.calculateProgress()) }

    var showBirthDialog by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var showMonthDetailDialog by remember { mutableStateOf<Int?>(null) }
    var reflectionNoteText by remember { mutableStateOf("") }
    var justDrilledMonthIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top Statistics Card (Memento Mori Overview)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عمر من (${PersianCalendarHelper.toPersianDigits(progressData.totalMonths.toString())} ماه - ${PersianCalendarHelper.toPersianDigits((progressData.totalMonths / 12).toString())} سال)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { showBirthDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تولد: ${PersianCalendarHelper.toPersianDigits(progressData.birthYear.toString())}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Time Elapsed & Remaining Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time Elapsed
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "زمان سپری شده:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianCalendarHelper.toPersianDigits(progressData.elapsedYears.toString())} سال و ${PersianCalendarHelper.toPersianDigits(progressData.elapsedRemainingMonths.toString())} ماه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "(${PersianCalendarHelper.toPersianDigits(progressData.elapsedMonths.toString())} ماه)",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Time Remaining
                    Surface(
                        color = Color(0xFFFEF3C7).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "زمان باقیمانده:",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianCalendarHelper.toPersianDigits(progressData.remainingYears.toString())} سال و ${PersianCalendarHelper.toPersianDigits(progressData.remainingMonths.toString())} ماه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "(${PersianCalendarHelper.toPersianDigits((progressData.totalMonths - progressData.elapsedMonths).toString())} ماه)",
                                fontSize = 10.sp,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Life Progress Bar
                LinearProgressIndicator(
                    progress = (progressData.percentElapsed / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFF59E0B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend & Instructions
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Past
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF475569), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سپری شده", fontSize = 10.sp)
                }

                // Current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ماه فعلی (لمس جهت ثبت)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }

                // Future
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("آینده", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 960 Months Grid (80 Years * 12 Months) via High-Performance Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(progressData) {
                        detectTapGestures { offset ->
                            val colWidth = size.width / 24f
                            val rowHeight = size.height / (progressData.totalMonths / 24f)
                            val col = (offset.x / colWidth).toInt()
                            val row = (offset.y / rowHeight).toInt()
                            val index = row * 24 + col

                            if (index in 0 until progressData.totalMonths) {
                                if (index < progressData.currentMonthIndex) {
                                    showMonthDetailDialog = index
                                } else if (index == progressData.currentMonthIndex) {
                                    justDrilledMonthIndex = progressData.currentMonthIndex
                                    manager.addManualDrilledMonth()
                                    progressData = manager.calculateProgress()
                                    reflectionNoteText = ""
                                    showReflectionDialog = true
                                }
                            }
                        }
                    }
            ) {
                val totalRows = progressData.totalMonths / 24
                val colWidth = size.width / 24f
                val rowHeight = size.height / totalRows.toFloat()
                val radius = min(colWidth, rowHeight) / 2f * 0.8f // 80% size for padding
                val strokeWidthPx = 1.dp.toPx()
                val strokeCurrentPx = 1.5.dp.toPx()

                for (i in 0 until progressData.totalMonths) {
                    val row = i / 24
                    val col = i % 24
                    val cx = col * colWidth + colWidth / 2f
                    val cy = row * rowHeight + rowHeight / 2f
                    val centerOffset = Offset(cx, cy)

                    if (i < progressData.currentMonthIndex) {
                        // Past
                        drawCircle(color = Color(0xFF0F172A), radius = radius, center = centerOffset)
                        drawCircle(color = Color(0xFF334155), radius = radius, center = centerOffset, style = Stroke(width = strokeWidthPx))
                        drawCircle(color = Color(0xFF020617), radius = radius * 0.3f, center = centerOffset)
                    } else if (i == progressData.currentMonthIndex) {
                        // Current
                        drawCircle(color = Color(0xFFF59E0B), radius = radius, center = centerOffset)
                        drawCircle(color = Color(0xFFD97706), radius = radius, center = centerOffset, style = Stroke(width = strokeCurrentPx))
                    } else {
                        // Future
                        drawCircle(color = Color(0xFFE2E8F0), radius = radius, center = centerOffset)
                    }
                }
            }
        }
    }

    // Dialog: "این ماه چطور گذشت؟" (Reflection Dialog after drilling)
    if (showReflectionDialog) {
        AlertDialog(
            onDismissRequest = { showReflectionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NoteAlt, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("این ماه چطور گذشت؟", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "یک ماه دیگر از عمر شما سپری و ثبت شد. مهم‌ترین خاطره، دستاورد یا حس این ماه را به یادگار بنویسید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = reflectionNoteText,
                        onValueChange = { reflectionNoteText = it },
                        placeholder = { Text("مثال: پروژه جدید را شروع کردم و سفر بسیار خوبی داشتم...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reflectionNoteText.isNotBlank()) {
                            manager.saveReflectionNote(justDrilledMonthIndex, reflectionNoteText.trim())
                            Toast.makeText(context, "یادداشت این ماه ثبت و ذخیره شد ✓", Toast.LENGTH_SHORT).show()
                        }
                        showReflectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("ذخیره یادداشت ماه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReflectionDialog = false }) {
                    Text("بعداً")
                }
            }
        )
    }

    // Dialog: View Past Month Reflection Note
    showMonthDetailDialog?.let { monthIdx ->
        val existingNote = manager.getReflectionNote(monthIdx)
        val monthYear = (monthIdx / 12) + 1
        val monthNumber = (monthIdx % 12) + 1
        val monthName = PersianCalendarHelper.PERSIAN_MONTH_NAMES.getOrNull(monthNumber - 1) ?: "ماه $monthNumber"

        AlertDialog(
            onDismissRequest = { showMonthDetailDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سال $monthYear زندگی - $monthName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "شماره ماه در عمر: ${PersianCalendarHelper.toPersianDigits((monthIdx + 1).toString())} از ${PersianCalendarHelper.toPersianDigits((progressData.totalMonths).toString())} ماه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (existingNote != null && existingNote.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "« $existingNote »",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = "یادداشتی برای این ماه ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthDetailDialog = null }) {
                    Text("بستن")
                }
            }
        )
    }

    // Dialog: Set Birth Year & Month
    if (showBirthDialog) {
        var tempYear by remember { mutableStateOf(progressData.birthYear.toString()) }
        var tempMonth by remember { mutableIntStateOf(progressData.birthMonth) }
        var tempLifespan by remember { mutableStateOf((progressData.totalMonths / 12).toString()) }
        val currentYear = PersianCalendarHelper.getCurrentJalaliDate().year

        AlertDialog(
            onDismissRequest = { showBirthDialog = false },
            title = { Text("تنظیمات عمر من", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("برای محاسبه دقیق ماه‌های سپری‌شده، مشخصات خود را وارد کنید:", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = tempYear,
                        onValueChange = { tempYear = it.filter { ch -> ch.isDigit() } },
                        label = { Text("سال تولد (شمسی)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempLifespan,
                        onValueChange = { tempLifespan = it.filter { ch -> ch.isDigit() } },
                        label = { Text("فکر می‌کنید چند سال عمر می‌کنید؟") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("ماه تولد:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PersianCalendarHelper.PERSIAN_MONTH_NAMES.forEachIndexed { i, name ->
                            val m = i + 1
                            FilterChip(
                                selected = tempMonth == m,
                                onClick = { tempMonth = m },
                                label = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val y = tempYear.toIntOrNull() ?: progressData.birthYear
                        val ls = tempLifespan.toIntOrNull()?.coerceIn(10, 150) ?: (progressData.totalMonths / 12)
                        
                        manager.setBirthDate(if (y in 1300..currentYear) y else progressData.birthYear, tempMonth)
                        manager.setExpectedLifespan(ls)
                        
                        progressData = manager.calculateProgress()
                        showBirthDialog = false
                    }
                ) {
                    Text("ذخیره و بروزرسانی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBirthDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}
