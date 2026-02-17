package com.litvidan.worker

import com.litvidan.grpc.Service
import com.litvidan.grpc.HashCrackerServiceGrpcKt
import io.grpc.ServerBuilder
import kotlinx.coroutines.yield
import java.math.BigInteger
import java.security.MessageDigest

class HashCrackerService : HashCrackerServiceGrpcKt.HashCrackerServiceCoroutineImplBase() {

    override suspend fun crack(request: Service.CrackRequest): Service.CrackResponse {
        println("Received crack request for hash: ${request.targetHash}")
        println("Range: from ${request.startIndex} to ${request.startIndex + request.rangeSize}")

        val alphabet = request.alphabet
        val targetHash = request.targetHash
        val startIndex = BigInteger.valueOf(request.startIndex)
        val rangeSize = BigInteger.valueOf(request.rangeSize)

        var i = BigInteger.ZERO
        while (i < rangeSize) {
            // Check if the coroutine has been cancelled by the gateway
            yield()

            val globalIndex = startIndex + i
            val word = indexToWord(globalIndex, alphabet)
            val hash = md5(word)
            if (hash == targetHash) {
                println("Hash found! Word: '$word' for index $globalIndex")
                return Service.CrackResponse.newBuilder().setFoundWord(word).build()
            }
            i = i.add(BigInteger.ONE)
        }

        println("Hash not found in the assigned range.")
        return Service.CrackResponse.newBuilder().setFoundWord("").build()
    }
}

internal fun indexToWord(index: BigInteger, alphabet: String): String {
    var n = index
    val base = BigInteger.valueOf(alphabet.length.toLong())
    val builder = StringBuilder()

    if (n == BigInteger.ZERO) {
        return alphabet[0].toString()
    }
    
    var totalForShorterWords = BigInteger.ZERO
    var len = 1
    while (true) {
        val combinationsForLen = base.pow(len)
        if (n < totalForShorterWords + combinationsForLen) {
            var indexInLen = n - totalForShorterWords
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

internal fun md5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray())
    return BigInteger(1, digest).toString(16).padStart(32, '0')
}


fun main() {
    val server = ServerBuilder.forPort(50051)
        .addService(HashCrackerService())
        .build()
    println("Worker started on port 50051...")
    server.start()
    server.awaitTermination()
}
