package com.example

import com.example.data.NoteBlockData
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.noteBlocksDataToJson
import com.example.data.parseNoteBlocksData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * سناریوهای یخ‌زدهٔ بکاپ و ریکاوری (از چک‌لیست QA بخش یادداشت‌ها):
 * هر چیزی که داخل بکاپ می‌رود باید دقیقاً همان‌طور برگردد.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRoundTripTest {

    @Test
    fun blocksJson_roundTrip_preservesKindsAndOrder() {
        val blocks = listOf(
            NoteBlockData("t", text = "سلام"),
            NoteBlockData("i", path = "/data/img_1.jpg"),
            NoteBlockData("d", path = "/data/drawing_1.png"),
            NoteBlockData("c", text = "[{\"text\":\"کار\",\"isChecked\":false}]"),
            NoteBlockData("a", path = "/data/voice_1.m4a"),
            NoteBlockData("t", text = "ادامهٔ متن")
        )
        val parsed = parseNoteBlocksData(noteBlocksDataToJson(blocks))
        assertEquals(blocks, parsed)
    }

    @Test
    fun syncData_roundTrip_preservesDrawingSidecars() {
        val note = NoteEntity(
            id = "note-1",
            title = "طرح‌دار",
            content = "متن",
            drawingPath = "/data/drawings/drawing_note-1.png",
            reminderAt = 1893456000000L,
            isArchived = true,
            blocksJson = noteBlocksDataToJson(
                listOf(
                    NoteBlockData("t", text = "قبل"),
                    NoteBlockData("d", path = "/data/drawings/block_1.png"),
                    NoteBlockData("a", path = "/data/notes_audio/voice_1.m4a")
                )
            )
        )
        val original = SyncData(
            deviceId = "test-device",
            timestamp = 1762900000000L,
            events = emptyList(),
            tasks = emptyList(),
            notes = listOf(note),
            noteDrawingsBase64 = mapOf("note-1" to "aGVsbG8="),
            noteDrawingSidecars = mapOf("note-1" to "{\"strokes\":[1,2,3]}"),
            noteBlockDrawingsBase64 = mapOf("note-1" to mapOf("/data/drawings/block_1.png" to "d29ybGQ=")),
            noteBlockDrawingSidecars = mapOf("note-1" to mapOf("/data/drawings/block_1.png" to "{\"strokes\":[9]}")),
            noteBlockAudiosBase64 = mapOf("note-1" to mapOf("/data/notes_audio/voice_1.m4a" to "c291bmQ="))
        )
        val restored = SyncData.fromJsonString(original.toJsonString())
        assertEquals(original.noteDrawingSidecars, restored.noteDrawingSidecars)
        assertEquals(original.noteBlockDrawingSidecars, restored.noteBlockDrawingSidecars)
        assertEquals(original.noteDrawingsBase64, restored.noteDrawingsBase64)
        assertEquals(original.noteBlockDrawingsBase64, restored.noteBlockDrawingsBase64)
        assertEquals(original.noteBlockAudiosBase64, restored.noteBlockAudiosBase64)
        val restoredNote = restored.notes.single()
        assertEquals(note.blocksJson, restoredNote.blocksJson)
        assertEquals(note.reminderAt, restoredNote.reminderAt)
        assertTrue(restoredNote.isArchived)
        assertEquals(note.drawingPath, restoredNote.drawingPath)
    }

    @Test
    fun syncData_roundTrip_keepsArchivedAndTrashedNotes() {
        val archived = NoteEntity(id = "n-arch", title = "بایگانی", content = "", isArchived = true)
        val trashed = NoteEntity(
            id = "n-trash",
            title = "حذف‌شده",
            content = "",
            isDeleted = true,
            deletedAt = 1762900000000L
        )
        val original = SyncData(
            deviceId = "test-device",
            timestamp = 1762900000000L,
            events = emptyList(),
            tasks = emptyList(),
            notes = listOf(archived, trashed)
        )
        val restored = SyncData.fromJsonString(original.toJsonString())
        assertEquals(2, restored.notes.size)
        assertTrue(restored.notes.first { it.id == "n-arch" }.isArchived)
        val restoredTrash = restored.notes.first { it.id == "n-trash" }
        assertTrue(restoredTrash.isDeleted)
        assertEquals(1762900000000L, restoredTrash.deletedAt)
    }
}
