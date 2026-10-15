package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ContentCopy
import android.net.Uri
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.PersianCalendarHelper
import com.example.chat.AiProvider
import com.example.chat.ApiKeyManager
import com.example.chat.ChatMessage
import com.example.chat.ChatRole
import com.example.chat.ChatSender
import com.example.ui.MainViewModel

@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.isGeneratingResponse.collectAsState()
    val selectedProvider by viewModel.selectedAiProvider.collectAsState()
    val isApiKeyConfigured by viewModel.isApiKeyConfigured.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val selectedRole by viewModel.selectedRole.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Voice Speech-to-Text Recognition Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                inputText = spokenText
                viewModel.sendChatMessage(spokenText)
            }
        }
    }

    val launchVoiceInput = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "دستور یا سوال تقویمی خود را بگویید...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "سرویس ورودی گفتار در دسترس نیست", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestionChips = when (selectedRole) {
        ChatRole.GENERAL -> listOf(
            "🎙️ یک رویداد کاری برای فردا ساعت ۱۰ ثبت کن",
            "🎙️ برنامه‌ها و کارهای امروز من چیست؟",
            "🎙️ اوقات شرعی و اذان ظهر چه ساعتی است؟",
            "چگونه وظایفم را اولویت‌بندی کنم؟"
        )
        ChatRole.COMPLEX_PLANNER -> listOf(
            "شکستن یک پروژه بزرگ به مراحل عملیاتی",
            "تحلیل وظایف امروز با ماتریس آیزنهاور",
            "استراتژی مدیریت زمان برای هفته جاری",
            "برنامه‌ریزی فصلی تقویم خورشیدی"
        )
        ChatRole.FAST_ASSISTANT -> listOf(
            "چک‌لیست سریع صبحگاهی",
            "خلاصه ۳ نکته کلیدی امروز",
            "پیشنهاد ۵ دقیقه‌ای برای شروع کار",
            "مرور سریع اهداف"
        )
        ChatRole.SOLAR_EXPERT -> listOf(
            "تبدیل دقیق تاریخ امروز به میلادی",
            "قوانین سال کبیسه در گاه‌شماری شمسی",
            "رویدادها و مناسبت‌های فصل جاری",
            "پیشنهاد تقسیم‌بندی ماه‌های خورشیدی"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Card with Model/Role Info and Actions
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    getRoleIcon(selectedRole),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دستیار هوشمند تقویم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (selectedProvider == AiProvider.GEMINI) "Google Gemini" else "OpenAI Compatible",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        maxLines = 1
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isApiKeyConfigured) Color(0xFF16A34A) else MaterialTheme.colorScheme.error)
                                    )
                                    Text(
                                        text = if (isApiKeyConfigured) "آماده" else "نیاز به کلید",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isApiKeyConfigured) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Voice trigger in header
                        IconButton(
                            onClick = launchVoiceInput,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_header_voice")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "ورودی گفتار",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // API Key Config Button
                        IconButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_configure_api_key")
                        ) {
                            Icon(
                                if (isApiKeyConfigured) Icons.Default.Key else Icons.Default.KeyOff,
                                contentDescription = "تنظیم سرویس هوش مصنوعی",
                                tint = if (isApiKeyConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear Chat Button
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "پاک‌سازی سابقه گفتگو",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Role Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ChatRole.entries) { role ->
                        val isSelected = selectedRole == role
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectRole(role) },
                            label = { Text(role.title, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(getRoleIcon(role), contentDescription = null, modifier = Modifier.size(12.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("chip_role_${role.name}")
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.text))
                        Toast.makeText(context, "متن در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${selectedRole.title} در حال بررسی و پردازش...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Suggestion Quick Prompt Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(suggestionChips) { chipText ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clickable {
                            val cleanText = chipText.replace("🎙️ ", "").trim()
                            inputText = cleanText
                            viewModel.sendChatMessage(cleanText)
                        }
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Bottom Input Row with Voice & Send Actions
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "بنویسید یا با ویس بگویید...",
                            fontSize = 12.sp
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                // Dedicated Microphone / Voice Button
                IconButton(
                    onClick = launchVoiceInput,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .testTag("btn_voice_input")
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "ورودی گفتار / ویس",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isGenerating) {
                            val msg = inputText
                            inputText = ""
                            viewModel.sendChatMessage(msg)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("btn_send_chat")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال پیام",
                        tint = if (inputText.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Configure AI Provider & API Key Dialog
    if (showApiKeyDialog) {
        AiProviderSetupDialog(
            initialProvider = selectedProvider,
            onDismiss = { showApiKeyDialog = false },
            onSave = { provider, key, baseUrl, model ->
                viewModel.updateAiConfiguration(
                    provider = provider,
                    apiKey = key,
                    customBaseUrl = baseUrl,
                    customModel = model
                )
                showApiKeyDialog = false
                Toast.makeText(context, "سرویس ${provider.displayName} با موفقیت فعال شد", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "پاک‌سازی سابقه گفتگو",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از پاک کردن سابقه پیام‌های گفتگو مطمئن هستید؟ تمامی پیام‌های این بخش حذف خواهند شد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearConfirmDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("پاک کردن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

private fun getRoleIcon(role: ChatRole): ImageVector {
    return when (role) {
        ChatRole.GENERAL -> Icons.Default.SmartToy
        ChatRole.COMPLEX_PLANNER -> Icons.Default.Psychology
        ChatRole.FAST_ASSISTANT -> Icons.Default.Bolt
        ChatRole.SOLAR_EXPERT -> Icons.Default.WbSunny
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit
) {
    val isUser = message.sender == ChatSender.USER
    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        message.isError -> MaterialTheme.colorScheme.error
        isUser -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clickable { onCopy() }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "شما" else "دستیار هوشمند",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "کپی",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 12.sp,
                    color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val timeStr = PersianCalendarHelper.toPersianDigits(
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                )
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AiProviderSetupDialog(
    initialProvider: AiProvider,
    onDismiss: () -> Unit,
    onSave: (provider: AiProvider, apiKey: String, customBaseUrl: String?, customModel: String?) -> Unit
) {
    val context = LocalContext.current
    var activeProvider by remember { mutableStateOf(initialProvider) }
    var keyText by remember(activeProvider) { mutableStateOf(ApiKeyManager.getApiKey(context, activeProvider)) }
    var baseUrlText by remember(activeProvider) { mutableStateOf(ApiKeyManager.getBaseUrl(context, activeProvider)) }
    var modelText by remember(activeProvider) { mutableStateOf(ApiKeyManager.getModel(context, activeProvider)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text(
                "تنظیم سرویس هوش مصنوعی",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "نوع سرویس‌دهنده هوش مصنوعی را انتخاب نمایید:",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 2 Provider Selection Chips: Google Gemini vs OpenAI Compatible
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AiProvider.entries.forEach { provider ->
                        val isSelected = activeProvider == provider
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                activeProvider = provider
                                keyText = ApiKeyManager.getApiKey(context, provider)
                                baseUrlText = ApiKeyManager.getBaseUrl(context, provider)
                                modelText = ApiKeyManager.getModel(context, provider)
                            },
                            label = {
                                Text(
                                    if (provider == AiProvider.GEMINI) "Google Gemini" else "OpenAI Compatible",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chip_provider_${provider.id}")
                        )
                    }
                }

                // API Key Field
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("کلید API (${if (activeProvider == AiProvider.GEMINI) "Google Gemini" else "OpenAI Compatible"})", fontSize = 11.sp) },
                    placeholder = { Text(activeProvider.keyHint, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field")
                )

                if (keyText.isNotBlank()) {
                    TextButton(
                        onClick = {
                            keyText = ""
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("پاک‌سازی کلید", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                }

                // Extra settings for OpenAI Compatible (Base URL and Model Name)
                if (activeProvider == AiProvider.OPENAI_COMPATIBLE) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "تنظیمات آدرس سرور و مدل:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = baseUrlText,
                                onValueChange = { baseUrlText = it },
                                label = { Text("آدرس سرور (Base URL)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = modelText,
                                onValueChange = { modelText = it },
                                label = { Text("نام مدل (Model Name)", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextButton(
                                onClick = {
                                    baseUrlText = activeProvider.defaultBaseUrl
                                    modelText = activeProvider.defaultModel
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("بازنشانی به پیش‌فرض OpenAI", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Help Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "💡 راهنما:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (activeProvider == AiProvider.GEMINI) {
                                "برای دریافت رایگان کلید Gemini به aistudio.google.com مراجعه کنید."
                            } else {
                                "از این گزینه می‌توانید برای اتصال به تمامی سرویس‌های سازگار با OpenAI (مثل OpenAI، DeepSeek، Groq یا مدل‌های محلی Ollama) استفاده کنید."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        if (activeProvider.helpUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeProvider.helpUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "عدم امکان باز کردن مرورگر", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "دریافت کلید از وب‌سایت ارائه‌دهنده",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(activeProvider, keyText.trim(), baseUrlText.trim(), modelText.trim())
                },
                modifier = Modifier.testTag("btn_save_api_key")
            ) {
                Text("ذخیره و فعال‌سازی", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ApiKeySetupDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    AiProviderSetupDialog(
        initialProvider = AiProvider.GEMINI,
        onDismiss = onDismiss,
        onSave = { _, key, _, _ -> onSave(key) }
    )
}

