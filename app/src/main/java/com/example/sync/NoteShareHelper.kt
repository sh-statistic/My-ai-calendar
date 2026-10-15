package com.example.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.NoteEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * مدیریت ذخیره دستی، اشتراک‌گذاری و ارسال یادداشت‌ها به Google Keep و سایر برنامه‌ها
 */
object NoteShareHelper {

    private const val GOOGLE_KEEP_PACKAGE = "com.google.android.keep"

    /**
     * ارسال یک یادداشت به برنامه Google Keep
     * در صورت نصب نبودن Google Keep، منوی عمومی اشتراک‌گذاری باز می‌شود.
     */
    fun sendToGoogleKeep(context: Context, note: NoteEntity) {
        val noteBody = buildString {
            if (note.persianDate.isNotBlank()) {
                append("تاریخ: ").append(note.persianDate).append("\n\n")
            }
            append(note.content)
        }

        val keepIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, note.title)
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n$noteBody")
            setPackage(GOOGLE_KEEP_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val isKeepInstalled = try {
            val pm = context.packageManager
            pm.getPackageInfo(GOOGLE_KEEP_PACKAGE, 0) != null
        } catch (_: Exception) {
            false
        }

        if (isKeepInstalled) {
            try {
                context.startActivity(keepIntent)
                Toast.makeText(context, "در حال انتقال به Google Keep...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                fallbackShare(context, note.title, "${note.title}\n\n$noteBody")
            }
        } else {
            Toast.makeText(
                context,
                "برنامه Google Keep روی گوشی نصب نیست. باز کردن منوی اشتراک‌گذاری...",
                Toast.LENGTH_LONG
            ).show()
            fallbackShare(context, note.title, "${note.title}\n\n$noteBody")
        }
    }

    /**
     * ارسال تمام یادداشت‌های فعال به برنامه Google Keep
     */
    fun sendAllToGoogleKeep(context: Context, notes: List<NoteEntity>) {
        if (notes.isEmpty()) {
            Toast.makeText(context, "هیچ یادداشتی برای ارسال وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }

        val aggregatedText = formatNotesAsReadableText(notes)
        val title = "یادداشت‌های تقویم (${notes.size} مورد)"

        val keepIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, aggregatedText)
            setPackage(GOOGLE_KEEP_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val isKeepInstalled = try {
            context.packageManager.getPackageInfo(GOOGLE_KEEP_PACKAGE, 0) != null
        } catch (_: Exception) {
            false
        }

        if (isKeepInstalled) {
            try {
                context.startActivity(keepIntent)
                Toast.makeText(context, "در حال انتقال تمام یادداشت‌ها به Google Keep...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                fallbackShare(context, title, aggregatedText)
            }
        } else {
            Toast.makeText(
                context,
                "برنامه Google Keep نصب نیست. باز کردن منوی اشتراک‌گذاری...",
                Toast.LENGTH_LONG
            ).show()
            fallbackShare(context, title, aggregatedText)
        }
    }

    /**
     * کپی کردن محتوای یک یادداشت در حافظه موقت (Clipboard)
     */
    fun copyToClipboard(context: Context, note: NoteEntity) {
        val textToCopy = buildString {
            appendLine(note.title)
            if (note.persianDate.isNotBlank()) {
                appendLine("تاریخ: ${note.persianDate}")
            }
            appendLine()
            append(note.content)
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(note.title, textToCopy)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "یادداشت در حافظه کپی شد ✓", Toast.LENGTH_SHORT).show()
    }

    /**
     * کپی کردن تمام یادداشت‌ها به صورت یکپارچه و ساختاریافته در حافظه موقت
     */
    fun copyAllNotesToClipboard(context: Context, notes: List<NoteEntity>) {
        if (notes.isEmpty()) {
            Toast.makeText(context, "هیچ یادداشتی برای کپی وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }

        val fullText = formatNotesAsReadableText(notes)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("یادداشت‌های تقویم", fullText)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "تمام یادداشت‌ها (${notes.size} مورد) در حافظه کپی شدند ✓", Toast.LENGTH_SHORT).show()
    }

    /**
     * اشتراک‌گذاری یک یادداشت با سایر برنامه‌ها (تلگرام، واتساپ، ایمیل و ...)
     */
    fun shareNote(context: Context, note: NoteEntity) {
        val content = buildString {
            appendLine(note.title)
            if (note.persianDate.isNotBlank()) {
                appendLine("تاریخ: ${note.persianDate}")
            }
            appendLine()
            append(note.content)
        }
        fallbackShare(context, note.title, content)
    }

    /**
     * اشتراک‌گذاری دسته‌جمعی تمام یادداشت‌ها
     */
    fun shareAllNotes(context: Context, notes: List<NoteEntity>) {
        if (notes.isEmpty()) {
            Toast.makeText(context, "هیچ یادداشتی برای اشتراک‌گذاری وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }
        val text = formatNotesAsReadableText(notes)
        fallbackShare(context, "یادداشت‌های تقویم", text)
    }

    /**
     * صدور خروجی استاندارد JSON از یادداشت‌ها و ذخیره در حافظه موقت یا اشتراک
     */
    fun exportNotesAsJson(context: Context, notes: List<NoteEntity>, asShare: Boolean = false) {
        if (notes.isEmpty()) {
            Toast.makeText(context, "هیچ یادداشتی برای خروجی گرفتن وجود ندارد.", Toast.LENGTH_SHORT).show()
            return
        }

        val jsonArray = JSONArray()
        for (note in notes) {
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("persianDate", note.persianDate)
                put("colorHex", note.colorHex)
                put("isPinned", note.isPinned)
                put("updatedAt", note.updatedAt)
            }
            jsonArray.put(obj)
        }

        val jsonString = jsonArray.toString(2)

        if (asShare) {
            fallbackShare(context, "پشتیبان یادداشت‌ها (JSON)", jsonString)
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("پشتیبان یادداشت‌ها JSON", jsonString)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "کد JSON یادداشت‌ها (${notes.size} مورد) در حافظه کپی شد ✓", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * قالب‌بندی خوانا و شکیل برای تجمیع یادداشت‌ها
     */
    private fun formatNotesAsReadableText(notes: List<NoteEntity>): String {
        return buildString {
            appendLine("═══════════════════════════════════")
            appendLine("📋 یادداشت‌های تقویم خورشیدی (${notes.size} مورد)")
            appendLine("═══════════════════════════════════")
            appendLine()

            notes.forEachIndexed { index, note ->
                appendLine("[${index + 1}] ${note.title}")
                if (note.persianDate.isNotBlank()) {
                    appendLine("📅 تاریخ: ${note.persianDate}")
                }
                appendLine(note.content)
                appendLine("───────────────────────────────────")
            }
        }
    }

    private fun fallbackShare(context: Context, subject: String, body: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری یا ذخیره یادداشت").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در اشتراک‌گذاری: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
