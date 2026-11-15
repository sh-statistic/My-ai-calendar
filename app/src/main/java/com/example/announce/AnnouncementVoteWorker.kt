package com.example.announce

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

/** Delivers one announcement vote to the relay, retrying a few times. */
class AnnouncementVoteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.success()
        val option = inputData.getInt(KEY_OPTION, -1)
        if (option < 0) return Result.success()
        val result = AnnouncementClient.vote(
            id,
            option,
            com.example.feedback.FeedbackSender.deviceLabel(),
            com.example.feedback.FeedbackSender.appVersionName(applicationContext)
        )
        return when (result) {
            // Tallied — done. Gone — the announcement was deleted or the
            // vote is invalid; retrying can never change that, so the
            // vote closes quietly instead of knocking ten more times.
            AnnouncementClient.VoteResult.DELIVERED,
            AnnouncementClient.VoteResult.GONE -> Result.success()
            AnnouncementClient.VoteResult.FAILED ->
                // A lost vote after several tries is acceptable; never loop
                // forever. The cap matches FeedbackFlushWorker's.
                if (runAttemptCount < 10) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_OPTION = "option"

        fun enqueue(context: Context, id: String, option: Int) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<AnnouncementVoteWorker>()
                    .setConstraints(constraints)
                    .setInputData(workDataOf(KEY_ID to id, KEY_OPTION to option))
                    .build()
                WorkManager.getInstance(context).enqueue(request)
            } catch (e: Exception) {
                // The tap already dismissed the notification; a failed
                // enqueue only loses that one vote.
            }
        }
    }
}
