package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Mutation
import com.litvidan.grpc.HashCrackerServiceGrpcKt
import com.litvidan.grpc.Service
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

class CrackHashMutation : Mutation {

    @OptIn(DelicateCoroutinesApi::class)
    fun crackHash(hash: String, maxLength: Int): String {
        val config = GatewayConfig.current
        val requestId = UUID.randomUUID().toString()
        println("Received crack request for hash '$hash'. Assigned requestId: $requestId")

        requestStates[requestId] = CrackRequestState(status = RequestStatus.IN_PROGRESS)

        GlobalScope.launch {
            handleCrackRequest(requestId, hash, maxLength, config)
        }

        return requestId
    }

    private suspend fun handleCrackRequest(
        requestId: String,
        hash: String,
        maxLength: Int,
        config: GatewayConfig.Config
    ) {
        try {
            coroutineScope {
                val totalCombinations = calculateTotalCombinations(maxLength, config.alphabet)

                val tasks = config.workerHosts.mapIndexed { index, workerHost ->
                    async {
                        val (startIndex, rangeSize) = calculateWorkerRange(index, totalCombinations, config.workerCount)
                        if (rangeSize <= BigInteger.ZERO) {
                            return@async WorkerResult(WorkerResultType.SUCCESS)
                        }
                        processSingleWorker(requestId, workerHost, hash, config, maxLength, startIndex, rangeSize)
                    }
                }

                val results = tasks.awaitAll()

                // --- Final Status Logic ---
                val foundWords = results.mapNotNull { it.foundWord.ifEmpty { null } }
                val numTimeouts = results.count { it.type == WorkerResultType.TIMEOUT }
                val numErrors = results.count { it.type == WorkerResultType.ERROR }

                val finalStatus = when {
                    foundWords.isNotEmpty() -> RequestStatus.READY
                    numTimeouts == config.workerCount -> RequestStatus.TIMEOUT
                    numErrors > 0 || numTimeouts > 0 -> RequestStatus.ERROR
                    else -> RequestStatus.READY
                }
                
                requestStates[requestId] = CrackRequestState(
                    status = finalStatus,
                    data = foundWords.ifEmpty { null }
                )
                println("Request $requestId finished with final status $finalStatus.")
            }
        } catch (e: Exception) {
            println("Request $requestId failed with a critical gateway error: ${e.message}")
            requestStates[requestId] = CrackRequestState(status = RequestStatus.ERROR)
        }
    }

    private suspend fun processSingleWorker(
        requestId: String,
        workerHost: String,
        hash: String,
        config: GatewayConfig.Config,
        maxLength: Int,
        startIndex: BigInteger,
        rangeSize: BigInteger
    ): WorkerResult {
        var channel: ManagedChannel? = null
        return try {
            withTimeout(config.requestTimeoutMillis) {
                channel = ManagedChannelBuilder.forAddress(workerHost, config.workerPort).usePlaintext().build()
                
                val stub = HashCrackerServiceGrpcKt.HashCrackerServiceCoroutineStub(channel)
                val request = buildWorkerRequest(hash, config.alphabet, maxLength, startIndex, rangeSize)

                println("Dispatching task to $workerHost for requestId $requestId")
                val response = stub.crack(request)

                if (response.foundWord.isNotEmpty()) {
                    println("Found word '${response.foundWord}' from $workerHost for request $requestId")
                    requestStates.compute(requestId) { _, currentState ->
                        val existingData = currentState?.data ?: emptyList()
                        val newData = existingData + response.foundWord
                        CrackRequestState(RequestStatus.PARTIAL, newData)
                    }
                }
                WorkerResult(WorkerResultType.SUCCESS, response.foundWord)
            }
        } catch (e: TimeoutCancellationException) {
            println("Worker $workerHost timed out: ${e.message}")
            WorkerResult(WorkerResultType.TIMEOUT)
        } catch (e: Exception) {
            println("Error processing worker $workerHost: ${e.message}")
            WorkerResult(WorkerResultType.ERROR)
        } finally {
            channel?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun calculateTotalCombinations(maxLength: Int, alphabet: String): BigInteger {
        val alphabetSize = alphabet.length.toBigInteger()
        return (1..maxLength).fold(BigInteger.ZERO) { sum, length ->
            sum + alphabetSize.pow(length)
        }
    }

    private fun calculateWorkerRange(
        workerIndex: Int,
        totalCombinations: BigInteger,
        workerCount: Int
    ): Pair<BigInteger, BigInteger> {
        val rangePerWorker = totalCombinations / workerCount.toBigInteger()
        val startIndex = workerIndex.toBigInteger() * rangePerWorker
        val rangeSize = if (workerIndex == workerCount - 1) {
            totalCombinations - startIndex
        } else {
            rangePerWorker
        }
        return Pair(startIndex, rangeSize)
    }

    private fun buildWorkerRequest(
        hash: String,
        alphabet: String,
        maxLength: Int,
        startIndex: BigInteger,
        rangeSize: BigInteger
    ): Service.CrackRequest {
        return Service.CrackRequest.newBuilder()
            .setTargetHash(hash)
            .setAlphabet(alphabet)
            .setMaxLength(maxLength)
            .setStartIndex(startIndex.toLong())
            .setRangeSize(rangeSize.toLong())
            .build()
    }
}
