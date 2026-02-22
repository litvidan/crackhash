package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Mutation
import com.litvidan.grpc.HashCrackerServiceGrpcKt
import com.litvidan.grpc.Service
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * GraphQL mutation for initiating a hash cracking request.
 */
class CrackHashMutation : Mutation {

    /**
     * Starts a new hash cracking task.
     *
     * @param hash      The target hash to crack.
     * @param maxLength Maximum length of the word to try.
     * @return Unique request ID that can be used to poll the status.
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun crackHash(hash: String, maxLength: Int): String {
        val config = GatewayConfig.current
        val requestId = UUID.randomUUID().toString()
        println("Received crack request for hash '$hash'. Assigned requestId: $requestId")

        // Initialize state
        requestStates[requestId] = CrackRequestState(status = RequestStatus.IN_PROGRESS)

        // Launch background processing
        GlobalScope.launch {
            handleCrackRequest(requestId, hash, maxLength, config)
        }

        return requestId
    }

    /**
     * Handles the complete lifecycle of a crack request:
     * - Dispatches tasks to workers
     * - Waits for all workers to complete (or timeout)
     * - Collects all found words (handles hash collisions)
     * - Updates the final state (READY with results or ERROR)
     */
    private suspend fun handleCrackRequest(
        requestId: String,
        hash: String,
        maxLength: Int,
        config: GatewayConfig.Config
    ) {
        try {
            withTimeout(config.requestTimeoutMillis) {
                coroutineScope {
                    val results = launchWorkers(requestId, hash, maxLength, config)

                    // All workers finished, set final READY status with all found words
                    requestStates[requestId] = CrackRequestState(
                        status = RequestStatus.READY,
                        data = results.ifEmpty { emptyList() }
                    )
                    println("Request $requestId finished with ${results.size} result(s).")
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("Request $requestId timed out.")
            requestStates[requestId] = CrackRequestState(status = RequestStatus.TIMEOUT)
        } catch (e: Exception) {
            println("Request $requestId failed with error: ${e.message}")
            requestStates[requestId] = CrackRequestState(status = RequestStatus.ERROR)
        }
    }

    /**
     * Launches all worker coroutines and waits for their completion.
     * Returns a list of all words found by any worker.
     */
    private suspend fun CoroutineScope.launchWorkers(
        requestId: String,
        hash: String,
        maxLength: Int,
        config: GatewayConfig.Config
    ): List<String> {
        val results = Collections.synchronizedList(mutableListOf<String>())
        val totalCombinations = calculateTotalCombinations(maxLength, config.alphabet)

        val jobs = config.workerHosts.mapIndexed { index, workerHost ->
            launch {
                processSingleWorker(
                    requestId = requestId,
                    workerIndex = index,
                    workerHost = workerHost,
                    hash = hash,
                    maxLength = maxLength,
                    totalCombinations = totalCombinations,
                    config = config,
                    results = results
                )
            }
        }

        jobs.joinAll()
        return results
    }

    /**
     * Processes one worker: computes its range, sends a gRPC request,
     * and if a result is found, adds it to the shared results list and updates the state.
     */
    private suspend fun processSingleWorker(
        requestId: String,
        workerIndex: Int,
        workerHost: String,
        hash: String,
        maxLength: Int,
        totalCombinations: BigInteger,
        config: GatewayConfig.Config,
        results: MutableList<String>
    ) {
        val (startIndex, rangeSize) = calculateWorkerRange(
            workerIndex = workerIndex,
            totalCombinations = totalCombinations,
            workerCount = config.workerCount
        )
        if (rangeSize <= BigInteger.ZERO) {
            return // No work for this worker
        }

        val channel = ManagedChannelBuilder.forAddress(workerHost, config.workerPort)
            .usePlaintext()
            .build()

        try {
            val stub = HashCrackerServiceGrpcKt.HashCrackerServiceCoroutineStub(channel)
            val request = buildWorkerRequest(hash, config.alphabet, maxLength, startIndex, rangeSize)

            println("Dispatching task to $workerHost for requestId $requestId")
            val response = stub.crack(request)

            if (response.foundWord.isNotEmpty()) {
                results.add(response.foundWord)
                println("Found word '${response.foundWord}' from $workerHost for request $requestId")

                // Atomically update the shared state to PARTIAL (if still IN_PROGRESS)
                requestStates.compute(requestId) { _, currentState ->
                    when {
                        currentState == null -> null
                        currentState.status == RequestStatus.ERROR -> currentState
                        else -> {
                            val existingData = currentState.data ?: emptyList()
                            val newData = existingData + response.foundWord
                            val newStatus = if (currentState.status == RequestStatus.IN_PROGRESS) {
                                RequestStatus.PARTIAL
                            } else {
                                currentState.status // already PARTIAL or READY (though READY shouldn't happen here)
                            }
                            currentState.copy(status = newStatus, data = newData)
                        }
                    }
                }
            }
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /**
     * Calculates the total number of combinations for given maxLength and alphabet.
     */
    private fun calculateTotalCombinations(maxLength: Int, alphabet: String): BigInteger {
        val alphabetSize = alphabet.length.toBigInteger()
        return (1..maxLength).fold(BigInteger.ZERO) { sum, length ->
            sum + alphabetSize.pow(length)
        }
    }

    /**
     * Determines the start index and range size for a specific worker.
     */
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

    /**
     * Builds a gRPC crack request from the given parameters.
     */
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