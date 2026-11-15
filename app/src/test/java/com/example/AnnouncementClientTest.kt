package com.example

import com.example.announce.Announcement
import com.example.announce.AnnouncementClient
import com.example.announce.AnnouncementStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the announcement contract with the relay: parsing the latest
 * announcement (options capped at the Android notification limit, junk
 * tolerated) and the show-once decision. Robolectric for the real
 * org.json, same as FeedbackSenderTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnnouncementClientTest {

    @Test
    fun parse_fullAnnouncement() {
        val ann = AnnouncementClient.parseLatest(
            """{"announcement":{"id":"1731","text":"نظرت درباره بخش یادداشت چیه؟","options":["لایک","دیسلایک"],"ts":1731}}"""
        )
        assertEquals("1731", ann?.id)
        assertEquals("نظرت درباره بخش یادداشت چیه؟", ann?.text)
        assertEquals(listOf("لایک", "دیسلایک"), ann?.options)
    }

    @Test
    fun parse_plainAnnouncementWithoutOptions() {
        val ann = AnnouncementClient.parseLatest(
            """{"announcement":{"id":"9","text":"نسخه تازه منتشر شد","options":[]}}"""
        )
        assertEquals("9", ann?.id)
        assertEquals(emptyList<String>(), ann?.options)
    }

    @Test
    fun parse_noAnnouncement_returnsNull() {
        assertNull(AnnouncementClient.parseLatest("""{"announcement":null}"""))
        assertNull(AnnouncementClient.parseLatest("""{}"""))
    }

    @Test
    fun parse_junk_returnsNull() {
        assertNull(AnnouncementClient.parseLatest("not json"))
        assertNull(AnnouncementClient.parseLatest(""))
    }

    @Test
    fun parse_blankIdOrText_returnsNull() {
        assertNull(AnnouncementClient.parseLatest("""{"announcement":{"id":"","text":"سلام"}}"""))
        assertNull(AnnouncementClient.parseLatest("""{"announcement":{"id":"5","text":"  "}}"""))
        assertNull(AnnouncementClient.parseLatest("""{"announcement":{"text":"سلام"}}"""))
    }

    @Test
    fun parse_optionsCappedAtThree_andBlanksDropped() {
        val ann = AnnouncementClient.parseLatest(
            """{"announcement":{"id":"7","text":"کدوم بخش؟","options":["تقویم"," ","یادداشت","وظیفه‌ها","اضافی"]}}"""
        )
        assertEquals(listOf("تقویم", "یادداشت", "وظیفه‌ها"), ann?.options)
    }

    @Test
    fun shouldShow_onlyForFreshAnnouncement() {
        val ann = Announcement("42", "سلام", emptyList())
        assertTrue(AnnouncementStore.shouldShow(ann, null))
        assertTrue(AnnouncementStore.shouldShow(ann, "41"))
        assertFalse(AnnouncementStore.shouldShow(ann, "42"))
        assertFalse(AnnouncementStore.shouldShow(null, null))
        assertFalse(AnnouncementStore.shouldShow(Announcement("", "سلام", emptyList()), null))
    }
}
