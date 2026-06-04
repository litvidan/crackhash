package com.litvidan.worker

import com.litvidan.common.RabbitMqService
import com.litvidan.common.ResultMessage
import kotlinx.coroutines.runBlocking
import java.math.BigInteger

fun main() {
    val config = WorkerConfig.current
    val rabbitMq = RabbitMqService(config.rabbitmqHost)
    val cracker = HashCracker(config.alphabet)

    println("Worker started. Listening for tasks on RabbitMQ...")

    // Subscribe to the task queue with manual confirmation
    rabbitMq.startTaskConsumer { task, deliveryTag ->
        println("Received task: requestId=${task.requestId}, range=[${task.startIndex}, ${task.startIndex + task.rangeSize})")

        val foundWord = cracker.crack(
            targetHash = task.hash,
            maxLength = task.maxLength,
            startIndex = BigInteger.valueOf(task.startIndex),
            rangeSize = BigInteger.valueOf(task.rangeSize)
        )

        // Sending the result back to the response queue
        rabbitMq.sendResult(ResultMessage(task.requestId, task.partId, foundWord))

        if (foundWord.isNotEmpty()) {
            println("Found word '$foundWord' for request ${task.requestId}")
        } else {
            println("No match in assigned range for request ${task.requestId}")
        }
        // Acknowledgement is sent automatically inside the startTaskConsumer upon success
    }

    // Blocking the main thread to prevent the app to terminate
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            rabbitMq.close()
        }
    })

    while (true) {
        Thread.sleep(1000)
    }
}