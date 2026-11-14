package com.example

import com.example.data.BackupArchive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files

/**
 * Round-trip tests for the ZIP backup archive format: the data JSON, the manifest
 * mapping and the raw media bytes must all come back exactly as they went in.
 */
class BackupArchiveTest {

    @Test
    fun archiveRoundTripPreservesDataAndMedia() {
        val dir = Files.createTempDirectory("backup-archive-test").toFile()
        val drawing = File(dir, "drawing1.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val sidecar = File(dir, "drawing1.json").apply { writeText("{\"strokes\":[]}") }
        val photo = File(dir, "photo1.jpg").apply { writeBytes(ByteArray(1000) { (it % 251).toByte() }) }
        val voice = File(dir, "voice1.m4a").apply { writeBytes(byteArrayOf(9, 9, 9)) }
        val blockPhoto = File(dir, "block1.png").apply { writeBytes(byteArrayOf(7, 7)) }

        val dataJson = "{\"deviceId\":\"test\",\"events\":[],\"tasks\":[],\"notes\":[]}"
        val media = listOf(
            BackupArchive.MediaItem("note-1", BackupArchive.KIND_DRAWING, null, drawing, sidecar),
            BackupArchive.MediaItem("note-1", BackupArchive.KIND_IMAGE, null, photo),
            BackupArchive.MediaItem("note-2", BackupArchive.KIND_AUDIO, null, voice),
            BackupArchive.MediaItem("note-3", BackupArchive.KIND_BLOCK_IMAGE, "/storage/block1.png", blockPhoto)
        )

        val out = ByteArrayOutputStream()
        BackupArchive.write(dataJson, media, out)
        val zipBytes = out.toByteArray()

        // Magic + header read
        val zipFile = File(dir, "backup.zip").apply { writeBytes(zipBytes) }
        assertTrue(BackupArchive.isArchive(zipFile))
        assertEquals(dataJson, BackupArchive.readDataJson(ByteArrayInputStream(zipBytes)))

        val read = BackupArchive.read(ByteArrayInputStream(zipBytes))
        assertEquals(dataJson, read.dataJson)
        assertEquals(4, read.media.size)

        val r0 = read.media[0]
        assertEquals("note-1", r0.noteId)
        assertEquals(BackupArchive.KIND_DRAWING, r0.kind)
        assertEquals(null, r0.blockKey)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5), r0.bytes)
        assertEquals("{\"strokes\":[]}", r0.sidecarText)

        val r1 = read.media[1]
        assertEquals(BackupArchive.KIND_IMAGE, r1.kind)
        assertArrayEquals(ByteArray(1000) { (it % 251).toByte() }, r1.bytes)
        assertEquals(null, r1.sidecarText)

        val r3 = read.media[3]
        assertEquals("note-3", r3.noteId)
        assertEquals(BackupArchive.KIND_BLOCK_IMAGE, r3.kind)
        assertEquals("/storage/block1.png", r3.blockKey)
        assertArrayEquals(byteArrayOf(7, 7), r3.bytes)
    }

    @Test
    fun plainJsonIsNotAnArchive() {
        val dir = Files.createTempDirectory("backup-archive-test2").toFile()
        val json = File(dir, "backup.json").apply { writeText("{\"deviceId\":\"x\"}") }
        assertFalse(BackupArchive.isArchive(json))
    }

    @Test
    fun progressCallbacksAreMonotonicAndComplete() {
        val dir = Files.createTempDirectory("backup-archive-progress").toFile()
        val photo = File(dir, "photo.jpg").apply { writeBytes(ByteArray(5000) { (it % 253).toByte() }) }
        val dataJson = "{\"deviceId\":\"progress\"}"
        val media = listOf(BackupArchive.MediaItem("n1", BackupArchive.KIND_IMAGE, null, photo))

        val writeFractions = mutableListOf<Float>()
        val out = ByteArrayOutputStream()
        BackupArchive.write(dataJson, media, out) { writeFractions.add(it) }
        assertTrue(writeFractions.isNotEmpty())
        assertEquals(1f, writeFractions.last(), 0.0001f)
        assertTrue(writeFractions.zipWithNext().all { (a, b) -> b >= a })

        val zipBytes = out.toByteArray()
        val readFractions = mutableListOf<Float>()
        BackupArchive.read(ByteArrayInputStream(zipBytes), zipBytes.size.toLong()) { readFractions.add(it) }
        assertTrue(readFractions.isNotEmpty())
        assertTrue(readFractions.all { it in 0f..1f })
        assertTrue(readFractions.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun archiveWithoutMediaRoundTrips() {
        val dataJson = "{\"deviceId\":\"empty\"}"
        val out = ByteArrayOutputStream()
        BackupArchive.write(dataJson, emptyList(), out)
        val read = BackupArchive.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(dataJson, read.dataJson)
        assertTrue(read.media.isEmpty())
    }
}
