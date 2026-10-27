package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.memento.LifeProgress
import com.example.memento.MementoMoriManager
import com.example.ui.MainViewModel
import com.example.ui.rememberAppStrings

// Adaptive Color Palette for Light and Dark Modes
data class LifeGridColors(
    val bgScreen: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val textMain: Color,
    val subtleGray: Color,
    val grayText: Color,
    val orangeAccent: Color,
    val elapsedDot: Color,
    val currentMonthDot: Color,
    val futureDot: Color,
    val currentYearBg: Color,
    val elapsedCardBg: Color,
    val elapsedCardTitle: Color,
    val remainingCardBg: Color,
    val remainingCardTitle: Color,
    val progressTrack: Color,
    val progressFill: Color
)

@Composable
fun rememberLifeGridColors(): LifeGridColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    return if (isDark) {
        LifeGridColors(
            bgScreen = Color(0xFF0F0F12), // Deep neutral crystal dark
            cardBg = Color(0xFF18181C), // Elevated crystal card
            cardBorder = Color(0xFF282830), // Crisp glass border
            textMain = Color(0xFFFFFFFF), // Pure crisp white
            subtleGray = Color(0xFFA1A1AA), // Clean light neutral gray
            grayText = Color(0xFF71717A), // Subtle gray
            orangeAccent = Color(0xFFF59E0B), // Vibrant amber
            elapsedDot = Color(0xFFD4D4D8), // Crisp silver crystal dots
            currentMonthDot = Color(0xFFF59E0B), // Radiant amber jewel
            futureDot = Color(0xFF27272A), // Clean deep graphite for future dots
            currentYearBg = Color(0xFF261D12), // Subtle crystal amber glow
            elapsedCardBg = Color(0xFF222228), // Sleek neutral card surface (no muddy brown!)
            elapsedCardTitle = Color(0xFFF59E0B),
            remainingCardBg = Color(0xFF222228), // Sleek neutral card surface (no muddy blue!)
            remainingCardTitle = Color(0xFF38BDF8),
            progressTrack = Color(0xFF27272A),
            progressFill = Color(0xFFF59E0B)
        )
    } else {
        LifeGridColors(
            bgScreen = Color(0xFFFDF9F3),
            cardBg = Color(0xFFFFFFFF),
            cardBorder = Color(0xFFEADFCD),
            textMain = Color(0xFF3A2F1F),
            subtleGray = Color(0xFF6B7280),
            grayText = Color(0xFF9CA3AF),
            orangeAccent = Color(0xFFF59E0B),
            elapsedDot = Color(0xFF1E293B),
            currentMonthDot = Color(0xFFF59E0B),
            futureDot = Color(0xFFD5DCEA),
            currentYearBg = Color(0xFFFDE7B0),
            elapsedCardBg = Color(0xFFFFF3C4),
            elapsedCardTitle = Color(0xFF92400E),
            remainingCardBg = Color(0xFFEAF0FB),
            remainingCardTitle = Color(0xFF1E40AF),
            progressTrack = Color(0xFFE5E7EB),
            progressFill = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeGridScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context  = LocalContext.current
    val strings  = rememberAppStrings()
    val colors   = rememberLifeGridColors()
    val manager  = remember { MementoMoriManager(context) }
    var progress by remember { mutableStateOf(manager.calculateProgress()) }

    var selectedYearIdx by remember { mutableStateOf<Int?>(null) }

    var showBirthDialog      by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var reflectionNoteText   by remember { mutableStateOf("") }
    var justDrilledMonthIdx  by remember { mutableIntStateOf(0) }

    val totalGridSlots = progress.totalMonths + (progress.birthMonth - 1)
    val totalYears = (totalGridSlots + 11) / 12
    val decades    = (totalYears + 9) / 10

    var showNewMonthPrompt by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(progress.currentMonthIndex) {
        if (progress.isConfigured && manager.checkNewMonthCompleted(progress.currentMonthIndex)) {
            showNewMonthPrompt = true
        }
    }

    val scrollState = rememberScrollState()

    if (showNewMonthPrompt) {
        AlertDialog(
            onDismissRequest = { showNewMonthPrompt = false },
            title = { Text(strings.anotherMonthPassedTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.anotherMonthPassedMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showNewMonthPrompt = false
                        justDrilledMonthIdx = progress.currentMonthIndex - 1
                        reflectionNoteText = manager.getReflectionNote(justDrilledMonthIdx) ?: ""
                        showReflectionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orangeAccent)
                ) { Text(strings.writeNoteConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showNewMonthPrompt = false }) { Text(strings.later) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgScreen)
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        SummaryCard(progress = progress, colors = colors, onEditClick = { showBirthDialog = true })
        Spacer(modifier = Modifier.height(10.dp))
        LegendRow(colors = colors)
        Spacer(modifier = Modifier.height(10.dp))
        WholeLifeCard(
            progress        = progress,
            colors          = colors,
            totalYears      = totalYears,
            decades         = decades,
            selectedYearIdx = selectedYearIdx,
            onYearSelected  = { selectedYearIdx = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Selected Year Details Dialog
    selectedYearIdx?.let { yearIdx ->
        AlertDialog(
            onDismissRequest = { selectedYearIdx = null },
            title = {
                SelectedYearDialogHeader(progress = progress, selectedYearIdx = yearIdx, colors = colors)
            },
            text = {
                SelectedYearDialogContent(
                    progress        = progress,
                    selectedYearIdx = yearIdx,
                    colors          = colors,
                    onMonthClick    = { clickedMonthIdx ->
                        justDrilledMonthIdx = clickedMonthIdx
                        if (clickedMonthIdx <= progress.currentMonthIndex) {
                            reflectionNoteText = manager.getReflectionNote(clickedMonthIdx) ?: ""
                            selectedYearIdx = null
                            showReflectionDialog = true
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedYearIdx = null }) { Text(strings.close) }
            }
        )
    }

    // Reflection Dialog
    if (showReflectionDialog) {
        AlertDialog(
            onDismissRequest = { showReflectionDialog = false },
            title  = { Text(strings.howWasThisMonthTitle, fontWeight = FontWeight.Bold) },
            text   = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        strings.howWasThisMonthMsg,
                        fontSize = 13.sp, color = colors.subtleGray
                    )
                    OutlinedTextField(
                        value         = reflectionNoteText,
                        onValueChange = { reflectionNoteText = it },
                        placeholder   = { Text(strings.notePlaceholder) },
                        minLines = 3, maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reflectionNoteText.isNotBlank()) {
                            manager.saveReflectionNote(justDrilledMonthIdx, reflectionNoteText.trim())
                        }
                        showReflectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orangeAccent)
                ) { Text(strings.saveNote) }
            },
            dismissButton = {
                TextButton(onClick = { showReflectionDialog = false }) { Text(strings.later) }
            }
        )
    }

    // Birth Date Dialog
    if (showBirthDialog) {
        var tempYear     by remember { mutableStateOf(if (progress.isConfigured) progress.birthYear.toString() else "") }
        var tempMonth    by remember { mutableIntStateOf(progress.birthMonth) }
        var tempLifespan by remember { mutableStateOf((progress.totalMonths / 12).toString()) }
        val currentYear  = PersianCalendarHelper.getCurrentJalaliDate().year

        AlertDialog(
            onDismissRequest = { showBirthDialog = false },
            title   = { Text(strings.lifeSettingsTitle, fontWeight = FontWeight.Bold) },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.enterDetailsDesc, fontSize = 13.sp)
                    OutlinedTextField(
                        value         = tempYear,
                        onValueChange = { tempYear = it.filter { c -> c.isDigit() } },
                        label         = { Text(strings.birthYearLabel) },
                        placeholder   = { Text(strings.birthYearExample) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value         = tempLifespan,
                        onValueChange = { tempLifespan = it.filter { c -> c.isDigit() } },
                        label         = { Text(strings.expectedLifespan) },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                    Text(strings.birthMonthLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement   = Arrangement.spacedBy(4.dp)
                    ) {
                        strings.jalaliMonths.forEachIndexed { i, name ->
                            FilterChip(
                                selected = tempMonth == i + 1,
                                onClick  = { tempMonth = i + 1 },
                                label    = { Text(name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = tempYear.isNotBlank() && (tempYear.toIntOrNull() ?: 0) in 1300..currentYear,
                    onClick = {
                        val y  = tempYear.toIntOrNull() ?: return@Button
                        val ls = tempLifespan.toIntOrNull()?.coerceIn(10, 150) ?: (progress.totalMonths / 12)
                        manager.setBirthDate(y, tempMonth)
                        manager.setExpectedLifespan(ls)
                        progress = manager.calculateProgress()
                        showBirthDialog = false
                    }
                ) { Text(strings.saveAndUpdate) }
            },
            dismissButton = {
                Row {
                    if (progress.isConfigured) {
                        TextButton(
                            onClick = {
                                manager.clearBirthDate()
                                progress = manager.calculateProgress()
                                showBirthDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text(strings.deleteBirthDate) }
                    }
                    TextButton(onClick = { showBirthDialog = false }) { Text(strings.cancel) }
                }
            }
        )
    }
}

// ─── Summary Card ─────────────────────────────────────────────────────────────
@Composable
private fun SummaryCard(progress: LifeProgress, colors: LifeGridColors, onEditClick: () -> Unit) {
    val strings = rememberAppStrings()
    Surface(
        color    = colors.cardBg,
        shape    = RoundedCornerShape(16.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!progress.isConfigured) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassBottom, null, tint = colors.orangeAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.lifeGridTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Icon(
                    Icons.Default.Cake,
                    contentDescription = null,
                    tint = colors.orangeAccent,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    strings.birthDateNotSet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    strings.birthDateDesc,
                    fontSize = 11.sp,
                    color = colors.subtleGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.orangeAccent)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.editBirthDate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HourglassBottom, null, tint = colors.orangeAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.currentAndExpectedLife, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    }
                    Surface(
                        color    = colors.elapsedCardBg,
                        shape    = RoundedCornerShape(20.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, colors.orangeAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onEditClick() }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(11.dp), tint = colors.orangeAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                strings.bornInfo(progress.birthYear.toString(), (progress.totalMonths / 12).toString()),
                                fontSize = 11.sp, color = colors.orangeAccent, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = colors.elapsedCardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(strings.currentAge, fontSize = 11.sp, color = colors.elapsedCardTitle, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                strings.ageSpan(progress.elapsedYears.toString(), progress.elapsedRemainingMonths.toString()),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain
                            )
                            Text(strings.monthsCount(progress.elapsedMonths.toString()), fontSize = 11.sp, color = colors.subtleGray)
                        }
                    }
                    Surface(color = colors.remainingCardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(strings.remainingLife, fontSize = 11.sp, color = colors.remainingCardTitle, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val remMonths = progress.totalMonths - progress.elapsedMonths
                            Text(
                                strings.ageSpan(progress.remainingYears.toString(), progress.remainingMonths.toString()),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain
                            )
                            Text(strings.monthsCount(remMonths.toString()), fontSize = 11.sp, color = colors.subtleGray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val pct     = (progress.percentElapsed / 100f).coerceIn(0f, 1f)
                val pctText = "${strings.formatDigits((progress.percentElapsed + 0.5f).toInt().toString())}% ${strings.percentOfLife}"
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.progressTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pct)
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.progressFill)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(pctText, fontSize = 11.sp, color = colors.textMain, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Legend Row ───────────────────────────────────────────────────────────────
@Composable
private fun LegendRow(colors: LifeGridColors) {
    val strings = rememberAppStrings()
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        LegendDot(colors.elapsedDot,  strings.elapsedLegend, colors.subtleGray)
        LegendDot(colors.orangeAccent, strings.currentMonthLegend, colors.subtleGray)
        LegendDot(colors.futureDot,   strings.futureLegend, colors.subtleGray)
    }
}

@Composable
private fun LegendDot(color: Color, label: String, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = textColor)
    }
}

// ─── Whole Life Card ──────────────────────────────────────────────────────────
@Composable
private fun WholeLifeCard(
    progress: LifeProgress,
    colors: LifeGridColors,
    totalYears: Int,
    decades: Int,
    selectedYearIdx: Int?,
    onYearSelected: (Int) -> Unit
) {
    val strings = rememberAppStrings()
    Surface(
        color    = colors.cardBg,
        shape    = RoundedCornerShape(16.dp),
        border   = androidx.compose.foundation.BorderStroke(1.dp, colors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(strings.wholeLifeHeader, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Text(strings.tapYearHint, fontSize = 11.sp, color = colors.grayText)
            }
            Spacer(modifier = Modifier.height(10.dp))

            val density = LocalDensity.current
            var gridWidthPx by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .onGloballyPositioned { coords -> gridWidthPx = coords.size.width }
            ) {
                if (gridWidthPx > 0) {
                    val blocksPerRow    = 10
                    val gapDp           = 3.dp
                    val gapPx           = with(density) { gapDp.toPx() }
                    val blockWidthPx    = (gridWidthPx - gapPx * (blocksPerRow - 1)) / blocksPerRow.toFloat()
                    val blockWidthDp: Dp = with(density) { blockWidthPx.toDp() }

                    val dotCols       = 4
                    val dotRows       = 3
                    val dotPadPx      = with(density) { 1.5.dp.toPx() }
                    val dotSizePx     = ((blockWidthPx - dotPadPx * (dotCols + 1)) / dotCols).coerceAtLeast(2f)
                    val blockHeightPx = dotSizePx * dotRows + dotPadPx * (dotRows + 1)
                    val blockHeightDp: Dp = with(density) { blockHeightPx.toDp() }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (decade in 0 until decades) {
                            val startAge = decade * 10
                            val endAge   = minOf(startAge + 9, totalYears - 1)

                            val displayStart = startAge + 1
                            val displayEnd   = minOf(startAge + 10, totalYears)

                            Text(
                                strings.ageRange(displayStart.toString(), displayEnd.toString()),
                                fontSize  = 10.sp, color = colors.grayText,
                                modifier  = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(gapDp)
                            ) {
                                for (yearInDecade in 0..9) {
                                    val absYear = decade * 10 + yearInDecade
                                    if (absYear >= totalYears) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        continue
                                    }
                                    val isSelected    = absYear == selectedYearIdx
                                    val isCurrentYear = absYear == (progress.currentMonthIndex / 12)
                                    val yearBg        = if (isCurrentYear) colors.currentYearBg else Color.Transparent
                                    val borderColor   = if (isSelected) colors.orangeAccent else Color.Transparent
                                    val borderWidth   = if (isSelected) 1.5.dp else 0.dp

                                    Canvas(
                                        modifier = Modifier
                                            .width(blockWidthDp)
                                            .height(blockHeightDp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(yearBg)
                                            .border(borderWidth, borderColor, RoundedCornerShape(3.dp))
                                            .clickable { onYearSelected(absYear) }
                                    ) {
                                        drawYearBlock(
                                            absYear       = absYear,
                                            progress      = progress,
                                            colors        = colors,
                                            dotSizePx     = dotSizePx,
                                            dotPadPx      = dotPadPx,
                                            dotCols       = dotCols,
                                            dotRows       = dotRows,
                                            blockWidthPx  = blockWidthPx,
                                            blockHeightPx = blockHeightPx
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawYearBlock(
    absYear: Int,
    progress: LifeProgress,
    colors: LifeGridColors,
    dotSizePx: Float,
    dotPadPx: Float,
    dotCols: Int,
    dotRows: Int,
    blockWidthPx: Float,
    blockHeightPx: Float
) {
    val startMonthIdx   = absYear * 12
    val currentMonthIdx = progress.currentMonthIndex

    val totalDotW = dotCols * dotSizePx + (dotCols - 1) * dotPadPx
    val totalDotH = dotRows * dotSizePx + (dotRows - 1) * dotPadPx
    val ox = (blockWidthPx  - totalDotW) / 2f
    val oy = (blockHeightPx - totalDotH) / 2f

    val totalGridSlots = progress.totalMonths + (progress.birthMonth - 1)

    for (row in 0 until dotRows) {
        for (col in 0 until dotCols) {
            val monthInYear    = row * dotCols + col
            val absMonthIdx    = startMonthIdx + monthInYear
            if (absMonthIdx >= totalGridSlots) continue

            val cx = ox + col * (dotSizePx + dotPadPx) + dotSizePx / 2f
            val cy = oy + row * (dotSizePx + dotPadPx) + dotSizePx / 2f
            val center = Offset(cx, cy)
            val radius = dotSizePx / 2f

            when {
                !progress.isConfigured -> drawCircle(colors.futureDot, radius, center)
                absYear == 0 && monthInYear < (progress.birthMonth - 1) -> {
                    // Draw muted dots for months before birth
                    drawCircle(colors.futureDot.copy(alpha = 0.25f), radius, center)
                }
                absMonthIdx < currentMonthIdx -> drawCircle(colors.elapsedDot, radius, center)
                absMonthIdx == currentMonthIdx -> {
                    drawCircle(colors.currentMonthDot, radius, center)
                    drawCircle(colors.currentMonthDot.copy(alpha = 0.35f), radius + 1.5f, center, style = Stroke(1.5f))
                }
                else -> drawCircle(colors.futureDot, radius, center)
            }
        }
    }
}

// ─── Selected Year Dialog Components ──────────────────────────────────────────
@Composable
private fun SelectedYearDialogHeader(progress: LifeProgress, selectedYearIdx: Int, colors: LifeGridColors) {
    val strings = rememberAppStrings()
    val persianCalYear = progress.birthYear + selectedYearIdx
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            strings.ageRange(selectedYearIdx.toString(), (selectedYearIdx + 1).toString()),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain
        )
        if (progress.isConfigured) {
            Text(strings.yearLabel(persianCalYear.toString()), fontSize = 12.sp, color = colors.subtleGray)
        }
    }
}

@Composable
private fun SelectedYearDialogContent(
    progress: LifeProgress,
    selectedYearIdx: Int,
    colors: LifeGridColors,
    onMonthClick: (Int) -> Unit
) {
    val strings         = rememberAppStrings()
    val monthNames      = strings.jalaliMonths
    val startMonthIdx   = selectedYearIdx * 12
    val currentMonthIdx = progress.currentMonthIndex
    val isDark          = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column {
        for (row in 0 until 3) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 4) {
                    val monthInYear = row * 4 + col
                    val absMonthIdx = startMonthIdx + monthInYear
                    val monthName   = monthNames.getOrElse(monthInYear) { "" }

                    val totalGridSlots = progress.totalMonths + (progress.birthMonth - 1)
                    if (absMonthIdx >= totalGridSlots) {
                        Spacer(modifier = Modifier.weight(1f))
                        continue
                    }

                    if (selectedYearIdx == 0 && monthInYear < (progress.birthMonth - 1)) {
                        Spacer(modifier = Modifier.weight(1f))
                        continue
                    }

                    val isElapsed = absMonthIdx < currentMonthIdx
                    val isCurrent = absMonthIdx == currentMonthIdx
                    val isClickable = isElapsed || isCurrent
                    val chipBg    = when {
                        isElapsed -> if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.8f) else Color(0xFF1B2A4A)
                        isCurrent -> colors.orangeAccent
                        else -> if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFF0EDE8)
                    }
                    val chipText  = if (isClickable) Color.White else (if (isDark) Color(0xFF94A3B8) else colors.grayText)
                    val subtitle  = when { isElapsed -> strings.viewOrAddNote; isCurrent -> strings.tapToAddNote; else -> "—" }

                    Surface(
                        color    = chipBg,
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isClickable) { if (isClickable) onMonthClick(absMonthIdx) }
                    ) {
                        Column(
                            modifier            = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(monthName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = chipText, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(subtitle, fontSize = 9.sp, color = chipText.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (row < 2) Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
