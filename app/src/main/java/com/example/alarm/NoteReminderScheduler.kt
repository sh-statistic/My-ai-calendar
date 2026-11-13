package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.NoteEntity

object NoteReminderScheduler {

    fun scheduleNoteReminder(context: Context, note: NoteEntity) {
        if (note.isDeleted || note.reminderAt <= System.currentTimeMillis()) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                action = NoteReminderReceiver.ACTION_NOTE_REMINDER
                putExtra("note_id", note.id)
                putExtra("note_title", note.title)
                putExtra(
                    "note_snippet",
                    note.content.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: ""
                )
            }
            val requestCode = ("note_" + note.id).hashCode()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, note.reminderAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, note.reminderAt, pendingIntent)
            }
        } catch (_: Exception) {
        }
    }

    fun cancelNoteReminder(context: Context, noteId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                action = NoteReminderReceiver.ACTION_NOTE_REMINDER
            }
            val requestCode = ("note_" + noteId).hashCode()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }
}
