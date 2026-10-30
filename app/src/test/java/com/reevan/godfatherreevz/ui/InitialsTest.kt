package com.reevan.godfatherreevz.ui

import com.reevan.godfatherreevz.ui.common.initialsOf
import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {

    @Test
    fun `first and last word`() {
        assertEquals("MR", initialsOf("Marco Rossi"))
        assertEquals("VC", initialsOf("Vito Andolini Corleone"))
    }

    @Test
    fun `one word, odd spacing and punctuation`() {
        assertEquals("C", initialsOf("cher"))
        assertEquals("AB", initialsOf("  (anna)   bell  "))
        assertEquals("?", initialsOf("   "))
    }
}
