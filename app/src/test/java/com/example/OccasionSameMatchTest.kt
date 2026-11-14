package com.example

import com.example.calendar.PersianOccasionsHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the dedupe matcher used when merging the fixed Shamsi list with
 * the yearly JSON / computed Qamari occasions.
 *
 * Regression cases from the fixed-list promotion (2026-11-14): generic
 * religious words (امام، حضرت، علیه، السلام) must not make two different
 * occasions "the same" — that collision silently dropped the martyrdom
 * of Imam Ali on 9 Esfand 1405 (and its holiday flag) once the long
 * 9 Esfand commemoration title joined the fixed list.
 */
class OccasionSameMatchTest {

    private val fixed9Esfand =
        "شهادت امام مجاهد حضرت آیت‌الله العظمی سیدعلی حسینی خامنه‌ای (قدس الله نفسه الزکیه) به همراه خانواده، جمعی از فرماندهان ارشد نظامی و مسئولان کشور، شهدای مدارس میناب و سالن ورزشی لامرد در تجاوز آمریکایی ـ صهیونی (1404 هـ ش) و روز حماسه دفاع از ایران"

    @Test
    fun differentOccasionsSharingGenericReligiousWords_areNotTheSame() {
        assertFalse(
            PersianOccasionsHelper.isSameOccasion(
                fixed9Esfand,
                "شهادت حضرت امام علی علیه السلام (۴۰ ه.ق) (تعطیل)"
            )
        )
        assertFalse(
            PersianOccasionsHelper.isSameOccasion(fixed9Esfand, "عید سعید فطر")
        )
    }

    @Test
    fun sameOccasionInDifferentWording_isStillDetected() {
        assertTrue(
            PersianOccasionsHelper.isSameOccasion(
                "آغاز نوروز (تعطیل)",
                "جشن نوروز / آغاز سال نو هجری شمسی"
            )
        )
        assertTrue(
            PersianOccasionsHelper.isSameOccasion(
                "روز معلم",
                "شهادت استاد مرتضی مطهری (1358 هـ ش) و روز معلم"
            )
        )
        assertTrue(
            PersianOccasionsHelper.isSameOccasion(
                "روز اصناف",
                "جشن آب‌پاشونک / آغاز تابستان / روز اصناف"
            )
        )
    }
}
