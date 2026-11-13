package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.ui.LocalAppLanguage
import com.example.ui.MainViewModel
import com.example.ui.rememberAppStrings
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// صفحهٔ سبک و شفاف برای اشتراک از برنامه‌های دیگر: فقط کارت کوچک ذخیره دیده می‌شود،
// روی همان برنامه‌ای که کاربر از آن اشتراک می‌گذارد — بدون باز شدن صفحه‌های برنامه.
class ShareNoteActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var seed by mutableStateOf<MainViewModel.SharedNoteSeed?>(null)
    private var seedReady by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseShareIntent(intent)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentLanguage by viewModel.appLanguage.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr,
                    LocalAppLanguage provides currentLanguage
                ) {
                    val strings = rememberAppStrings()
                    val currentSeed = seed
                    if (seedReady && currentSeed != null) {
                        QuickShareNoteDialog(
                            seed = currentSeed,
                            isFa = strings.isFa,
                            onDismiss = { finish() },
                            onSave = { t, c, tag, img ->
                                val bj = if (!img.isNullOrBlank()) {
                                    com.example.data.noteBlocksDataToJson(
                                        listOf(
                                            com.example.data.NoteBlockData("i", path = img),
                                            com.example.data.NoteBlockData("t", text = c)
                                        )
                                    )
                                } else {
                                    ""
                                }
                                viewModel.addNote(
                                    title = t, content = c, colorHex = "#FEF3C7", tag = tag,
                                    imagePath = img, blocksJson = bj
                                )
                                finish()
                            }
                        )
                    } else if (seedReady) {
                        // محتوای قابل ذخیره‌ای در اشتراک نبود
                        LaunchedEffect(Unit) { finish() }
                    }
                    // تا آماده شدن محتوا (کپی عکس) هیچ چیزی رسم نمی‌شود تا صفحه شفاف بماند
                }
            }
        }
    }

    private fun parseShareIntent(intent: Intent?) {
        val action = intent?.action
        val type = intent?.type
        when {
            action == Intent.ACTION_SEND && type == "text/plain" -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText == null) {
                    seedReady = true
                } else {
                    seed = MainViewModel.SharedNoteSeed(
                        title = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "",
                        content = sharedText
                    )
                    seedReady = true
                }
            }
            action == Intent.ACTION_SEND && type?.startsWith("image/") == true -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri == null) {
                    seedReady = true
                } else {
                    copySharedImage(uri, intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
                }
            }
            action == Intent.ACTION_SEND_MULTIPLE && type?.startsWith("image/") == true -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                val first = uris?.firstOrNull()
                if (first == null) {
                    seedReady = true
                } else {
                    copySharedImage(first, intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "")
                }
            }
            else -> seedReady = true
        }
    }

    private fun copySharedImage(uri: Uri, sharedTitle: String) {
        lifecycleScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                try {
                    val dir = File(filesDir, "notes_images").apply { mkdirs() }
                    val file = File(dir, "shared_" + UUID.randomUUID().toString() + ".jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (file.exists() && file.length() > 0) file.absolutePath else null
                } catch (_: Exception) {
                    null
                }
            }
            if (savedPath != null) {
                seed = MainViewModel.SharedNoteSeed(title = sharedTitle, imagePath = savedPath)
            }
            seedReady = true
        }
    }
}
