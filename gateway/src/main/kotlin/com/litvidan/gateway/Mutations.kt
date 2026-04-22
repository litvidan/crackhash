package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Mutation
import com.litvidan.common.RabbitMqService
import com.litvidan.common.TaskMessage
import kotlinx.coroutines.*
import java.math.BigInteger
import java.util.*

class CrackHashMutation : Mutation {
    private val repository = TaskRepository()
    private val rabbitMq = RabbitMqService(GatewayConfig.current.rabbitmqHost)
    private val config = GatewayConfig.current

    // Background scope for restoring tasks on restart
    private val recoveryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Launching the listener for responses from the workers
        rabbitMq.startResultConsumer { result ->
            handleWorkerResponse(result)
        }

        // Restoring tasks that were in operation before the gateway crash
        recoveryScope.launch {
            delay(5000) // даём время на подключение к RabbitMQ
            val pendingTasks = repository.findAllByStatus(TaskStatus.PENDING_WORKER)
            pendingTasks.forEach { task ->
                publishTaskParts(task)
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
            status = TaskStatus.PENDING_QUEUE
        )

        runBlocking {
            // Save the task in MongoDB with a guarantee of replication
            repository.save(taskDoc)

            // Split and publish the parts in the queue
            publishTaskParts(taskDoc)

            // Update status to PENDING_WORKER
            repository.updateStatus(requestId, TaskStatus.PENDING_WORKER)
        }

        return requestId
    }

    private suspend fun publishTaskParts(task: TaskDocument) {
        val totalCombinations = calculateTotalCombinations(task.maxLength, task.alphabet)
        val parts = splitIntoParts(totalCombinations, config.taskPartitionCount)

        parts.forEach { (start, size) ->
            val taskMessage = TaskMessage(
                requestId = task.requestId,
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

    private suspend fun handleWorkerResponse(result: com.litvidan.common.ResultMessage) {
        val requestId = result.requestId
        if (result.foundWord.isNotEmpty()) {
            println("Received result for $requestId: ${result.foundWord}")
            repository.addFoundWord(requestId, result.foundWord)
            repository.updateStatus(requestId, TaskStatus.COMPLETED)
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