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
                    val announcement = result.announcement
                    if (AnnouncementStore.shouldShow(announcement, AnnouncementStore.lastSeenId(applicationContext))) {
                        val fresh = announcement!!
                        if (AnnouncementNotifier.canPost(applicationContext)) {
                            // Mark seen BEFORE showing: a duplicate run or a
                            // process death can then never show it twice.
                            // But only when it can really be shown — while
                            // notifications are off the announcement stays
                            // unseen, so a later check shows it once they
                            // are turned on instead of it being lost.
                            AnnouncementStore.markSeen(applicationContext, fresh.id)
                            AnnouncementNotifier.show(applicationContext, fresh)
                        }
                    }
                    Result.success()
                }
            }
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }
}
