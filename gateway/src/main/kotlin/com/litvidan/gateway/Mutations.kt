package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Mutation
import com.litvidan.common.RabbitMqService
import com.litvidan.common.ResultMessage
import com.litvidan.common.TaskMessage
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*

class CrackHashMutation : Mutation {
    private val repository = TaskRepository()
    private val rabbitMq = RabbitMqService(GatewayConfig.current.rabbitmqHost)
    private val config = GatewayConfig.current

    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Start the listener for worker results
        rabbitMq.startResultConsumer { result ->
            handleWorkerResponse(result)
        }

        // On startup, re-send any pending parts that were not yet processed
        recoveryScope.launch {
            delay(5_000) // give RabbitMQ some time to connect
            val pendingTasks = repository.findAllByStatus(TaskStatus.PENDING_WORKER)
            pendingTasks.forEach { task ->
                resendMissingParts(task)
            }
        }
    }

    fun crackHash(hash: String, maxLength: Int): String {
        val requestId = UUID.randomUUID().toString()
        println("Received crack request for hash '$hash'. Assigned requestId: $requestId")

        val taskDoc = TaskDocument(
            requestId = requestId,
            hash = hash,
            maxLength = maxLength,
            alphabet = config.alphabet,
            totalParts = config.taskPartitionCount,   // store total parts from the beginning
            status = TaskStatus.PENDING_QUEUE
        )

        runBlocking {
            // Save to MongoDB with majority write concern
            repository.save(taskDoc)

            // Split and publish all parts
            publishTaskParts(taskDoc)

            // Mark as waiting for workers
            repository.updateStatus(requestId, TaskStatus.PENDING_WORKER)
        }

        return requestId
    }

    private suspend fun publishTaskParts(task: TaskDocument) {
        val totalCombinations = calculateTotalCombinations(task.maxLength, task.alphabet)
        val parts = splitIntoParts(totalCombinations, task.totalParts)

        parts.forEachIndexed { partId, (start, size) ->
            val taskMessage = TaskMessage(
                requestId = task.requestId,
                partId = partId,
                hash = task.hash,
                alphabet = task.alphabet,
                maxLength = task.maxLength,
                startIndex = start.toLong(),
                rangeSize = size.toLong()
            )
            rabbitMq.sendTask(taskMessage)
        }

        println("Published ${parts.size} parts for request ${task.requestId}")
    }

    /**
     * Re-send only the parts that haven't been processed yet (for crash recovery).
     */
    private suspend fun resendMissingParts(task: TaskDocument) {
        if (task.status != TaskStatus.PENDING_WORKER) return

        val processed = task.processedParts
        val totalParts = task.totalParts
        val missingPartIds = (0 until totalParts).filter { it !in processed }
        if (missingPartIds.isEmpty()) {
            // All parts already done – mark completed just in case
            repository.updateStatus(task.requestId, TaskStatus.COMPLETED)
            return
        }

        val totalCombinations = calculateTotalCombinations(task.maxLength, task.alphabet)
        val allParts = splitIntoParts(totalCombinations, totalParts)

        missingPartIds.forEach { partId ->
            val (start, size) = allParts[partId]
            val taskMessage = TaskMessage(
                requestId = task.requestId,
                partId = partId,
                hash = task.hash,
                alphabet = task.alphabet,
                maxLength = task.maxLength,
                startIndex = start.toLong(),
                rangeSize = size.toLong()
            )
            rabbitMq.sendTask(taskMessage)
        }
        println("Recovery: re-published ${missingPartIds.size} missing parts for request ${task.requestId}")
    }

    private suspend fun handleWorkerResponse(result: ResultMessage) {
        val processed = repository.tryProcessPart(result.requestId, result.partId, result.foundWord)
        if (result.foundWord.isNotEmpty()) {
            println("Received result for ${result.requestId} part ${result.partId}: ${result.foundWord}")
        }
        if (!processed) {
            println("Ignored duplicate response for ${result.requestId} part ${result.partId}")
        }
    }

    private fun calculateTotalCombinations(maxLength: Int, alphabet: String): BigInteger {
        val alphabetSize = alphabet.length.toBigInteger()
        return (1..maxLength).fold(BigInteger.ZERO) { sum, length ->
            sum + alphabetSize.pow(length)
        }
    }

    private fun splitIntoParts(total: BigInteger, partsCount: Int): List<Pair<BigInteger, BigInteger>> {
        val rangePerWorker = total / partsCount.toBigInteger()
        return (0 until partsCount).map { i ->
            val start = i.toBigInteger() * rangePerWorker
            val size = if (i == partsCount - 1) total - start else rangePerWorker
            Pair(start, size)
        }
    }
}