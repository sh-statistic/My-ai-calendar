package com.example.announce

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * One announcement check: fetch the latest from the relay and, when it is
 * new to this phone, post it as a notification. A failed fetch retries a
 * few times (like the update checker); "no announcement" is a success.
 */
class AnnouncementCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            when (val result = AnnouncementClient.fetchLatest()) {
                is AnnouncementClient.LatestResult.Failed ->
                    if (runAttemptCount < 3) Result.retry() else Result.success()
                is AnnouncementClient.LatestResult.Ok -> {
                    // Several announcements can be live at once; show each
                    // unseen one exactly once, oldest first. While
                    // notifications cannot be posted, stop at the first
                    // unseen one and leave it (and the rest) unseen, so a
                    // later check shows them once posting is possible
                    // instead of them being lost.
                    val seen = AnnouncementStore.seenIds(applicationContext)
                    for (fresh in result.announcements) {
                        if (!AnnouncementStore.shouldShow(fresh, seen)) continue
                        if (!AnnouncementNotifier.canPost(applicationContext)) break
                        AnnouncementStore.markSeen(applicationContext, fresh.id)
                        AnnouncementNotifier.show(applicationContext, fresh)
                    }
                    Result.success()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }
}
