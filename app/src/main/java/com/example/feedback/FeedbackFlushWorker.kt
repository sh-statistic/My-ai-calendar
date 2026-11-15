package com.example.feedback

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background delivery for the feedback outbox: drains the queue through the
 * relay, oldest first. While entries remain undelivered the work retries
 * with backoff (WorkManager only runs it with a connected network), so a
 * feedback written during a relay outage still arrives once things recover —
 * the user is never asked to resend anything.
 */
class FeedbackFlushWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        FeedbackOutbox.flush(applicationContext) { item, photo ->
            FeedbackSender.sendQueued(item, photo)
        }
        val remaining = FeedbackOutbox.pendingCount(applicationContext)
        return when {
            remaining == 0 -> Result.success()
            // Bounded retries; the queue itself is never dropped, and the
            // next enqueue or app start re-arms a fresh flush.
            runAttemptCount < 10 -> Result.retry()
            else -> Result.success()
        }
    }
}
