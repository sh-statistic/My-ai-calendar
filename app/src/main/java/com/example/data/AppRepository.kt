package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(private val database: AppDatabase) {

    private val eventDao = database.eventDao()
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()
    private val syncLogDao = database.syncLogDao()

    val allActiveEvents: Flow<List<EventEntity>> = eventDao.getAllActiveEvents()
    val allActiveTasks: Flow<List<TaskEntity>> = taskDao.getAllActiveTasks()
    val allActiveNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val recentSyncLogs: Flow<List<SyncLogEntity>> = syncLogDao.getRecentLogs()

    fun getEventsByDate(persianDate: String): Flow<List<EventEntity>> =
        eventDao.getEventsByDate(persianDate)

    // Events
    suspend fun insertEvent(event: EventEntity) = withContext(Dispatchers.IO) {
        eventDao.insertEvent(event.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateEvent(event: EventEntity) = withContext(Dispatchers.IO) {
        eventDao.updateEvent(event.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteEvent(id: String) = withContext(Dispatchers.IO) {
        eventDao.softDeleteEvent(id, System.currentTimeMillis())
    }

    // Tasks
    suspend fun insertTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleTask(id: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        taskDao.toggleTaskCompletion(id, isCompleted, System.currentTimeMillis())
    }

    suspend fun deleteTask(id: String) = withContext(Dispatchers.IO) {
        taskDao.softDeleteTask(id, System.currentTimeMillis())
    }

    // Notes
    suspend fun insertNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.softDeleteNote(id, System.currentTimeMillis())
    }

    // Export local state to SyncData
    suspend fun getFullSyncData(deviceId: String = "Android-Device"): SyncData = withContext(Dispatchers.IO) {
        val events = eventDao.getAllEventsForSync()
        val tasks = taskDao.getAllTasksForSync()
        val notes = noteDao.getAllNotesForSync()
        SyncData(
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            events = events,
            tasks = tasks,
            notes = notes
        )
    }

    // Merge incoming sync data using Last-Write-Wins (LWW) conflict resolution
    suspend fun mergeSyncData(remoteData: SyncData, source: String): SyncData = withContext(Dispatchers.IO) {
        var mergedCount = 0

        // Merge Events
        for (remoteEvent in remoteData.events) {
            val localEvent = eventDao.getEventById(remoteEvent.id)
            if (localEvent == null) {
                eventDao.insertEvent(remoteEvent)
                mergedCount++
            } else if (remoteEvent.updatedAt > localEvent.updatedAt) {
                eventDao.insertEvent(remoteEvent)
                mergedCount++
            }
        }

        // Merge Tasks
        for (remoteTask in remoteData.tasks) {
            val localTask = taskDao.getTaskById(remoteTask.id)
            if (localTask == null) {
                taskDao.insertTask(remoteTask)
                mergedCount++
            } else if (remoteTask.updatedAt > localTask.updatedAt) {
                taskDao.insertTask(remoteTask)
                mergedCount++
            }
        }

        // Merge Notes
        for (remoteNote in remoteData.notes) {
            val localNote = noteDao.getNoteById(remoteNote.id)
            if (localNote == null) {
                noteDao.insertNote(remoteNote)
                mergedCount++
            } else if (remoteNote.updatedAt > localNote.updatedAt) {
                noteDao.insertNote(remoteNote)
                mergedCount++
            }
        }

        // Record log
        syncLogDao.insertLog(
            SyncLogEntity(
                source = source,
                details = "همگام‌سازی $mergedCount مورد با شناسه مبدأ ${remoteData.deviceId}",
                itemsCount = mergedCount,
                isSuccess = true
            )
        )

        // Return latest merged full data to send back
        getFullSyncData()
    }

    // Sync logs
    suspend fun recordLog(source: String, details: String, itemsCount: Int = 0, isSuccess: Boolean = true) =
        withContext(Dispatchers.IO) {
            syncLogDao.insertLog(
                SyncLogEntity(
                    source = source,
                    details = details,
                    itemsCount = itemsCount,
                    isSuccess = isSuccess
                )
            )
        }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        syncLogDao.clearLogs()
    }
}
