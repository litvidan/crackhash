package com.litvidan.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class HashCrackerServiceTest {

    @Test
    fun `test indexToWord with simple alphabet`() {
        val alphabet = "ab"
        assertEquals("a", indexToWord(BigInteger.valueOf(0), alphabet))
        assertEquals("b", indexToWord(BigInteger.valueOf(1), alphabet))
        assertEquals("aa", indexToWord(BigInteger.valueOf(2), alphabet))
        assertEquals("ab", indexToWord(BigInteger.valueOf(3), alphabet))
        assertEquals("ba", indexToWord(BigInteger.valueOf(4), alphabet))
        assertEquals("bb", indexToWord(BigInteger.valueOf(5), alphabet))
        assertEquals("aaa", indexToWord(BigInteger.valueOf(6), alphabet))
    }

    @Test
    fun `test indexToWord with numeric alphabet`() {
        val alphabet = "0123456789"
        assertEquals("0", indexToWord(BigInteger.valueOf(0), alphabet))
        assertEquals("9", indexToWord(BigInteger.valueOf(9), alphabet))
        assertEquals("00", indexToWord(BigInteger.valueOf(10), alphabet))
        assertEquals("01", indexToWord(BigInteger.valueOf(11), alphabet))
        assertEquals("99", indexToWord(BigInteger.valueOf(109), alphabet))
        assertEquals("000", indexToWord(BigInteger.valueOf(110), alphabet))
    }

    @Test
    fun `test indexToWord with large index`() {
        val alphabet = "abcdefghijklmnopqrstuvwxyz"
        // Total words of length 1, 2, 3 = 26 + 26^2 + 26^3 = 18278
        // Index 18278 should be the first word of length 4, which is "aaaa"
        assertEquals("aaaa", indexToWord(BigInteger.valueOf(18278), alphabet))
    }
}
