package com.example.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SyncData(
    val deviceId: String,
    val timestamp: Long,
    val events: List<EventEntity>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>,
    val mementoMoriJson: JSONObject? = null,
    val noteDrawingsBase64: Map<String, String>? = null, // key: note id, value: base64 drawing content
    val customOccasionsData: String? = null,
    val customOccasionsHiddenData: String? = null,
    val appPreferences: Map<String, Any?>? = null,
    val prayerPreferences: Map<String, Any?>? = null,
    val hijriOffsetPreferences: Map<String, Any?>? = null
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("deviceId", deviceId)
        root.put("timestamp", timestamp)

        val eventsArray = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("description", e.description)
            obj.put("persianDate", e.persianDate)
            obj.put("gregorianDate", e.gregorianDate)
            obj.put("startTime", e.startTime)
            obj.put("endTime", e.endTime)
            obj.put("category", e.category)
            obj.put("colorHex", e.colorHex)
            obj.put("recurrenceId", e.recurrenceId ?: JSONObject.NULL)
            obj.put("recurrenceRule", e.recurrenceRule)
            obj.put("updatedAt", e.updatedAt)
            obj.put("isDeleted", e.isDeleted)
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)

        val tasksArray = JSONArray()
        for (t in tasks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("title", t.title)
            obj.put("isCompleted", t.isCompleted)
            obj.put("persianDueDate", t.persianDueDate)
            obj.put("gregorianDueDate", t.gregorianDueDate)
            obj.put("priority", t.priority)
            obj.put("category", t.category)
            obj.put("updatedAt", t.updatedAt)
            obj.put("isDeleted", t.isDeleted)
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        val notesArray = JSONArray()
        for (n in notes) {
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("title", n.title)
            obj.put("content", n.content)
            obj.put("persianDate", n.persianDate)
            obj.put("colorHex", n.colorHex)
            obj.put("isPinned", n.isPinned)
            obj.put("tag", n.tag)
            if (n.drawingPath != null) {
                obj.put("drawingPath", n.drawingPath)
            }
            val drawingB64 = noteDrawingsBase64?.get(n.id)
            if (!drawingB64.isNullOrBlank()) {
                obj.put("drawingBase64", drawingB64)
            }
            obj.put("updatedAt", n.updatedAt)
            obj.put("isDeleted", n.isDeleted)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)
        if (mementoMoriJson != null) {
            root.put("mementoMori", mementoMoriJson)
        }

        if (customOccasionsData != null || customOccasionsHiddenData != null) {
            val occObj = JSONObject()
            if (customOccasionsData != null) occObj.put("data", customOccasionsData)
            if (customOccasionsHiddenData != null) occObj.put("hidden_data", customOccasionsHiddenData)
            root.put("customOccasions", occObj)
        }

        if (appPreferences != null) {
            root.put("appPreferences", JSONObject(appPreferences))
        }

        if (prayerPreferences != null) {
            root.put("prayerPreferences", JSONObject(prayerPreferences))
        }

        if (hijriOffsetPreferences != null) {
            root.put("hijriOffsetPreferences", JSONObject(hijriOffsetPreferences))
        }

        return root.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): SyncData {
            val root = JSONObject(jsonStr)
            val deviceId = root.optString("deviceId", "unknown_device")
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val events = mutableListOf<EventEntity>()
            val eventsArray = root.optJSONArray("events")
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val obj = eventsArray.getJSONObject(i)
                    events.add(
                        EventEntity(
                            id = obj.optString("id"),
                            title = obj.optString("title", "بدون عنوان"),
                            description = obj.optString("description", ""),
                            persianDate = obj.optString("persianDate", ""),
                            gregorianDate = obj.optString("gregorianDate", ""),
                            startTime = obj.optString("startTime", ""),
                            endTime = obj.optString("endTime", ""),
                            category = obj.optString("category", "کاری"),
                            colorHex = obj.optString("colorHex", "#F59E0B"),
                            recurrenceId = if (obj.isNull("recurrenceId")) null else obj.optString("recurrenceId").takeIf { it.isNotBlank() },
                            recurrenceRule = obj.optString("recurrenceRule", "NONE"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val tasks = mutableListOf<TaskEntity>()
            val tasksArray = root.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    tasks.add(
                        TaskEntity(
                            id = obj.optString("id"),
                            title = obj.optString("title", ""),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            persianDueDate = obj.optString("persianDueDate", ""),
                            gregorianDueDate = obj.optString("gregorianDueDate", ""),
                            priority = obj.optString("priority", "متوسط"),
                            category = obj.optString("category", "عمومی"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val notes = mutableListOf<NoteEntity>()
            val drawingsMap = mutableMapOf<String, String>()
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val noteId = obj.optString("id")
                    val drawingB64 = if (obj.isNull("drawingBase64")) null else obj.optString("drawingBase64").takeIf { it.isNotBlank() }
                    if (drawingB64 != null) {
                        drawingsMap[noteId] = drawingB64
                    }
                    notes.add(
                        NoteEntity(
                            id = noteId,
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            persianDate = obj.optString("persianDate", ""),
                            colorHex = obj.optString("colorHex", "#FEF3C7"),
                            isPinned = obj.optBoolean("isPinned", false),
                            drawingPath = if (obj.isNull("drawingPath")) null else obj.optString("drawingPath").takeIf { it.isNotBlank() },
                            tag = obj.optString("tag", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            val mementoMoriJson = root.optJSONObject("mementoMori")

            val customOccasionsObj = root.optJSONObject("customOccasions")
            val customOccasionsData = if (customOccasionsObj != null && !customOccasionsObj.isNull("data")) {
                customOccasionsObj.optString("data")
            } else null
            val customOccasionsHiddenData = if (customOccasionsObj != null && !customOccasionsObj.isNull("hidden_data")) {
                customOccasionsObj.optString("hidden_data")
            } else null

            fun jsonObjectToMap(jObj: JSONObject?): Map<String, Any?>? {
                if (jObj == null) return null
                val map = mutableMapOf<String, Any?>()
                val keys = jObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = if (jObj.isNull(key)) null else jObj.get(key)
                }
                return map
            }

            val appPreferences = jsonObjectToMap(root.optJSONObject("appPreferences"))
            val prayerPreferences = jsonObjectToMap(root.optJSONObject("prayerPreferences"))
            val hijriOffsetPreferences = jsonObjectToMap(root.optJSONObject("hijriOffsetPreferences"))

            return SyncData(
                deviceId = deviceId,
                timestamp = timestamp,
                events = events,
                tasks = tasks,
                notes = notes,
                mementoMoriJson = mementoMoriJson,
                noteDrawingsBase64 = if (drawingsMap.isNotEmpty()) drawingsMap else null,
                customOccasionsData = customOccasionsData,
                customOccasionsHiddenData = customOccasionsHiddenData,
                appPreferences = appPreferences,
                prayerPreferences = prayerPreferences,
                hijriOffsetPreferences = hijriOffsetPreferences
            )
        }
    }
}
