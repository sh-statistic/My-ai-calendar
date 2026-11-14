package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The complete backup format: a ZIP archive instead of one giant JSON text.
 *
 * Layout:
 *  - `data.json`     — the [SyncData] JSON with every base64 media map left null.
 *  - `manifest.json` — where each media file belongs (note id, kind, block key).
 *  - `media/<n>.<ext>` — the raw media files themselves (drawings, images, voice),
 *                      copied byte for byte, each with its real file extension so
 *                      any tool (or AI) opening the archive knows what it is;
 *                      drawing stroke sidecars ride along as `.sidecar` entries.
 *  - `README.txt`    — a plain-language explanation of this archive, so the data
 *                      stays fully readable and rebuildable even without the app.
 *
 * Nothing is base64-inflated and nothing needs to be held as one huge String for
 * the UI: writing streams file by file. Reading materializes the entries on a
 * background thread and [toSyncData] rebuilds the exact [SyncData] the legacy
 * importer already knows how to restore, so old and new restore paths converge.
 *
 * This object is deliberately Android-free (only java.util.zip / org.json) so it
 * is covered by plain JVM unit tests.
 */
object BackupArchive {

    const val KIND_DRAWING = "drawing"
    const val KIND_IMAGE = "image"
    const val KIND_AUDIO = "audio"
    const val KIND_BLOCK_IMAGE = "blockImage"
    const val KIND_BLOCK_DRAWING = "blockDrawing"
    const val KIND_BLOCK_AUDIO = "blockAudio"

    /** A media file that belongs in the archive, with its restore destination. */
    data class MediaItem(
        val noteId: String,
        val kind: String,
        /** For block media: the original path key the importer maps back. Null otherwise. */
        val blockKey: String?,
        val file: File,
        val sidecarFile: File? = null
    )

    /** A media entry read back from an archive. */
    data class ResolvedMedia(
        val noteId: String,
        val kind: String,
        val blockKey: String?,
        val bytes: ByteArray,
        val sidecarText: String?
    )

    data class ArchiveData(
        val dataJson: String,
        val media: List<ResolvedMedia>
    )

