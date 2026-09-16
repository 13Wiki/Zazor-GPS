package com.gps.zazor

import com.gps.zazor.data.prefs.CodeHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHasherTest {

    @Test
    fun `a code matches its own hash`() {
        val stored = CodeHasher.hash("12345")

        assertTrue(CodeHasher.matches("12345", stored))
    }

    @Test
    fun `another code does not`() {
        val stored = CodeHasher.hash("12345")

        assertFalse(CodeHasher.matches("54321", stored))
        assertFalse(CodeHasher.matches("", stored))
    }

    @Test
    fun `the same code hashes differently every time`() {
        assertNotEquals(CodeHasher.hash("12345"), CodeHasher.hash("12345"))
    }

    @Test
    fun `the stored value carries no trace of the code`() {
        assertFalse(CodeHasher.hash("12345").contains("12345"))
    }

    @Test
    fun `a value written before codes were hashed is recognised as such`() {
        assertFalse(CodeHasher.isHashed("12345"))
        assertTrue(CodeHasher.isHashed(CodeHasher.hash("12345")))
    }

    @Test
    fun `a damaged stored value is refused rather than thrown at the caller`() {
        assertFalse(CodeHasher.matches("12345", "nonsense"))
        assertFalse(CodeHasher.matches("12345", "zz:zz"))
        assertFalse(CodeHasher.matches("12345", "abc:"))
    }
}
