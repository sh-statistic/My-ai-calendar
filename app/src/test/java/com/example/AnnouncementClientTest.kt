package com.example

import com.example.announce.Announcement
import com.example.announce.AnnouncementClient
import com.example.announce.AnnouncementStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the announcement contract with the relay: parsing the live
 * announcement list (options capped at the Android notification limit,
 * junk tolerated, the legacy single field still understood) and the
 * show-once-per-announcement decision over the seen-id set. Robolectric
 * for the real org.json, same as FeedbackSenderTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnnouncementClientTest {

    @Test
    fun parse_listOfAnnouncements() {
        val list = AnnouncementClient.parseAnnouncements(
            """{"announcements":[{"id":"1731","text":"نظرت درباره بخش یادداشت چیه؟","options":["لایک","دیسلایک"],"ts":1731},{"id":"1732","text":"نسخه تازه منتشر شد","options":[]}],"announcement":{"id":"1732","text":"نسخه تازه منتشر شد","options":[]}}"""
        )
        assertEquals(2, list.size)
        assertEquals("1731", list[0].id)
        assertEquals(listOf("لایک", "دیسلایک"), list[0].options)
        assertEquals("1732", list[1].id)
        assertEquals(emptyList<String>(), list[1].options)
    }

    @Test
    fun parse_legacySingleFieldStillUnderstood() {
        val list = AnnouncementClient.parseAnnouncements(
            """{"announcement":{"id":"9","text":"نسخه تازه منتشر شد","options":[]}}"""
        )
        assertEquals(1, list.size)
        assertEquals("9", list[0].id)
    }

    @Test
    fun parse_noAnnouncement_returnsEmpty() {
        assertTrue(AnnouncementClient.parseAnnouncements("""{"announcement":null}""").isEmpty())
        assertTrue(AnnouncementClient.parseAnnouncements("""{"announcements":[]}""").isEmpty())
        assertTrue(AnnouncementClient.parseAnnouncements("""{}""").isEmpty())
    }

    @Test
    fun parse_junk_returnsEmpty() {
        assertTrue(AnnouncementClient.parseAnnouncements("not json").isEmpty())
        assertTrue(AnnouncementClient.parseAnnouncements("").isEmpty())
    }

    @Test
    fun parse_blankEntriesDroppedFromList() {
        val list = AnnouncementClient.parseAnnouncements(
            """{"announcements":[{"id":"","text":"سلام"},{"id":"5","text":"  "},{"id":"6","text":"درست"}]}"""
        )
        assertEquals(listOf("6"), list.map { it.id })
    }

    @Test
    fun parse_optionsCappedAtThree_andBlanksDropped() {
        val list = AnnouncementClient.parseAnnouncements(
            """{"announcements":[{"id":"7","text":"کدوم بخش؟","options":["تقویم"," ","یادداشت","وظیفه‌ها","اضافی"]}]}"""
        )
        assertEquals(listOf("تقویم", "یادداشت", "وظیفه‌ها"), list[0].options)
    }

    @Test
    fun shouldShow_onlyForUnseenAnnouncement() {
        val ann = Announcement("42", "سلام", emptyList())
        assertTrue(AnnouncementStore.shouldShow(ann, emptySet()))
        assertTrue(AnnouncementStore.shouldShow(ann, setOf("41")))
        assertFalse(AnnouncementStore.shouldShow(ann, setOf("41", "42")))
        assertFalse(AnnouncementStore.shouldShow(null, emptySet()))
        assertFalse(AnnouncementStore.shouldShow(Announcement("", "سلام", emptyList()), emptySet()))
    }

    @Test
    fun seenSet_addAndCodecRoundTrip() {
        var seen = AnnouncementStore.addSeen(emptySet(), "100")
        seen = AnnouncementStore.addSeen(seen, "200")
        seen = AnnouncementStore.addSeen(seen, "100")
        assertEquals(setOf("100", "200"), seen)
        assertEquals(seen, AnnouncementStore.decodeSeen(AnnouncementStore.encodeSeen(seen)))
        assertEquals(emptySet<String>(), AnnouncementStore.decodeSeen(null))
        assertEquals(emptySet<String>(), AnnouncementStore.decodeSeen("junk"))
    }

    @Test
    fun seenSet_cappedAtNewestFifty() {
        var seen: Set<String> = emptySet()
        for (i in 1..60) seen = AnnouncementStore.addSeen(seen, i.toString())
        assertEquals(AnnouncementStore.MAX_SEEN, seen.size)
        assertTrue("60" in seen)
        assertFalse("1" in seen)
        assertFalse("10" in seen)
        assertTrue("11" in seen)
    }
}
