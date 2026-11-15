package com.example.announce

import android.content.Context
import org.json.JSONArray

/**
 * Remembers which announcements this phone has already shown, so each one
 * pops up exactly once. Several announcements can be live on the relay at
 * the same time, so the memory is a set of seen ids (capped, newest kept),
 * not just the latest one. The pre-list single "last seen id" is migrated
 * into the set on first read.
 */
object AnnouncementStore {

    private const val PREFS = "announcements"
    private const val KEY_LAST_SEEN_ID = "last_seen_id"
    private const val KEY_SEEN_IDS = "seen_ids"
    const val MAX_SEEN = 50

    /** Pure decision (unit-tested): show only a fresh announcement. */
    fun shouldShow(announcement: Announcement?, seenIds: Set<String>): Boolean =
        announcement != null &&
            announcement.id.isNotBlank() &&
            announcement.id !in seenIds

    /** Announcement ids start with their publish time in millis (a random
     *  suffix may follow), which is what "newest" sorts by. */
    private fun idOrder(id: String): Long =
        id.substringBefore('-').toLongOrNull() ?: 0L

    /** Pure merge (unit-tested): adds an id, keeping only the newest ids. */
    fun addSeen(seenIds: Set<String>, id: String): Set<String> {
        if (id.isBlank()) return seenIds
        val merged = (seenIds + id).sortedByDescending { idOrder(it) }
        return merged.take(MAX_SEEN).toSet()
    }

    /** Pure codec (unit-tested): the seen set as a JSON array string. */
    fun encodeSeen(seenIds: Set<String>): String {
        val array = JSONArray()
        // Sorted so the stored form is deterministic.
        for (id in seenIds.sortedByDescending { idOrder(it) }) array.put(id)
        return array.toString()
    }

    fun decodeSeen(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return try {
            val array = JSONArray(raw)
            val out = LinkedHashSet<String>()
            for (i in 0 until array.length()) {
                val id = array.optString(i)
                if (id.isNotBlank()) out.add(id)
            }
            out
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun seenIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = decodeSeen(prefs.getString(KEY_SEEN_IDS, null))
        // One-way migration: the old single last-seen id joins the set.
        val legacy = prefs.getString(KEY_LAST_SEEN_ID, null)
        return if (!legacy.isNullOrBlank()) stored + legacy else stored
    }

    fun markSeen(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SEEN_IDS, encodeSeen(addSeen(seenIds(context), id)))
            .remove(KEY_LAST_SEEN_ID)
            .apply()
    }
}
