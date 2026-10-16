package com.example.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLogger {
    fun log(context: Context, source: String, details: String, isSuccess: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context.applicationContext)
                db.syncLogDao().insertLog(
                    SyncLogEntity(
                        source = source,
                        details = details,
                        isSuccess = isSuccess
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
