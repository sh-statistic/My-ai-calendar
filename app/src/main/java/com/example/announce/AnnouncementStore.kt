package com.example.announce

import android.content.Context

/**
 * Remembers which announcement this phone has already shown, so each one
 * pops up exactly once. Announcements are ephemeral: only the latest is
 * ever kept on the relay.
 */
object AnnouncementStore {

    private const val PREFS = "announcements"
    private const val KEY_LAST_SEEN_ID = "last_seen_id"

    /** Pure decision (unit-tested): show only a fresh announcement. */
    fun shouldShow(announcement: Announcement?, lastSeenId: String?): Boolean =
        announcement != null &&
            announcement.id.isNotBlank() &&
            announcement.id != lastSeenId

    fun lastSeenId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_ID, null)

    fun markSeen(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SEEN_ID, id)
            .apply()
    }
}
