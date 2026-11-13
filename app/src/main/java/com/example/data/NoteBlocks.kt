package com.example.data

import org.json.JSONArray
import org.json.JSONObject

// یک بلوک از محتوای درون‌خطی یادداشت: متن، عکس، نقاشی یا چک‌لیست — به همان ترتیبی که کاربر چیده است.
// kind: "t" = text (uses text), "i" = image (uses path), "d" = drawing (uses path),
//       "c" = checklist (uses text as the checklist JSON payload)
data class NoteBlockData(
    val kind: String,
    val text: String = "",
    val path: String? = null
)

fun parseNoteBlocksData(blocksJson: String?): List<NoteBlockData> {
    if (blocksJson.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(blocksJson)
        val result = mutableListOf<NoteBlockData>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            when (obj.optString("k")) {
                "t" -> result.add(NoteBlockData("t", text = obj.optString("x", "")))
                "c" -> result.add(NoteBlockData("c", text = obj.optString("x", "")))
                "i" -> result.add(NoteBlockData("i", path = obj.optString("p").takeIf { it.isNotBlank() }))
                "d" -> result.add(NoteBlockData("d", path = obj.optString("p").takeIf { it.isNotBlank() }))
            }
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

fun noteBlocksDataToJson(blocks: List<NoteBlockData>): String {
    if (blocks.isEmpty()) return ""
    val arr = JSONArray()
    for (b in blocks) {
        val obj = JSONObject()
        obj.put("k", b.kind)
        if (b.kind == "t" || b.kind == "c") {
            obj.put("x", b.text)
        } else {
            obj.put("p", b.path ?: "")
        }
        arr.put(obj)
    }
    return arr.toString()
}

// فقط بلوک‌های رسانه‌ای (عکس/نقاشی) که فایل دارند
fun extractNoteBlockMedia(blocksJson: String?): List<NoteBlockData> =
    parseNoteBlocksData(blocksJson).filter { it.kind != "t" && !it.path.isNullOrBlank() }
