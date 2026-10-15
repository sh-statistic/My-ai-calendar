package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.chat.AiProvider
import com.example.chat.ApiKeyManager
import com.example.memento.MementoMoriManager
import com.example.prayer.PrayerTimesCalculator
import com.example.settings.AppPreferences
import com.example.ui.LocalAppLanguage
import com.example.ui.MainViewModel

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4

    // Step 1: Language
    var selectedLanguage by remember { mutableStateOf(AppPreferences.getAppLanguage(context)) }

    // Step 2: Prayer Times City
    var selectedCityName by remember { mutableStateOf("تهران") }
    var citySearchQuery by remember { mutableStateOf("") }

    // Step 3: Life Grid & Birth Date
    var userBirthYearText by remember { mutableStateOf("") }
    var selectedBirthMonth by remember { mutableIntStateOf(1) } // 1 for Farvardin

    // Step 4: AI Setup
    var enableAi by remember { mutableStateOf(false) }
    var selectedAiProvider by remember { mutableStateOf(AiProvider.GEMINI) }
    var apiKeyText by remember { mutableStateOf("") }
    var customBaseUrlText by remember { mutableStateOf("") }
    var customModelText by remember { mutableStateOf("") }

    val isRtl = selectedLanguage == "fa"
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun completeOnboarding() {
        // Save Language
        AppPreferences.setAppLanguage(context, selectedLanguage)

        // Save City
        val matchedCity = PrayerTimesCalculator.CITIES.firstOrNull { it.nameFa == selectedCityName }
            ?: PrayerTimesCalculator.CITIES.first()
        viewModel.selectCity(matchedCity)

        // Save Birth date (year & month)
        val birthYear = userBirthYearText.toIntOrNull()
        if (birthYear != null && birthYear in 1300..1420) {
            MementoMoriManager(context).setBirthDate(birthYear, selectedBirthMonth)
        }

        // Save AI Provider and Configuration if enabled
        if (enableAi) {
            ApiKeyManager.saveSelectedProvider(context, selectedAiProvider)
            if (apiKeyText.isNotBlank()) {
                ApiKeyManager.saveApiKey(context, apiKeyText.trim(), selectedAiProvider)
            }
            if (selectedAiProvider == AiProvider.OPENAI_COMPATIBLE) {
                if (customBaseUrlText.isNotBlank()) {
                    ApiKeyManager.saveBaseUrl(context, customBaseUrlText.trim(), selectedAiProvider)
                }
                if (customModelText.isNotBlank()) {
                    ApiKeyManager.saveModel(context, customModelText.trim(), selectedAiProvider)
                }
            }
        }

        // Mark as completed
        AppPreferences.setOnboardingCompleted(context, true)
        onFinished()
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalAppLanguage provides selectedLanguage
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip button and step dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == currentStep) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                // Skip all
                TextButton(onClick = { completeOnboarding() }) {
                    Text(
                        if (isRtl) "رد شدن و ورود" else "Skip",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Step Content with animated transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    label = "onboarding_steps"
                ) { step ->
                    when (step) {
                        0 -> LanguageStep(
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { newLang ->
                                selectedLanguage = newLang
                                viewModel.setAppLanguage(newLang)
                            }
                        )
                        1 -> CityStep(
                            selectedCityName = selectedCityName,
                            searchQuery = citySearchQuery,
                            onSearchQueryChange = { citySearchQuery = it },
                            onSelectCity = { selectedCityName = it },
                            isRtl = isRtl
                        )
                        2 -> AgeStep(
                            userBirthYearText = userBirthYearText,
                            onBirthYearChange = { userBirthYearText = it },
                            selectedBirthMonth = selectedBirthMonth,
                            onBirthMonthChange = { selectedBirthMonth = it },
                            isRtl = isRtl
                        )
                        3 -> AiStep(
                            enableAi = enableAi,
                            onEnableAiChange = { enableAi = it },
                            selectedProvider = selectedAiProvider,
                            onProviderChange = { selectedAiProvider = it },
                            apiKeyText = apiKeyText,
                            onApiKeyChange = { apiKeyText = it },
                            customBaseUrlText = customBaseUrlText,
                            onBaseUrlChange = { customBaseUrlText = it },
                            customModelText = customModelText,
                            onModelChange = { customModelText = it },
                            isRtl = isRtl
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isRtl) "قبلی" else "Back",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRtl) "مرحله قبل" else "Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            completeOnboarding()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (currentStep == totalSteps - 1) {
                            if (isRtl) "ورود به تقویم همگام" else "Get Started"
                        } else {
                            if (isRtl) "مرحله بعد" else "Next"
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (currentStep < totalSteps - 1) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = if (isRtl) "بعدی" else "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = if (isRtl) "تایید" else "Confirm", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
}

@Composable
private fun LanguageStep(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "زبان برنامه را انتخاب کنید",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose your preferred language",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Persian Option
        LanguageCard(
            title = "فارسی (پیش‌فرض)",
            subtitle = "تقویم جلالی، اوقات شرعی و مناسبت‌های رسمی",
            isSelected = selectedLanguage == "fa",
            onClick = { onLanguageChange("fa") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // English Option
        LanguageCard(
            title = "English",
            subtitle = "Solar Hijri Calendar, Prayers & Events",
            isSelected = selectedLanguage == "en",
            onClick = { onLanguageChange("en") }
        )
    }
}

@Composable
private fun LanguageCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CityStep(
    selectedCityName: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectCity: (String) -> Unit,
    isRtl: Boolean
) {
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) PrayerTimesCalculator.CITIES
        else PrayerTimesCalculator.CITIES.filter { it.nameFa.contains(searchQuery.trim()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (isRtl) "شهر اوقات شرعی شما" else "Prayer Times City",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) "اوقات شرعی و اذان صبح بر اساس این شهر محاسبه می‌شود" else "Daily prayer times will be calculated for this city",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(if (isRtl) "جستجوی شهر (مثال: تهران، مشهد، اصفهان...)" else "Search city...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredCities) { city ->
                val isSelected = city.nameFa == selectedCityName
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCity(city.nameFa) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = city.nameFa,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgeStep(
    userBirthYearText: String,
    onBirthYearChange: (String) -> Unit,
    selectedBirthMonth: Int,
    onBirthMonthChange: (Int) -> Unit,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isRtl) "جدول روزهای عمر من" else "My Life Grid",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD97706)
        )
        Text(
            text = if (isRtl) "نمایش بصری ماه‌های سپری‌شده از زندگی به شیوه Memento Mori" else "Visualize the precious months of your lifespan",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Purpose Explanation Card (Reassuring the user)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isRtl) "چرا این اطلاعات را می‌پرسیم؟" else "Why do we ask this?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isRtl)
                            "بخش «عمر من» با الهام از فلسفه خردمندانه Memento Mori، ماه‌های سپری‌شده و باقی‌مانده از عمر را به صورت خانه‌های بصری نمایش می‌دهد تا قدردان تک‌تک روزهای زندگی باشیم.\n🔒 محرمانگی کامل: کلیه اطلاعات صرفاً روی حافظه محلی گوشی شما نگهداری شده و هیچ‌گونه ارسال به اینترنت یا سرور ندارد."
                        else
                            "The Life Grid visualizes lived months inspired by Memento Mori, reminding us of the value of each moment.\n🔒 Completely private: stored locally on your device only, never sent to any server.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Solar Birth Year Input Field
        OutlinedTextField(
            value = userBirthYearText,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onBirthYearChange(it) },
            label = { Text(if (isRtl) "سال تولد شمسی (مثال: ۱۳۷۸)" else "Solar birth year (e.g. 1378)") },
            placeholder = { Text(if (isRtl) "مثال: ۱۳۷۵" else "e.g. 1375") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Birth Month Selector (Exactly like LifeGridScreen)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (isRtl) "ماه تولد:" else "Birth Month:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PersianCalendarHelper.PERSIAN_MONTH_NAMES.forEachIndexed { i, name ->
                    FilterChip(
                        selected = selectedBirthMonth == i + 1,
                        onClick = { onBirthMonthChange(i + 1) },
                        label = { Text(name, fontSize = 10.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isRtl) "اختیاری - هر زمان در تب «عمر من» نیز قابل ویرایش یا تکمیل است" else "Optional - you can always set or change it later in My Life",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AiStep(
    enableAi: Boolean,
    onEnableAiChange: (Boolean) -> Unit,
    selectedProvider: AiProvider,
    onProviderChange: (AiProvider) -> Unit,
    apiKeyText: String,
    onApiKeyChange: (String) -> Unit,
    customBaseUrlText: String,
    onBaseUrlChange: (String) -> Unit,
    customModelText: String,
    onModelChange: (String) -> Unit,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isRtl) "دستیار هوشمند تقویم" else "AI Calendar Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isRtl) "دستیار صوتی و متنی هوشمند برای برنامه‌ریزی رویدادها، وظایف و اهداف" else "Intelligent assistant for planning events and tasks",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRtl) "تمایل به استفاده از هوش مصنوعی دارم" else "Enable AI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isRtl) "با کلید گوگل، OpenAI، DeepSeek یا سرور دلخواه" else "Using Google, OpenAI, DeepSeek, or custom servers",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = enableAi, onCheckedChange = onEnableAiChange)
            }
        }

        AnimatedVisibility(visible = enableAi) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Provider Selection
                Text(
                    text = if (isRtl) "انتخاب ارائه‌دهنده هوش مصنوعی:" else "Select AI Provider:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Google Gemini Option
                    val isGemini = selectedProvider == AiProvider.GEMINI
                    Surface(
                        color = if (isGemini) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProviderChange(AiProvider.GEMINI) }
                            .then(
                                if (isGemini) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Google Gemini",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isGemini) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRtl) "رایگان و سریع" else "Free & Fast",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // OpenAI Compatible Option
                    val isOpenAi = selectedProvider == AiProvider.OPENAI_COMPATIBLE
                    Surface(
                        color = if (isOpenAi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onProviderChange(AiProvider.OPENAI_COMPATIBLE) }
                            .then(
                                if (isOpenAi) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OpenAI Compatible",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isOpenAi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isRtl) "OpenAI, DeepSeek, Groq..." else "OpenAI, DeepSeek...",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // API Key Field
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = onApiKeyChange,
                    label = {
                        Text(
                            if (selectedProvider == AiProvider.GEMINI) {
                                if (isRtl) "کلید Google Gemini API Key" else "Gemini API Key"
                            } else {
                                if (isRtl) "کلید API (مانند sk-...)" else "API Key"
                            }
                        )
                    },
                    placeholder = { Text(selectedProvider.keyHint, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Extra Server & Model settings for OpenAI Compatible
                if (selectedProvider == AiProvider.OPENAI_COMPATIBLE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isRtl) "تنظیمات آدرس و مدل (اختیاری):" else "Server & Model (Optional):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = customBaseUrlText,
                                onValueChange = onBaseUrlChange,
                                placeholder = { Text(selectedProvider.defaultBaseUrl, fontSize = 10.sp) },
                                label = { Text(if (isRtl) "آدرس سرور (Base URL)" else "Base URL", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customModelText,
                                onValueChange = onModelChange,
                                placeholder = { Text(selectedProvider.defaultModel, fontSize = 10.sp) },
                                label = { Text(if (isRtl) "نام مدل (Model Name)" else "Model Name", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Privacy and help note
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRtl)
                                "کلیدهای هوش مصنوعی فقط روی حافظه محلی دستگاه شما نگهداری شده و مستقیماً با سرور هوش مصنوعی ارتباط برقرار می‌کند."
                            else
                                "API keys are stored safely on your device and communicate directly with the AI provider.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (isRtl) "می‌توانید این مرحله را رد کرده و هر زمان در تنظیمات کلید را وارد نمایید." else "You can skip this step and configure AI anytime in Settings.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
