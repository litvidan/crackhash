package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Mutation
import com.litvidan.grpc.HashCrackerServiceGrpcKt
import com.litvidan.grpc.Service
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

        // Initialize request state
        requestStates[requestId] = CrackRequestState(status = RequestStatus.IN_PROGRESS)

        // Launch the background processing coroutine
        GlobalScope.launch {
            handleCrackRequest(requestId, hash, maxLength, config)
        }

        return requestId
    }

    /**
     * Handles the complete lifecycle of a crack request:
     * - Dispatches tasks to workers
     * - Waits for the first successful result
     * - Updates the final state (READY or ERROR)
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
                    val (resultChannel, workerJobs) = launchWorkers(requestId, hash, maxLength, config)

                    // Wait for the first successful result
                    val foundWord = resultChannel.receive()
                    println("Result received for $requestId! Word: $foundWord. Cancelling other workers.")

                    // Cancel all remaining worker jobs
                    workerJobs.forEach { it.cancelAndJoin() }

                    // Update state to READY with the found word
                    requestStates[requestId] = CrackRequestState(
                        status = RequestStatus.READY,
                        data = listOf(foundWord)
                    )
                    println("Request $requestId finished successfully.")
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("Request $requestId timed out.")
            requestStates[requestId] = CrackRequestState(status = RequestStatus.ERROR)
        } catch (e: Exception) {
            println("Request $requestId failed with error: ${e.message}")
            requestStates[requestId] = CrackRequestState(status = RequestStatus.ERROR)
        }
    }

    /**
     * Launches all worker coroutines and returns a channel that will receive
     * the first found word, plus the list of worker jobs for cancellation.
     */
    private suspend fun CoroutineScope.launchWorkers(
        requestId: String,
        hash: String,
        maxLength: Int,
        config: GatewayConfig.Config
    ): Pair<Channel<String>, List<Job>> {
        val resultChannel = Channel<String>()
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
                    resultChannel = resultChannel
                )
            }
        }
        return Pair(resultChannel, jobs)
    }

    /**
     * Processes one worker: computes its range, sends a gRPC request,
     * and if a result is found, pushes it into the result channel.
     */
    private suspend fun processSingleWorker(
        requestId: String,
        workerIndex: Int,
        workerHost: String,
        hash: String,
        maxLength: Int,
        totalCombinations: BigInteger,
        config: GatewayConfig.Config,
        resultChannel: Channel<String>
    ) {
        val (startIndex, rangeSize) = calculateWorkerRange(
            workerIndex = workerIndex,
            totalCombinations = totalCombinations,
            workerCount = config.workerCount
        )
        if (rangeSize <= BigInteger.ZERO) {
            // No work for this worker
            return
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
                // Send the found word to the channel (only the first one will be used)
                resultChannel.send(response.foundWord)
            }
        } catch (e: CancellationException) {
            println("Worker $workerHost for request $requestId was cancelled.")
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