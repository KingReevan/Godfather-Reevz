package com.reevan.godfatherreevz.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun `clean trims and collapses whitespace`() {
        assertEquals("+91 98765 43210", PhoneNumbers.clean("  +91  98765\t43210 "))
    }

    @Test
    fun `common formats are valid`() {
        listOf(
            "+91 98765 43210",
            "098765-43210",
            "(022) 2345.6789",
            "9876543210",
            "100",
            "+1 (415) 555-0100",
            "+966 50 123 4567",
        ).forEach { assertTrue(it, PhoneNumbers.isValid(it)) }
    }

    @Test
    fun `junk, letters and misplaced plus signs are invalid`() {
        listOf(
            "",
            "   ",
            "12",
            "abc1234",
            "98765 4321O", // letter O
            "91+98765",
            "++91 98765 43210",
            "1234567890123456", // 16 digits
            "#123#",
        ).forEach { assertFalse(it, PhoneNumbers.isValid(it)) }
    }

    @Test
    fun `the same number written differently matches`() {
        assertTrue(PhoneNumbers.sameNumber("+91 98765 43210", "098765 43210"))
        assertTrue(PhoneNumbers.sameNumber("9876543210", "+91-98765-43210"))
        assertTrue(PhoneNumbers.sameNumber("100", "1 0 0"))
    }

    @Test
    fun `different numbers do not match`() {
        assertFalse(PhoneNumbers.sameNumber("+91 98765 43210", "+91 98765 43211"))
        assertFalse(PhoneNumbers.sameNumber("100", "9876543100"))
        assertFalse(PhoneNumbers.sameNumber("", ""))
    }

    @Test
    fun `the dial string keeps only the plus and digits`() {
        assertEquals("+919876543210", PhoneNumbers.dialString(" +91 (98765) 43-210 "))
        assertEquals("02223456789", PhoneNumbers.dialString("(022) 2345.6789"))
    }
}
