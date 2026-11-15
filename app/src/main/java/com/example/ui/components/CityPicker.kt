package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.prayer.IranianCity
import com.example.prayer.PrayerTimesCalculator

/**
 * City picker with a search box and province drill-down, shared by Settings and
 * Onboarding: with an empty query the user first picks a province and then a
 * city inside it; typing in the search box searches across all cities at once.
 * The 427-city list is never one long flat scroll.
 */
@Composable
fun CityPickerContent(
    selectedCityName: String?,
    onSelectCity: (IranianCity) -> Unit,
    isFa: Boolean,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var province by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(if (isFa) "جستجوی شهر (مثال: تهران، مشهد...)" else "Search city...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (query.isNotBlank()) {
            val results = remember(query) { PrayerTimesCalculator.searchCities(query) }
            if (results.isEmpty()) {
                Text(
                    if (isFa) "شهری با این نام پیدا نشد" else "No city found",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results) { city ->
                        CityPickerRow(
                            title = if (isFa) city.nameFa else city.nameEn,
                            caption = city.province,
                            isSelected = city.nameFa == selectedCityName || city.nameEn == selectedCityName,
                            onClick = { onSelectCity(city) }
                        )
                    }
                }
            }
        } else if (province == null) {
            val grouped = remember { PrayerTimesCalculator.citiesByProvince() }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(grouped.entries.toList()) { (prov, cities) ->
                    CityPickerRow(
                        title = prov,
                        caption = if (isFa) {
                            PersianCalendarHelper.toPersianDigits(cities.size.toString()) + " شهر"
                        } else {
                            "${cities.size} cities"
                        },
                        isSelected = false,
                        onClick = { province = prov }
                    )
                }
            }
        } else {
            val currentProvince = province!!
            TextButton(onClick = { province = null }) {
                Text(if (isFa) "‹ همهٔ استان‌ها" else "‹ All provinces")
            }
            val cities = remember(currentProvince) { PrayerTimesCalculator.citiesInProvince(currentProvince) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(cities) { city ->
                    CityPickerRow(
                        title = if (isFa) city.nameFa else city.nameEn,
                        caption = null,
                        isSelected = city.nameFa == selectedCityName || city.nameEn == selectedCityName,
                        onClick = { onSelectCity(city) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CityPickerRow(
    title: String,
    caption: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else if (caption != null) {
                Text(caption, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
