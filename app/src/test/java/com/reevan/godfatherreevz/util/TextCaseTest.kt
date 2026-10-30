package com.reevan.godfatherreevz.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextCaseTest {

    @Test
    fun capitalisesTheFirstLetterOfEveryWord() {
        assertEquals("Marco Rossi", capitalizeWords("marco rossi"))
        assertEquals("Uncle Vito 2", capitalizeWords("uncle vito 2"))
    }

    @Test
    fun leavesTheRestOfEachWordAsTyped() {
        assertEquals("Dj ROB", capitalizeWords("dj ROB"))
        assertEquals("Anna-maria", capitalizeWords("anna-maria"))
        assertEquals("O'brien", capitalizeWords("o'brien"))
    }

    @Test
    fun handlesRepeatedLeadingAndTrailingWhitespace() {
        assertEquals("  Two  Spaces ", capitalizeWords("  two  spaces "))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", capitalizeWords(""))
    }

    @Test
    fun keepsTheLengthSoTheCursorStaysPut() {
        listOf("ßtraße", "ǆungla", "émile zola", "ﬁx it").forEach {
            assertEquals(it, it.length, capitalizeWords(it).length)
        }
    }
}