    /** True when the file starts with the ZIP magic ("PK"). */
    fun isArchive(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            file.inputStream().use { input ->
                val b = ByteArray(2)
                input.read(b) == 2 && b[0] == 'P'.code.toByte() && b[1] == 'K'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun write(dataJson: String, media: List<MediaItem>, out: OutputStream, onProgress: ((Float) -> Unit)? = null) {
        val dataBytes = dataJson.toByteArray(Charsets.UTF_8)
        val totalBytes = dataBytes.size + media.sumOf { it.file.length() + (it.sidecarFile?.length() ?: 0L) }
        var writtenBytes = 0L
        var lastPercent = -1
        fun reportProgress() {
            if (onProgress == null || totalBytes <= 0L) return
            val pct = (writtenBytes * 100 / totalBytes).toInt().coerceIn(0, 100)
            if (pct != lastPercent) {
                lastPercent = pct
                onProgress(pct / 100f)
            }
        }
        val manifest = JSONArray()
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("README.txt"))
            zip.write(README_TEXT.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(dataBytes)
            zip.closeEntry()
            writtenBytes += dataBytes.size
            reportProgress()

            // Manifest is written after the entries are named but before their bytes;
            // entry names are index-based, so they are known up front.
            media.forEachIndexed { index, item ->
                val extension = item.file.extension.trim()
                val entryName = if (extension.isNotEmpty()) {
                    "media/%04d.%s".format(index, extension)
                } else {
                    "media/%04d".format(index)
                }
                val obj = JSONObject()
                obj.put("noteId", item.noteId)
                obj.put("kind", item.kind)
                if (item.blockKey != null) obj.put("blockKey", item.blockKey)
                obj.put("entry", entryName)
                if (item.sidecarFile != null) obj.put("sidecarEntry", "$entryName.sidecar")
                manifest.put(obj)
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            media.forEachIndexed { index, item ->
                val extension = item.file.extension.trim()
                val entryName = if (extension.isNotEmpty()) {
                    "media/%04d.%s".format(index, extension)
                } else {
                    "media/%04d".format(index)
                }
                zip.putNextEntry(ZipEntry(entryName))
                item.file.inputStream().buffered().use { input -> input.copyTo(zip) }
                zip.closeEntry()
                writtenBytes += item.file.length()
                reportProgress()
                if (item.sidecarFile != null) {
                    val sidecarBytes = item.sidecarFile.readText().toByteArray(Charsets.UTF_8)
                    zip.putNextEntry(ZipEntry("$entryName.sidecar"))
                    zip.write(sidecarBytes)
                    zip.closeEntry()
                    writtenBytes += sidecarBytes.size
                    reportProgress()
                }
            }
        }
        onProgress?.invoke(1f)
    }

    /** Reads only the `data.json` entry (written first), or null when absent. */
    fun readDataJson(input: InputStream): String? {
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "data.json") {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    fun read(input: InputStream, totalBytes: Long = -1L, onProgress: ((Float) -> Unit)? = null): ArchiveData {
        val source = if (onProgress != null && totalBytes > 0L) {
            CountingInputStream(input, totalBytes, onProgress)
        } else {
            input
        }
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(source)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                entry = zip.nextEntry
            }
        }
        val dataJson = entries["data.json"]?.toString(Charsets.UTF_8)
            ?: throw IllegalArgumentException("Not a backup archive: data.json missing")
        val manifestText = entries["manifest.json"]?.toString(Charsets.UTF_8)
        val media = mutableListOf<ResolvedMedia>()
        if (manifestText != null) {
            val manifest = JSONArray(manifestText)
            for (i in 0 until manifest.length()) {
                val obj = manifest.getJSONObject(i)
                val entryName = obj.getString("entry")
                val bytes = entries[entryName] ?: continue
                val sidecarEntry = if (obj.has("sidecarEntry")) obj.getString("sidecarEntry") else null
                media += ResolvedMedia(
                    noteId = obj.getString("noteId"),
                    kind = obj.getString("kind"),
                    blockKey = if (obj.has("blockKey")) obj.getString("blockKey") else null,
                    bytes = bytes,
                    sidecarText = sidecarEntry?.let { entries[it]?.toString(Charsets.UTF_8) }
                )
            }
        }
        return ArchiveData(dataJson, media)
    }

    /**
     * Rebuilds the full [SyncData] (with the legacy base64 media maps filled) from a
     * read archive, so the existing, proven restore pipeline can consume it unchanged.
     * [encodeBase64] is injected to keep this object Android-free and unit-testable.
     */
    fun toSyncData(data: ArchiveData, encodeBase64: (ByteArray) -> String): SyncData {
        val base = SyncData.fromJsonString(data.dataJson)
        val drawings = mutableMapOf<String, String>()
        val images = mutableMapOf<String, String>()
        val audios = mutableMapOf<String, String>()
        val blockImages = mutableMapOf<String, MutableMap<String, String>>()
        val blockDrawings = mutableMapOf<String, MutableMap<String, String>>()
        val blockAudios = mutableMapOf<String, MutableMap<String, String>>()
        val drawingSidecars = mutableMapOf<String, String>()
        val blockDrawingSidecars = mutableMapOf<String, MutableMap<String, String>>()
        for (item in data.media) {
            val b64 = encodeBase64(item.bytes)
            when (item.kind) {
                KIND_DRAWING -> {
                    drawings[item.noteId] = b64
                    if (item.sidecarText != null) drawingSidecars[item.noteId] = item.sidecarText
                }
                KIND_IMAGE -> images[item.noteId] = b64
                KIND_AUDIO -> audios[item.noteId] = b64
                KIND_BLOCK_IMAGE -> blockImages.getOrPut(item.noteId) { mutableMapOf() }[item.blockKey ?: ""] = b64
                KIND_BLOCK_DRAWING -> {
                    blockDrawings.getOrPut(item.noteId) { mutableMapOf() }[item.blockKey ?: ""] = b64
                    if (item.sidecarText != null) {
                        blockDrawingSidecars.getOrPut(item.noteId) { mutableMapOf() }[item.blockKey ?: ""] = item.sidecarText
                    }
                }
                KIND_BLOCK_AUDIO -> blockAudios.getOrPut(item.noteId) { mutableMapOf() }[item.blockKey ?: ""] = b64
            }
        }
        return base.copy(
            noteDrawingsBase64 = drawings.ifEmpty { null },
            noteImagesBase64 = images.ifEmpty { null },
            noteAudiosBase64 = audios.ifEmpty { null },
            noteBlockImagesBase64 = blockImages.ifEmpty { null },
            noteBlockDrawingsBase64 = blockDrawings.ifEmpty { null },
            noteBlockAudiosBase64 = blockAudios.ifEmpty { null },
            noteDrawingSidecars = drawingSidecars.ifEmpty { null },
            noteBlockDrawingSidecars = blockDrawingSidecars.ifEmpty { null }
        )
    }


    /**
     * Written into every archive as README.txt: the portability contract. The user's
     * data must stay readable and rebuildable even if the app itself is gone — any
     * person or AI with this file must be able to reconstruct everything from the
     * archive alone, using only this explanation.
     */
    private const val README_TEXT = """Hamgam Calendar - Backup Archive
====================================

This file is a complete backup of one user's data from the Hamgam calendar app.
It is a standard ZIP archive. You do NOT need the app to read it.

CONTENTS
- data.json ......... All of the user's data as plain JSON (UTF-8):
                        events ................ the calendar events
                        tasks ................. the to-do tasks
                        notes ................. the notes (their media files
                                                are in media/, see below)
                        customOccasionsData and customOccasionsHiddenData
                                              ... the occasions the user added,
                                                edited or hid (stored as text)
                        mementoMoriJson ....... the life-grid points (as text)
                        appPreferences and the other preference fields
                                              ... the app settings
                      Media files are NOT embedded in data.json; they are
                      separate files in this archive (see manifest.json).
- manifest.json ..... A list mapping every media file in this archive to the
                      note it belongs to. Each entry has:
                        noteId ...... the "id" of the note in data.json
                        kind ........ drawing | image | audio |
                                      blockImage | blockDrawing | blockAudio
                        entry ....... the media file's name in this archive
                        sidecarEntry  (drawings only) a small JSON with the
                                      drawing's editable strokes; the drawing
                                      itself is the image file beside it
                        blockKey .... (block media only) the path the media
                                      had inside its note's content
- media/ ............ The media files themselves, with real extensions
                      (.jpg photos, .png drawings, .m4a voice notes).
- README.txt ........ This file.

TO REBUILD THE DATA ANYWHERE
1. Read data.json for the events, tasks and notes.
2. Use manifest.json to attach each file in media/ to its note.

Persian summary:
این فایل، پشتیبان کامل داده‌های یک کاربر از برنامه «تقویم همگام» است.
یک فایل فشرده استاندارد است و برای خواندنش به خود برنامه نیازی نیست.
همه داده‌ها — رویدادها، وظیفه‌ها، یادداشت‌ها، مناسبت‌هایی که خود کاربر
ساخته و نقطه‌های عمر — در data.json است، فایل‌های عکس و صوت و نقاشی
در پوشه media و نقشه اتصال هر فایل به یادداشتش در manifest.json است.
"""

    /** Reports read progress as a fraction of the archive's total byte size. */
    private class CountingInputStream(
        inner: InputStream,
        private val total: Long,
        private val onProgress: (Float) -> Unit
    ) : FilterInputStream(inner) {
        private var readBytes = 0L
        private var lastPercent = -1

        override fun read(): Int {
            val n = super.read()
            if (n >= 0) {
                readBytes++
                report()
            }
            return n
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = super.read(b, off, len)
            if (n > 0) {
                readBytes += n
                report()
            }
            return n
        }

        override fun skip(n: Long): Long {
            val skipped = super.skip(n)
            if (skipped > 0) {
                readBytes += skipped
                report()
            }
            return skipped
        }

        private fun report() {
            val pct = (readBytes * 100 / total).toInt().coerceIn(0, 100)
            if (pct != lastPercent) {
                lastPercent = pct
                onProgress(pct / 100f)
            }
        }
    }
}
