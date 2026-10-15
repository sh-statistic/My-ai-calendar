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
import com.example.ui.rememberAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerTimesCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val prayerTimes by viewModel.prayerTimes.collectAsState()
    val nextPrayer by viewModel.nextPrayerInfo.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val strings = rememberAppStrings()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_times_card")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Fixed City Display (Changes only in Settings) + Next Prayer Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // City Display - Fixed text badge, not clickable
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
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
                            text = strings.cityName(selectedCity),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
                        Text(
                            text = strings.nextPrayerRemaining(nextPrayer.name, nextPrayer.time, hours, mins),
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
                PrayerItem(title = strings.fajr, time = strings.formatDigits(prayerTimes.fajr), icon = Icons.Default.NightsStay, isNext = nextPrayer.name.contains("صبح") || nextPrayer.name.contains("Fajr"))
                PrayerItem(title = strings.sunrise, time = strings.formatDigits(prayerTimes.sunrise), icon = Icons.Default.WbSunny, isNext = nextPrayer.name.contains("طلوع") || nextPrayer.name.contains("Sunrise"))
                PrayerItem(title = strings.dhuhr, time = strings.formatDigits(prayerTimes.dhuhr), icon = Icons.Default.WbSunny, isNext = nextPrayer.name.contains("ظهر") || nextPrayer.name.contains("Dhuhr"))
                PrayerItem(title = strings.sunset, time = strings.formatDigits(prayerTimes.sunset), icon = Icons.Default.WbTwilight, isNext = nextPrayer.name.contains("غروب") || nextPrayer.name.contains("Sunset"))
                PrayerItem(title = strings.maghrib, time = strings.formatDigits(prayerTimes.maghrib), icon = Icons.Default.WbTwilight, isNext = nextPrayer.name.contains("مغرب") || nextPrayer.name.contains("Maghrib"))
                PrayerItem(title = strings.midnight, time = strings.formatDigits(prayerTimes.midnight), icon = Icons.Default.NightsStay, isNext = nextPrayer.name.contains("نیمه‌شب") || nextPrayer.name.contains("Midnight"))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accuracy Disclaimer / Caution Note
            Text(
                text = strings.prayerTimesAccuracyNote,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
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
            text = time,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
