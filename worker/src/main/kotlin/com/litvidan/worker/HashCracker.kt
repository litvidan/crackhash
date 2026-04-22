package com.litvidan.worker

import kotlinx.coroutines.yield
import java.math.BigInteger
import java.security.MessageDigest

class HashCracker(private val alphabet: String) {

    suspend fun crack(
        targetHash: String,
        maxLength: Int,
        startIndex: BigInteger,
        rangeSize: BigInteger
    ): String {
        var i = BigInteger.ZERO
        while (i < rangeSize) {
            yield()

            val globalIndex = startIndex + i
            val word = indexToWord(globalIndex, alphabet)
            val hash = md5(word)
            if (hash == targetHash) {
                return word
            }
            i = i.add(BigInteger.ONE)
        }
        return ""
    }

    private fun indexToWord(index: BigInteger, alphabet: String): String {
        val base = BigInteger.valueOf(alphabet.length.toLong())
        if (index == BigInteger.ZERO) {
            return alphabet[0].toString()
        }

        var totalForShorterWords = BigInteger.ZERO
        var len = 1
        while (true) {
            val combinationsForLen = base.pow(len)
            if (index < totalForShorterWords + combinationsForLen) {
                var indexInLen = index - totalForShorterWords
                val builder = StringBuilder()
                for (i in 0 until len) {
                    val remainder = indexInLen % base
                    builder.insert(0, alphabet[remainder.toInt()])
                    indexInLen /= base
                }
                while (builder.length < len) {
                    builder.insert(0, alphabet[0])
                }
                return builder.toString()
            }
            totalForShorterWords += combinationsForLen
            len++
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return BigInteger(1, digest).toString(16).padStart(32, '0')
    }
}