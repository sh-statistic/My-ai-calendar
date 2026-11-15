package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.GregorianDate
import com.example.calendar.HijriCalendarUtils
import com.example.calendar.JalaliDate
import com.example.calendar.PersianCalendarHelper
import com.example.calendar.PersianOccasion
import com.example.calendar.PersianOccasionsHelper
import com.example.data.EventEntity
import com.example.data.TaskEntity
import com.example.ui.screens.EventItemCard
import com.example.ui.screens.TaskItemCard
import com.example.ui.MainViewModel
import com.example.ui.rememberAppStrings
import com.example.prayer.PrayerTimesCalculator
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.TimeZone

const val WEEK_PAGER_BASE_PAGE = 15000

// Base date: Saturday 1405/01/01 (2026-03-21)
private val BASE_SATURDAY_EPOCH_DAY: Long by lazy {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, 2, 21) // 2026-03-21 Saturday
    }
    cal.timeInMillis / 86400000L
}

fun getSaturdayEpochDay(jYear: Int, jMonth: Int, jDay: Int): Long {
    val dow = PersianCalendarHelper.getPersianDayOfWeek(jYear, jMonth, jDay)
    val gDate = PersianCalendarHelper.jalaliToGregorian(jYear, jMonth, jDay)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(gDate.year, gDate.month - 1, gDate.day)
        add(Calendar.DAY_OF_MONTH, -dow)
    }
    return cal.timeInMillis / 86400000L
}

fun epochDayToJalali(epochDay: Long): JalaliDate {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        timeInMillis = epochDay * 86400000L
    }
    val gy = cal.get(Calendar.YEAR)
    val gm = cal.get(Calendar.MONTH) + 1
    val gd = cal.get(Calendar.DAY_OF_MONTH)
    return PersianCalendarHelper.gregorianToJalali(gy, gm, gd)
}

fun jalaliToWeekPage(date: JalaliDate): Int {
    val satEpoch = getSaturdayEpochDay(date.year, date.month, date.day)
    val diffDays = satEpoch - BASE_SATURDAY_EPOCH_DAY
    val diffWeeks = Math.floorDiv(diffDays, 7)
    return WEEK_PAGER_BASE_PAGE + diffWeeks.toInt()
}

fun weekPageToSaturday(page: Int): JalaliDate {
    val diffWeeks = page - WEEK_PAGER_BASE_PAGE
    val targetEpoch = BASE_SATURDAY_EPOCH_DAY + diffWeeks * 7L
    return epochDayToJalali(targetEpoch)
}

fun getJalaliWeekOfYear(year: Int, month: Int, day: Int): Int {
    val doy = if (month <= 6) {
        (month - 1) * 31 + day
    } else {
        186 + (month - 7) * 30 + day
    }
    val startDow = PersianCalendarHelper.getPersianDayOfWeek(year, 1, 1)
    return (doy - 1 + startDow) / 7 + 1
}

// نگه‌داشتنِ لازم برای باز شدن صفحهٔ روز در نمای هفتگی: عمداً از نگه‌داشتنِ معمول سیستم طولانی‌تر است
private const val WEEK_DAY_HOLD_MILLIS = 800L

