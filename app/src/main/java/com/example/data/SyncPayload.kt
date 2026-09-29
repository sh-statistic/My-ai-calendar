package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class SyncData(
    val deviceId: String,
    val timestamp: Long,
    val events: List<EventEntity>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>
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
            obj.put("updatedAt", n.updatedAt)
            obj.put("isDeleted", n.isDeleted)
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

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
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    notes.add(
                        NoteEntity(
                            id = obj.optString("id"),
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            persianDate = obj.optString("persianDate", ""),
                            colorHex = obj.optString("colorHex", "#FEF3C7"),
                            isPinned = obj.optBoolean("isPinned", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = obj.optBoolean("isDeleted", false)
                        )
                    )
                }
            }

            return SyncData(deviceId, timestamp, events, tasks, notes)
        }
    }
}
