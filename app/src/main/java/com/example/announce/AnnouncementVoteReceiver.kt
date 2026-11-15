package com.example.announce

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/**
 * Receives a tap on one of an announcement notification's option buttons:
 * the notification is dismissed at once (the vote is cast) and the vote
 * itself is delivered by a background worker.
 */
class AnnouncementVoteReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_VOTE = "com.example.announce.VOTE"
        const val EXTRA_ANN_ID = "ann_id"
        const val EXTRA_OPTION = "option"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_VOTE) return
        val id = intent.getStringExtra(EXTRA_ANN_ID) ?: return
        val option = intent.getIntExtra(EXTRA_OPTION, -1)
        if (option < 0) return
        NotificationManagerCompat.from(context).cancel(AnnouncementNotifier.notificationIdFor(id))
        AnnouncementVoteWorker.enqueue(context, id, option)
    }
}