// لمس کوتاه فقط روز را انتخاب می‌کند؛ صفحهٔ روز (افزودن رویداد و وظیفه) فقط با نگه‌داشتنِ طولانی باز می‌شود
// تا با هر لمس ساده‌ای نپرد. رد شدن انگشت از حد لغزش یعنی «ورق زدن» و هیچ عملی انجام نمی‌شود.
private suspend fun PointerInputScope.detectDayHoldGestures(onTap: () -> Unit, onHold: () -> Unit) {
    awaitPointerEventScope {
        while (true) {
            val downEvent = awaitPointerEvent()
            val down = downEvent.changes.firstOrNull { it.changedToDown() } ?: continue
            val downPosition = down.position
            var moved = false
            var released = false
            val heldLongEnough = withTimeoutOrNull(WEEK_DAY_HOLD_MILLIS) {
                var done = false
                while (!done) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        if ((change.position - downPosition).getDistance() > viewConfiguration.touchSlop) {
                            moved = true
                        }
                    }
                    if (event.changes.all { !it.pressed }) {
                        released = true
                        done = true
                    }
                }
                true
            } == null
            when {
                heldLongEnough && !moved -> {
                    onHold()
                    // تا رها شدن انگشت صبر می‌کنیم تا همین نگه‌داشتن دوباره خوانده نشود
                    var waiting = true
                    while (waiting) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) waiting = false
                    }
                }
                released && !moved -> onTap()
                !released -> {
                    var waiting = true
                    while (waiting) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) waiting = false
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyNotebookCalendarView(
    viewModel: MainViewModel,
    onAddEventClick: (JalaliDate) -> Unit,
    onAddTaskClick: (JalaliDate) -> Unit,
    onEditEventClick: (EventEntity) -> Unit,
    onEditTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val events by viewModel.activeEvents.collectAsState()
    val tasks by viewModel.activeTasks.collectAsState()
    val occasionsVersion by viewModel.occasionsVersion.collectAsState()
    val todayNavPulse by viewModel.todayNavPulse.collectAsState()
    val strings = rememberAppStrings()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val todayJalali = remember { PersianCalendarHelper.getCurrentJalaliDate() }

    val initialPage = remember { jalaliToWeekPage(selectedDate) }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { 30000 }
    )

    // دکمهٔ «امروز»: با پالس ناوبری کار می‌کند، نه فقط تغییر تاریخ، تا حتی وقتی تاریخ انتخابی از قبل امروز است هم همیشه و فوری بپرد
    LaunchedEffect(todayNavPulse) {
        if (todayNavPulse > 0) {
            val targetPage = jalaliToWeekPage(PersianCalendarHelper.getCurrentJalaliDate())
            val distance = kotlin.math.abs(targetPage - pagerState.currentPage)
            if (distance <= 2) {
                pagerState.animateScrollToPage(targetPage)
            } else {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    // Sync external selectedDate changes (like Today button) to pager
    LaunchedEffect(selectedDate) {
        val targetPage = jalaliToWeekPage(selectedDate)
        if (pagerState.currentPage != targetPage && pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Sync pager swipe changes to ViewModel viewYear/viewMonth
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val sat = weekPageToSaturday(page)
            val midWeek = PersianCalendarHelper.getWeekDates(sat.year, sat.month, sat.day).getOrNull(3) ?: sat
            viewModel.setViewYearMonth(midWeek.year, midWeek.month)
        }
    }

    // Fast O(1) content lookups
    val eventDatesMap = remember(events) { events.groupBy { it.persianDate } }
    val taskDatesMap = remember(tasks) { tasks.groupBy { it.persianDueDate } }

    var sheetDate by remember { mutableStateOf<JalaliDate?>(null) }
    var occasionToEdit by remember { mutableStateOf<PersianOccasion?>(null) }
    var showAddOccasionDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("weekly_notebook_view")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            val saturday = remember(page) { weekPageToSaturday(page) }
            val weekDates = remember(saturday) {
                PersianCalendarHelper.getWeekDates(saturday.year, saturday.month, saturday.day)
            }
            val friday = weekDates.last()

            // Header titles calculation
            val shamsiTitle = remember(saturday, friday) {
                if (saturday.month == friday.month && saturday.year == friday.year) {
                    "${saturday.monthName} ${PersianCalendarHelper.toPersianDigits(saturday.year.toString())}"
                } else if (saturday.year == friday.year) {
                    "${saturday.monthName} - ${friday.monthName} ${PersianCalendarHelper.toPersianDigits(saturday.year.toString())}"
                } else {
                    "${saturday.monthName} ${PersianCalendarHelper.toPersianDigits(saturday.year.toString())} - ${friday.monthName} ${PersianCalendarHelper.toPersianDigits(friday.year.toString())}"
                }
            }

            val hijriTitle = remember(saturday, friday) {
                val h1 = PersianOccasionsHelper.getHijriDate(saturday.year, saturday.month, saturday.day)
                    ?: HijriCalendarUtils.persianToIslamic(saturday.year, saturday.month, saturday.day)
                val h2 = PersianOccasionsHelper.getHijriDate(friday.year, friday.month, friday.day)
                    ?: HijriCalendarUtils.persianToIslamic(friday.year, friday.month, friday.day)
                val hijriMonthNames = listOf("محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه")
                val m1Name = hijriMonthNames.getOrElse(h1.month - 1) { "" }
                val m2Name = hijriMonthNames.getOrElse(h2.month - 1) { "" }
                if (h1.month == h2.month) {
                    "$m1Name ${PersianCalendarHelper.toPersianDigits(h1.year.toString())}"
                } else {
                    "$m1Name - $m2Name ${PersianCalendarHelper.toPersianDigits(h1.year.toString())}"
                }
            }

            val gregorianTitle = remember(saturday, friday) {
                val g1 = PersianCalendarHelper.jalaliToGregorian(saturday.year, saturday.month, saturday.day)
                val g2 = PersianCalendarHelper.jalaliToGregorian(friday.year, friday.month, friday.day)
                if (g1.month == g2.month && g1.year == g2.year) {
                    "${g1.monthName} ${g1.year}"
                } else if (g1.year == g2.year) {
                    "${g1.monthName} - ${g2.monthName} ${g1.year}"
                } else {
                    "${g1.monthName} ${g1.year} - ${g2.monthName} ${g2.year}"
                }
            }

            val gridBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.55f else 0.75f)
            val shamsiColumnBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.5f else 0.65f)

            // حس ورق خوردن کاغذ هنگام سوایپ: چرخش خیلی ملایم دور محور عمودی + کمرنگ شدن لبه‌ها
            // آفست داخل خود لایهٔ گرافیکی خوانده می‌شود تا حین سوایپ بازترکیبی (recomposition) رخ ندهد
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val fraction = pageOffset.coerceIn(-1f, 1f)
                        rotationY = fraction * 7f
                        cameraDistance = 14f * density
                        alpha = 1f - kotlin.math.abs(fraction) * 0.18f
                        val flipScale = 1f - kotlin.math.abs(fraction) * 0.03f
                        scaleX = flipScale
                        scaleY = flipScale
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, gridBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    // 1. Notebook Page Header (Right: Shamsi | Center: Week of Month | Left: Gregorian)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDarkTheme) 0.65f else 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, gridBorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shamsiTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = hijriTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = gregorianTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                )
                            )
                        }
                    }

                    // 2. The 7 Days Rows (Continuous, fully-connected grid matching native app theme)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, gridBorderColor, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        weekDates.forEachIndexed { idx, day ->
                            val isFriday = (idx == 6)
                            val isHoliday = remember(day, occasionsVersion) {
                                isFriday || PersianOccasionsHelper.isHoliday(day.month, day.day, day.year)
                            }
                            val isSelected = (day.formatted == selectedDate.formatted)
                            val isToday = (day.formatted == todayJalali.formatted)

                            val dayEvents = eventDatesMap[day.formatted].orEmpty()
                            val dayTasks = taskDatesMap[day.formatted].orEmpty()
                            val occasions = remember(day, occasionsVersion) {
                                PersianOccasionsHelper.getOccasionsForDate(day.month, day.day, day.year)
                            }
                            val gDate = remember(day) {
                                PersianCalendarHelper.jalaliToGregorian(day.year, day.month, day.day)
                            }
                            val hDate = remember(day) {
                                PersianOccasionsHelper.getHijriDate(day.year, day.month, day.day)
                                    ?: HijriCalendarUtils.persianToIslamic(day.year, day.month, day.day)
                            }

                            // Strictly follows app's official Dark & Light theme colors
                            val todayBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDarkTheme) 0.75f else 0.6f)
                            val todayBorderColor = MaterialTheme.colorScheme.primary
                            val selectedBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDarkTheme) 0.5f else 0.45f)

                            val rowModifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .then(
                                    if (isToday) {
                                        Modifier
                                            .background(todayBgColor)
                                            .border(1.5.dp, todayBorderColor)
                                    } else if (isSelected) {
                                        Modifier
                                            .background(selectedBgColor)
                                            .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    } else {
                                        Modifier
                                    }
                                )
                                .pointerInput(day) {
                                    detectDayHoldGestures(
                                        onTap = { viewModel.setSelectedDate(day) },
                                        onHold = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setSelectedDate(day)
                                            sheetDate = day
                                        }
                                    )
                                }

                            Row(
                                modifier = rowModifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column 1: Weekday Name (Compact width = 54.dp, blinking today dot with balanced spacing)
                                Box(
                                    modifier = Modifier
                                        .width(54.dp)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isToday) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "today_dot_pulse")
                                            val dotAlpha by infiniteTransition.animateFloat(
                                                initialValue = 1f,
                                                targetValue = 0.15f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 800),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "today_dot_alpha"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(todayBorderColor.copy(alpha = dotAlpha), CircleShape)
                                            )
                                        }
                                        Text(
                                            text = strings.weekdayNames.getOrElse(idx) { "" },
                                            fontSize = 10.5.sp,
                                            letterSpacing = (-0.2).sp,
                                            fontWeight = if (isToday || isFriday || isHoliday || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = when {
                                                isFriday || isHoliday -> MaterialTheme.colorScheme.error
                                                isToday -> todayBorderColor
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Continuous Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(gridBorderColor)
                                )

                                // Column 2: Persian Day Number (Width = 26.dp)
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .fillMaxHeight()
                                        .background(if (isToday) Color.Transparent else shamsiColumnBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.formatDigits(day.day.toString()),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when {
                                            isFriday || isHoliday -> MaterialTheme.colorScheme.error
                                            isToday -> todayBorderColor
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                // Continuous Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(gridBorderColor)
                                )

                                // Column 3: Hijri Day Number (Compact width = 20.dp)
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = strings.formatDigits(hDate.day.toString()),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isFriday || isHoliday) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    )
                                }

                                // Continuous Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(gridBorderColor)
                                )

                                // Column 4: Middle Space (Generous space for occasions & plans!)
                                val userPlansCount = dayEvents.size + dayTasks.size
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (occasions.isNotEmpty()) {
                                        Text(
                                            text = occasions.first().title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isHoliday) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isHoliday) {
                                                if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = if (userPlansCount == 0) 2 else 1,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 13.sp
                                        )
                                    }

                                    // User plans preview (Clean separate line, aligned to right/start)
                                    if (userPlansCount > 0) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = if (occasions.isNotEmpty()) 2.dp else 0.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            if (dayEvents.isNotEmpty()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "📌 ${dayEvents.first().title}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                    )
                                                }
                                            }
                                            if (dayTasks.isNotEmpty()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                                    shape = RoundedCornerShape(3.dp)
                                                ) {
                                                    Text(
                                                        text = "✓ ${dayTasks.first().title}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                    )
                                                }
                                            }
                                            if (userPlansCount > 2) {
                                                Text(
                                                    text = "+${userPlansCount - 2}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    } else if (occasions.isEmpty()) {
                                        Text(
                                            text = "—",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        )
                                    }
                                }

                                // Continuous Vertical Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(gridBorderColor)
                                )

                                // Column 5: Gregorian Day Number (Compact width = 22.dp)
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gDate.day.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isFriday || isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = androidx.compose.ui.text.TextStyle(
                                            textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                        )
                                    )
                                }
                            }

                            if (idx < 6) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = gridBorderColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Interactive Day Detail Modal Bottom Sheet (اوقات شرعی، ویرایش مناسبت، کارهای روز، افزودن رویداد و وظیفه)
        sheetDate?.let { date ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val sheetEvents = eventDatesMap[date.formatted].orEmpty()
            val sheetTasks = taskDatesMap[date.formatted].orEmpty()
            val sheetOccasions = remember(date, occasionsVersion) {
                PersianOccasionsHelper.getOccasionsForDate(date.month, date.day, date.year)
            }
            val isSheetHoliday = remember(date, occasionsVersion) {
                PersianOccasionsHelper.isHoliday(date.month, date.day, date.year)
            }
            val gDate = remember(date) { PersianCalendarHelper.jalaliToGregorian(date.year, date.month, date.day) }
            val hDate = remember(date) {
                PersianOccasionsHelper.getHijriDate(date.year, date.month, date.day)
                    ?: HijriCalendarUtils.persianToIslamic(date.year, date.month, date.day)
            }
            val hijriMonths = listOf("محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه")
            val hMonthName = hijriMonths.getOrElse(hDate.month - 1) { "" }
            val dowName = strings.weekdayNames.getOrElse(PersianCalendarHelper.getPersianDayOfWeek(date.year, date.month, date.day)) { "" }

            ModalBottomSheet(
                onDismissRequest = { sheetDate = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("day_detail_bottom_sheet"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header Date Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$dowName ${strings.formatDigits(date.day.toString())} ${date.monthName} ${strings.formatDigits(date.year.toString())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = if (isSheetHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${gDate.day} ${gDate.monthName} ${gDate.year}   •   ${strings.formatDigits(hDate.day.toString())} \u200E$hMonthName\u200E",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                )
                            )
                        }

                        IconButton(
                            onClick = { sheetDate = null },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // 1. Prayer Times for Selected Date (درخواست کاربر: اول اوقات شرعی)
                    PrayerTimesCard(viewModel = viewModel)

                    // 2. Day Tasks & Events Section (درخواست کاربر: بعد از اوقات شرعی، قسمت وظایف و رویداد)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                sheetDate = null
                                onAddEventClick(date)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addEvent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                sheetDate = null
                                onAddTaskClick(date)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.addTask, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Day Plans & Tasks List
                    if (sheetEvents.isEmpty() && sheetTasks.isEmpty()) {
                        Text(
                            text = strings.noPlansForDay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if ((sheetEvents.size + sheetTasks.size) > 2) 160.dp else 100.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sheetEvents, key = { it.id }) { ev ->
                                EventItemCard(
                                    event = ev,
                                    onClick = {
                                        sheetDate = null
                                        onEditEventClick(ev)
                                    },
                                    onEdit = {
                                        sheetDate = null
                                        onEditEventClick(ev)
                                    },
                                    onDelete = { viewModel.deleteEvent(ev.id) }
                                )
                            }
                            items(sheetTasks, key = { it.id }) { t ->
                                TaskItemCard(
                                    task = t,
                                    onToggle = { isChecked -> viewModel.toggleTask(t.id, isChecked) },
                                    onEdit = {
                                        sheetDate = null
                                        onEditTaskClick(t)
                                    },
                                    onDelete = { viewModel.deleteTask(t.id) }
                                )
                            }
                        }
                    }

                    // 3. Occasions Box with Edit / Add Button (درخواست کاربر: در آخر قسمت مناسبت)
                    Surface(
                        color = if (isSheetHoliday) {
                            if (isDarkTheme) Color(0xFF331414) else Color(0xFFFEE2E2)
                        } else {
                            if (isDarkTheme) Color(0xFF162A22) else Color(0xFFF0FDF4)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSheetHoliday) Color(0xFFF87171).copy(alpha = 0.5f) else Color(0xFF34D399).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSheetHoliday) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.occasionsOfDayTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSheetHoliday) MaterialTheme.colorScheme.error else Color(0xFF166534)
                                    )
                                }

                                // دکمه ثبت مناسبت دلخواه برای این روز
                                Surface(
                                    color = if (isDarkTheme) Color(0xFF2DD4BF).copy(alpha = 0.15f) else Color(0xFF16A34A).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { showAddOccasionDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(strings.addOccasion, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF166534))
                                    }
                                }
                            }

                            if (sheetOccasions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                sheetOccasions.forEach { occ ->
                                    val isCustom = occ.customMonth != -1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${occ.title}",
                                            fontSize = 11.sp,
                                            fontWeight = if (occ.isHoliday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (occ.isHoliday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // دکمه ویرایش مناسبت (درخواست کاربر)
                                        IconButton(
                                            onClick = { occasionToEdit = occ },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = strings.edit,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }

                                        if (isCustom) {
                                            IconButton(
                                                onClick = {
                                                    PersianOccasionsHelper.removeCustomOccasion(context, occ)
                                                    viewModel.refreshOccasions()
                                                    viewModel.setSelectedDate(date)
                                                },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = strings.delete,
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Edit Occasion Dialog
        occasionToEdit?.let { occ ->
            val curDate = sheetDate ?: selectedDate
            EditOccasionDialog(
                occasion = occ,
                currentDate = curDate,
                onDismiss = { occasionToEdit = null },
                onConfirm = { newTitle, month, day, isHoliday ->
                    PersianOccasionsHelper.updateCustomOccasion(
                        context, occ, newTitle, month, day, isHoliday
                    )
                    viewModel.refreshOccasions()
                    viewModel.setSelectedDate(curDate)
                    occasionToEdit = null
                },
                onDelete = {
                    PersianOccasionsHelper.removeCustomOccasion(context, occ)
                    viewModel.refreshOccasions()
                    viewModel.setSelectedDate(curDate)
                    occasionToEdit = null
                }
            )
        }

        // Add Custom Occasion Dialog
        if (showAddOccasionDialog) {
            val targetDate = sheetDate ?: selectedDate
            var occasionTitle by remember { mutableStateOf("") }
            var isHolidayCheck by remember { mutableStateOf(false) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddOccasionDialog = false },
                title = {
                    Text(
                        "ثبت مناسبت توسط کاربر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${PersianCalendarHelper.toPersianDigits(targetDate.day.toString())} ${targetDate.monthName}  (هر سال تکرار می‌شود)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color(0xFF2DD4BF) else Color(0xFF166534)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = occasionTitle,
                            onValueChange = { occasionTitle = it },
                            label = { Text("عنوان مناسبت") },
                            placeholder = { Text("مثلاً: سالگرد، تولد...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isHolidayCheck,
                                onCheckedChange = { isHolidayCheck = it }
                            )
                            Text("روز تعطیل", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (occasionTitle.isNotBlank()) {
                                PersianOccasionsHelper.addCustomOccasion(
                                    context, occasionTitle,
                                    targetDate.month, targetDate.day,
                                    isHolidayCheck
                                )
                                showAddOccasionDialog = false
                                viewModel.refreshOccasions()
                                viewModel.setSelectedDate(targetDate)
                            }
                        }
                    ) { Text("ثبت") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddOccasionDialog = false }) {
                        Text("انصراف")
                    }
                }
            )
        }
    }
}
