package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.prayer.PrayerTimesCalculator
import com.example.ui.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerTimesCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayer by viewModel.nextPrayerInfo.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val alarmSettings by viewModel.fajrAlarmSettings.collectAsState()

    var showCityDropdown by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_times_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: City Selector + Next Prayer Badge (Scalable for small screens)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // City Dropdown Button
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { showCityDropdown = true }
                            .testTag("btn_select_city")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = selectedCity.nameFa,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCityDropdown,
                        onDismissRequest = { showCityDropdown = false }
                    ) {
                        PrayerTimesCalculator.CITIES.forEach { city ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(city.nameFa, fontWeight = if (city.nameFa == selectedCity.nameFa) FontWeight.Bold else FontWeight.Normal)
                                        if (city.nameFa == selectedCity.nameFa) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.selectCity(city)
                                    showCityDropdown = false
                                }
                            )
                        }
                    }
                }

                // Next Prayer Countdown Pill
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("badge_next_prayer")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Mosque,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val hours = nextPrayer.remainingMinutes / 60
                        val mins = nextPrayer.remainingMinutes % 60
                        val remainingStr = when {
                            hours > 0 -> "${hours} ساعت و ${mins} دقیقه"
                            else -> "${mins} دقیقه"
                        }
                        Text(
                            text = "${nextPrayer.name}: ${PersianCalendarHelper.toPersianDigits(nextPrayer.time)} (${PersianCalendarHelper.toPersianDigits(remainingStr)})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Prayer Times 6-Column Grid (Equally Weighted & Fully Scalable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PrayerItem(title = "اذان صبح", time = prayerTimes.fajr, icon = Icons.Default.NightsStay, isNext = nextPrayer.name.contains("صبح"))
                PrayerItem(title = "طلوع", time = prayerTimes.sunrise, icon = Icons.Default.WbSunny, isNext = nextPrayer.name.contains("طلوع"))
                PrayerItem(title = "اذان ظهر", time = prayerTimes.dhuhr, icon = Icons.Default.WbSunny, isNext = nextPrayer.name.contains("ظهر"))
                PrayerItem(title = "غروب", time = prayerTimes.sunset, icon = Icons.Default.WbTwilight, isNext = nextPrayer.name.contains("غروب"))
                PrayerItem(title = "اذان مغرب", time = prayerTimes.maghrib, icon = Icons.Default.WbTwilight, isNext = nextPrayer.name.contains("مغرب"))
                PrayerItem(title = "نیمه‌شب", time = prayerTimes.midnight, icon = Icons.Default.NightsStay, isNext = nextPrayer.name.contains("نیمه‌شب"))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Smart Fajr Alarm Section Toggle
            Surface(
                color = if (alarmSettings.isEnabled) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fajr_smart_alarm_card")
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (alarmSettings.isEnabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (alarmSettings.isEnabled) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "بیدارباش هوشمند اذان صبح",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alarmSettings.isEnabled) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (alarmSettings.isEnabled)
                                        "محاسبه و زمان‌بندی پویا روزانه (${PersianCalendarHelper.toPersianDigits(alarmSettings.offsetMinutesBefore.toString())} دقیقه قبل از اذان)"
                                    else
                                        "آلارم خودکار بیدارباش قبل از اذان صبح",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = alarmSettings.isEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateFajrAlarm(isChecked, alarmSettings.offsetMinutesBefore)
                            },
                            modifier = Modifier.testTag("toggle_fajr_alarm")
                        )
                    }

                    // Offset selection chips if alarm is enabled
                    AnimatedVisibility(visible = alarmSettings.isEnabled) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Text(
                                text = "زمان پخش هشدار:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val offsets = listOf(
                                    Pair(0, "همزمان با اذان"),
                                    Pair(5, "۵ دقیقه قبل"),
                                    Pair(10, "۱۰ دقیقه قبل"),
                                    Pair(15, "۱۵ دقیقه قبل"),
                                    Pair(20, "۲۰ دقیقه قبل"),
                                    Pair(30, "۳۰ دقیقه قبل")
                                )
                                offsets.forEach { (mins, label) ->
                                    val isSelected = alarmSettings.offsetMinutesBefore == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateFajrAlarm(true, mins)
                                        },
                                        label = { Text(label, fontSize = 9.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFD97706),
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("chip_offset_$mins")
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

@Composable
private fun RowScope.PrayerItem(
    title: String,
    time: String,
    icon: ImageVector,
    isNext: Boolean
) {
    val bgColor = if (isNext) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    val borderCol = if (isNext) MaterialTheme.colorScheme.primary else Color.Transparent

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = PersianCalendarHelper.toPersianDigits(time),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
