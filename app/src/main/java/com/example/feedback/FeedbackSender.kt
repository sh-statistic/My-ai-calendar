package com.example.feedback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import com.example.ui.components.ImageDownscale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends in-app user feedback to the feedback relay (a small Cloudflare Worker
 * on the developer's own domain). The relay forwards each message to the
 * developer's Telegram and to a GitHub issue; the user needs no account and
 * the app holds no token of any kind.
 */
object FeedbackSender {

    const val MAX_TEXT_LENGTH = 4000
    const val MAX_CONTACT_LENGTH = 200

    private const val ENDPOINT = "https://feedback.sh2103.shop/f/7pytoxb6dr1e"
    private const val PHOTO_MAX_DIMENSION = 1600
    private const val PREFS = "feedback_draft"

    /** Pure payload builder (unit-tested): trims and caps every field. */
    fun buildPayloadJson(
        text: String,
        contact: String,
        appVersion: String,
        device: String,
        photoBase64: String?,
        crashLog: String? = null
    ): String {
        val json = JSONObject()
        json.put("text", text.trim().take(MAX_TEXT_LENGTH))
        json.put("contact", contact.trim().take(MAX_CONTACT_LENGTH))
        json.put("appVersion", appVersion)
        json.put("device", device)
        if (photoBase64 != null) json.put("photoBase64", photoBase64)
        if (crashLog != null) json.put("crashLog", crashLog)
        return json.toString()
    }

    fun appVersionName(context: Context): String = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    fun deviceLabel(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    /**
     * Sends one queued feedback. True only when the relay confirms at least
     * one delivery channel worked; the outbox keeps the entry otherwise.
     */
    suspend fun sendQueued(item: FeedbackOutbox.PendingFeedback, photoJpeg: ByteArray?): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val photoB64 = photoJpeg?.let {
                    android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                }
                val payload = buildPayloadJson(
                    item.text, item.contact, item.appVersion, item.device, photoB64, item.crashLog
                )
                val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) {
                    false
                } else {
                    val body = conn.inputStream?.bufferedReader()?.readText().orEmpty()
                    JSONObject(body).optBoolean("ok", false)
                }
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Decodes a picked photo, bakes its EXIF orientation, downscales it and
     * returns JPEG bytes small enough to relay. Null when the image cannot be
     * decoded at all.
     */
    suspend fun prepareFeedbackPhoto(context: Context, uri: Uri): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = ImageDownscale.sampleSize(
                        bounds.outWidth, bounds.outHeight, PHOTO_MAX_DIMENSION
                    )
                }
                var bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@withContext null
                bitmap = applyExifOrientation(context, uri, bitmap)
                val (tw, th) = ImageDownscale.targetSize(
                    bitmap.width, bitmap.height, PHOTO_MAX_DIMENSION
                )
                if (tw != bitmap.width || th != bitmap.height) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, tw, th, true)
                }
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                var bytes = out.toByteArray()
                if (bytes.size > 3_500_000) {
                    out.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 65, out)
                    bytes = out.toByteArray()
                }
                // The relay drops photos past its cap; fail loudly instead of
                // silently sending the feedback without its photo.
                if (bytes.size > 5_500_000) null else bytes
            } catch (e: Exception) {
                null
            }
        }

    /** Same EXIF bake the note photos use, kept local so this feature stays self-contained. */
    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return bitmap
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use {
                android.media.ExifInterface(it)
            } ?: return bitmap
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                android.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                android.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(1f, -1f)
                }
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    // Draft: the text being typed survives a dismissed dialog and an app
    // restart. Once the user taps Send with a connection, the draft is
    // cleared and the feedback itself lives in FeedbackOutbox until the
    // relay confirms delivery.

    fun loadDraft(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (prefs.getString("text", "") ?: "") to (prefs.getString("contact", "") ?: "")
    }

    fun saveDraft(context: Context, text: String, contact: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("text", text)
            .putString("contact", contact)
            .apply()
    }

    fun clearDraft(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
